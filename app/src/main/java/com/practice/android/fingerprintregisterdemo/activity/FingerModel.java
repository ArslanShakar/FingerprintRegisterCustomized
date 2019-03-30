package com.practice.android.fingerprintregisterdemo.activity;

import org.litepal.crud.DataSupport;

public class FingerModel extends DataSupport {
	private String model_ID;
	private byte[] model;

	public String getId() {
		return model_ID;
	}

	public void setId(String id) {
		this.model_ID = id;
	}

	public byte[] getModel() {
		return model;
	}

	public void setModel(byte[] model) {
		this.model = model;
	}
}