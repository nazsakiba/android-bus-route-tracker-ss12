package usc.ss.tracker;

import java.util.ArrayList;

import usc.ss.tracker.Utility.WebListener;
import android.app.Activity;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityInput extends Activity implements OnInitListener,
		OnGesturePerformedListener
{
	protected static final int TYPE_TAP = 0x0;
	protected static final int TYPE_GESTURES = 0x1;

	protected static final String TAG = "ACTIVITY_INPUT";

	protected static final String URL_STOPS = "http://developer.metro.net/tm/stops.php?apikey=LQ1~*)7J,oGw-S2qdXF!%26%26ua+%3E&format=json&route=";

	private static final int MY_DATA_CHECK_CODE = 0x0;

	private TextToSpeech mTts;

	private GestureOverlayView mGestures;

	private GestureLibrary mLibrary;

	private int status;
	private int busCount;
	private int selectedInputType;
	private int busNumber;
	private boolean isLongClick;

	private Button buttonTap;
	private TextView tvGestures;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// Set layout content
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_input);

		// Initialize Speech
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

		// Initialize Gestures
		mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);

		if (!mLibrary.load())
		{
			Log.e(TAG, "Unable to load gestures");
		}

		mGestures = (GestureOverlayView) findViewById(R.id.gestures_view);
		mGestures.addOnGesturePerformedListener(this);

		// Get the button
		buttonTap = (Button) findViewById(R.id.button_input);

		// Get the gesture text view
		tvGestures = (TextView) findViewById(R.id.textview_gestures);

		busCount = 0;

		busNumber = 0;

		isLongClick = false;

		// Initialize with tap view
		selectedInputType = TYPE_TAP;
		
		// Initialize input
		initializeClickEvents();

		// Initialize View
		alterView(selectedInputType);
	}

	@Override
	public void onDestroy()
	{
		mTts.shutdown();
		
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_input, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.menu_switch_tap:

				selectedInputType = TYPE_TAP;
				alterView(selectedInputType);

				return true;
			case R.id.menu_switch_gesture:

				selectedInputType = TYPE_GESTURES;
				alterView(selectedInputType);

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void initializeClickEvents()
	{
		buttonTap.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (isLongClick != true)
				{
					busCount++;

					if (busCount > 11)
					{
						busCount = 0;
					}

					if (busCount == 10)
					{
						speakText("Selected Bus Number " + busNumber);
					} else if (busCount == 11)
					{
						speakText("Reset Bus Number");
					} else
					{
						speakText(String.valueOf(busCount));
					}
				} else
				{
					isLongClick = false;
				}

				buttonTap.setText("Bus Number : " + busNumber + "\nCounter : "
						+ busCount);
			}
		});

		buttonTap.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				if (busCount == 10)
				{
					speakText("Entering Bus Number " + busNumber);

					// Fetch Bus Routes
					fetchBusRoutes();
				} 
				else if (busCount == 11)
				{
					speakText("Bus Number has been deleted. Please select bus number");
					busNumber = 0;
					busCount = 0;
				} else
				{
					busNumber *= 10;
					busNumber += busCount;

					busCount = 0;

					speakText("Bus Number is " + String.valueOf(busNumber));
				}

				isLongClick = true;

				return false;
			}
		});
	}

	private void alterView(int type)
	{
		switch (type)
		{
			case TYPE_TAP:
				buttonTap.setText("Bus Number : " + busNumber + "\nCounter : " + busCount);
				buttonTap.setVisibility(View.VISIBLE);
				mGestures.setVisibility(View.GONE);
				return;

			case TYPE_GESTURES:
				buttonTap.setVisibility(View.GONE);
				mGestures.setVisibility(View.VISIBLE);
				tvGestures.setText("Bus Number : " + busNumber);
				return;

			default:
				return;

		}
	}

	private void fetchBusRoutes()
	{
		String url = URL_STOPS + busNumber;

		try
		{
			Utility.openURL(url, new WebListener()
			{
				@Override
				public void onComplete(String response)
				{
					Log.i(TAG, "Response : " + response);

					Intent intent = new Intent(ActivityInput.this, ActivityStops.class);
					intent.putExtra(ActivityStops.STRING_RESPONSE, response);
					startActivity(intent);
					
					ActivityInput.this.finish();
				}
			});

		} catch (Exception e)
		{
			speakText("Could not connect to the internet");
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
		if (ActivityInput.this.status == 0)
		{
			mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onInit(int status)
	{
		ActivityInput.this.status = status;
	}

	@Override
	public void onGesturePerformed(GestureOverlayView gView, Gesture gesture)
	{
		ArrayList<Prediction> predictions = mLibrary.recognize(gesture);

		// We want at least one prediction
		if (predictions.size() > 0)
		{
			Prediction prediction = predictions.get(0);

			// We want at least some confidence in the result
			if (prediction.score > 1.0)
			{
				Toast.makeText(ActivityInput.this,
						"Number : " + prediction.name, Toast.LENGTH_SHORT)
						.show();

				busCount = Integer.parseInt(prediction.name);

				// Show the spell
				if (busCount == 10)
				{
					speakText("Entering Bus Number " + busNumber);	
					
					// Fetch Bus Routes
					fetchBusRoutes();
				} 
				else if (busCount == 11)
				{
					speakText("Bus Number has been deleted. Please select bus number");
					busNumber = 0;
					busCount = 0;
				} else
				{
					busNumber *= 10;
					busNumber += busCount;

					busCount = 0;

					speakText("Bus Number is " + String.valueOf(busNumber));
				}

				tvGestures.setText("Bus Number : " + busNumber);
			}
		}
	}

}