package com.practice.android.fingerprintregisterdemo.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
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
import com.practice.android.fingerprintregisterdemo.asynctask.AsyncFingerprint.OnCalibrationListener;
import com.practice.android.fingerprintregisterdemo.asynctask.AsyncFingerprint.OnEmptyListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android_serialport_api.FingerprintAPI;
import com.practice.android.fingerprintregisterdemo.utils.ToastUtil;

public class RegisterVerifyActivity extends BaseActivity implements OnClickListener {

    private AsyncFingerprint asyncFingerprint;

    private Spinner spinner;

    private Button register, validate, register2, validate2, clear, calibration, back;

    private ImageView fingerprintImage;

    private ProgressDialog progressDialog;

    private byte[] model;

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
                    RegisterVerifyActivity.this.model = (byte[]) msg.obj;
                    if (RegisterVerifyActivity.this.model != null) {
                        Log.i("whw", "#################model.length=" + RegisterVerifyActivity.this.model.length);
                    }
                    cancelProgressDialog();
                    break;
                case AsyncFingerprint.REGISTER_SUCCESS:
                    cancelProgressDialog();
                    if (msg.obj != null) {
                        Integer id = (Integer) msg.obj;
                        ToastUtil.showToast(RegisterVerifyActivity.this,
                                getString(R.string.register_success) + "  pageId=" + id);
                    } else {
                        ToastUtil.showToast(RegisterVerifyActivity.this, R.string.register_success);
                    }

                    break;
                case AsyncFingerprint.REGISTER_FAIL:
                    cancelProgressDialog();
                    ToastUtil.showToast(RegisterVerifyActivity.this, R.string.register_fail);
                    break;
                case AsyncFingerprint.VALIDATE_RESULT1:
                    cancelProgressDialog();
                    showValidateResult((Boolean) msg.obj);
                    break;
                case AsyncFingerprint.VALIDATE_RESULT2:
                    cancelProgressDialog();
                    Integer r = (Integer) msg.obj;
                    if (r != -1) {
                        ToastUtil.showToast(RegisterVerifyActivity.this,
                                getString(R.string.verifying_through) + "  pageId=" + r);
                    } else {
                        showValidateResult(false);
                    }
                    break;
                case AsyncFingerprint.UP_IMAGE_RESULT:
                    cancelProgressDialog();
                    ToastUtil.showToast(RegisterVerifyActivity.this, (Integer) msg.obj);
                    break;
                case AsyncFingerprint.VERIFYMY:
                    cancelProgressDialog();
                    ToastUtil.showToast(RegisterVerifyActivity.this, (Integer) msg.obj + "");
                    break;
                default:
                    break;
            }
        }

    };

    private void showValidateResult(boolean matchResult) {
        if (matchResult) {
            ToastUtil.showToast(RegisterVerifyActivity.this, R.string.verifying_through);
        } else {
            ToastUtil.showToast(RegisterVerifyActivity.this, R.string.verifying_fail);
        }
    }

    private void showFingerImage(int fingerType, byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        fingerprintImage.setImageBitmap(bitmap);
        writeToFile(data);
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
        register2 = findViewById(R.id.register2);
        validate2 = findViewById(R.id.validate2);
        clear = findViewById(R.id.clear_flash);
        calibration = findViewById(R.id.calibration);
        back = findViewById(R.id.backRegister);

        fingerprintImage = findViewById(R.id.fingerprintImage);

    }

    private String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();

    private void writeToFile(byte[] data) {
        String dir = rootPath + "/fingerprint_image";
        File dirPath = new File(dir);
        if (!dirPath.exists()) {
            dirPath.mkdir();
        }

        String filePath = dir + "/" + System.currentTimeMillis() + ".bmp";
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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

        asyncFingerprint.setOnEmptyListener(new OnEmptyListener() {

            @Override
            public void onEmptySuccess() {
                ToastUtil.showToast(RegisterVerifyActivity.this, R.string.clear_flash_success);

            }

            @Override
            public void onEmptyFail() {
                ToastUtil.showToast(RegisterVerifyActivity.this, R.string.clear_flash_fail);

            }
        });

        asyncFingerprint.setOnCalibrationListener(new OnCalibrationListener() {
            @Override
            public void onCalibrationSuccess() {
                Log.i("whw", "onCalibrationSuccess");
                ToastUtil.showToast(RegisterVerifyActivity.this, R.string.calibration_success);
            }

            @Override
            public void onCalibrationFail() {
                Log.i("whw", "onCalibrationFail");
                ToastUtil.showToast(RegisterVerifyActivity.this, R.string.calibration_fail);
            }
        });

    }

    private void initViewListener() {
        register.setOnClickListener(this);
        validate.setOnClickListener(this);
        register2.setOnClickListener(this);
        validate2.setOnClickListener(this);
        calibration.setOnClickListener(this);
        clear.setOnClickListener(this);
        back.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register:
                asyncFingerprint.setStop(false);
                asyncFingerprint.register();
                break;
            case R.id.validate:
                if (model != null) {
                    asyncFingerprint.validate(model);
                } else {
                    ToastUtil.showToast(RegisterVerifyActivity.this, R.string.first_register);
                }
                break;
            case R.id.register2:
                asyncFingerprint.register2();
                break;
            case R.id.validate2:
                asyncFingerprint.validate2();
                break;
            case R.id.calibration:
                Log.i("whw", "calibration start");
                asyncFingerprint.PS_Calibration();
                break;
            case R.id.clear_flash:
                asyncFingerprint.PS_Empty();
                break;
            case R.id.backRegister:
                finish();
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
        asyncFingerprint.setStop(true);
        Log.i("whw", "onPause");
    }
}