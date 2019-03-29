package io;

import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import android_serialport_api.SerialPort;

public class SerialAPI implements InterfaceAPI{
	public SerialPort mSerialport = null;
	  private File mFile;
	  private int mBaudrate;
	  private int mFlowContorl;
	  private static final String LOG_TAG = "COM_POS";
	  public static final int WRITEBYTEMAX = 4096;
	  public static final int READBYTEMAX = 1024;
	  private boolean ThreadFlg = false;

	  public SerialAPI(File device, int baudrate)
	  {
	    this.mFile = device;
	    this.mBaudrate = baudrate;
	  }

	  public int openDevice()
	  {
	    try
	    {
	      this.mSerialport = new SerialPort(this.mFile, this.mBaudrate, 0);
	    } catch (SecurityException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    if (this.mSerialport == null) {
	      return -1;
	    }
	    return 0;
	  }

	  public int writeBuffer(byte[] writeBuffer, int offsetSize, int writeSize, int waitTime)
	  {
	    int i = 0;
	    int res_len = 0;
	    int mode_len = 0;
	    int count = 0;
	    long tick = 0L;
	    long totulTick = 0L;
	    byte[] temp_buf = null;
	    byte[] res_buf = null;
	    if ((writeBuffer == null) || (writeSize <= 0) || (offsetSize < 0) || 
	      (this.mSerialport == null)) {
	      return -2;
	    }
	    if (offsetSize + writeSize > writeBuffer.length) {
	      return -2;
	    }
	    tick = System.currentTimeMillis();
	    totulTick = tick + waitTime;

	    byte[] des_buf = new byte[writeSize];
	    Arrays.fill(des_buf, (byte)0);
	    System.arraycopy(writeBuffer, offsetSize, des_buf, 0, writeSize);
	    if (des_buf.length > 4096) {
	      temp_buf = new byte[4096];
	      Arrays.fill(temp_buf, (byte)0);
	      mode_len = des_buf.length / 4096;
	      res_len = des_buf.length % 4096;
	      for (count = 0; count < mode_len; count++) {
	        tick = System.currentTimeMillis();
	        if (tick > totulTick) {
	          temp_buf = null;
	          des_buf = null;
	          return count * 4096;
	        }
	        System.arraycopy(des_buf, count * 4096 + i, temp_buf, 0, 4096);
	        try {
	          this.mSerialport.getOutputStream().write(temp_buf);
	        } catch (IOException e) {
	          e.printStackTrace();
	          temp_buf = null;
	          des_buf = null;
	          return count * 4096;
	        }
	      }
	      if (res_len != 0) {
	        res_buf = new byte[res_len];
	        for (i = 0; i < res_len; i++)
	          res_buf[i] = des_buf[(mode_len * 4096 + i)];
	        try
	        {
	          this.mSerialport.getOutputStream().write(res_buf);
	        } catch (IOException e) {
	          e.printStackTrace();
	          temp_buf = null;
	          res_buf = null;
	          des_buf = null;
	          return -1;
	        }
	      }
	    } else {
	      try {
	        this.mSerialport.getOutputStream().write(des_buf);
	      } catch (IOException e) {
	        e.printStackTrace();
	        temp_buf = null;
	        res_buf = null;
	        des_buf = null;
	        return -1;
	      }
	    }
	    temp_buf = null;
	    res_buf = null;
	    des_buf = null;
	    return writeSize;
	  }

	  public int readBuffer(byte[] readBuffer, int offsetSize, int readSize, int waitTime)
	  {
	    int read_size = 0;
	    int i = 0;

	    ReadInput read_thread = new ReadInput();
	    if ((this.mSerialport == null) || (readSize < 1) || (readSize > 1024)) {
	      return -2;
	    }
	    byte[] temp_buf = new byte[1024];
	    if (!this.ThreadFlg) {
	      read_thread.start();

	      long tick = System.currentTimeMillis();
	      long totulTick = tick + waitTime;
	      while (!read_thread.isread()) {
	        SystemClock.sleep(200L);
	        tick = System.currentTimeMillis();
	        if (totulTick < tick) {
	          break;
	        }
	      }
	      if (read_thread.isread()) {
	        read_size = read_thread.getlen();
	        temp_buf = read_thread.gettemp();
	      } else {
	        this.ThreadFlg = false;
	        return -1;
	      }
	    }
	    if (read_size > 0) {
	      for (i = offsetSize; i < offsetSize + read_size; i++) {
	        readBuffer[i] = temp_buf[(i - offsetSize)];
	      }
	      return read_size;
	    }
	    return 0;
	  }

	  public Boolean isOpen() {
	    if (this.mSerialport == null) {
	      return Boolean.valueOf(false);
	    }
	    return Boolean.valueOf(true);
	  }

	  public int closeDevice() {
	    if (isOpen().booleanValue()) {
	      this.mSerialport.close();
	      this.mSerialport = null;
	    }
	    return 0;
	  }

	  private class ReadInput extends Thread
	  {
	    private boolean isread = false;
	    private int len;
	    private byte[] temp = new byte[1024];

	    private ReadInput() {
	    }

	    public void run() {
	      super.run();
	      SerialAPI.this.ThreadFlg = true;
	      try {
	        this.len = SerialAPI.this.mSerialport.getInputStream().read(
	          this.temp);
	      } catch (IOException e) {
	        this.isread = false;
	        e.printStackTrace();
	      }
	      this.isread = true;
	      SerialAPI.this.ThreadFlg = false;
	    }

	    public boolean isread() {
	      return this.isread;
	    }

	    public int getlen() {
	      return this.len;
	    }

	    public byte[] gettemp() {
	      return this.temp;
	    }
	  }
}
