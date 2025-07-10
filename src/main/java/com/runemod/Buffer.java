package com.runemod;

import lombok.extern.slf4j.Slf4j;

import static com.runemod.SharedMemoryManager.rsDataTypeToOpCode;

@Slf4j
public class Buffer
{
	public static final char[] cp1252AsciiExtension = new char[]{'€', '\u0000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\u0000', 'Ž', '\u0000', '\u0000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\u0000', 'ž', 'Ÿ'};
	public static String formattedOperatingSystemName;
	static int[] crc32Table;
	static long[] crc64Table;

	static
	{
		crc32Table = new int[256];

		int var2;
		for (int var1 = 0; var1 < 256; ++var1)
		{
			int var4 = var1;

			for (var2 = 0; var2 < 8; ++var2)
			{
				if ((var4 & 1) == 1)
				{
					var4 = var4 >>> 1 ^ -306674912;
				}
				else
				{
					var4 >>>= 1;
				}
			}

			crc32Table[var1] = var4;
		}

		crc64Table = new long[256];

		for (var2 = 0; var2 < 256; ++var2)
		{
			long var0 = var2;

			for (int var3 = 0; var3 < 8; ++var3)
			{
				if ((var0 & 1L) == 1L)
				{
					var0 = var0 >>> 1 ^ -3932672073523589310L;
				}
				else
				{
					var0 >>>= 1;
				}
			}

			crc64Table[var2] = var0;
		}

	}

	public byte[] array;
	public int offset;
	boolean isOverFlowed = false;

	public Buffer(byte[] var1)
	{
		this.array = var1;
		this.offset = 0;
	}

	public static String decodeStringCp1252(byte[] var0, int var1, int var2)
	{
		char[] var3 = new char[var2];
		int var4 = 0;

		for (int var5 = 0; var5 < var2; ++var5)
		{
			int var6 = var0[var5 + var1] & 255;
			if (var6 != 0)
			{
				if (var6 >= 128 && var6 < 160)
				{
					char var7 = cp1252AsciiExtension[var6 - 128];
					if (var7 == 0)
					{
						var7 = '?';
					}

					var6 = var7;
				}

				var3[var4++] = (char) var6;
			}
		}

		return new String(var3, 0, var4);
	}

	public void setOffset(int offset_)
	{
		offset = offset_;
	}

	public void writeByte(int var1)
	{
		this.array[++this.offset - 1] = (byte) var1;
	}

	public void writeShort(int var1)
	{
		this.array[++this.offset - 1] = (byte) (var1 >> 8);
		this.array[++this.offset - 1] = (byte) var1;
	}

	public void writeLong(long var1)
	{
		this.array[++this.offset - 1] = (byte) ((int) (var1 >> 56));
		this.array[++this.offset - 1] = (byte) ((int) (var1 >> 48));
		this.array[++this.offset - 1] = (byte) ((int) (var1 >> 40));
		this.array[++this.offset - 1] = (byte) ((int) (var1 >> 32));
		this.array[++this.offset - 1] = (byte) ((int) (var1 >> 24));
		this.array[++this.offset - 1] = (byte) ((int) (var1 >> 16));
		this.array[++this.offset - 1] = (byte) ((int) (var1 >> 8));
		this.array[++this.offset - 1] = (byte) ((int) var1);
	}

	void writeLong_Array(long[] var1, int arrayLen)
	{
		if(var1 == null) {var1 = new long[0];}
		writeInt(arrayLen);
		for (int i = 0; i < arrayLen; i++)
		{
			writeLong(var1[i]);
		}
	}

	void writeLong_Array(int[] var1)
	{
		if(var1 == null) {var1 = new int[0];}
		writeInt(var1.length);
		for (int i = 0; i < var1.length; i++)
		{
			writeLong(var1[i]);
		}
	}

