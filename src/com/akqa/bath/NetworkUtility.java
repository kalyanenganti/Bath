package com.akqa.bath;

import java.lang.reflect.Method;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

@SuppressWarnings("unused")
public class NetworkUtility {
	private Context context;
	private ConnectivityManager cm;
	public NetworkUtility(Context context) {
		this.context = context;
		cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
  //uses reflection to check if  mobile data is on/off
	public boolean isMobileDataEnabled() {
		boolean mobileDataEnabled = false; // Assume disabled
		
		try {
			
			Class cmClass = Class.forName(cm.getClass().getName());
			Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
			method.setAccessible(true); // Make the method callable
			// get the setting for "mobile data"
			mobileDataEnabled = (Boolean) method.invoke(cm);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mobileDataEnabled;
	}
	//checks if  phone wifi is connected
	public boolean isWifiConnected(){
		
		boolean wifiConnected = false; // Assume disabled
		NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) {
		   wifiConnected=true;
		}
		return wifiConnected;
	}
	
}
