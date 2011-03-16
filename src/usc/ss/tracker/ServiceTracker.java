package usc.ss.tracker;

import java.util.ArrayList;

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

	ArrayList<String> listStops;
	ArrayList<Double> listLatitude;
	ArrayList<Double> listLongitude;

	private int destination;

	LocationManager locationManager;

	private int lastStop;

	@Override
	public void onCreate()
	{
		super.onCreate();

		mBinder = new LocalBinder();

		isTracking = false;
		notificationNumber = 3;

		lastStop = -1;

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

	public void setDestinationAndStart(ArrayList<String> listStops,
			ArrayList<Double> listLatitude, ArrayList<Double> listLongitude,
			int destination)
	{
		isTracking = true;

		setDestinationCoordinates(listStops, listLatitude, listLongitude,
				destination);

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

			float minimumDistance = -1.0f;
			int currentStop = -1;

			if (mListener != null)
			{
				mListener.onLocationUpdate((float) location.getLatitude(),
						(float) location.getLongitude());
			}

			// Check for nearest stop
			for (int i = 0; i < listLatitude.size(); i++)
			{
				if (currentStop == -1)
				{
					// Calculate distance from ith stop
					Location.distanceBetween(location.getLatitude(),
							location.getLongitude(), listLatitude.get(i),
							listLatitude.get(i), distance);

					// Convert into miles
					distanceInMiles = distance[0] / 1609.0f;

					minimumDistance = distanceInMiles;
					currentStop = i;
				} else
				{
					// Calculate distance from ith stop
					Location.distanceBetween(location.getLatitude(),
							location.getLongitude(), listLatitude.get(i),
							listLatitude.get(i), distance);

					// Convert into miles
					distanceInMiles = distance[0] / 1609.0f;

					if (distanceInMiles < minimumDistance)
					{
						minimumDistance = distanceInMiles;
						currentStop = i;
					}

				}
			}

			// Calculate distance from destination
			Location.distanceBetween(location.getLatitude(),
					location.getLongitude(), listLatitude.get(destination),
					listLongitude.get(destination), distance);

			// Convert into miles
			distanceInMiles = distance[0] / 1609.0f;

			// Send update if minimumDistance for a stop is less than 0.2 miles
			if (lastStop != currentStop)
			{
				lastStop = currentStop;

				if (currentStop != destination)
				{
					if (mListener != null)
					{
						mListener.onStopReached(currentStop, distanceInMiles);
					}
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

	public void setDestinationCoordinates(ArrayList<String> listStops,
			ArrayList<Double> listLatitude, ArrayList<Double> listLongitude,
			int destination)
	{
		ServiceTracker.this.listStops = listStops;
		ServiceTracker.this.listLatitude = listLatitude;
		ServiceTracker.this.listLongitude = listLongitude;

		ServiceTracker.this.destination = destination;
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
		public void onStopReached(int current, float distanceFromDestination);

		public void onLocationUpdate(float latitude, float longitude);
	}
}
