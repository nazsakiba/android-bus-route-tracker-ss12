package usc.ss.tracker;

import java.util.ArrayList;
import java.util.List;

import usc.ss.tracker.ServiceTracker.ServiceSensorListener;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class ActivityMaps extends MapActivity implements OnInitListener
{
	protected static final String TAG = "ACTIVITY_MAPS";

	protected static final String STRING_DESTINATION = "string_destination";
	protected static final String STRING_BUS_NUMBER = "string_bus_number";
	protected static final String STRING_STOPS = "string_stops";
	protected static final String STRING_LATITUDES = "string_latitudes";
	protected static final String STRING_LONGITUDES = "string_longitudes";

	private static final int MY_DATA_CHECK_CODE = 0x1;

	private MapView mapView;
	private TextView tvDisplay;

	private ServiceTracker mService;

	private Vibrator vibrator;

	private TextToSpeech mTts;

	private int status;

	private boolean isAssistanceNeeded;

	private boolean isFirstTime;

	private int destination;
	private int busNumber;

	private ArrayList<String> listStops;
	private ArrayList<Double> listLatitude;
	private ArrayList<Double> listLongitude;

	private SharedPreferences sharedPreferences;

	List<Overlay> mapOverlays;

	MapItemizedOverlay itemizedOverlayCurrentLocation;

	OverlayItem overlayItemCurrent;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

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

		tvDisplay = (TextView) findViewById(R.id.textview_display);
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		isFirstTime = true;

		// Start the service
		bindToService();

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		if (getIntent().getExtras() != null)
		{
			destination = getIntent().getExtras().getInt(STRING_DESTINATION);
			busNumber = getIntent().getExtras().getInt(STRING_BUS_NUMBER);
			listStops = (ArrayList<String>) getIntent().getExtras()
					.getSerializable(STRING_STOPS);
			listLatitude = (ArrayList<Double>) getIntent().getExtras()
					.getSerializable(STRING_LATITUDES);
			listLongitude = (ArrayList<Double>) getIntent().getExtras()
					.getSerializable(STRING_LONGITUDES);
		}

		mapOverlays = mapView.getOverlays();

		MapItemizedOverlay itemizedOverlayStops = new MapItemizedOverlay(this
				.getResources().getDrawable(R.drawable.maps_stop),
				ActivityMaps.this);
		MapItemizedOverlay itemizedOverlayDestination = new MapItemizedOverlay(
				this.getResources().getDrawable(R.drawable.maps_destination),
				ActivityMaps.this);
		itemizedOverlayCurrentLocation = new MapItemizedOverlay(this
				.getResources().getDrawable(R.drawable.bus_map_icon),
				ActivityMaps.this);

		OverlayItem overlayitem;

		for (int i = 0; i < listLatitude.size(); i++)
		{
			GeoPoint point = new GeoPoint(
					(int) (listLatitude.get(i) * 1000000),
					(int) (listLongitude.get(i) * 1000000));

			overlayitem = new OverlayItem(point, "Bus # " + busNumber,
					listStops.get(i));

			if (i == destination)
			{
				itemizedOverlayDestination.addOverlay(overlayitem);
			} else
			{
				itemizedOverlayStops.addOverlay(overlayitem);
			}
		}

		mapOverlays.add(itemizedOverlayDestination);
		mapOverlays.add(itemizedOverlayStops);
	}

	@Override
	public void onDestroy()
	{
		unbindFromService();

		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
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
						new Intent(ActivityMaps.this, ServiceTracker.class),
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

			mService.setDestinationAndStart(listStops, listLatitude,
					listLongitude, destination);

			mService.setOnServiceSensorListener(new ServiceSensorListener()
			{
				@Override
				public void onStopReached(int location,
						float distanceFromDestination)
				{
					int destinationDistance = (int) (distanceFromDestination * 100);

					if (distanceFromDestination < 0.1f)
					{
						vibrator.vibrate(4000);
						speakText("Destination is "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles ahead");
						
						tvDisplay.setText("Bus # " + busNumber + "\n Distance from destination : "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles");
					} 
					else if (distanceFromDestination < 0.5f)
					{
						vibrator.vibrate(2000);
						speakText("Destination is "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles ahead");
						
						tvDisplay.setText("Bus # " + busNumber + "\n Distance from destination : "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles");
					} 
					else if (distanceFromDestination < 1.0f)
					{
						vibrator.vibrate(1000);
						speakText("Destination is "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles ahead");
						
						tvDisplay.setText("Bus # " + busNumber + "\n Distance from destination : "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles");
					} 
					else if (distanceFromDestination < 2.0f)
					{
						vibrator.vibrate(500);
						speakText("Destination is "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles ahead");
						
						tvDisplay.setText("Bus # " + busNumber + "\n Distance from destination : "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles");
					} else
					{
						// Break before and after decimals
						speakText("Next stop is " + listStops.get(location)
								+ ". Destination is "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles ahead");
						
						tvDisplay.setText("Bus # " + busNumber + "\n Distance from destination : "
								+ (destinationDistance / 100) + "."
								+ (destinationDistance % 100) + " miles");
					}
				}

				@Override
				public void onLocationUpdate(float latitude, float longitude)
				{
					// Remove previous location
					mapOverlays.remove(itemizedOverlayCurrentLocation);
					itemizedOverlayCurrentLocation.remove(overlayItemCurrent);

					// Update maps here
					GeoPoint point = new GeoPoint((int) (latitude * 1000000),
							(int) (longitude * 1000000));

					overlayItemCurrent = new OverlayItem(point, "Bus # "
							+ busNumber, "Your location");

					itemizedOverlayCurrentLocation
							.addOverlay(overlayItemCurrent);

					mapOverlays.add(itemizedOverlayCurrentLocation);

					if (isFirstTime)
					{
						isFirstTime = false;
						MapController controller = mapView.getController();
						controller.animateTo(point);
						controller.setZoom(16);
					}
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
		if (ActivityMaps.this.status == 0)
		{
			mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onInit(int status)
	{
		ActivityMaps.this.status = status;
	}

}
