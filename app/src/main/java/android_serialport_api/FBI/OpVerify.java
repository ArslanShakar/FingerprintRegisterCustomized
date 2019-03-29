package android_serialport_api.FBI;

import com.upek.android.ptapi.PtConnectionI;
import com.upek.android.ptapi.PtConstants;
import com.upek.android.ptapi.PtException;
import com.upek.android.ptapi.callback.PtGuiStateCallback;
import com.upek.android.ptapi.struct.PtGuiSampleImage;
import com.upek.android.ptapi.struct.PtInputBir;

public abstract class OpVerify extends Thread implements PtGuiStateCallback {

	private PtConnectionI mConn;
	private PtInputBir mTemplate;
	public OpVerify(PtConnectionI conn, PtInputBir template) {
		super("VerifyAllThread");
		mConn = conn;
		mTemplate = template;
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
	 * Operation execution code.
	 */
	@Override
	public void run() {
		try {
			// Register notification callback of operation state
			// Valid for entire PTAPI session lifetime
			mConn.setGUICallbacks(null, this);

			boolean index = mConn.verify(null, null, null, mTemplate, null, null, null, null, PtConstants.PT_BIO_INFINITE_TIMEOUT,
					true, null, null, null);
			if (index) {
				onDisplayMessage("Finger matched");
			} else {
				onDisplayMessage("No match found.");
			}

		} catch (PtException e) {
			onDisplayMessage("Verification failed - " + e.getMessage());
		}

		onFinished();
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

}
