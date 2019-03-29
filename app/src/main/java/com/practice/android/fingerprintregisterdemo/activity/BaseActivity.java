package com.practice.android.fingerprintregisterdemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;

import android_serialport_api.SerialPortManager;

public class BaseActivity extends Activity {
	protected MyApplication application;
	protected HandlerThread handlerThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		application = (MyApplication) getApplicationContext();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!SerialPortManager.getInstance().isOpen()) {
			SerialPortManager.getInstance().openSerialPort();
		}
		Log.i("whw", "onResume=" + SerialPortManager.getInstance().isOpen());
		handlerThread = application.getHandlerThread();
	}

	@Override
	protected void onPause() {
		super.onPause();
		SerialPortManager.getInstance().closeSerialPort();
		handlerThread = null;
	}
}