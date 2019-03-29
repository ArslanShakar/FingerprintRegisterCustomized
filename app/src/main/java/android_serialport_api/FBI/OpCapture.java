package android_serialport_api.FBI;

import android.util.Log;


import com.upek.android.ptapi.PtConnectionI;
import com.upek.android.ptapi.PtConstants;
import com.upek.android.ptapi.PtException;
import com.upek.android.ptapi.callback.PtGuiStateCallback;
import com.upek.android.ptapi.resultarg.PtBirArg;
import com.upek.android.ptapi.struct.PtBir;
import com.upek.android.ptapi.struct.PtGuiSampleImage;
import com.upek.android.ptapi.struct.PtInputBir;
import com.upek.android.ptapi.struct.PtSessionCfgV5;

import utils.DataUtils;

public abstract class OpCapture extends Thread implements PtGuiStateCallback {
	private static short SESSION_CFG_VERSION = 5;
	private PtConnectionI mConn;
	public OpCapture(PtConnectionI conn) {
		super("CaptureThread" );
		mConn = conn;
	}
	
	/**
	 * Callback function called by PTAPI.
	 */
	public byte guiStateCallbackInvoke(int guiState, int message, byte progress, PtGuiSampleImage sampleBuffer,
			byte[] data) {
		String s = PtHelper.GetGuiStateCallbackMessage(guiState, message, progress);

		if (s != null) {
			onDisplayMessage(s);
		}

		// With sensor only solution isn't necessary because of PtCancel()
		// presence
		return isInterrupted() ? PtConstants.PT_CANCEL : PtConstants.PT_CONTINUE;
	}

	/**
	 * Cancel running operation
	 */
	@Override
	public void interrupt() {

		super.interrupt();

		try {
			mConn.cancel(0);
		} catch (PtException e) {
			// Ignore errors
		}
	}

	/**
	 * Enrollment execution code.
	 */
	@Override
	public void run() {
		try {
			// Optional: Set session configuration to enroll 3-5 swipes instead
			// of 5-10
			modifyEnrollmentType();
			// Obtain finger template
			PtInputBir template = capture();
			onCallbackTemplate(template);
			byte[] mIsoRawTemplate = mConn.convertTemplateEx(PtConstants.PT_TEMPLATE_TYPE_AUTO,
					PtConstants.PT_TEMPLATE_ENVELOPE_NONE, template.bir.data, PtConstants.PT_TEMPLATE_TYPE_ISO_FMR,
					PtConstants.PT_TEMPLATE_ENVELOPE_NONE, null, 0);
			Log.d("jokey", "ISO--->template"+DataUtils.toHexString(mIsoRawTemplate));
			onCallbackTemplateISO(mIsoRawTemplate);
		} catch (PtException e) {
			// Errors reported in nested methods
			if (e.getCode() == PtException.PT_STATUS_OPERATION_CANCELED) {
			}
		}

		// Un-cancel session
		try {
			mConn.cancel(1);
		} catch (PtException e1) {
		}

		onFinished();
	}

	/**
	 * Simple conversion PtBir to PtInputBir
	 */
	private static PtInputBir MakeInputBirFromBir(PtBir aBir) {
		PtInputBir aInputBir = new PtInputBir();
		aInputBir.form = PtConstants.PT_FULLBIR_INPUT;
		aInputBir.bir = aBir;
		return aInputBir;
	}
	
	/**
	 * Modify enrollment to 3-5 swipes.
	 */
	private void modifyEnrollmentType() throws PtException {
		try {
			PtSessionCfgV5 sessionCfg = (PtSessionCfgV5) mConn.getSessionCfgEx(SESSION_CFG_VERSION);
			sessionCfg.enrollMinTemplates = (byte) 3;
			sessionCfg.enrollMaxTemplates = (byte) 5;
			mConn.setSessionCfgEx(SESSION_CFG_VERSION, sessionCfg);
		} catch (PtException e) {
			onDisplayMessage("Unable to set session cfg - " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Obtain finger template.
	 */
	private PtInputBir capture() throws PtException {
		PtBirArg newTemplate = new PtBirArg();

		try {
			// Register notification callback of operation state
			// Valid for entire PTAPI session lifetime
			mConn.setGUICallbacks(null, this);
			//Scan the live finger, process it into a fingerprint template and optionally return it to the caller.
			mConn.capture(PtConstants.PT_PURPOSE_ENROLL, newTemplate, PtConstants.PT_BIO_INFINITE_TIMEOUT, null, null, null);
			
		} catch (PtException e) {
			onDisplayMessage("Enrollment failed - " + e.getMessage());
			throw e;
		}

		// Convert obtained BIR to INPUT BIR class
		return MakeInputBirFromBir(newTemplate.value);
	}

	/**
	 * Display message. To be overridden by sample activity.
	 * 
	 * @param message
	 *            Message text.
	 */
	abstract protected void onDisplayMessage(String message);

	/**
	 * Called, if operation is finished.
	 * 
	 * @param message
	 *            Message text.
	 */
	abstract protected void onFinished();
	/**
	 * Called,if template is grabbed.
	 * @param template  
	 * 				finger template
	 */
	abstract protected void onCallbackTemplate(PtInputBir template);
	/**
	 * Called,if template is grabbed.
	 * @param template  
	 * 				ISO template
	 */
	abstract protected void onCallbackTemplateISO(byte[] templateISO);
}
