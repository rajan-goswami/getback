package com.codeperf.getback.core;

public interface ILocationFinderCallback {

	public void onLocationFound(String address);

	public void onLocationError(int errorCode);

	public static enum ErrorCode {

	}
}
