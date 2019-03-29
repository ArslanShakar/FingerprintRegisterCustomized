package io;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.HashMap;
import java.util.Iterator;

public class USBAPI implements InterfaceAPI{
	private Context mActivity = null;
	  private UsbManager mUsbManager = null;
	  private UsbDevice mDevice = null;
	  private UsbDeviceConnection mUsbDeviceConnection = null;
	  private PendingIntent mPermissionIntent = null;
	  private UsbEndpoint mEndpointIn = null;
	  private UsbEndpoint mEndpointOut = null;

	  private UsbCheckSRT[] mUsbCheckSRT = { new UsbCheckSRT(1155, 30016), 
	    new UsbCheckSRT(1157, 30017), new UsbCheckSRT(6790, 30084), 
	    new UsbCheckSRT(3544, 5120), new UsbCheckSRT(483, 7540) };
	  private static final String ACTION_USB_PERMISSION = "com.szsicod.print.USB_PERMISSION";
	  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	      String action = intent.getAction();
	      if ("com.szsicod.print.USB_PERMISSION".equals(action)) {
	        synchronized (this) {
	          UsbDevice device = 
	            (UsbDevice)intent
	            .getParcelableExtra("device");
	          if ((intent.getBooleanExtra("permission", false)) && 
	            (device != null)) {
	            USBAPI.this.mDevice = device;
	          }
	        }
	      }

	      if ("android.hardware.usb.action.USB_DEVICE_DETACHED"
	        .equals(action)) {
	        USBAPI.this.mDevice = 
	          ((UsbDevice)intent
	          .getParcelableExtra("device"));
	      }
	      context.unregisterReceiver(USBAPI.this.mUsbReceiver);
	    }
	  };

	  public USBAPI(Context context)
	  {
	    this.mActivity = context;
	    this.mUsbManager = ((UsbManager)this.mActivity.getSystemService("usb"));
	    this.mPermissionIntent = PendingIntent.getBroadcast(this.mActivity, 0, 
	      new Intent("com.szsicod.print.USB_PERMISSION"), 0);
	  }

	  private boolean checkID(int pid, int vid)
	  {
	    for (int n = 0; n < this.mUsbCheckSRT.length; n++) {
	      if (this.mUsbCheckSRT[n].check(pid, vid)) {
	        return true;
	      }
	    }
	    return false;
	  }

	  public int openDevice()
	  {
	    if (isOpen().booleanValue()) {
	      return 0;
	    }
	    HashMap deviceList = this.mUsbManager
	      .getDeviceList();
	    Iterator deviceIterator = deviceList.values().iterator();
	    while (deviceIterator.hasNext()) {
	      this.mDevice = ((UsbDevice)deviceIterator.next());
	      if (checkID(this.mDevice.getVendorId(), this.mDevice.getProductId())) {
	        break;
	      }
	      this.mDevice = null;
	    }
	    if (this.mDevice == null) {
	      return -1;
	    }
	    IntentFilter filter = new IntentFilter("com.szsicod.print.USB_PERMISSION");
	    this.mActivity.registerReceiver(this.mUsbReceiver, filter);
	    if (!this.mUsbManager.hasPermission(this.mDevice)) {
	      this.mUsbManager.requestPermission(this.mDevice, 
	        this.mPermissionIntent);
	      if (!this.mUsbManager.hasPermission(this.mDevice)) {
	        this.mActivity.unregisterReceiver(this.mUsbReceiver);
	        return -1;
	      }
	    }
	    this.mUsbDeviceConnection = this.mUsbManager.openDevice(this.mDevice);
	    if (this.mUsbDeviceConnection == null) {
	      this.mActivity.unregisterReceiver(this.mUsbReceiver);
	      return -1;
	    }
	    if (!InitIOConfig().booleanValue()) {
	      closeDevice();
	      return -1;
	    }
	    return 0;
	  }

	  private Boolean InitIOConfig()
	  {
	    this.mEndpointIn = null;
	    this.mEndpointOut = null;
	    if ((this.mDevice == null) || (this.mUsbDeviceConnection == null)) {
	      return Boolean.valueOf(false);
	    }
	    int interfaceCount = 1;
	    UsbInterface intf = null;
	    UsbEndpoint endpoint = null;
	    for (int n = 0; n < interfaceCount; n++) {
	      intf = this.mDevice.getInterface(n);
	      boolean forceClaim = true;
	      this.mUsbDeviceConnection.claimInterface(intf, forceClaim);

	      int endpoitCount = intf.getEndpointCount();
	      for (int m = 0; m < endpoitCount; m++) {
	        endpoint = intf.getEndpoint(m);
	        if (endpoint.getType() == 2) {
	          if (endpoint.getDirection() == 0)
	            this.mEndpointOut = endpoint;
	          else if (endpoint.getDirection() == 128) {
	            this.mEndpointIn = endpoint;
	          }
	        }
	      }
	    }
	    if ((this.mEndpointOut == null) || (this.mEndpointIn == null)) {
	      return Boolean.valueOf(false);
	    }
	    return Boolean.valueOf(true);
	  }

	  public int closeDevice()
	  {
	    if (!isOpen().booleanValue()) {
	      return -1;
	    }
	    this.mUsbDeviceConnection.close();
	    this.mUsbDeviceConnection = null;
	    this.mActivity.unregisterReceiver(this.mUsbReceiver);
	    return 0;
	  }

	  public Boolean isOpen()
	  {
	    if (this.mUsbDeviceConnection == null) {
	      return Boolean.valueOf(false);
	    }
	    return Boolean.valueOf(true);
	  }

	  @SuppressLint({"NewApi"})
	  public int readBuffer(byte[] readBuffer, int offsetSize, int readSize, int waitTime)
	  {
	    if (this.mEndpointIn == null) {
	      return -1;
	    }
	    int bulkTransferSize = 0;
	    bulkTransferSize = this.mUsbDeviceConnection.bulkTransfer(
	      this.mEndpointIn, readBuffer, offsetSize, readSize, waitTime);
	    if (bulkTransferSize == readSize) {
	      return bulkTransferSize;
	    }
	    return -1;
	  }

	  @SuppressLint({"NewApi"})
	  public int writeBuffer(byte[] writeBuffer, int offsetSize, int writeSize, int waitTime)
	  {
	    if (this.mEndpointOut == null) {
	      return -1;
	    }
	    int bulkTransferSize = 0;
	    bulkTransferSize = this.mUsbDeviceConnection
	      .bulkTransfer(this.mEndpointOut, writeBuffer, offsetSize, 
	      writeSize, waitTime);
	    if (bulkTransferSize == writeSize) {
	      return bulkTransferSize;
	    }
	    return -1;
	  }

	  private class UsbCheckSRT
	  {
	    public int PID = 0;
	    public int VID = 0;

	    public UsbCheckSRT(int vid, int pid) {
	      this.PID = pid;
	      this.VID = vid;
	    }

	    public boolean check(int vid, int pid) {
	      if ((this.PID != pid) || (this.VID != vid)) {
	        return false;
	      }
	      return true;
	    }
	  }
}
