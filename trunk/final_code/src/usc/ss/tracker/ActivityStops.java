package usc.ss.tracker;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityStops extends ListActivity implements OnInitListener
{
	protected static final String TAG = "ACTIVITY_STOPS";
	protected static final String STRING_RESPONSE = "string_response";
	protected static final String STRING_BUS_NUMBER = "string_bus_number";

	protected static final String URL_LOCATION = "http://maps.googleapis.com/maps/api/geocode/json?sensor=true&address=";

	private static final int MY_DATA_CHECK_CODE = 0x1;

	private TextToSpeech mTts;

	private int status;

	private boolean isAssistanceNeeded;

	private SharedPreferences sharedPreferences;

	private boolean hasSent;
	
	private int busNumber;

	private ArrayList<String> listStops;
	private ArrayList<Double> listLatitude;
	private ArrayList<Double> listLongitude;

	private int isBusFound;

	private ProgressBar pbEmpty;
	private TextView tvEmpty;

	private ArrayList<String> mListDestinations;
	private ArrayAdapter<String> adapter;

	private ListView lvDestinations;

	private int destinationStop;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set layout content
		setContentView(R.layout.activity_stops);

		// Get shared preferences
		sharedPreferences = getSharedPreferences(
				getResources().getString(R.string.preferences_key_bustrackr),
				Activity.MODE_PRIVATE);

		// Check if assistance is needed
		isAssistanceNeeded = sharedPreferences.getBoolean(getResources()
				.getString(R.string.preference_assistance), true);

		// Initialize Speech
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

		pbEmpty = (ProgressBar) findViewById(R.id.progressbar_empty_stops);
		tvEmpty = (TextView) findViewById(R.id.textview_stops_empty);

		mListDestinations = new ArrayList<String>();

		lvDestinations = (ListView) getListView();
		adapter = new ArrayAdapter<String>(ActivityStops.this,
				android.R.layout.simple_list_item_1, mListDestinations);
		lvDestinations.setAdapter(adapter);

		listStops = new ArrayList<String>();
		listLatitude = new ArrayList<Double>();
		listLongitude = new ArrayList<Double>();

		busNumber = 0;
		
		destinationStop = -1;

		hasSent = false;

		isBusFound = -1;

		// Initialize status of speak text
		status = -1;

		lvDestinations.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3)
			{
				if (isAssistanceNeeded)
				{
					if (destinationStop == -1)
					{
						speakText("Please choose your destination");
						return;
					} else
					{
						if (!hasSent)
						{
							hasSent = true;

							if (isAssistanceNeeded)
							{
								speakText("Loading Destination");
							} else
							{
								Toast.makeText(ActivityStops.this,
										"Loading Destination",
										Toast.LENGTH_SHORT);
							}

							loadDestination(destinationStop);
						}
					}
				} else
				{
					loadDestination(arg2);
				}
			}
		});

		// Parse the data
		if (getIntent().getExtras() != null)
		{
			busNumber = getIntent().getExtras().getInt(STRING_BUS_NUMBER);
			parseResponse(getIntent().getExtras().getString(STRING_RESPONSE));
		}
	}

	@Override
	public void onDestroy()
	{
		mTts.shutdown();

		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (isAssistanceNeeded)
		{
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			{
				destinationStop = destinationStop - 1;

				if (destinationStop < 0)
				{
					destinationStop = 0;
					speakText("No destinations beyond. Starting of Route. Currently on "
							+ listStops.get(destinationStop).toLowerCase());
				} else
				{
					speakText(listStops.get(destinationStop).toLowerCase());
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
			{
				destinationStop = destinationStop + 1;

				if (destinationStop >= listStops.size())
				{
					destinationStop = listStops.size() - 1;
					speakText("No destinations beyond. End of Route. Currently on "
							+ listStops.get(destinationStop).toLowerCase());
				} else
				{
					speakText(listStops.get(destinationStop).toLowerCase());
				}
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	public void loadDestination(int number)
	{
		Intent intent = new Intent(ActivityStops.this, ActivityMaps.class);
		intent.putExtra(ActivityMaps.STRING_BUS_NUMBER, busNumber);
		intent.putExtra(ActivityMaps.STRING_DESTINATION, number);
		intent.putExtra(ActivityMaps.STRING_STOPS, listStops);
		intent.putExtra(ActivityMaps.STRING_LATITUDES, listLatitude);
		intent.putExtra(ActivityMaps.STRING_LONGITUDES, listLongitude);
		startActivity(intent);
	}

	private void parseResponse(String response)
	{
		// Parse the response
		try
		{
			if (response.compareTo("Error: Bus not found") == 0)
			{
				isBusFound = 0;

				if (isAssistanceNeeded)
				{
					speakText("Bus number " + busNumber + " not found");
				} else
				{
					Toast.makeText(ActivityStops.this, "Bus number " + busNumber + " not found",
							Toast.LENGTH_SHORT).show();
				}

				pbEmpty.setVisibility(View.GONE);
				tvEmpty.setText("Bus number " + busNumber + " not found");

				return;
			}

			isBusFound = 1;

			JSONObject mMainObject;
			JSONObject mStops;
			JSONObject mArrayItem;
			JSONArray mArray;
			String stop;
			double latitude;
			double longitude;

			mMainObject = new JSONObject(response);

			if (mMainObject.has("stops"))
			{
				mArray = mMainObject.getJSONArray("stops");

				for (int i = 0; i < mArray.length(); i++)
				{
					mArrayItem = mArray.getJSONObject(i);

					if (mArrayItem.has("stop"))
					{
						mStops = mArrayItem.getJSONObject("stop");

						stop = "";
						latitude = 0.0f;
						longitude = 0.0f;

						if (mStops.has("name"))
						{
							stop = mStops.getString("name");
						}

						if (mStops.has("name"))
						{
							latitude = mStops.getDouble("latitude");
						}

						if (mStops.has("name"))
						{
							longitude = mStops.getDouble("longitude");
						}

						stop = stop.replace("/", " and ");
						
						listStops.add(stop);
						listLatitude.add(latitude);
						listLongitude.add(longitude);

						adapter.add(stop);

						adapter.notifyDataSetChanged();
					}
				}
			} else
			{
				Log.e(TAG, "No stops found !!!");
			}
		} catch (JSONException e)
		{
			Log.e(TAG, "Error parsing JSON : " + e.getMessage());
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == MY_DATA_CHECK_CODE)
		{
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				// Create new TextToSpeech instance
				mTts = new TextToSpeech(this, this);

				if (isAssistanceNeeded)
				{
					speakText("Please select a destination");
				}
				else
				{
					Toast.makeText(ActivityStops.this, "Please select a destination", Toast.LENGTH_SHORT).show();
				}
			} else
			{
				// Missing Data, Request to install package
				Intent installIntent = new Intent();
				installIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}

	private void speakText(String text)
	{
		if (ActivityStops.this.status == 0)
		{
			mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onInit(int status)
	{
		ActivityStops.this.status = status;

		if (isBusFound == 1)
		{
			if (isAssistanceNeeded)
			{
			speakText("Please select a destination");
			}
			else
			{
				Toast.makeText(ActivityStops.this, "Please select a destination", Toast.LENGTH_SHORT).show();
			}
		} else if (isBusFound == 0)
		{
			speakText("Bus number " + busNumber + " not found");
		}
	}
}
