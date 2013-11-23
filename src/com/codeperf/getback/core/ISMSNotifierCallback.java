package com.codeperf.getback.core;

public interface ISMSNotifierCallback {

	public void onSmsSent(String receipient);

	public void onSMSError(int errorCode);

	public static enum ErrorCode {

	}
}
