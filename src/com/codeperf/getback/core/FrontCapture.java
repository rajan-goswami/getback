package com.codeperf.getback.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;

public class FrontCapture implements SurfaceHolder.Callback, PictureCallback,
		ErrorCallback {

	private Context context = null;

	// a surface holder
	private SurfaceHolder sHolder;

	private static Camera camera;

	private boolean storeOnSdCard = false;

	private Parameters parameters;

	private AudioManager audioMgr = null;

	private WindowManager.LayoutParams params = null;

	private IFrontCaptureCallback callback;

	public FrontCapture(Context ctx) {
		context = ctx;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
			audioMgr = (AudioManager) context
					.getSystemService(Context.AUDIO_SERVICE);
	}

	@Override
	public void onError(int error, Camera camera) {
		Utils.LogUtil.LogE(Constants.LOG_TAG, "Camera Error : " + error, null);
		callback.onCaptureError(-1);
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
			audioMgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);

		String photoPath = null;
		if (storeOnSdCard)
			photoPath = storePhotoOnSdcard(data);
		else
			photoPath = storePhotoPrivate(data);

		callback.onPhotoCaptured(photoPath);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// get camera parameters
		parameters = camera.getParameters();

		// set camera parameters
		camera.setParameters(parameters);
		camera.startPreview();

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
			audioMgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Taking picture");

		try {
			Thread.sleep(4000);
			camera.takePicture(null, null, this);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			Utils.LogUtil.LogD(Constants.LOG_TAG, "Camera Opened");
			camera.setPreviewDisplay(sHolder);
		} catch (IOException exception) {
			camera.release();
			camera = null;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		// stop the preview
		camera.stopPreview();

		// release the camera
		camera.release();
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Camera released");

		// unbind the camera from this object
		camera = null;
	}

	private String storePhotoPrivate(byte[] data) {
		FileOutputStream fos = null;
		String filePath = "";
		try {
			String fileName = getUniquePhotoFileName();
			fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			fos.write(data);
			filePath = context.getFilesDir() + File.separator + fileName;
		} catch (Throwable t) {
			Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception", t);
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception", e);
			}
		}

		return filePath;
	}

	private static String getUniquePhotoFileName() {
		String photoFile = "IMG_"
				+ new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
						.format(new Date()) + ".jpg";
		return photoFile;
	}

	private String storePhotoOnSdcard(byte[] data) {
		String applicationName = Utils.getApplicationName(context);
		if (applicationName == null)
			applicationName = "GetBack";

		File sdDir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES
						+ File.separator + applicationName);

		if (!sdDir.exists() && !sdDir.mkdirs()) {
			Utils.LogUtil.LogD(Constants.LOG_TAG,
					"Can't create directory to save image.");
			return "";
		}
		String photoFilePath = sdDir.getPath() + File.separator
				+ getUniquePhotoFileName();
		File photoPath = new File(photoFilePath);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(photoPath);
			fos.write(data);
			Utils.LogUtil.LogD(Constants.LOG_TAG, "New Image saved:"
					+ photoFilePath);
		} catch (Exception error) {
			Utils.LogUtil.LogE(Constants.LOG_TAG, "File" + photoFilePath
					+ "not saved. ", error);
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
			}
		}
		return photoFilePath;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private void shutterSoundOffLatest() {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info);
		if (info.canDisableShutterSound) {
			camera.enableShutterSound(false);
		}
	}

	public void capturePhoto(IFrontCaptureCallback frontCaptureCb) {

		callback = frontCaptureCb;

		if (!Utils.isFrontCameraPresent(context))
			callback.onCaptureError(-1);

		SurfaceView surfaceview = new SurfaceView(context);
		WindowManager winMan = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		params = new WindowManager.LayoutParams(1, 1,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		winMan.addView(surfaceview, params);

		surfaceview.setZOrderOnTop(true);

		SurfaceHolder holder = surfaceview.getHolder();

		holder.setFormat(PixelFormat.TRANSPARENT);

		sHolder = holder;
		sHolder.addCallback(this);
		sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Opening Camera");

		// The Surface has been created, acquire the camera
		camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
	}

}
