package usc.ss.tracker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class Utility
{
	public static HttpResponse openURL(String url, String method, WebListener listener) throws MalformedURLException, IOException
	{
		// Set HttpParams
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, 10000);

		// Create http client
		HttpClient httpClient = new DefaultHttpClient(myParams);

		StringEntity sendEntity = null;
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