	public void writeInt(int var1)
	{
		this.array[++this.offset - 1] = (byte) (var1 >> 24);
		this.array[++this.offset - 1] = (byte) (var1 >> 16);
		this.array[++this.offset - 1] = (byte) (var1 >> 8);
		this.array[++this.offset - 1] = (byte) var1;
	}

	void writeByte_Array(byte[] var1, int arrayLen)
	{
		if(var1 == null) {var1 = new byte[0];}
		writeInt(arrayLen);
		for (int i = 0; i < arrayLen; i++)
		{
			writeByte(var1[i]);
		}
	}

	void writeByte_Array(int[] var1, int arrayLen)
	{
		if(var1 == null) {var1 = new int[0];}
		writeInt(arrayLen);
		for (int i = 0; i < arrayLen; i++)
		{
			writeByte(var1[i]);
		}
	}

	byte[] readByte_Array()
	{
		int arrayLen = readInt();
		byte[] arr = new byte[arrayLen];
		for (int i = 0; i < arrayLen; i++)
		{
			arr[i] = array[offset + i];
		}
		offset += arrayLen;
		return arr;
	}

	void writeInt_Array(int[] var1, int arrayLen)
	{
		if(var1 == null) {var1 = new int[0];}
		writeInt(arrayLen);
		for (int i = 0; i < arrayLen; i++)
		{
			writeInt(var1[i]);
		}
	}

	void writeInt_Array(int[] var1)
	{
		if(var1 == null) { var1 = new int[0];}
		writeInt_Array(var1, var1.length);
	}

	void writeInt_Array(short[] var1, int arrayLen)
	{
		if(var1 == null) {var1 = new short[0];}
		writeInt(arrayLen);
		for (int i = 0; i < arrayLen; i++)
		{
			writeInt(var1[i]);
		}
	}

	void writeInt_Array(short[] var1)
	{
		if(var1 == null) { var1 = new short[0];}
		writeInt_Array(var1, var1.length);
	}

	void writeShort_Array(short[] var1, int arrayLen)
	{
		if(var1 == null) {var1 = new short[0];}
		writeInt(arrayLen);
		for (int i = 0; i < arrayLen; i++)
		{
			writeShort(var1[i]);
		}
	}

	void writeShort_Array(short[] var1)
	{
		if(var1 == null) { var1 = new short[0];}
		writeShort_Array(var1, var1.length);
	}

	public int readUnsignedByte()
	{
		return this.array[++this.offset - 1] & 255;
	}

	public byte readByte()
	{
		return this.array[++this.offset - 1];
	}

