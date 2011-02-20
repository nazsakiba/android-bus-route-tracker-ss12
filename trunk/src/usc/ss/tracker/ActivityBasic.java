package usc.ss.tracker;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.IBinder;
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
import android.widget.Toast;

public class ActivityBasic extends Activity implements OnInitListener,
		OnGesturePerformedListener
{
	protected static final String TAG = "ACTIVITY_INPUT";

	private ServiceTracker mService;

	private static final int MY_DATA_CHECK_CODE = 0x0;

	private TextToSpeech mTts;

	private GestureOverlayView mGestures;

	private GestureLibrary mLibrary;

	private int status;

	private Button button;

	private int busCount;

	private int busNumber;

	private boolean isLongClick;

	/** Called when the activity is first created. */
	@Override
	public void setContentView(int layout_id)
	{
		// Set layout content
		super.setContentView(layout_id);

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

		mGestures = (GestureOverlayView) findViewById(R.id.gestures);
		mGestures.addOnGesturePerformedListener(this);

		// Start the service
		bindToService();

		// Get the button
		button = (Button) findViewById(R.id.button_input);

		busCount = 0;

		busNumber = 0;

		isLongClick = false;

		// Initialize input
		initializeClickEvents();
	}

	public void initializeClickEvents()
	{
		button.setOnClickListener(new OnClickListener()
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
						button.setText("Bus Number : " + busNumber);
					} else if (busCount == 11)
					{
						speakText("Reset Bus Number");
						button.setText("Bus Number : " + busNumber
								+ "\nDigit Count" + (busCount - 1));
					} else
					{
						speakText(String.valueOf(busCount));
						button.setText("Bus Number : " + busNumber
								+ "\nDigit Count : " + busCount);
					}
				} else
				{
					isLongClick = false;
				}

				button.setText("Bus Number : " + busNumber + "\nCounter : "
						+ busCount);
			}
		});

		button.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				if (busCount == 10)
				{
					speakText("Entering Bus Number " + busNumber);
					
					// Go to next activity
					
				} else if (busCount == 11)
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

	private void bindToService()
	{
		// Bind to the service to get sensor values
		new Thread()
		{
			@Override
			public void run()
			{
				bindService(
						new Intent(ActivityBasic.this, ServiceTracker.class),
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

	@Override
	public void onDestroy()
	{
		unbindFromService();

		mTts.shutdown();

		super.onDestroy();
	}

	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			Log.i(TAG, "Service bounded");
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
		if (ActivityBasic.this.status == 0)
		{
			mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onInit(int status)
	{
		ActivityBasic.this.status = status;
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
				// Show the spell
				Toast.makeText(this, prediction.name, Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

}