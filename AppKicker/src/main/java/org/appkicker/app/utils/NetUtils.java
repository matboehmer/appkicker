package org.appkicker.app.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

/**
 * Basic functionality for communication via HTTP.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class NetUtils {

	private static final String default_settings_serverurl = "http://api.appkicker.org/interactions.php";

	/**
	 * Returns the URL of the script for putting data into mysql running on the server as configured by the user in settings.
	 * @return
	 */
	public static String getScriptURL(Context c) {
		
		return default_settings_serverurl;
		
		/*
		 * URL fixed now
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String addr = settings.getString("settings_serverurl", default_settings_serverurl);
		return addr;
		 */
	}
	
	
	public static boolean doSyncWifiOnly(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		boolean wifionly = settings.getBoolean("settings_syncwifionly", false);
		return wifionly;
	}
	
	private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	private static final int HTTP_SOCKET_BUFFERSIZE = 8192;

	/* default buffersize = 8kb */
	public static final int BUFFER_SIZE = 8 * 1024;

	/* secret for signing the code */
	private static final String SIGNATURE = "xxx"; // removed so that database will not be swamped with test data

	private Context context;

	private HttpClient httpClient;

	public NetUtils(Context c) {
		this.context = c;
		this.httpClient = null;
	}

	/**
	 * Generate and return a {@link HttpClient} configured for general use,
	 * including setting an application-specific user-agent string.
	 */
	public static HttpClient createHttpClient(Context context) {
		
		final HttpParams params = new BasicHttpParams();

		// Use generous timeouts for slow mobile networks
		HttpConnectionParams.setConnectionTimeout(params, 30 * SECOND_IN_MILLIS);
		HttpConnectionParams.setSoTimeout(params, 30 * SECOND_IN_MILLIS);

		HttpConnectionParams.setSocketBufferSize(params, HTTP_SOCKET_BUFFERSIZE);
		HttpProtocolParams.setContentCharset(params, Utils.CHARSET);
		HttpProtocolParams.setUserAgent(params, buildUserAgent(context));

		final DefaultHttpClient client = new DefaultHttpClient(params);

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});

		Log.d(Utils.TAG, "created a new http client");
		return client;
	}
	
	/**
	 * Initializes the member http client with a 
	 * new instance of the HttpClient object  by
	 * using the current context
	 */
	private void initHttpClient() {
		if(httpClient == null) httpClient = createHttpClient(context);
	}
	
	/**
	 * Releases all resources of the member http client by shutting it down and disposing it 
	 */
	private void releaseHttpClient() {
		if(httpClient != null && httpClient.getConnectionManager() != null) httpClient.getConnectionManager().shutdown();
		httpClient = null;
	}

	/**
	 * Build and return a user-agent string that can identify this application
	 * to remote servers. Contains the package name and version code.
	 */
	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " (" + info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	/**
	 * Simple {@link HttpEntityWrapper} that inflates the wrapped
	 * {@link HttpEntity} by passing it through {@link GZIPInputStream}.
	 */
	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	@Deprecated
	public static JSONObject getJSONObjectFromResponse(HttpResponse response) {
		JSONObject ret = null;
		InputStream inputStream;
		try {
			inputStream = response.getEntity().getContent();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), BUFFER_SIZE);
			StringBuilder sb = new StringBuilder();

			int c = reader.read();
			while (c > 0) {
				sb.append((char) c);
				c = reader.read();
			}
			Log.d(Utils.TAG, "Utils: getJsonObjectFromResponse (" + sb.length() + ") :" + sb);

			response.getEntity().consumeContent();
			inputStream.close();
			reader.close();

			if (sb.length() > 0) {
				String resultString = sb.toString();
				if (resultString == null) {
					Log.d(Utils.TAG, "resultstring : <null>");
					return null;
				} else if (resultString.equals("null")) {
					Log.d(Utils.TAG, "resultstring : null");
					return null;
				} else if (resultString.length() == 0) {
					Log.d(Utils.TAG, "resultstring : length 0");
					return null;
				} else {
					Log.d(Utils.TAG, "creating JSON Object");
					return new JSONObject(resultString);
				}
			}

		} catch (IllegalStateException e) {
			Log.e(Utils.TAG, "IllegalStateException: " + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.e(Utils.TAG, "IOException: " + e.getLocalizedMessage());
		} catch (JSONException e) {
			Log.e(Utils.TAG, "JSONException: " + e.getLocalizedMessage());
		}

		return ret;
	}

	/**
	 * Sends an HTTP POST to the given url with the given data as payload.
	 * 
	 * @param url
	 * @param data
	 */
	public boolean postToURL(String url, String data) {
		// this currently fails for some thread reason
		// Utils.dToast("sending data " + SERVER_HOST_APACHE);
		
		// we cannot send anything to server if user has not acknowledged disclaimer
		boolean acked = Utils.isDisclaimerAcknowledged(AppKickerApplication.getAppContext());
		if (!acked) {
			Utils.dToast(this, "disclaimer not acked");
			return false;
		}
		
		try {
			
			
			HttpPost req = new HttpPost(url);
			HttpEntity entity;
			Utils.d(this, "sending " + data.length() + " bytes to url " + url);

			String hash = Utils.md5(data + SIGNATURE);
			req.addHeader("check", hash);
			
			entity = new StringEntity(data, Utils.CHARSET);
			req.setEntity(entity);
			
			initHttpClient();
			HttpResponse res = httpClient.execute(req);
			releaseHttpClient();
			
			Utils.d(this, "getStatusLine " + res.getStatusLine());
			if (res.getStatusLine().getStatusCode() == 200) {
				Utils.dToast(this, "http request succeeded: " + res.getStatusLine());
				return true;
			} else {
				Utils.dToast(this, "http request failed: " + res.getStatusLine());
				return false;
			}
		} catch (IllegalArgumentException e) {
			Utils.e(this, "IllegalArgumentException" + e.getLocalizedMessage());
			return false;
		} catch (UnsupportedEncodingException e) {
			Utils.e(this, "UnsupportedEncodingException" + e.getLocalizedMessage());
			return false;
		} catch (ClientProtocolException e) {
			Utils.e(this, "ClientProtocolException" + e.getLocalizedMessage());
			return false;
		} catch (IOException e) {
			Utils.e(this, "IOException" + e.getLocalizedMessage());
			return false;
		}
	}


}
