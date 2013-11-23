package com.codeperf.getback.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.codeperf.getback.R;
import com.codeperf.getback.core.ISendEmailCallback;

public class MainActivity extends Activity implements ISendEmailCallback {

	private SurfaceView sv;

	private static final String LOG_TAG = MainActivity.class.getSimpleName();

	private WindowManager.LayoutParams params = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (/* Utils.isFrontCameraPresent(this) == */true) {

			// SurfaceView surfaceview = new SurfaceView(this);
			// WindowManager winMan = (WindowManager) this
			// .getSystemService(Context.WINDOW_SERVICE);
			// params = new WindowManager.LayoutParams(1, 1,
			// WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
			// WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
			// PixelFormat.TRANSLUCENT);
			// winMan.addView(surfaceview, params);
			//
			// surfaceview.setZOrderOnTop(true);
			//
			// SurfaceHolder holder = surfaceview.getHolder();
			//
			// holder.setFormat(PixelFormat.TRANSPARENT);

			// get the Surface View at the main.xml file
			// sv = (SurfaceView) findViewById(R.id.dummy_surface);

			// Get a surface
			// SurfaceHolder holder = sv.getHolder();

			// FrontCapture frontCapture = new FrontCapture(this, holder);

			// Intent intent = new Intent(this, GetBackCoreService.class);
			// startService(intent);

			startActivity(new Intent(this, HelpActivity.class));
			finish();


		} else {
			Log.w(LOG_TAG, "Front Camera is missing !!!");
		}
	}

	@Override
	public void onEmailSent() {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "Email sent");
	}

	@Override
	public void onEmailError(int errorCode) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "Email sent Error");
	}

}
