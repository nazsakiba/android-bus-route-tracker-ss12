package usc.ss.tracker;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import usc.ss.tracker.ServiceTracker.ServiceSensorListener;
import usc.ss.tracker.Utility.WebListener;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class ActivityStops extends Activity implements OnInitListener
{
	protected static final String TAG = "ACTIVITY_STOPS";
	protected static final String STRING_RESPONSE = "string_response";

	protected static final String URL_LOCATION = "http://maps.googleapis.com/maps/api/geocode/json?sensor=true&address=";

	private ServiceTracker mService;

	private static final int MY_DATA_CHECK_CODE = 0x1;

	private TextToSpeech mTts;

	private int status;

	private boolean hasSent;

	ArrayList<Integer> listDistance;
	ArrayList<String> listStops;
	ArrayList<Integer> list_ID;

	private TextView tvStops;
	private ImageButton ibStops;
	private String stringStops;

	private int destinationStop;
	
	private Vibrator vibrator;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set layout content
		setContentView(R.layout.activity_stops);

		// Initialize Speech
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

		// Start the service
		bindToService();

		tvStops = (TextView) findViewById(R.id.textview_stops);
		ibStops = (ImageButton) findViewById(R.id.imagebutton_stops);

		listDistance = new ArrayList<Integer>();
		listStops = new ArrayList<String>();
		list_ID = new ArrayList<Integer>();

		stringStops = "";

		destinationStop = -1;

		hasSent = false;
		
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		ibStops.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
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
						
						speakText("Loading Destination");

						loadDestination(destinationStop);
					}
				}
			}
		});

		// Parse the data
		parseResponse(getIntent().getExtras().getString(STRING_RESPONSE));
	}

	@Override
	public void onDestroy()
	{
		unbindFromService();

		mTts.shutdown();

		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			destinationStop = destinationStop - 1;

			if (destinationStop < 0)
			{
				destinationStop = 0;
				speakText("No destinations beyond. Starting of Route. Currently on "
						+ listStops.get(destinationStop).replace("/", " and ")
								.toLowerCase());
			} else
			{
				speakText(listStops.get(destinationStop).replace("/", " and ")
						.toLowerCase());
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
			destinationStop = destinationStop + 1;

			if (destinationStop >= listStops.size())
			{
				destinationStop = listStops.size() - 1;
				speakText("No destinations beyond. End of Route. Currently on "
						+ listStops.get(destinationStop).replace("/", " and ")
								.toLowerCase());
			} else
			{
				speakText(listStops.get(destinationStop).replace("/", " and ")
						.toLowerCase());
			}
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	public void loadDestination(int number)
	{
		String address = listStops.get(number).replace("/", "+AND+")
				.replace(" ", "+")
				+ ",+LOS+ANGELES,+CA";
		String url = URL_LOCATION + address;

		Log.i(TAG, "URL : " + url);

		try
		{
			Utility.openURL(url, new WebListener()
			{
				@Override
				public void onComplete(String response)
				{
					Log.i(TAG, "Response : " + response);

					parseLocation(response);
				}
			});

		} catch (Exception e)
		{
			speakText("Could not connect to the internet");
		}
	}

	private void parseResponse(String response)
	{
		// Parse the response
		try
		{
			JSONObject mMainObject;
			JSONObject mMetadata;
			JSONObject mStops;
			JSONArray mArray;
			JSONObject temp;
			String splitString[];

			int recordsFound = 0;

			mMainObject = new JSONObject(response);

			if (mMainObject.has("metadata"))
			{
				mMetadata = mMainObject.getJSONObject("metadata");

				if (mMetadata.has("records_found"))
				{
					recordsFound = mMetadata.getInt("records_found");

					Log.i(TAG, "Records : " + recordsFound);

					if (recordsFound > 0)
					{
						if (mMainObject.has("stops"))
						{
							mStops = mMainObject.getJSONObject("stops");

							if (mStops.has("item"))
							{
								mArray = mStops.getJSONArray("item");

								for (int i = 0; i < mArray.length(); i++)
								{
									temp = mArray.getJSONObject(i);

									if (temp.has("id"))
									{
										list_ID.add(temp.getInt("id"));
									}

									if (temp.has("text"))
									{
										splitString = temp.getString("text")
												.split("-");

										stringStops += splitString[1] + "\n";

										listStops.add(splitString[1]);
									}

									if (temp.has("distance"))
									{
										listDistance.add(temp
												.getInt("distance"));
									}
								}
							}
						}
					}
				} else
				{
					Log.e(TAG, "No records found !!!");
				}
			} else
			{
				Log.e(TAG, "No metadata !!!");
			}
		} catch (JSONException e)
		{
			Log.e(TAG, "Error parsing JSON : " + e.getMessage());
		}

		tvStops.setText(stringStops);
	}

	private void parseLocation(String response)
	{
		try
		{
			JSONObject mMainObject;
			JSONArray arrayResults;
			JSONObject mGeometry;
			JSONObject mLocation;

			float latitude = 0.0f;
			float longitude = 0.0f;

			mMainObject = new JSONObject(response);

			if (mMainObject.has("results"))
			{
				arrayResults = mMainObject.getJSONArray("results");

				if (arrayResults.length() > 0)
				{
					if (((JSONObject) arrayResults.get(0)).has("geometry"))
					{
						mGeometry = ((JSONObject) arrayResults.get(0))
								.getJSONObject("geometry");

						if (mGeometry.has("location"))
						{
							mLocation = mGeometry.getJSONObject("location");

							if (mLocation.has("lat"))
							{
								latitude = (float) mLocation.getDouble("lat");
							}

							if (mLocation.has("lng"))
							{
								longitude = (float) mLocation.getDouble("lng");
							}

							Log.i(TAG, "Latitude : " + latitude
									+ ", Longitude : " + longitude);

							mService.setDestinationAndStart(latitude, longitude);
						}
					}
				}
			}
		} catch (JSONException e)
		{
			Log.e(TAG, "Error parsing JSON : " + e.getMessage());
		}
	}

	private void bindToService()
	{
		// Bind to the service to get sensor values
		new Thread()
		{
			@Override
			public void run()
			{
				bindService(
						new Intent(ActivityStops.this, ServiceTracker.class),
						mConnection, Context.BIND_AUTO_CREATE);
			}
		}.start();
	}

	private void unbindFromService()
	{
		// Check if service is not null
		if (mService != null)
		{
			mService.removeOnServiceSensorListener();
		}

		// Check if connection is not null
		if (mConnection != null)
		{
			// Unbind the service
			unbindService(mConnection);
		}

		mConnection = null;
	}

	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			Log.i(TAG, "Service bounded");

			mService = ((ServiceTracker.LocalBinder) service).getService();

			mService.setOnServiceSensorListener(new ServiceSensorListener()
			{
				@Override
				public void onLocationUpdate(float distance)
				{
					if (distance < 2.0f)
					{
						vibrator.vibrate(500);
					}
					else if (distance < 1.0f)
					{
						vibrator.vibrate(1000);
					}
					else if (distance < 0.5f)
					{
						vibrator.vibrate(2000);
					}
					else if (distance < 0.1f)
					{
						vibrator.vibrate(4000);
					}
					
					speakText("Destination is " + distance + "miles ahead");
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			mService = null;

			Log.e(TAG, "Service crashed unexpectedly.");
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == MY_DATA_CHECK_CODE)
		{
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				// Create new TextToSpeech instance
				mTts = new TextToSpeech(this, this);

				speakText("Please select starting bus stop.");
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
	}

}
