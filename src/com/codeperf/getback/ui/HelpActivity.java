package com.codeperf.getback.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import com.codeperf.getback.R;
import com.codeperf.getback.common.Utils;

public class HelpActivity extends Activity {
	WebView webView = null;;

	private static int helpPageCounter = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Utils.isHelpEnableAtStartup(this)) {

			this.requestWindowFeature(Window.FEATURE_NO_TITLE);

			setContentView(R.layout.help_activity);

			helpPageCounter = 1;

			webView = (WebView) findViewById(R.id.help_web_view);

			// WebSettings webSettings = webView.getSettings();
			// webSettings.setBuiltInZoomControls(true);

			webView.setWebViewClient(new Callback());
			webView.loadUrl("file:///android_asset/help_1.html");
			webView.setWillNotCacheDrawing(true);

			ImageButton skipBt = (ImageButton) findViewById(R.id.bt_help_skip);
			skipBt.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					startActivity(new Intent(HelpActivity.this,
							GetBackPreferenceActivity.class));
					finish();
				}
			});

			// ImageButton backBt = (ImageButton)
			// findViewById(R.id.bt_help_back);
			// backBt.setOnClickListener(new OnClickListener() {
			//
			// @Override
			// public void onClick(View v) {
			// helpPageCounter--;
			// if (helpPageCounter == 1) {
			// webView.loadUrl("file:///android_asset/help_1.html");
			// } else if (helpPageCounter == 2) {
			// webView.loadUrl("file:///android_asset/help_2.html");
			// }
			// }
			// });

			ImageButton forwardBt = (ImageButton) findViewById(R.id.bt_help_forward);
			forwardBt.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					helpPageCounter++;
					if (helpPageCounter == 2) {
						webView.loadUrl("file:///android_asset/help_2.html");
					} else if (helpPageCounter == 3) {
						webView.loadUrl("file:///android_asset/help_3.html");
					} else if (helpPageCounter == 4) {
						startActivity(new Intent(HelpActivity.this,
								GetBackPreferenceActivity.class));
						finish();
					}
				}
			});

			// Hide add space if no network
//			if (!Utils.getConnectivityStatus(this)) {
//				View adView = findViewById(R.id.ad);
//				adView.setVisibility(View.GONE);
//			}
		} else {
			startActivity(new Intent(HelpActivity.this,
					GetBackPreferenceActivity.class));
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (webView != null) {
			final ViewGroup viewGroup = (ViewGroup) webView.getParent();
			if (viewGroup != null) {
				viewGroup.removeView(webView);
			}
			webView.removeAllViews();
			webView.clearHistory();
			webView.destroy();
			webView = null;
		}

	}

	@Override
	public void onBackPressed() {

		helpPageCounter--;
		if (helpPageCounter == 1) {
			webView.loadUrl("file:///android_asset/help_1.html");
		} else if (helpPageCounter == 2) {
			webView.loadUrl("file:///android_asset/help_2.html");
		} else
			super.onBackPressed();
	}

	private class Callback extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return (false);
		}
	}
}
