package com.practice.android.fingerprintregisterdemo.asynctask;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.practice.android.fingerprintregisterdemo.R;
import com.practice.android.fingerprintregisterdemo.activity.FModel;

import java.util.ArrayList;

import android_serialport_api.FingerprintAPI;

public class AsyncFingerprint extends Handler {
    public static final int VERIFYMY = 100001;
    private static final int PS_GetImage = 0x01;
    private static final int PS_GenChar = 0x02;
    private static final int PS_Match = 0x03;
    private static final int PS_RegModel = 0x05;
    private static final int PS_StoreChar = 0x06;
    private static final int PS_LoadChar = 0x07;
    private static final int PS_UpChar = 0x08;

    private static final int PS_DownChar = 0x09;
    private static final int PS_UpImage = 0x0a;
    private static final int PS_DownImage = 0x0b;
    private Handler mWorkerThreadHandler;
    private static final int REGISTER = 0x12;
    private static final int VALIDATE = 0x13;

    public static final int SHOW_PROGRESSDIALOG = 1;
    public static final int SHOW_FINGER_IMAGE = 2;
    public static final int SHOW_FINGER_MODEL = 3;
    public static final int REGISTER_SUCCESS = 4;
    public static final int REGISTER_FAIL = 5;
    public static final int VALIDATE_RESULT1 = 6;
    public static final int VALIDATE_RESULT_STORE = 7;
    public static final int UP_IMAGE_RESULT = 8;
    public static final int UP_IMAGE_FAIL = 9;

    private FingerprintAPI fingerprint;

    public AsyncFingerprint(Looper looper, Handler mHandler) {
        this.mHandler = mHandler;
        createHandler(looper);
        fingerprint = new FingerprintAPI();
    }

    public void setFingerprintType(int type) {
        fingerprint.setFingerprintType(type);
    }

    private Handler createHandler(Looper looper) {
        return mWorkerThreadHandler = new WorkerHandler(looper);
    }

    private Handler mHandler;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case PS_GetImage:
                if (onGetImageListener == null) {
                    return;
                }
                if (msg.arg1 == 0) {
                    onGetImageListener.onGetImageSuccess();
                } else {
                    onGetImageListener.onGetImageFail();
                }
                break;
            case PS_UpImage:
                if (onUpImageListener == null) {
                    return;
                }
                if (msg.obj != null) {
                    onUpImageListener.onUpImageSuccess((byte[]) msg.obj);
                } else {
                    onUpImageListener.onUpImageFail();
                }
                break;
            case PS_DownImage:
                if (onDownImageListener == null) {
                    return;
                } else {
                    if (msg.arg1 == 0) {
                        onDownImageListener.onDownImageSuccess();
                    } else {
                        onDownImageListener.onDownImageFail();
                    }
                }
                break;
            case PS_GenChar:
                if (onGenCharListener == null) {
                    return;
                } else {
                    if (msg.arg1 == 0) {
                        onGenCharListener.onGenCharSuccess(msg.arg2);
                    } else {
                        onGenCharListener.onGenCharFail();
                    }
                }
                break;
            case PS_RegModel:
                if (onRegModelListener == null) {
                    return;
                } else {
                    if (msg.arg1 == 0) {
                        onRegModelListener.onRegModelSuccess();
                    } else {
                        onRegModelListener.onRegModelFail();
                    }
                }
                break;
            case PS_UpChar:
                if (onUpCharListener == null) {
                    return;
                } else {
                    if (msg.obj != null) {
                        onUpCharListener.onUpCharSuccess((byte[]) msg.obj);
                    } else {
                        onUpCharListener.onUpCharFail();
                    }
                }
                break;
            case PS_DownChar:
                if (onDownCharListener == null) {
                    return;
                } else {
                    if (msg.arg1 == 0) {
                        onDownCharListener.onDownCharSuccess();
                    } else {
                        onDownCharListener.onDownCharFail();
                    }
                }
                break;
            case PS_Match:
                if (onMatchListener == null) {
                    return;
                } else {
                    if ((Boolean) msg.obj) {
                        onMatchListener.onMatchSuccess();
                    } else {
                        onMatchListener.onMatchFail();
                    }
                }
                break;
            case PS_StoreChar:
                if (onStoreCharListener == null) {
                    return;
                } else {
                    if (msg.arg1 == 0) {
                        onStoreCharListener.onStoreCharSuccess();
                    } else {
                        onStoreCharListener.onStoreCharFail();
                    }
                }
                break;
            case PS_LoadChar:
                if (onLoadCharListener == null) {
                    return;
                } else {
                    if (msg.arg1 == 0) {
                        onLoadCharListener.onLoadCharSuccess();
                    } else {
                        onLoadCharListener.onLoadCharFail();
                    }
                }
                break;

