package com.codeperf.getback.ui;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codeperf.getback.R;

public class AdPreference extends Preference {

	// AdView adView = null;

	public AdPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public AdPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AdPreference(Context context) {
		super(context);
	}

	@Override
	protected void onPrepareForRemoval() {

		// if (adView != null) {
		// final ViewGroup viewGroup = (ViewGroup) adView.getParent();
		// if (viewGroup != null) {
		// viewGroup.removeView(adView);
		// }
		// adView.removeAllViews();
		// adView.destroy();
		// }
		super.onPrepareForRemoval();
	}

	@Override
	protected View onCreateView(ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.admob_preference_layout, null);
	}

}
