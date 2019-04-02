package android_serialport_api;

import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import com.practice.android.fingerprintregisterdemo.utils.DataUtils;

public class SerialPortManager {
    private static int BAUDRATE = 460800;

    public static boolean switchRFID = false;

    final byte[] UP = {'1'};
    final byte[] DOWN = {'0'};
    final byte[] FBIDOWN = {'3'};

    private static String PATH = "/dev/ttyHSL0";
    private static String GPIO_DEV = "/sys/class/pwv_gpios/as602-en/enable";

    private static SerialPortManager mSerialPortManager = new SerialPortManager();


    private SerialPort mSerialPort = null;

    private boolean isOpen;

    private boolean firstOpen = false;

    private OutputStream mOutputStream;

    private InputStream mInputStream;

    private byte[] mBuffer = new byte[50 * 1024];

    private int mCurrentSize = 0;

    private ReadThread mReadThread;

    private SerialPortManager() {
    }

    /**
     * @return
     */
    public static SerialPortManager getInstance() {
        return mSerialPortManager;
    }

    public boolean isOpen() {
        return isOpen;
    }


    /**
     * 打开串口，如果需要读取身份证和指纹信息，必须先打开串口，调用此方法
     *
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidParameterException
     */
    public boolean openSerialPort() {
        if (mSerialPort == null) {
            // 上电
            try {
                setUpGpio();
                if (isFBIDevice())
                    setDownGpioFbi();
                Log.i("whw", "setUpGpio status=" + getGpioStatus());
                mSerialPort = new SerialPort(new File(PATH), BAUDRATE, 0);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mReadThread = new ReadThread();
            mReadThread.start();
            isOpen = true;
            firstOpen = true;
            return true;
        }
        return false;
    }


    private boolean openSerialPort2() {
        if (mSerialPort == null) {
            try {
                mSerialPort = new SerialPort(new File(PATH), BAUDRATE, 0);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i("whw", "mSerialPort=" + mSerialPort);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mReadThread = new ReadThread();
            mReadThread.start();
            isOpen = true;
            firstOpen = true;
            return true;
        }
        return false;
    }

    /**
     * 关闭串口，如果不需要读取指纹或身份证信息时，就关闭串口(可以节约电池电量)，建议程序退出时关闭
     */
    public void closeSerialPort() {
        if (mReadThread != null)
            mReadThread.interrupt();
        mReadThread = null;
        try {
            // 断电
            setDownGpio();
            Log.i("whw", "setDownGpio status=" + getGpioStatus());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (mSerialPort != null) {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSerialPort.close();
            mSerialPort = null;
        }
        isOpen = false;
        firstOpen = false;
        mCurrentSize = 0;
        switchRFID = false;
        if (looperBuffer != null) {
            looperBuffer = null;
        }
    }


    private void closeSerialPort2() {
        if (mReadThread != null)
            mReadThread.interrupt();
        mReadThread = null;
        if (mSerialPort != null) {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSerialPort.close();
            mSerialPort = null;
        }
        isOpen = false;
        firstOpen = false;
        mCurrentSize = 0;
        switchRFID = false;
        if (looperBuffer != null) {
            looperBuffer = null;
        }
    }

    protected synchronized int read(byte buffer[], int waittime, int interval) {
        if (!isOpen) {
            return 0;
        }
        int sleepTime = 5;
        int length = waittime / sleepTime;
        boolean shutDown = false;
        for (int i = 0; i < length; i++) {
            if (mCurrentSize == 0) {
                SystemClock.sleep(sleepTime);
                continue;
            } else {
                break;
            }
        }

        if (mCurrentSize > 0) {
            long lastTime = System.currentTimeMillis();
            long currentTime = 0;
            int lastRecSize = 0;
            int currentRecSize = 0;
            while (!shutDown && isOpen) {
                currentTime = System.currentTimeMillis();
                currentRecSize = mCurrentSize;
                if (currentRecSize > lastRecSize) {
                    lastTime = currentTime;
                    lastRecSize = currentRecSize;
                } else if (currentRecSize == lastRecSize && currentTime - lastTime >= interval) {
                    shutDown = true;
                }
            }
            if (mCurrentSize <= buffer.length) {
                System.arraycopy(mBuffer, 0, buffer, 0, mCurrentSize);
            }
        } else {
            // closeSerialPort2();
            SystemClock.sleep(100);
            // openSerialPort2();
        }
        return mCurrentSize;
    }

    protected synchronized int readFixedLength(byte buffer[], int waittime, int requestLength, int interval) {
        if (!isOpen) {
            return 0;
        }
        int sleepTime = 5;
        int length = waittime / sleepTime;
        boolean shutDown = false;
        for (int i = 0; i < length; i++) {
            if (mCurrentSize == 0) {
                SystemClock.sleep(sleepTime);
                continue;
            } else {
                break;
            }
        }

        if (mCurrentSize > 0) {
            long lastTime = System.currentTimeMillis();
            long currentTime = 0;
            int lastRecSize = 0;
            int currentRecSize = 0;
            while (!shutDown && isOpen) {
                if (mCurrentSize == requestLength) {
                    shutDown = true;
                } else {
                    currentTime = System.currentTimeMillis();
                    currentRecSize = mCurrentSize;
                    if (currentRecSize > lastRecSize) {
                        lastTime = currentTime;
                        lastRecSize = currentRecSize;
                    } else if (currentRecSize == lastRecSize && currentTime - lastTime >= interval) {
                        shutDown = true;
                    }
                }
            }

            if (mCurrentSize <= buffer.length) {
                System.arraycopy(mBuffer, 0, buffer, 0, mCurrentSize);
            }
        } else {
            closeSerialPort2();
            SystemClock.sleep(100);
            openSerialPort2();
        }
        return mCurrentSize;
    }

    private LooperBuffer looperBuffer;

    private void writeCommand(byte[] data) {
        if (!isOpen) {
            return;
        }
        if (firstOpen) {
            SystemClock.sleep(2000);
            firstOpen = false;
        }
        mCurrentSize = 0;
        try {
            mOutputStream.write(data);
        } catch (IOException e) {
        }
    }

    public synchronized void write(byte[] data) {
        Log.i("whw", "send commnad=" + DataUtils.toHexString(data));
        writeCommand(data);
    }

    private void setUpGpio() throws IOException {
        FileOutputStream fw = new FileOutputStream(GPIO_DEV);
        fw.write(UP);
        fw.close();
    }

    private void setDownGpio() throws IOException {
        FileOutputStream fw = new FileOutputStream(GPIO_DEV);
        fw.write(DOWN);
        fw.close();
    }


    private void setDownGpioFbi() throws IOException {
        FileOutputStream fw = new FileOutputStream("/sys/class/fbicode_gpios/fbicoe_state/control");
        fw.write(FBIDOWN);
        fw.close();
    }

    private boolean isFBIDevice() {
        String path = "/sys/class/fbicode_gpios/fbicoe_state/control";
        File file = new File(path);
        if (file.exists())
            return true;
        else
            return false;
    }

    public String getGpioStatus() throws IOException {
        String value;
        BufferedReader br = null;
        FileInputStream inStream = new FileInputStream(GPIO_DEV);
        br = new BufferedReader(new InputStreamReader(inStream));
        value = br.readLine();
        inStream.close();
        return value;

    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            byte[] buffer = new byte[512];
            while (!isInterrupted()) {
                int length = 0;
                try {
                    if (mInputStream == null)
                        return;
                    length = mInputStream.read(buffer);
                    if (length > 0) {
                        if (looperBuffer != null) {
                            byte[] buf = new byte[length];
                            System.arraycopy(buffer, 0, buf, 0, length);
                            Log.i("xuws", "recv buf=" + DataUtils.toHexString(buf));
                            looperBuffer.add(buf);
                        } // else {
                        System.arraycopy(buffer, 0, mBuffer, mCurrentSize, length);
                        mCurrentSize += length;
                        // }
                        Log.i("whw", "mCurrentSize=" + mCurrentSize + "  length=" + length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

}
