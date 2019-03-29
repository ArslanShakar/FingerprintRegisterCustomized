package android_serialport_api;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.upek.android.ptapi.PtConnectionAdvancedI;
import com.upek.android.ptapi.PtConstants;
import com.upek.android.ptapi.PtException;
import com.upek.android.ptapi.PtGlobal;
import com.upek.android.ptapi.struct.PtInfo;
import com.upek.android.ptapi.struct.PtInputBir;
import com.upek.android.ptapi.struct.PtSessionCfgV5;

import java.io.File;

import android_serialport_api.FBI.FingerId;
import android_serialport_api.FBI.OpCapture;
import android_serialport_api.FBI.OpEnroll;
import android_serialport_api.FBI.OpGrab;
import android_serialport_api.FBI.OpNavigate;
import android_serialport_api.FBI.OpNavigateSettings;
import android_serialport_api.FBI.OpVerify;
import android_serialport_api.FBI.OpVerifyAll;

public class FBIFingerPrintAPI {
	private Context mContext;
	private Handler mHandler;
	private PtGlobal mPtGlobal = null;
	private static PtConnectionAdvancedI mConn = null;
	private PtInfo mSensorInfo = null;
	// This variable configures support for STM32 area reader. This sensor
	// requires additional data storage (temporary file)
	// On emulated environment (emulator + bridge) it must be set to zero, so
	// the default setting will be used
	// On real Android device the default place for storage doesn't work as it
	// must be detected in runtime
	// Set this variable to 1 to enable this behavior
	public static final int miRunningOnRealHardware = 1;
	// will contain path for temporary files on real Android device
	private String msNvmPath = null;
	private Thread mRunningOp = null;
	private final Object mCond = new Object();
	
	public static final int SHOW_MESSAGE = 0;
	public static final int GET_FINGER_IMAGE = 1;
	public static final int GET_FINGER_TEMPLATE = 2;
	public static final int GET_FINGER_TEMPLATE_ISO = 3;
	
	public FBIFingerPrintAPI(Context mContext, Handler mHandler) {
		this.mContext = mContext;
		this.mHandler = mHandler;
		if (miRunningOnRealHardware != 0) {
			// find the directory for temporary files
			if (mContext != null) {
				File aDir = mContext.getDir("tcstore", Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
				if (aDir != null) {
					msNvmPath = aDir.getAbsolutePath();
				}
			}
		}
	}

	/**
	 * Load PTAPI library and initialize its interface.
	 * 
	 * @return True, if library is ready for use.
	 */
	public boolean initializePtapi() {
		// Load PTAPI library
		mPtGlobal = new PtGlobal(mContext);
		try {
			// Initialize PTAPI interface
			// Note: If PtBridge development technology is in place and a
			// network
			// connection cannot be established, this call hangs forever.
			mPtGlobal.initialize();
			return true;
		} catch (UnsatisfiedLinkError ule) {
			// Library wasn't loaded properly during PtGlobal object
			// construction
			mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "libjniPtapi.so not loaded"));
			mPtGlobal = null;
			return false;

		} catch (PtException e) {
			mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, e.getMessage()));
			return false;
		}
	}

	/**
	 * Terminate PTAPI library.
	 */
	public boolean terminatePtapi() {
		try {
			if (mPtGlobal != null) {
				mPtGlobal.terminate();
			}
		} catch (PtException e) {
			// ignore errors
			return false;
		}
		mPtGlobal = null;
		return true;
	}
