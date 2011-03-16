package usc.ss.tracker;

import java.util.ArrayList;
import java.util.Locale;

import usc.ss.tracker.Utility.WebListener;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityInput extends Activity implements OnInitListener,
		OnGesturePerformedListener
{
	protected static final String TAG = "ACTIVITY_INPUT";

	protected static final String URL_STOPS = "http://owstarr.com/bustrackr/stops.php?apikey=1123581317&busnumber=";

	private static final int MY_DATA_CHECK_CODE = 0x0;

	private static final int NUMBER_ROWS = 4;
	private static final int NUMBER_COLUMNS = 3;

	private TextToSpeech mTts;

	private GestureOverlayView mGestures;

	private GestureLibrary mLibrary;

	private int status;
	private int busCount;
	private int selectedInputType;
	private int busNumber;
	private boolean isLongClick;

	private Rect gridRect;
	private int tileHeight;
	private int tileWidth;
	private int rowNumber;
	private int columnNumber;

	private int previousBusCount;

	// Layouts for normal input
	private RelativeLayout rlNormalInput;
	private ImageButton ibNormalEnter;
	private EditText etNormalBusNumber;

	// Tap button for normal input
	private Button buttonTap;

	// Gesture view for gesture input
	private TextView tvGestures;

	// ImageButton for grid input
	private ImageButton ibGridInput;

	boolean isAssistanceNeeded;

	private SharedPreferences sharedPreferences;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// Set layout content
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_input);

		// Get shared preferences
		sharedPreferences = getSharedPreferences(
				getResources().getString(R.string.preferences_key_bustrackr),
				Activity.MODE_PRIVATE);

		// Initialize if not already initialized
		if (!sharedPreferences.contains(getResources().getString(
				R.string.preference_assistance)))
		{
			Utility.saveMyPreference(getApplicationContext(), getResources()
					.getString(R.string.preference_assistance), true);
			Utility.saveMyPreference(getApplicationContext(), getResources()
					.getString(R.string.preference_input_type), getResources()
					.getString(R.string.default_input_type));
		}

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

		// Get the gesture view
		mGestures = (GestureOverlayView) findViewById(R.id.gestureview_input);
		mGestures.addOnGesturePerformedListener(this);

		// Initialize rectangle for grid
		gridRect = new Rect();

		// Get the button
		buttonTap = (Button) findViewById(R.id.button_input_tap);

		// Get the gesture text view
		tvGestures = (TextView) findViewById(R.id.textview_gestures);

		// Get the grid view
		ibGridInput = (ImageButton) findViewById(R.id.imagebutton_input_grid);

		// Get relative layout for normal input
		rlNormalInput = (RelativeLayout) findViewById(R.id.relativelayout_input_normal);
		etNormalBusNumber = (EditText) findViewById(R.id.edittext_input_normal_bus);
		ibNormalEnter = (ImageButton) findViewById(R.id.imagebutton_input_normal_bus);

		etNormalBusNumber.setInputType(InputType.TYPE_CLASS_PHONE);

		busCount = 0;

		busNumber = 0;

		isLongClick = false;

		// Initialize input
		initializeClickEvents();

		// Initialize View
		alterView(selectedInputType);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// Check if assistance is needed
		isAssistanceNeeded = sharedPreferences.getBoolean(getResources()
				.getString(R.string.preference_assistance), true);

		if (isAssistanceNeeded)
		{
			// If yes then get input type
			String[] array = getResources().getStringArray(R.array.input_type);

			for (int i = 0; i < array.length; i++)
			{
				if (array[i].compareTo(sharedPreferences.getString(
						getResources()
								.getString(R.string.preference_input_type),
						getResources().getString(R.string.default_input_type))) == 0)
				{
					selectedInputType = i;

					break;
				}
			}
		} else
		{
			selectedInputType = Constants.INPUT_TYPE_NORMAL;
		}

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
			case R.id.menu_settings:

				Intent intent = new Intent(ActivityInput.this,
						ActivitySettings.class);
				startActivity(intent);

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void initializeClickEvents()
	{
		etNormalBusNumber.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					if (keyCode == KeyEvent.KEYCODE_ENTER)
					{
						try
						{
							busNumber = Integer.parseInt(etNormalBusNumber
									.getEditableText().toString());
						} catch (Exception ex)
						{
							Toast.makeText(ActivityInput.this,
									"Please enter valid number.",
									Toast.LENGTH_SHORT).show();
							return true;
						}

						fetchBusRoutes();

						return true;
					}
				}

				return false;
			}
		});

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
						speakText("Select Bus Number " + busNumber);
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

				buttonTap.setText(getResources().getString(R.string.bus_number)
						+ " " + busNumber + "\n"
						+ getResources().getString(R.string.count) + " "
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
					// speakText("Entering Bus Number " + busNumber);

					// Fetch Bus Routes
					fetchBusRoutes();
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

		ibNormalEnter.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try
				{
					busNumber = Integer.parseInt(etNormalBusNumber
							.getEditableText().toString());
				} catch (Exception ex)
				{
					Toast.makeText(ActivityInput.this,
							"Please enter valid number.", Toast.LENGTH_SHORT)
							.show();
					return;
				}

				fetchBusRoutes();
			}
		});

		ibGridInput.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if ((event.getAction() == MotionEvent.ACTION_MOVE)
						|| (event.getAction() == MotionEvent.ACTION_DOWN))
				{
					if (tileHeight == 0)
					{
						ibGridInput.getDrawingRect(gridRect);

						tileHeight = gridRect.height() / NUMBER_ROWS;
						tileWidth = gridRect.width() / NUMBER_COLUMNS;
					} else
					{
						rowNumber = (int) ((event.getY() - gridRect.top) / tileHeight);
						columnNumber = (int) ((event.getX() - gridRect.left) / tileWidth);

						busCount = rowNumber * NUMBER_COLUMNS + columnNumber
								+ 1;

						if (busCount != previousBusCount)
						{
							previousBusCount = busCount;

							if (busCount < 10)
							{
								speakText(String.valueOf(previousBusCount));
							} else if (busCount == 10)
							{
								speakText("delete bus number " + busNumber);
							} else if (busCount == 11)
							{
								speakText("0");
							} else if (busCount == 12)
							{
								speakText("select bus number " + busNumber);
							}
						}
					}
				}

				if (event.getAction() == MotionEvent.ACTION_UP)
				{
					if (busCount == 12)
					{
						// speakText("Entering Bus Number " + busNumber);

						// Fetch Bus Routes
						fetchBusRoutes();
					} else if (busCount == 10)
					{
						speakText("Bus Number has been deleted. Please select bus number");
						busNumber = 0;
						busCount = -1;
					} else
					{
						busNumber *= 10;

						if (busCount != 11)
						{
							busNumber += busCount;
						}

						busCount = -1;
						previousBusCount = -1;

						speakText("Bus Number is " + String.valueOf(busNumber));
					}
				}

				return false;
			}
		});
	}

	private void alterView(int type)
	{
		busCount = 0;
		busNumber = 0;

		switch (type)
		{
			case Constants.INPUT_TYPE_NORMAL:
				// Alter layouts
				buttonTap.setVisibility(View.GONE);
				mGestures.setVisibility(View.GONE);
				ibGridInput.setVisibility(View.GONE);
				rlNormalInput.setVisibility(View.VISIBLE);

				// Set text
				etNormalBusNumber.setText("");

				return;

			case Constants.INPUT_TYPE_TAP:
				// Alter layouts
				mGestures.setVisibility(View.GONE);
				rlNormalInput.setVisibility(View.GONE);
				ibGridInput.setVisibility(View.GONE);
				buttonTap.setVisibility(View.VISIBLE);

				// Set text
				buttonTap.setText(getResources().getString(R.string.bus_number)
						+ " " + busNumber + "\n"
						+ getResources().getString(R.string.count) + " "
						+ busCount);
				return;

			case Constants.INPUT_TYPE_GRID:
				// Alter layouts
				mGestures.setVisibility(View.GONE);
				rlNormalInput.setVisibility(View.GONE);
				buttonTap.setVisibility(View.GONE);
				ibGridInput.setVisibility(View.VISIBLE);

				ibGridInput.getDrawingRect(gridRect);

				tileHeight = gridRect.height() / NUMBER_ROWS;
				tileWidth = gridRect.width() / NUMBER_COLUMNS;

				previousBusCount = -1;

				// Set text
				return;

			case Constants.INPUT_TYPE_GESTURES:
				// Alter layouts
				buttonTap.setVisibility(View.GONE);
				mGestures.setVisibility(View.VISIBLE);
				ibGridInput.setVisibility(View.GONE);
				rlNormalInput.setVisibility(View.GONE);

				// Set text
				tvGestures.setText(getResources()
						.getString(R.string.bus_number) + " " + busNumber);
				return;

			default:
				return;

		}
	}

	private void fetchBusRoutes()
	{
		if (busNumber == 0)
		{
			String text = "Bus number can not be 0";
			speakText(text);
			Toast.makeText(ActivityInput.this, text, Toast.LENGTH_SHORT).show();
			return;
		}

		String url = URL_STOPS + busNumber;

		try
		{
			Utility.openURL(url, new WebListener()
			{
				@Override
				public void onComplete(String response)
				{
					Log.i(TAG, "Response : " + response);

					Intent intent = new Intent(ActivityInput.this,
							ActivityStops.class);
					intent.putExtra(ActivityStops.STRING_RESPONSE, response);
					intent.putExtra(ActivityStops.STRING_BUS_NUMBER, busNumber);
					startActivity(intent);

					// ActivityInput.this.finish();
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
		Locale locale = Locale.getDefault();

		if (ActivityInput.this.status == 0)
		{
			mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onInit(int status)
	{
		ActivityInput.this.status = status;

		if (isAssistanceNeeded)
		{
			speakText("Please enter bus number");
		} else
		{
			Toast.makeText(ActivityInput.this, "Please select a destination",
					Toast.LENGTH_SHORT).show();
		}
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
					// speakText("Entering Bus Number " + busNumber);

					// Fetch Bus Routes
					fetchBusRoutes();
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

				tvGestures.setText(getResources()
						.getString(R.string.bus_number) + " " + busNumber);
			}
		}
	}

}