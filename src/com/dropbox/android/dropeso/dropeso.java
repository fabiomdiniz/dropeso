/*
 * Copyright (c) 2010 Evenflow, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dropbox.android.dropeso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.dropbox.android.*;
import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Entry;

import org.json.JSONException;
import org.json.JSONObject;

public class dropeso extends Activity {
	// private static final String TAG = "dropeso";

	final static private String CONSUMER_KEY = "ovu2u0jxd7tykzn";
	final static private String CONSUMER_SECRET = "avv59ibroji0yv7";

	private boolean mLoggedIn = false;
	private boolean json_found = false;
	private EditText mLoginEmail;
	private EditText mLoginPassword;
	private Button mSubmit;
	private EditText mWeightValue;
	private Button mUploadWeight;
	private TextView mText;
	private ProgressDialog mProgress;
	private Dropbox mDropbox;

	final static private String loginfile = "login_data";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mDropbox = new Dropbox(this, CONSUMER_KEY, CONSUMER_SECRET);

		mLoginEmail = (EditText) findViewById(R.id.login_email);
		mLoginPassword = (EditText) findViewById(R.id.login_password);
		mSubmit = (Button) findViewById(R.id.login_submit);
		mText = (TextView) findViewById(R.id.text);
		
		mUploadWeight = (Button) findViewById(R.id.upload_button);
		mWeightValue = (EditText) findViewById(R.id.weight_value);
		
		mProgress = new ProgressDialog(this);
		mProgress.setMessage("Please wait...");
		mProgress.setTitle("Login Info Found! Signing in...");

		File file = getApplicationContext().getFileStreamPath(loginfile);
		if (file.exists()) {
			byte[] buffer = new byte[(int) getApplicationContext()
					.getFileStreamPath(loginfile).length()];
			FileInputStream f = null;
			try {
				f = openFileInput(loginfile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			try {
				f.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String s = new String(buffer);
			JSONObject json = null;
			try {
				json = new JSONObject(s);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			mProgress.show();
			String email = null;
			String password = null;
			try {
				email = json.getString("email");
				password = json.getString("password");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			mDropbox.login(mLoginListener, email, password);
		}
		mSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mLoggedIn) {
					// We're going to log out
					mDropbox.deauthenticate();
					setLoggedIn(false);
					mText.setText("");
				} else {
					// Try to log in
					try {
						getAccountInfo();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});

		mUploadWeight.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				runOnUiThread(new Runnable() {
				    public void run() {
				    	showToast("Downloading file...");
				    }
				});
				try {
					
					mDropbox.downloadDropboxFile("/Dropeso/dropeso.json", getApplicationContext().getFileStreamPath("dropeso.json"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				FileInputStream f = null;
				try {
					f = openFileInput("dropeso.json");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				byte[] buffer = new byte[(int) getApplicationContext()
				     					.getFileStreamPath("dropeso.json").length()];
				try {
					f.read(buffer);
				} catch (IOException e) {
					e.printStackTrace();
				}
				String s = new String(buffer);
				Boolean json_ok = false;
				JSONObject json = null;
				if(!s.isEmpty()) {
					try {
						json = new JSONObject(s);
					} catch (JSONException e) {
						e.printStackTrace();
					};
					
					if(json.has("dates") && json.has("values"))
					{
						showToast("Data ok, appending...");
						json_ok = true;
					}
				}
				if(json_ok) {
					
				}
				else
				{
					json = new JSONObject();
					showToast("Data corrupted. Starting fresh...");
					ArrayList<Double> values = new ArrayList<Double>();
					values.add(Double.parseDouble(mWeightValue.getText().toString()));
					ArrayList<String> dates = new ArrayList<String>();
					Date date = new Date();
					SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
					dates.add(mSimpleDateFormat.format(date));
					try {
						json.put("values", values);
						json.put("dates", dates);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					FileOutputStream fos;
					try {
						fos = openFileOutput("dropeso.json",
								Context.MODE_PRIVATE);
						fos.write(json.toString().getBytes());
						fos.close();
						mDropbox.putFile(
								"dropbox",
								"/Dropeso",
								getApplicationContext().getFileStreamPath(
										"dropeso.json"));
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

				
			}
		});
		
		/*
		 * if (mDropbox.authenticate()) { // We can query the account info
		 * already, since we have stored // credentials getAccountInfo(); }
		 */
	}

	/**
	 * Notifies our Activity when a login process succeeded or failed.
	 */
	DropboxLoginListener mLoginListener = new DropboxLoginListener() {

		@Override
		public void loginFailed(String message) {
			mProgress.dismiss();
			showToast("Login failed: " + message);
			setLoggedIn(false);
		}

		@Override
		public void loginSuccessfull() {
			mProgress.dismiss();
			showToast("Logged in!");
			setLoggedIn(true);
			try {
				displayAccountInfo(mDropbox.accountInfo());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	};

	/**
	 * Convenience function to change UI state based on being logged in
	 */
	public void setLoggedIn(boolean loggedIn) {
		mLoggedIn = loggedIn;
		mLoginEmail.setEnabled(!loggedIn);
		mLoginPassword.setEnabled(!loggedIn);
		mWeightValue.setEnabled(loggedIn);
		mUploadWeight.setEnabled(loggedIn);
		
		if (loggedIn) {
			mSubmit.setText("Log Out of Dropbox");
		} else {
			mSubmit.setText("Log In to Dropbox");
		}
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

	private void getAccountInfo() throws Exception {

		if (mDropbox.isAuthenticated()) {
			// If we're already authenticated, we don't need to get the login
			// info
			mProgress.show();
			mDropbox.login(mLoginListener);

		} else {

			String email = mLoginEmail.getText().toString();
			if (email.length() < 5 || email.indexOf("@") < 0
					|| email.indexOf(".") < 0) {
				showToast("Error, invalid e-mail");
				return;
			}

			String password = mLoginPassword.getText().toString();
			if (password.length() < 6) {
				showToast("Error, password too short");
				return;
			}

			// It's good to do Dropbox API (and any web API) calls in a separate
			// thread,
			// so we don't get a force-close due to the UI thread stalling.

			JSONObject json = new JSONObject();
			try {
				json.put("email", email);
				json.put("password", password);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			FileOutputStream fos = openFileOutput(loginfile,
					Context.MODE_PRIVATE);
			fos.write(json.toString().getBytes());
			fos.close();
			mProgress.setTitle("Login Info Saved! Signing in...");
			mProgress.show();
			mDropbox.login(mLoginListener, email, password);
		}
	}

	/**
	 * Displays some useful info about the account, to demonstrate that we've
	 * successfully logged in
	 * 
	 * @param account
	 * @throws Exception
	 */
	public void displayAccountInfo(DropboxAPI.Account account) throws Exception {
		if (account != null) {
			String info = "Name: " + account.displayName + "\n" + "E-mail: "
					+ account.email + "\n" + "User ID: " + account.uid + "\n"
					+ "Quota: " + account.quotaQuota + "\nFound: ";
			ArrayList<Entry> dropeso_files = mDropbox.listDirectory("/Dropeso");
			if (dropeso_files == null) {
				showToast("Creating Folder and File");
				mDropbox.createFolder("dropbox", "/Dropeso");
				FileOutputStream fos = openFileOutput("dropeso.json",
							Context.MODE_PRIVATE);
					fos.close();
					mDropbox.putFile(
							"dropbox",
							"/Dropeso",
							getApplicationContext().getFileStreamPath(
									"dropeso.json"));
			} else {
				for (Entry e : dropeso_files) {
					if (!e.is_dir) {
						if (e.path.equals("/Dropeso/dropeso.json")) {
							json_found = true;
							break;
						}
					}
				}
				if (!json_found) {
					showToast("Creating File");
					FileOutputStream fos = openFileOutput("dropeso.json",
							Context.MODE_PRIVATE);
					fos.close();
					mDropbox.putFile(
							"dropbox",
							"/Dropeso",
							getApplicationContext().getFileStreamPath(
									"dropeso.json"));
				}
			}

			info += json_found;
			mText.setText(info);
		}
	}

}