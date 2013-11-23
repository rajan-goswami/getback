package com.codeperf.getback.core;

public interface IFrontCaptureCallback {

	public void onPhotoCaptured(String filePath);

	public void onCaptureError(int errorCode);

	public static enum ErrorCode {

	}
}
