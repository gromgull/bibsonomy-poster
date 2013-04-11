/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gromgull.android.bibsonomyposter;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlSerializer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single view: an EditText that
 * displays and edits some internal text.
 */
@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class BibsonomyPosterActivity extends Activity {

	static final private int SAVE_ID = Menu.FIRST;

	private static final String PREF_NAME = "BibsonomyPosterPrefs";
	private static final String PREF_USERNAME = "username";
	private static final String PREF_APIKEY = "apikey";
	private static final String LOGTAG = "BibsonomyPoster";

	private String username;
	private String apikey;

	public BibsonomyPosterActivity() {
	}

	/** Called with the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		load();

		Intent intent = getIntent();
		String action = intent.getAction();
		if (action.equalsIgnoreCase(Intent.ACTION_SEND)) {
			final String uri = intent.getStringExtra(Intent.EXTRA_TEXT);
			final String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);

			final Context context = getApplicationContext();

			new AsyncTask<Void, Integer, Boolean>() {

				@Override
				protected Boolean doInBackground(Void... v) {
					try {
						bookmark(uri, title);
						return true;
					} catch (Exception e) {
						Log.w(LOGTAG, "Could not bookmark: " + title + " / "
								+ uri, e);
						return false;
					}
				}

				protected void onPostExecute(Boolean result) {
					if (result) {
						Toast toast = Toast.makeText(context, "Bookmarked "
								+ title, Toast.LENGTH_SHORT);
						toast.show();
					} else {
						Toast toast = Toast.makeText(context,
								"Error bookmarking " + title,
								Toast.LENGTH_SHORT);
						toast.show();
					}
				}

			}.execute();

			finish();
			return;
		}

		// Inflate our UI from its XML layout description.
		setContentView(R.layout.bibsonomyposter_activity);

		if (username != null)
			((EditText) findViewById(R.id.username)).setText(username);
		if (apikey != null)
			((EditText) findViewById(R.id.apikey)).setText(apikey);

		// Hook up button presses to the appropriate event handler.
		((Button) findViewById(R.id.save)).setOnClickListener(mSaveListener);

	}

	private void load() {
		SharedPreferences p = getSharedPreferences(PREF_NAME, 0);

		username = p.getString(PREF_USERNAME, null);
		apikey = p.getString(PREF_APIKEY, null);
	}

	private void save() {
		SharedPreferences p = getSharedPreferences(PREF_NAME, 0);
		Editor e = p.edit();

		e.putString(PREF_USERNAME, getUsername());
		e.putString(PREF_APIKEY, getAPIKey());

		e.commit();

		Toast toast = Toast.makeText(getApplicationContext(),
				"Saved username/apikey - now go bookmark something!",
				Toast.LENGTH_SHORT);
		toast.show();
		
		finish();

	}

	private String getAPIKey() {
		return ((EditText) findViewById(R.id.apikey)).getText().toString();
	}

	private String getUsername() {
		return ((EditText) findViewById(R.id.username)).getText().toString();
	}

	/**
	 * Called when the activity is about to start interacting with the user.
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	/**
	 * Called when your activity's options menu needs to be created.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// We are going to create two menus. Note that we assign them
		// unique integer IDs, labels from our string resources, and
		// given them shortcuts.
		menu.add(0, SAVE_ID, 0, R.string.save).setShortcut('0', 's');

		return true;
	}

	/**
	 * Called right before your activity's option menu is displayed.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		return true;
	}

	/**
	 * Called when a menu item is selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SAVE_ID:
			save();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A call-back for when the user presses the back button.
	 */
	OnClickListener mSaveListener = new OnClickListener() {
		public void onClick(View v) {
			save();
		}
	};

	public void bookmark(String url, String title)
			throws ClientProtocolException, IOException {
		CredentialsProvider credProvider = new BasicCredentialsProvider();

		credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST,

		AuthScope.ANY_PORT, AuthScope.ANY_REALM),
				new UsernamePasswordCredentials(username, apikey));

		StringWriter sw = new StringWriter();

		XmlSerializer x = Xml.newSerializer();
		x.setOutput(sw);
		x.startDocument(null, null);
		x.startTag(null, "bibsonomy");
		x.startTag(null, "post");
		x.attribute(null, "description", "a bookmark");

		x.startTag(null, "user");
		x.attribute(null, "name", username);
		x.endTag(null, "user");

		x.startTag(null, "tag");
		x.attribute(null, "name", "from_android");
		x.endTag(null, "tag");

		x.startTag(null, "group");
		x.attribute(null, "name", "public");
		x.endTag(null, "group");

		x.startTag(null, "bookmark");
		x.attribute(null, "url", url);
		x.attribute(null, "title", title);
		x.endTag(null, "bookmark");

		x.endTag(null, "post");
		x.endTag(null, "bibsonomy");

		x.endDocument();

		Log.v(LOGTAG, "XML: " + sw.toString());

		HttpPost httppost = new HttpPost("http://www.bibsonomy.org/api/users/"
				+ username + "/posts");
		StringEntity e = new StringEntity(sw.toString());
		e.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
				"application/xml"));
		httppost.setEntity(e);

		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.setCredentialsProvider(credProvider);
		HttpResponse response = httpclient.execute(httppost);
		Log.i(LOGTAG, "Bibsonomy said :" + response.getStatusLine());
		if (response.getStatusLine().getStatusCode() != 201) {
			HttpEntity re = response.getEntity();
			byte b[] = new byte[(int) re.getContentLength()];
			re.getContent().read(b);
			Log.v(LOGTAG, "Bibsonomy said: " + new String(b));
			throw new IOException("Bibsonomy said :" + response.getStatusLine());

		}
	}

}
