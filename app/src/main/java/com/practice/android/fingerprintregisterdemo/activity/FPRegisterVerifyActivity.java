package com.practice.android.fingerprintregisterdemo.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.practice.android.fingerprintregisterdemo.R;
import com.practice.android.fingerprintregisterdemo.asynctask.AsyncFingerprint;
import com.practice.android.fingerprintregisterdemo.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;

import android_serialport_api.FingerprintAPI;

public class FPRegisterVerifyActivity extends BaseActivity implements OnClickListener {

    private AsyncFingerprint asyncFingerprint;

    private Spinner spinner;

    private Button register, validate;

    private ImageView fingerprintImage;

    private ProgressDialog progressDialog;

    private byte[] model;

    private ArrayList<FModel> arrayListFingers = new ArrayList<>();

    public static byte[] byteArrayTemp;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case AsyncFingerprint.SHOW_PROGRESSDIALOG:
                    cancelProgressDialog();
                    showProgressDialog((Integer) msg.obj);
                    break;
                case AsyncFingerprint.SHOW_FINGER_IMAGE:
                    showFingerImage(msg.arg1, (byte[]) msg.obj);
                    break;
                case AsyncFingerprint.SHOW_FINGER_MODEL:
                    FPRegisterVerifyActivity.this.model = (byte[]) msg.obj;
                    if (FPRegisterVerifyActivity.this.model != null) {
                        Log.i("whw", "#################model.length=" + FPRegisterVerifyActivity.this.model.length);
                    }
                    cancelProgressDialog();
                    break;
                case AsyncFingerprint.REGISTER_SUCCESS:
                    cancelProgressDialog();
                    if (msg.obj != null) {
                        Integer id = (Integer) msg.obj;
                        ToastUtil.showToast(FPRegisterVerifyActivity.this,
                                getString(R.string.register_success) + "  pageId=" + id);
                    } else {
                        ToastUtil.showToast(FPRegisterVerifyActivity.this, R.string.register_success);
                    }

                    break;
                case AsyncFingerprint.REGISTER_FAIL:
                    cancelProgressDialog();
                    ToastUtil.showToast(FPRegisterVerifyActivity.this, R.string.register_fail);
                    break;
                case AsyncFingerprint.VALIDATE_RESULT1:
                    cancelProgressDialog();
                    showValidateResult((Boolean) msg.obj);
                    break;
                case AsyncFingerprint.UP_IMAGE_RESULT:
                    cancelProgressDialog();
                    ToastUtil.showToast(FPRegisterVerifyActivity.this, (Integer) msg.obj);
                    break;
                default:
                    break;
            }
        }

    };

    private void showValidateResult(boolean matchResult) {
        if (matchResult) {
            ToastUtil.showToast(FPRegisterVerifyActivity.this, R.string.verifying_through);
        } else {
            ToastUtil.showToast(FPRegisterVerifyActivity.this, R.string.verifying_fail);
        }
    }

    private void showFingerImage(int fingerType, byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        fingerprintImage.setImageBitmap(bitmap);
        //ToastUtil.showToast(this, Arrays.toString(data));
        Log.i("MyTag", Arrays.toString(data));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_register_verify);

        initView();
        initViewListener();
        initData();
    }

    private void initView() {
        spinner = findViewById(R.id.spinner);
        register = findViewById(R.id.register);
        validate = findViewById(R.id.validate);
        fingerprintImage = findViewById(R.id.fingerprintImage);

    }

    private void initData() {
        String[] m = this.getResources().getStringArray(R.array.fingerprint_size);

        //ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new SpinnerSelectedListener());

    }

    class SpinnerSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
            Log.i("whw", "position=" + position);
            switch (position) {
                case 0:
                    asyncFingerprint.setFingerprintType(FingerprintAPI.SMALL_FINGERPRINT_SIZE);
                    break;
                default:
                    break;
            }
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    private void initData2() {
        asyncFingerprint = new AsyncFingerprint(handlerThread.getLooper(), mHandler);
    }

    private void initViewListener() {
        register.setOnClickListener(this);
        validate.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register:
                asyncFingerprint.setStop(false);
                asyncFingerprint.register();
                break;
            case R.id.validate:
                if (!arrayListFingers.isEmpty()) {
                    asyncFingerprint.validate(arrayListFingers);
                } else {
                    ToastUtil.showToast(FPRegisterVerifyActivity.this, R.string.first_register);
                }
                break;
            default:
                break;
        }
    }

    private void showProgressDialog(int resId) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(resId));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_BACK == keyCode) {
                    asyncFingerprint.setStop(true);
                }
                return false;
            }
        });

        progressDialog.show();
    }

    private void cancelProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
            progressDialog = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        cancelProgressDialog();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData2();
        Log.i("whw", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelProgressDialog();
        //   asyncFingerprint.setStop(true);
        Log.i("whw", "onPause");
    }


    private static int id = 0;

    public void btTemp(View view) {

        arrayListFingers.add(new FModel(id++, model));
        byteArrayTemp = model;
    }
}