            default:
                break;
        }
    }

    protected class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (isStop) {
                return;
            }
            switch (msg.what) {
                case PS_GetImage:
                    int valueGetImage = fingerprint.PSGetImage();
                    AsyncFingerprint.this.obtainMessage(PS_GetImage, valueGetImage, -1).sendToTarget();
                    break;
                case PS_UpImage:
                    byte[] imageData = fingerprint.PSUpImage();
                    AsyncFingerprint.this.obtainMessage(PS_UpImage, imageData).sendToTarget();
                    break;
                case PS_DownImage:
                    int valueDownImage = fingerprint.PSDownImage((byte[]) msg.obj);
                    AsyncFingerprint.this.obtainMessage(PS_DownImage, valueDownImage, -1).sendToTarget();
                    break;
                case PS_GenChar:
                    int valueGenChar = fingerprint.PSGenChar(msg.arg1);
                    AsyncFingerprint.this.obtainMessage(PS_GenChar, valueGenChar, msg.arg1).sendToTarget();
                    break;
                case PS_RegModel:
                    int valueRegModel = fingerprint.PSRegModel();
                    AsyncFingerprint.this.obtainMessage(PS_RegModel, valueRegModel, -1).sendToTarget();
                    break;
                case PS_UpChar:
                    byte[] charData = fingerprint.PSUpChar(2);
                    AsyncFingerprint.this.obtainMessage(PS_UpChar, charData).sendToTarget();
                    break;
                case PS_DownChar:
                    int valueDownChar = fingerprint.PSDownChar(msg.arg1, (byte[]) msg.obj);
                    AsyncFingerprint.this.obtainMessage(PS_DownChar, valueDownChar, -1).sendToTarget();
                    break;
                case PS_Match:
                    boolean valueMatch = fingerprint.PSMatch();
                    AsyncFingerprint.this.obtainMessage(PS_Match, Boolean.valueOf(valueMatch)).sendToTarget();
                    break;
                case PS_StoreChar:
                    int valueStoreChar = fingerprint.PSStoreChar(msg.arg1, msg.arg2);
                    AsyncFingerprint.this.obtainMessage(PS_StoreChar, valueStoreChar, -1).sendToTarget();
                    break;
                case PS_LoadChar:
                    int valueLoadChar = fingerprint.PSLoadChar(msg.arg1, msg.arg2);
                    AsyncFingerprint.this.obtainMessage(PS_LoadChar, valueLoadChar, -1).sendToTarget();
                    break;

                case REGISTER:
                    boolean isSuccess = registerFinger();
                    if (isSuccess) {
                        mHandler.sendEmptyMessage(REGISTER_SUCCESS);
                    } else {
                        mHandler.sendEmptyMessage(REGISTER_FAIL);
                    }
                    break;
                case VALIDATE:
                    //Single verification
                    /*byte[] data = (byte[]) msg.obj;
                    boolean match = validateFinger(data);*/

                    ArrayList<FModel> arrayList = (ArrayList<FModel>) msg.obj;
                    boolean match = validateFinger(arrayList);
                    mHandler.obtainMessage(VALIDATE_RESULT1, match).sendToTarget();
                    break;

                default:
                    break;
            }
        }
    }

    private OnGetImageListener onGetImageListener;

    private OnUpImageListener onUpImageListener;

    private OnDownImageListener onDownImageListener;

    private OnGenCharListener onGenCharListener;

    private OnRegModelListener onRegModelListener;

    private OnUpCharListener onUpCharListener;

    private OnDownCharListener onDownCharListener;

    private OnMatchListener onMatchListener;

    private OnStoreCharListener onStoreCharListener;

    private OnLoadCharListener onLoadCharListener;


    public interface OnGetImageListener {
        void onGetImageSuccess();

        void onGetImageFail();
    }

    public interface OnUpImageListener {
        void onUpImageSuccess(byte[] data);

        void onUpImageFail();
    }

    public interface OnDownImageListener {
        void onDownImageSuccess();

        void onDownImageFail();
    }

    public interface OnGenCharListener {
        void onGenCharSuccess(int bufferId);

        void onGenCharFail();
    }

    public interface OnRegModelListener {
        void onRegModelSuccess();

        void onRegModelFail();
    }

    public interface OnUpCharListener {
        void onUpCharSuccess(byte[] model);

        void onUpCharFail();
    }

    public interface OnDownCharListener {
        void onDownCharSuccess();

        void onDownCharFail();
    }

    public interface OnMatchListener {
        void onMatchSuccess();

        void onMatchFail();
    }

    public interface OnStoreCharListener {
        void onStoreCharSuccess();

        void onStoreCharFail();
    }

    public interface OnLoadCharListener {
        void onLoadCharSuccess();

        void onLoadCharFail();
    }

    public void PS_GetImage() {
        mWorkerThreadHandler.sendEmptyMessage(PS_GetImage);
    }

    public void PS_UpImage() {
        mWorkerThreadHandler.sendEmptyMessage(PS_UpImage);
    }

    public void PS_DownImage(byte[] image) {
        mWorkerThreadHandler.obtainMessage(PS_DownImage, image).sendToTarget();
    }

    public void PS_GenChar(int bufferId) {
        mWorkerThreadHandler.obtainMessage(PS_GenChar, bufferId, -1).sendToTarget();
    }

    public void PS_RegModel() {
        mWorkerThreadHandler.sendEmptyMessage(PS_RegModel);
    }

    public void PS_UpChar() {
        mWorkerThreadHandler.sendEmptyMessage(PS_UpChar);
    }

    public void PS_DownChar(int bufferId, byte[] model) {
        mWorkerThreadHandler.obtainMessage(PS_DownChar, bufferId, -1, model).sendToTarget();
    }

    public void PS_Match() {
        mWorkerThreadHandler.sendEmptyMessage(PS_Match);
    }


    public boolean isStop;

    public void setStop(boolean isStop) {
        this.isStop = isStop;
    }

    public void register() {
        mWorkerThreadHandler.sendEmptyMessage(REGISTER);
    }

    public void validate() {
        mWorkerThreadHandler.sendEmptyMessage(VALIDATE);
    }

    private boolean registerFinger() {
        isStop = false;
        for (int i = 1; i < 3; i++) {
            if (i == 1) {
                mHandler.obtainMessage(SHOW_PROGRESSDIALOG, R.string.print_finger).sendToTarget();
            } else {
                mHandler.obtainMessage(SHOW_PROGRESSDIALOG, R.string.print_finger_again).sendToTarget();
            }
            int getImage = -1;
            do {
                if (!isStop) {
                    getImage = fingerprint.PSGetImage();
                } else {
                    return false;
                }
            } while (getImage != 0x00);
            mHandler.obtainMessage(SHOW_PROGRESSDIALOG, R.string.processing).sendToTarget();
            byte[] image = fingerprint.PSUpImage();
            if (image == null) {
                mHandler.obtainMessage(UP_IMAGE_RESULT, R.string.up_image_fail).sendToTarget();
                return false;
            }
            mHandler.obtainMessage(SHOW_FINGER_IMAGE, image).sendToTarget();
            int genChar = fingerprint.PSGenChar(i);
            if (genChar != 0x00) {
                return false;
            }
        }
        int regModel = fingerprint.PSRegModel();
        if (regModel != 0x00) {
            return false;
        }
        byte[] model = fingerprint.PSUpChar(2);
        if (model == null) {
            return false;
        }
        Log.i("whw", "model length=" + model.length);
        mHandler.obtainMessage(SHOW_FINGER_MODEL, model).sendToTarget();
        return true;
    }


    public void validate(ArrayList<FModel> arrayList) {
        mWorkerThreadHandler.obtainMessage(VALIDATE, arrayList).sendToTarget();
    }

    private boolean validateFinger(ArrayList<FModel> arrayList) {
        isStop = false;
        mHandler.obtainMessage(SHOW_PROGRESSDIALOG, R.string.print_finger).sendToTarget();
        int getImage = -1;
        do {
            if (!isStop) {
                getImage = fingerprint.PSGetImage();
            } else {
                isStop = false;
                return false;
            }
        } while (getImage != 0x00);
        mHandler.obtainMessage(SHOW_PROGRESSDIALOG, R.string.processing).sendToTarget();
        byte[] image = fingerprint.PSUpImage();
        if (image == null) {
            return false;
        }
        mHandler.obtainMessage(SHOW_FINGER_IMAGE, image).sendToTarget();
        int genChar = fingerprint.PSGenChar(1);
        if (genChar != 0x00) {
            return false;
        }

        boolean match = false;
        for (int i = 0; i < arrayList.size(); i++) {
            FModel data = arrayList.get(i);
            int downChar = fingerprint.PSDownChar(2, data.getModel());
            if (downChar != 0x00) {
                return false;
            }
            match = fingerprint.PSMatch();
            if (match) {
                break;
            }
        }
        return match;
    }


}