	public int readUnsignedShort()
	{
		this.offset += 2;
		return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8);
	}

	public int readShort()
	{
		this.offset += 2;
		int var1 = (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8);
		if (var1 > 32767)
		{
			var1 -= 65536;
		}

		return var1;
	}

	public int readShortSmart()
	{
		int var1 = this.array[this.offset] & 255;
		return var1 < 128 ? this.readUnsignedByte() - 64 : this.readUnsignedShort() - 0xc000;
	}

	public void writeMedium(int var1)
	{
		this.array[++this.offset - 1] = (byte) (var1 >> 16);
		this.array[++this.offset - 1] = (byte) (var1 >> 8);
		this.array[++this.offset - 1] = (byte) var1;
	}

	public int readMedium()
	{
		this.offset += 3;
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8);
	}

	public int readInt()
	{
		this.offset += 4;
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 24);
	}

	public long readLong()
	{
		long var1 = (long) this.readInt() & 4294967295L;
		long var3 = (long) this.readInt() & 4294967295L;
		return (var1 << 32) + var3;
	}

	public boolean readBoolean()
	{
		return (this.readUnsignedByte() & 1) == 1;
	}

	public void writeBoolean(boolean var1)
	{
		this.writeByte(var1 ? 1 : 0);
	}

	public void writePacket(Buffer packetContent, String dataType)
	{
		if ((offset + packetContent.offset) > (array.length - 100))
		{ //if packet would overflow this buffer (aka the backBuffer), send backbuffer to unreal, then continue;
			try
			{
				log.debug("backbuffer has overflowed, sending existing packets before continuing");
				Thread.sleep(200);
				isOverFlowed = true;
				RuneModPlugin.runeModPlugin.alreadyCommunicatedUnreal = false;
				log.debug("dot4");
				RuneModPlugin.runeModPlugin.communicateWithUnreal("writePacket");
				isOverFlowed = false;
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		byte packetOpCode = rsDataTypeToOpCode(dataType);
		writeByte(packetOpCode);
		int packetContentLength = packetContent.offset;
		writeMedium(packetContentLength);
		System.arraycopy(packetContent.array, 0, array, offset, packetContentLength);
		offset += packetContentLength;
	}

	public void reset()
	{
		offset = 0;
	}

	public String readStringCp1252NullTerminated()
	{
		int var1 = this.offset;

		while (this.array[++this.offset - 1] != 0)
		{
		}

		int var2 = this.offset - var1 - 1;
		return var2 == 0 ? "" : decodeStringCp1252(this.array, var1, var2);
	}

	public void writeStringCp1252NullTerminated(String var1) {
		int var2 = var1.indexOf(0);
		if (var2 >= 0) {
			throw new IllegalArgumentException("");
		} else {
			this.offset += encodeStringCp1252(var1, 0, var1.length(), this.array, this.offset);
			this.array[++this.offset - 1] = 0;
		}
	}

	public static int encodeStringCp1252(CharSequence var0, int var1, int var2, byte[] var3, int var4) {
		int var5 = var2 - var1;

		for (int var6 = 0; var6 < var5; ++var6) {
			char var7 = var0.charAt(var6 + var1);
			if ((var7 <= 0 || var7 >= 128) && (var7 < 160 || var7 > 255)) {
				if (var7 == 8364) {
					var3[var6 + var4] = -128;
				} else if (var7 == 8218) {
					var3[var6 + var4] = -126;
				} else if (var7 == 402) {
					var3[var6 + var4] = -125;
				} else if (var7 == 8222) {
					var3[var6 + var4] = -124;
				} else if (var7 == 8230) {
					var3[var6 + var4] = -123;
				} else if (var7 == 8224) {
					var3[var6 + var4] = -122;
				} else if (var7 == 8225) {
					var3[var6 + var4] = -121;
				} else if (var7 == 710) {
					var3[var6 + var4] = -120;
				} else if (var7 == 8240) {
					var3[var6 + var4] = -119;
				} else if (var7 == 352) {
					var3[var6 + var4] = -118;
				} else if (var7 == 8249) {
					var3[var6 + var4] = -117;
				} else if (var7 == 338) {
					var3[var6 + var4] = -116;
				} else if (var7 == 381) {
					var3[var6 + var4] = -114;
				} else if (var7 == 8216) {
					var3[var6 + var4] = -111;
				} else if (var7 == 8217) {
					var3[var6 + var4] = -110;
				} else if (var7 == 8220) {
					var3[var6 + var4] = -109;
				} else if (var7 == 8221) {
					var3[var6 + var4] = -108;
				} else if (var7 == 8226) {
					var3[var6 + var4] = -107;
				} else if (var7 == 8211) {
					var3[var6 + var4] = -106;
				} else if (var7 == 8212) {
					var3[var6 + var4] = -105;
				} else if (var7 == 732) {
					var3[var6 + var4] = -104;
				} else if (var7 == 8482) {
					var3[var6 + var4] = -103;
				} else if (var7 == 353) {
					var3[var6 + var4] = -102;
				} else if (var7 == 8250) {
					var3[var6 + var4] = -101;
				} else if (var7 == 339) {
					var3[var6 + var4] = -100;
				} else if (var7 == 382) {
					var3[var6 + var4] = -98;
				} else if (var7 == 376) {
					var3[var6 + var4] = -97;
				} else {
					var3[var6 + var4] = 63;
				}
			} else {
				var3[var6 + var4] = (byte)var7;
			}
		}

		return var5;
	}
}
