package com.codeperf.getback.ui;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.codeperf.getback.R;
import com.suredigit.inappfeedback.FeedbackDialog;

public class AboutDialog extends DialogPreference {

	FeedbackDialog feedBackDialog = null;

	public AboutDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.about_screen);
	}

	@Override
	protected View onCreateDialogView() {
		return super.onCreateDialogView();
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		builder.setPositiveButton(null, null);
		builder.setNegativeButton(null, null);
		super.onPrepareDialogBuilder(builder);
	}

	@Override
	protected void onBindDialogView(View view) {

		Button btReport = (Button) view.findViewById(R.id.bt_report_bug);
		btReport.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				feedBackDialog = new FeedbackDialog((Activity) AboutDialog.this
						.getContext(), "AF-AEB115D7CD8F-38");
				feedBackDialog.show();
			}
		});

		Button btRate = (Button) view.findViewById(R.id.bt_rate_getback);
		btRate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id="
						+ AboutDialog.this.getContext().getPackageName()));
				AboutDialog.this.getContext().startActivity(intent);
			}
		});
		super.onBindDialogView(view);
	}

}
