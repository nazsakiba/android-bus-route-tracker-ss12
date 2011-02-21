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

	private boolean isTracking;
	private int notificationNumber;

	private double destinationLatitude;

	private double destinationLongitude;

	LocationManager locationManager;

	@Override
	public void onCreate()
	{
		super.onCreate();

		mBinder = new LocalBinder();

		isTracking = false;
		notificationNumber = 3;

		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i(TAG, "Service Bounded");

		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		Log.i(TAG, "Service Unbounded");

		if (isTracking == true)
		{
			destinationReached();
		}

		return true;
	}

	public void setDestinationAndStart(float latitude, float longitude)
	{
		ServiceTracker.this.destinationLatitude = latitude;
		ServiceTracker.this.destinationLongitude = longitude;

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				10000, 0, locationListener);
	}

	public void destinationReached()
	{
		isTracking = false;

		// Remove the listener that was previously added
		locationManager.removeUpdates(locationListener);
	}

	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener()
	{
		public void onLocationChanged(Location location)
		{
			float distance[] = { 0.0f, 0.0f, 0.0f };
			float distanceInMiles = 0.0f;
			Location.distanceBetween(location.getLatitude(),
					location.getLongitude(), destinationLatitude,
					destinationLongitude, distance);

			distanceInMiles = distance[0] / 1609.0f;
			
			if (!isTracking)
			{
				isTracking = true;

				if (mListener != null)
				{
					mListener.onLocationUpdate(distanceInMiles);
					return;
				}
			} else
			{
				switch (notificationNumber)
				{
					case 3:
						if (distanceInMiles < 2.0f)
						{
							notificationNumber --;
							
							if (mListener != null)
							{
								mListener.onLocationUpdate(distanceInMiles);
								return;
							}
						}
						return;
					case 2:
						if (distanceInMiles < 1.0f)
						{
							notificationNumber --;
							
							if (mListener != null)
							{
								mListener.onLocationUpdate(distanceInMiles);
								return;
							}
						}
						return;
					case 1:
						if (distanceInMiles < 0.5f)
						{
							notificationNumber --;
							
							if (mListener != null)
							{
								mListener.onLocationUpdate(distanceInMiles);
								return;
							}
						}
						return;
					case 0:
						if (distanceInMiles < 0.1f)
						{
							notificationNumber --;
							
							if (mListener != null)
							{
								mListener.onLocationUpdate(distanceInMiles);
								return;
							}
						}
						return;
					default:
						return;
				}
			}
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
		public void onLocationUpdate(float distance);
	}
}
