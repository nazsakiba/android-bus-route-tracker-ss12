package usc.ss.tracker;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Utility
{
	static public boolean saveMyPreference(Context context, String key, boolean value)
	  {
		  SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.preferences_key_bustrackr), Activity.MODE_PRIVATE);
		  Editor editor = sharedPreferences.edit();
		  editor.putBoolean(key, value);
		  return editor.commit();
	  }
	
	static public boolean saveMyPreference(Context context, String key, String value)
	  {
		  SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.preferences_key_bustrackr), Activity.MODE_PRIVATE);
		  Editor editor = sharedPreferences.edit();
		  editor.putString(key, value);
		  return editor.commit();
	  }
	
	static public boolean saveMyPreference(Context context, String key, Float value)
	  {
		  SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.preferences_key_bustrackr), Activity.MODE_PRIVATE);
		  Editor editor = sharedPreferences.edit();
		  editor.putFloat(key, value);
		  return editor.commit();
	  }
	
	static public boolean saveMyPreference(Context context, String key, int value)
	  {
		  SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.preferences_key_bustrackr), Activity.MODE_PRIVATE);
		  Editor editor = sharedPreferences.edit();
		  editor.putInt(key, value);
		  return editor.commit();
	  }
	
	static public boolean saveMyPreference(Context context, String key, long value)
	  {
		  SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.preferences_key_bustrackr), Activity.MODE_PRIVATE);
		  Editor editor = sharedPreferences.edit();
		  editor.putLong(key, value);
		  return editor.commit();
	  }
	
	public static HttpResponse openURL(String url, WebListener listener) throws ClientProtocolException, IOException
	{
		// Set HttpParams
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, 10000);

		// Create http client
		HttpClient httpClient = new DefaultHttpClient(myParams);

		HttpResponse httpResponse = null;

		Log.i("URL_LINKED", "URL : " + url);

		// Initialize HttpGet method
		HttpGet httpGet = new HttpGet(url);

		// Execute method to read HttpResponse
		httpResponse = httpClient.execute(httpGet);

		switch (httpResponse.getStatusLine().getStatusCode())
		{
			case HttpURLConnection.HTTP_OK:

				Log.i("URL_RESPONSE", "HTTP_OK");
				
				listener.onComplete(EntityUtils.toString(httpResponse.getEntity()));
				
				break;
			default:
				break;
		}

		return httpResponse;
	}

	public static interface WebListener
	{
		/**
		 * Called when a request is complete
		 * 
		 * @param response
		 *            Response String in JSON format
		 */
		public void onComplete(String response);
	}
}
