package usc.ss.tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ServiceTracker extends Service
{
	protected static final String TAG = "SERVICE_TRACKER";

	private IBinder mBinder;

	private ServiceSensorListener mListener;

	private ServiceTracker mService;
	
	private double destinationLatitude;

	private double destinationLongitude;


	LocationManager locationManager;

	@Override
	public void onCreate()
	{
		super.onCreate();

		mBinder = new LocalBinder();

		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i(TAG, "Service Bounded");

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				10000, 0, locationListener);

		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		Log.i(TAG, "Service Unbounded");

		// Remove the listener that was previously added
		locationManager.removeUpdates(locationListener);

		return true;
	}

	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener()
	{
		public void onLocationChanged(Location location)
		{
			float distance[] = {0.0f, 0.0f, 0.0f};
			Location.distanceBetween(location.getLatitude(),
					location.getLongitude(), 34.061726, -118.284216, distance);
		}

		public void onStatusChanged(String provider, int status, Bundle extras)
		{
		}

		public void onProviderEnabled(String provider)
		{
		}

		public void onProviderDisabled(String provider)
		{
		}
	};

	public void setDestinationCoordinates(double latitude, double longitude)
	{
		ServiceTracker.this.destinationLatitude = latitude;
		ServiceTracker.this.destinationLongitude = longitude;
	}

	public class LocalBinder extends Binder
	{
		public ServiceTracker getService()
		{
			return ServiceTracker.this;
		}
	}

	public void setOnServiceSensorListener(ServiceSensorListener mListener)
	{
		this.mListener = mListener;
	}

	public void removeOnServiceSensorListener()
	{
		if (mListener != null)
		{
			this.mListener = null;
		}
	}

	public interface ServiceSensorListener
	{
		public void onLocationUpdate(double latitude, double longitude);
	}
}