//	/**
//	 * Power on FBI model
//	 */
//	public void powerOn() {
//		try {
//			String m_GpioDev = "/sys/class/cw_gpios/printer_en/enable";
//			byte[] m_PowerOn = { '1' };
//			FileOutputStream fw = new FileOutputStream(m_GpioDev);
//			fw.write(m_PowerOn);
//			fw.close();
//
//			SystemClock.sleep(2000);
//		} catch (Exception ex) {
//			Log.e("zzd", "PowerOn failed!!!");
//		}
//	}
//	/**
//	 * Power off FBI model
//	 */
//	public boolean powerOff() {
//		try {
//			String m_GpioDev = "/sys/class/cw_gpios/printer_en/enable";
//			byte[] m_PowerOn = { '0' };
//			FileOutputStream fw = new FileOutputStream(m_GpioDev);
//			fw.write(m_PowerOn);
//			fw.close();
//		} catch (Exception ex) {
//			Log.e("zzd", "PowerOff failed!!!");
//			return false;
//		}
//		return true;
//	}

	/**
	 * Open PTAPI session.
	 */
	@SuppressWarnings("unused")
	public void openPtapiSession() {
		PtException openException = null;

		/*
		 * if(miUseSerialConnection != 0) { try { // Try to open session
		 * openPtapiSessionInternal(msDSNSerial); // Device successfully opened
		 * return; } catch (PtException e) { // Remember error and try remaining
		 * devices openException = e; } } else { // Walk through the most common
		 * DSN strings on USB for(int i=0; i<mDSN.length; i++) {
		 * PtDeviceListItem[] devices = null; // Enumerate devices try { devices
		 * = mPtGlobal.enumerateDevices(mDSN[i]); } catch (PtException e1) {
		 * if(e1.getCode() != PtException.PT_STATUS_INVALID_PARAMETER) {
		 * dislayMessage("Enumeration failed - " + e1.getMessage()); return; }
		 * 
		 * // Try to enumerate next DSN string continue; }
		 * 
		 * // Walk through enumerated devices and try to open them for(int d=0;
		 * d<devices.length; d++) { String dsn = devices[d].dsnSubString; try {
		 * // Try to open session openPtapiSessionInternal(dsn);
		 * 
		 * // Device successfully opened return; } catch (PtException e) { //
		 * Remember error and try remaining devices openException = e; } } } }
		 */

		try {
			// Try to open session
			openPtapiSessionInternal("");

			// Device successfully opened
			return;
		} catch (PtException e) {
			// Remember error and try remaining devices
			openException = e;
		}

		// No device has been opened
		if (openException == null) {
			mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "No device found"));
		} else {
			mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "Error during device opening - " + openException.getMessage()));
		}
	}
	/**
	 * Close PTAPI session
	 */
	public boolean closeSession() {
		if (mConn != null) {
			try {
				mConn.close();
			} catch (PtException e) {
				// Ignore errors
				return false;
			}
			mConn = null;
		}
		return true;
	}

	private void openPtapiSessionInternal(String dsn) throws PtException {
		// Try to open device with given DSN string
		/*
		 * try { PtUsbHost.PtUsbCheckDevice(aContext,0); } catch (PtException e)
		 * { throw e; }
		 */
		mConn = (PtConnectionAdvancedI) mPtGlobal.open(dsn);

		try {
			// Verify that emulated NVM is initialized and accessible
			mSensorInfo = mConn.info();
		} catch (PtException e) {

			if ((e.getCode() == PtException.PT_STATUS_EMULATED_NVM_INVALID_FORMAT)
					|| (e.getCode() == PtException.PT_STATUS_NVM_INVALID_FORMAT)
					|| (e.getCode() == PtException.PT_STATUS_NVM_ERROR)) {
				if (miRunningOnRealHardware != 0) {
					// try add storage configuration and reopen the device
					dsn += ",nvmprefix=" + msNvmPath + '/';
					// Reopen session
					mConn.close();
					mConn = null;

					mConn = (PtConnectionAdvancedI) mPtGlobal.open(dsn);
					try {
						// Verify that emulated NVM is initialized and
						// accessible
						mSensorInfo = mConn.info();
						configureOpenedDevice();
						return;
					} catch (PtException e2) {
						// ignore errors and continue
					}
				}

				// We have found the device, but it seems to be either opened
				// for the first time
				// or its emulated NVM was corrupted.
				// Perform the manufacturing procedure.
				// To properly initialize it, we have to:
				// 1. Format its emulated NVM storage
				// 2. Calibrate the sensor

				// Format internal NVM
				mConn.formatInternalNVM(0, null, null);

				// Reopen session
				mConn.close();
				mConn = null;

				mConn = (PtConnectionAdvancedI) mPtGlobal.open(dsn);

				// Verify that emulated NVM is initialized and accessible
				mSensorInfo = mConn.info();
				// check if sensor is calibrated
				if ((mSensorInfo.sensorType & PtConstants.PT_SENSORBIT_CALIBRATED) == 0) {
					// No, so calibrate it
					mConn.calibrate(PtConstants.PT_CALIB_TYPE_TURBOMODE_CALIBRATION);
					// Update mSensorInfo
					mSensorInfo = mConn.info();
				}

				// Device successfully opened
			} else {
				throw e;
			}
		}
		configureOpenedDevice();
	}

	private void configureOpenedDevice() throws PtException {
		PtSessionCfgV5 sessionCfg = (PtSessionCfgV5) mConn.getSessionCfgEx(PtConstants.PT_CURRENT_SESSION_CFG);
		sessionCfg.sensorSecurityMode = PtConstants.PT_SSM_DISABLED;
		sessionCfg.callbackLevel |= PtConstants.CALLBACKSBIT_NO_REPEATING_MSG;
		mConn.setSessionCfgEx(PtConstants.PT_CURRENT_SESSION_CFG, sessionCfg);
	}

	/**
	 * Destroy all
	 */
	public boolean destroyAll() {
		// Cancel running operation
		synchronized (mCond) {
			while (mRunningOp != null) {
				mRunningOp.interrupt();
				try {
					mCond.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		// Close PTAPI session
		if(!closeSession()){
			return false;
		}
		// Terminate PTAPI library
		if(!terminatePtapi()){
			return false;
		}
//		// Power off
//		if(!powerOff()){
//			return false;
//		}

		return true;
	}

	/**
	 * Enroll finger and return finger template.              
	 * 1.Get a finger template data without head by using PtInputBir.bir.data.
	 * 2.Get a finger template data include with head by using PtInputBir.bir.getPtDataFormat().
	 * Moreover,you can get headerVersion,type,formatOwner,formatID,quality,purpose,factorsMask and so on if you need.
	 * @param fingerId   Finger ID.
	 * @return Finger template of PtInputBir.
	 */
	public void enroll(int fingerId) {
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = new OpEnroll(mConn, fingerId) {
					protected void onFinished() {
						synchronized (mCond) {
							mRunningOp = null;
							mCond.notifyAll(); // notify onDestroy that operation has finished
						}
					}
					@Override
					protected void onDisplayMessage(String message) {
						mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, message));
					}
					@Override
					protected void onCallbackTemplate(PtInputBir template) {
//						mHandler.sendMessage(mHandler.obtainMessage(GET_FINGER_TEMPLATE, 0, 0, template));
					}
				};
				mRunningOp.start();
			}
		}
	}
	/**
	 * Verify finger print
	 */
	public void VerifyAll(){
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = new OpVerifyAll(mConn) {
					@Override
					protected void onDisplayMessage(String message) {
						mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, message));
					}

					@Override
					protected void onFinished() {
						synchronized (mCond) {
							mRunningOp = null;
							mCond.notifyAll(); // notify onDestroy that operation has finished
						}
					}
				};
				mRunningOp.start();
			}
		}
	}
	/**
	 * Verify finger print
	 */
	public void Verify(PtInputBir template){
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = new OpVerify(mConn,template) {
					@Override
					protected void onDisplayMessage(String message) {
						mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, message));
					}
					
					@Override
					protected void onFinished() {
						synchronized (mCond) {
							mRunningOp = null;
							mCond.notifyAll(); // notify onDestroy that operation has finished
						}
					}
				};
				mRunningOp.start();
			}
		}
	}
	
	/**
	 * Grab finger image and return Bitmap image
	 * @param mActivity
	 * @return Finger image
	 */
	public void grabImage(Activity mActivity){
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = new OpGrab(mConn, PtConstants.PT_GRAB_TYPE_THREE_QUARTERS_SUBSAMPLE,mActivity) {
					@Override
					protected void onDisplayMessage(String message) {
						mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, message));
					}
					@Override
					protected void onFinished() {
						Log.d("jokey", "onFinished");
						synchronized (mCond) {
							mRunningOp = null;
							mCond.notifyAll(); // notify onDestroy that operation has finished
						}
					}
					@Override
					protected void onCallbackImage(Bitmap mBitmap) {
						Log.d("jokey", "onCallbackImage");
						mHandler.sendMessage(mHandler.obtainMessage(GET_FINGER_IMAGE, 0, 0, mBitmap));
					}
				};
				mRunningOp.start();
			}
		}
	}
	/**
	 * Scan the live finger, process it into a fingerprint template and optionally return it to the caller. 
	 */
	public void capture(){
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = new OpCapture(mConn) {
					@Override
					protected void onFinished() {
						Log.d("jokey", "onFinished");
						synchronized (mCond) {
							mRunningOp = null;
							mCond.notifyAll(); // notify onDestroy that operation has finished
						}
					}
					@Override
					protected void onDisplayMessage(String message) {
						mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, message));
					}
					@Override
					protected void onCallbackTemplate(PtInputBir template) {
						Log.d("jokey", "onCallbackTemplate");
						mHandler.sendMessage(mHandler.obtainMessage(GET_FINGER_TEMPLATE, 0, 0, template));
					}
					@Override
					protected void onCallbackTemplateISO(byte[] templateISO) {
						Log.d("jokey", "onCallbackTemplateISO");
						mHandler.sendMessage(mHandler.obtainMessage(GET_FINGER_TEMPLATE_ISO, 0, 0, templateISO));
					}
				};
				mRunningOp.start();
			}
		}
	}
	/**
	 * Delete all finger.
	 * Delete all the fingerprint templates from the fingerprint database in FM's non-volatile memory.
	 * All additional public data assigned by PTSetFingerData() or PTSetFingerDataEx() are deleted
	 * too. deleted too.
	 */
	public void deleteAll(){
		synchronized (mCond) {
			if (mRunningOp == null) {
				try {
					// No interaction with a user needed
					mConn.deleteAllFingers();
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "All fingers deleted"));
				} catch (PtException e) {
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "Delete All failed - " + e.getMessage()));
				}
			}
		}
	}
	/**
	 * Delete finger by fingerId.
	 * Delete the given fingerprint template (identified by its slot number) from the fingerprint
	 * database in FM's non-volatile memory. Additional public data assigned to this slot by PTSetFingerData() 
	 * or PTSetFingerDataEx() are deleted too.
	 * @param fingerId 
	 * 				The slot number of the template to be associated with data.
	 */
	public void deletefinger(int fingerId){
		synchronized (mCond) {
			if (mRunningOp == null) {
				try {
					mConn.deleteFinger(fingerId);
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0,FingerId.NAMES[fingerId]+ "deleted"));
				} catch (PtException e) {
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "Delete finger failed - " + e.getMessage()));
				}
			}
		}
	}
	/**
	 * Assign an additional public data to a finger template stored in FM's fingerprint database
	 * @param fingerId
	 * 				The slot number of the template to be associated with data.
	 * @param data
	 * 				The data to be stored together with the template. 
	 */
	public void setFingerData(int fingerId,byte[] data){
		synchronized (mCond) {
			if (mRunningOp == null) {
				try {
					mConn.setFingerData(fingerId, data);
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "Success!"));
				} catch (PtException e) {
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "Failed - " + e.getMessage()));
				}
			}
		}
	}
	/**
	 * Read the additional public data associated with a finger template stored in FM's template database.
	 * @param fingerId
	 * 				The slot number of the template to be associated with data.
	 * @return The data to be stored together with the template. 
	 */
	public byte[] getFingerData(int fingerId){
		byte[] fingerData;
		synchronized (mCond) {
			if (mRunningOp == null) {
				try {
					fingerData = mConn.getFingerData(fingerId);
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "Success!"));
					return fingerData;
				} catch (PtException e) {
					mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, "Failed - " + e.getMessage()));
					return null;
				}
			}
		}
		return null;
	}
	
	public void navigationRaw(){
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = createNavigationOperationHelper(null);
				mRunningOp.start();
			}
		}
	}
	
	public void navigationMouse(){
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = createNavigationOperationHelper(OpNavigateSettings.createDefaultMousePostprocessingParams());
				mRunningOp.start();
			}
		}
	}
	
	public void navigationDiscrete(){
		synchronized (mCond) {
			if (mRunningOp == null) {
				mRunningOp = createNavigationOperationHelper(OpNavigateSettings.createDefaultDiscretePostprocessingParams());
				mRunningOp.start();
			}
		}
	}
	
	private OpNavigate createNavigationOperationHelper(OpNavigateSettings aSettings){
		OpNavigate aOperation = new OpNavigate(mConn, aSettings) {
			@Override
			protected void onDisplayMessage(String message) {
				mHandler.sendMessage(mHandler.obtainMessage(SHOW_MESSAGE, 0, 0, message));
			}

			@Override
			protected void onFinished() {
				synchronized (mCond) {
					mRunningOp = null;
					mCond.notifyAll(); // notify onDestroy that operation has
										// finished
				}
			}
		};
		return aOperation;
	}
	
	public PtConnectionAdvancedI getmConn() {
		return mConn;
	}

	public PtInfo getmSensorInfo() {
		return mSensorInfo;
	}

}
