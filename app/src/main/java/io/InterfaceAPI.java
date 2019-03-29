package io;

public abstract interface InterfaceAPI {
	public static final int SUCCESS = 0;
	public static final int FAIL = -1;
	public static final int ERR_PARAM = -2;

	public abstract int openDevice();

	public abstract int closeDevice();

	public abstract Boolean isOpen();

	public abstract int readBuffer(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3);

	public abstract int writeBuffer(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3);
}
