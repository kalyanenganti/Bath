package com.akqa.bath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
	volatile float mean_temp;  ///volatile fields read and write from more than a thread
	volatile float tub_capacity;
	
	volatile float m1;// rate of hot water filling the tub ml/sec
	volatile float m2;// rate of cold water filling the tub ml/sec
	
	//getters and setters
	public float getM1() {
		return m1;
	}

	public void setM1(float m1) {
		this.m1 = m1;
	}

	public float getM2() {
		return m2;
	}

	public void setM2(float m2) {
		this.m2 = m2;
	}

	private boolean isHotWaterOn;//flags to set taps/on/off
	private boolean isColdWaterOn;

	private ImageView imageView1, imageView2;
	private TextView tv;
	private Bath bath;
	private NetworkUtility networkUtility;
	private final String url = "http://static.content.akqa.net/mobile-test/bath.json";//url to fetch values from
	private Thread hot, cold;
	float tmp1, tmp2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		bath=new Bath();
		//System.out.println("on create called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		networkUtility = new NetworkUtility(this);
		imageView1 = (ImageView) findViewById(R.id.coldButton);
	    imageView2 = (ImageView) findViewById(R.id.hotButton);
		tv = (TextView) findViewById(R.id.textView1);
		m1 = 0;
		m2 = 0;
		if (savedInstanceState == null) {
			//run on first run when bundle is null
			tub_capacity = 150000;
			if (!networkUtility.isMobileDataEnabled() & !networkUtility.isWifiConnected()) {
				// if no WIFI or mobile data connectivity
				// show Dialog
				showCustomDialog(R.string.no_network_dialog_title,
						R.string.no_network_dialog_msg);
			} else {
				// do a http get request in a background thread and obtain
				// temperatures on start
				new HttpRequestTask().execute(url);
			}
		} 
		else {
			tub_capacity = tub_capacity;
			hot = new HotWaterTap();
			cold = new ColdWaterTap();
		}
		
		imageView2.setOnClickListener(new OnClickListener() {
			//listen for click on image

			@Override
			public void onClick(View v) {
				if (imageView2.isPressed()&!isHotWaterOn) {
					imageView2.setRotation(30);
					Log.i("hot water", "on");
					isHotWaterOn = true;
					hot = new HotWaterTap();
					hot.start();
				} else if (imageView2.isPressed()&isHotWaterOn){
					imageView2.setRotation(360);
					Log.i("hot water", "off");
					isHotWaterOn = false;
					if (hot != null) {
						hot.interrupt();
						hot = null;
					}
				}
			}
		});

		imageView1.setOnClickListener(new OnClickListener() {
        //listen for click on imageView
			@Override
			public void onClick(View v) {
				if (imageView1.isPressed()&!isColdWaterOn) {
					imageView1.setRotation(30);
					Log.i("cold water", "on");
					isColdWaterOn = true;
					cold = new ColdWaterTap();
					cold.start();

				} else if(imageView1.isPressed()&isColdWaterOn){
					imageView1.setRotation(360);
					Log.i("cold water", "off");
					isColdWaterOn = false;
					if (cold != null) {
						cold.interrupt();
						cold = null;
					}
				}
			}
		});
	}

	public float getMean_temp() {
		return mean_temp;
	}

	public void setMean_temp(float mean_temp) {
		this.mean_temp = mean_temp;
	}

	private void showCustomDialog(int title, int message) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set title
		alertDialogBuilder.setTitle(title);

		// set dialog message
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// if this button is clicked, just close
								// the dialog box and do nothing
								dialog.dismiss();
							}
						});

		// create alert dialog
		AlertDialog alertDialog2 = alertDialogBuilder.create();

		// show it
		alertDialog2.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// class performs get request to our webservice in the background and parses
	// the data on successful completion
	class HttpRequestTask extends AsyncTask<String, String, String> {
		String responseString;

		@Override
		protected String doInBackground(String... url) {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			try {
				response = httpclient.execute(new HttpGet(url[0]));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					responseString = out.toString();
				} else {
					// Closes the connection.
					response.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return responseString;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			try {
				JSONObject jsonObj = new JSONObject(responseString);// parse
																	// json
																	// string
																	// from the
																	// get
																	// request
				bath.setHot_water_temp(Float.parseFloat(jsonObj // and set the
																// temperatures
						.getString("hot_water")));
				bath.setCold_water_temp(Float.parseFloat(jsonObj
						.getString("cold_water")));
				tmp1 = (float) bath.getHot_water_temp();
				tmp2 = (float) bath.getCold_water_temp();
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
	}

	//Hot Water Tap thread simulates a hot water tap 
	class HotWaterTap extends Thread {
		@Override
		public void run() {
			m1 = (m1 == 0) ? 167 : m1;// m1=167/sec the first time tap is turned
										// on,else it is equal to mass the last
										// time it was turned on
			System.out.println("m1: " + m1);
			while (!isInterrupted()) {
				while (isHotWaterOn) {
					tub_capacity = tub_capacity - 167;
					mean_temp = getMeanTemp();

					try {
						Thread.sleep(1000);//sleep for a second update the display and values
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								tv.setText(" Tub Free: " + (tub_capacity)
										/ 1000.00f + " ltrs \n Tub temp: "
										+ String.format("%.2f", mean_temp));

							}
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					m1 = m1 + 167;//adds 167ml/sec every second
					
				}
			}
		}
	}

	// Cold Water Tap thread simulates a cold water tap 
	class ColdWaterTap extends Thread {
		@Override
		public void run() {
			m2 = (m2 == 0) ? 200 : m2; // m2=200/sec the first time tap is
										// turned on else it is equal to mass
										// the last time it was turned on
			System.out.println("m2: " + m2);
			while (!isInterrupted()) {
				while (isColdWaterOn) {
					tub_capacity = tub_capacity - 200;
					mean_temp = getMeanTemp();
					try {
						Thread.sleep(1000);//sleep for a second update the display and values
						MainActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								tv.setText(" Tub Free: " + (tub_capacity)
										/ 1000.00f + " ltrs \n Tub temp: "
										+ String.format("%.2f", mean_temp));

							}
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					m2 = m2 + 200;//adds 200ml/sec to the tub
				}
			}
		}

	}

	private synchronized float getMeanTemp() {

		return ((m1 * tmp1) + (m2 * tmp2)) / (m1 + m2);
	}

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		// save data and UI before activity is destroyed
		state.putBoolean("isHotWaterOn", isHotWaterOn);
		state.putBoolean("isColdWaterOn", isColdWaterOn);
		state.putFloat("mean_temp", mean_temp);
		state.putFloat("m1", m1);
		state.putFloat("m2", m2);
		state.putFloat("tmp1", tmp1);
		state.putFloat("tmp2", tmp2);
		state.putFloat("tub_free", tub_capacity);
		if (hot != null) {
			state.putBoolean("isHotRunning", hot.isAlive());
		}
		if (cold != null) {
			state.putBoolean("isColdRunning", cold.isAlive());
		}

	}
     

	
	
	@Override
	public void onRestoreInstanceState(Bundle instate) {
		super.onRestoreInstanceState(instate);
		// Restore UI state from the instate
		isHotWaterOn = instate.getBoolean("isHotWaterOn");
		isColdWaterOn = instate.getBoolean("isColdWaterOn");

		if (isHotWaterOn) {
			imageView2.setPressed(true);
			imageView2.setRotation(30);
		}
		if (isColdWaterOn) {
			imageView1.setPressed(true);
			imageView1.setRotation(30);
		}
		mean_temp = instate.getFloat("mean_temp");
		tmp1 = instate.getFloat("tmp1");
		tmp2 = instate.getFloat("tmp2");
		m1 = instate.getFloat("m1");
		m2 = instate.getFloat("m2");
		tub_capacity = instate.getFloat("tub_free");

		boolean isHotRunning = instate.getBoolean("isHotRunning");
		if (isHotRunning) {
			hot = new HotWaterTap();
			hot.start();
		}
		boolean isColdRunning = instate.getBoolean("isColdRunning");
		if (isColdRunning) {
			cold = new ColdWaterTap();
			cold.start();
		}

	}

	@Override
	protected void onResume() {
		System.out.println("on resume called");
		super.onResume();

	}

	@Override
	protected void onPause() {
		System.out.println("on pause called");
		super.onPause();

	}

	@Override
	protected void onStart() {
		System.out.println("on start called");
		super.onStart();

	}

	@Override
	protected void onStop() {
		System.out.println("on stop called");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		hot = null;
		cold = null;
		System.out.println("on destroy called");
		super.onDestroy();

	}

	/*
	 * @Override public boolean onKeyDown(int keyCode, KeyEvent event) { if
	 * (keyCode == KeyEvent.KEYCODE_BACK) { moveTaskToBack(true); } return
	 * super.onKeyDown(keyCode, event);
	 * 
	 * }
	 */
}
