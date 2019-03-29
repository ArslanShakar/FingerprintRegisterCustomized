package io;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketAPI implements InterfaceAPI{
	static Socket client;
	  private String BM = "GB2312";
	  private String site;
	  private int port;
	  OutputStream out;

	  public SocketAPI(String site, int port)
	  {
	    this.site = site;
	    this.port = port;
	  }

	  private int printbyte(byte[] bytes, PrintWriter out)
	  {
	    char[] cmd = new char[bytes.length];
	    for (int i = 0; i < bytes.length; i++) {
	      cmd[i] = ((char)bytes[i]);
	    }
	    out.write(cmd, 0, bytes.length);
	    return bytes.length;
	  }

	  public void closeSocket() {
	    try {
	      client.close();
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }

	  public static byte[] hexStringToBytes(String hexString)
	  {
	    if ((hexString == null) || (hexString.equals(""))) {
	      return null;
	    }
	    hexString = hexString.toUpperCase();
	    int length = hexString.length() / 2;
	    char[] hexChars = hexString.toCharArray();
	    byte[] d = new byte[length];
	    for (int i = 0; i < length; i++) {
	      int pos = i * 2;
	      d[i] = ((byte)(charToByte(hexChars[pos]) << 4 | charToByte(hexChars[(pos + 1)])));
	    }
	    return d;
	  }

	  private static synchronized byte charToByte(char c)
	  {
	    return (byte)"0123456789ABCDEF".indexOf(c);
	  }

	  public synchronized int openDevice()
	  {
	    return 0;
	  }

	  public synchronized int closeDevice()
	  {
	    return 0;
	  }

	  public synchronized Boolean isOpen()
	  {
	    return Boolean.valueOf(true);
	  }

	  public synchronized int readBuffer(byte[] readBuffer, int offsetSize, int readSize, int waitTime)
	  {
	    return 0;
	  }

	  public synchronized int writeBuffer(byte[] writeBuffer, int offsetSize, int writeSize, int waitTime)
	  {
	    try
	    {
	      if ((client == null) || (client.isClosed())) {
	        client = new Socket(this.site, this.port);
	        this.out = client.getOutputStream();
	      }
	      if (client.isConnected()) {
	        this.out = client.getOutputStream();
	      }
	      this.out.write(writeBuffer, offsetSize, writeSize);
	      this.out.flush();
	      return writeSize;
	    } catch (Exception e) {
	      e.printStackTrace();
	    }return -1;
	  }
}
