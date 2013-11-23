package com.codeperf.getback.core;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;

public class LocationFinder implements LocationListener {

	// The minimum distance to change Updates in meters
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // meters

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 30 * 1;

	// flag for GPS status
	private boolean isGPSEnabled = false;

	// flag for network status
	private boolean isNetworkEnabled = false;

	private boolean locationAccepted = false;

	// Declaring a Location Manager
	protected LocationManager locationManager;

	private Context context;

	private ILocationFinderCallback callback;

	public LocationFinder(Context ctx) {
		context = ctx;
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
	}

	public boolean canFindLocation() {
		isGPSEnabled = locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);
		isNetworkEnabled = locationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		if (isGPSEnabled || isNetworkEnabled)
			return true;
		return false;
	}

	public boolean findLocation(ILocationFinderCallback locationFinderCb) {

		callback = locationFinderCb;

		if (canFindLocation()) {
			if (isNetworkEnabled)
				locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER,
						MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES,
						this);
			if (isGPSEnabled)
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER,
						MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES,
						this);
			return true;
		}
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {

		if (!locationAccepted) {

			String latitude = "" + location.getLatitude();
			Utils.LogUtil.LogD(Constants.LOG_TAG, "latitude" + latitude);
			String longitude = "" + location.getLongitude();
			Utils.LogUtil.LogD(Constants.LOG_TAG, "longitude" + longitude);

			Geocoder gcd = new Geocoder(context, Locale.getDefault());
			List<Address> addresses = null;
			try {
				addresses = gcd.getFromLocation(location.getLatitude(),
						location.getLongitude(), 1);
				locationAccepted = true;
				locationManager.removeUpdates(this);
				Address addr = addresses.get(0);
				if (addr != null) {
					int i = 0;
					String finalAddress = "";
					while (i <= addr.getMaxAddressLineIndex()) {
						finalAddress = finalAddress + " "
								+ addr.getAddressLine(i);
						i++;
					}
					callback.onLocationFound(finalAddress.toString());
				}
			} catch (IOException e) {
				Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception : ", e);
				callback.onLocationError(-1);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

}
