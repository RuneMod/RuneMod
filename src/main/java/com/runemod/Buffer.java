package com.runemod;

import static com.runemod.SharedMemoryManager.rsDataTypeToOpCode;

public class Buffer {
    static int[] crc32Table;
    static long[] crc64Table;
    public static String formattedOperatingSystemName;
    public byte[] array;
    public int offset;

    static {
        crc32Table = new int[256]; // L: 16

        int var2;
        for (int var1 = 0; var1 < 256; ++var1) { // L: 21
            int var4 = var1; // L: 22

            for (var2 = 0; var2 < 8; ++var2) { // L: 23
                if ((var4 & 1) == 1) { // L: 24
                    var4 = var4 >>> 1 ^ -306674912;
                } else {
                    var4 >>>= 1; // L: 25
                }
            }

            crc32Table[var1] = var4; // L: 27
        }

        crc64Table = new long[256]; // L: 31

        for (var2 = 0; var2 < 256; ++var2) { // L: 36
            long var0 = (long)var2; // L: 37

            for (int var3 = 0; var3 < 8; ++var3) { // L: 38
                if ((var0 & 1L) == 1L) { // L: 39
                    var0 = var0 >>> 1 ^ -3932672073523589310L;
                } else {
                    var0 >>>= 1; // L: 40
                }
            }

            crc64Table[var2] = var0; // L: 42
        }

    }


    public Buffer(byte[] var1) {
        this.array = var1; // L: 61
        this.offset = 0; // L: 62
    } // L: 63

    public void setOffset(int offset_)
    {
        offset = offset_;
    }

    public void writeByte(int var1) {
        this.array[++this.offset - 1] = (byte)var1; // L: 71
    }

    public void writeShort(int var1) {
        this.array[++this.offset - 1] = (byte)(var1 >> 8);
        this.array[++this.offset - 1] = (byte)var1;
    }

    public void writeLong(long var1) {
        this.array[++this.offset - 1] = (byte)((int)(var1 >> 56));
        this.array[++this.offset - 1] = (byte)((int)(var1 >> 48));
        this.array[++this.offset - 1] = (byte)((int)(var1 >> 40));
        this.array[++this.offset - 1] = (byte)((int)(var1 >> 32));
        this.array[++this.offset - 1] = (byte)((int)(var1 >> 24));
        this.array[++this.offset - 1] = (byte)((int)(var1 >> 16));
        this.array[++this.offset - 1] = (byte)((int)(var1 >> 8));
        this.array[++this.offset - 1] = (byte)((int)var1);
    }

    public void writeInt(int var1) {
        this.array[++this.offset - 1] = (byte)(var1 >> 24);
        this.array[++this.offset - 1] = (byte)(var1 >> 16);
        this.array[++this.offset - 1] = (byte)(var1 >> 8);
        this.array[++this.offset - 1] = (byte)var1;
    }

    void writeByte_Array(byte[] var1, int arrayLen)
    {
        writeInt(arrayLen);
        for (int i = 0; i < arrayLen; i++) {
            writeByte(var1[i]);
        }
    }

    void writeInt_Array(int[] var1, int arrayLen)
    {
        writeInt(arrayLen);
        for (int i = 0; i < arrayLen; i++) {
            writeInt(var1[i]);
        }
    }

    public int readUnsignedByte() {
        return this.array[++this.offset - 1] & 255;
    }

    public byte readByte() {
        return this.array[++this.offset - 1];
    }

    public int readUnsignedShort() {
        this.offset += 2; // L: 208
        return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8);
    }

    public int readShort() {
        this.offset += 2; // L: 213
        int var1 = (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8);
        if (var1 > 32767) { // L: 215
            var1 -= 65536;
        }

        return var1; // L: 216
    }

    public int readShortSmart()
    {
        int var1 = this.array[this.offset] & 255;
        return var1 < 128 ? this.readUnsignedByte() - 64 : this.readUnsignedShort() - 0xc000;
    }

    public void writeMedium(int var1) {
        this.array[++this.offset - 1] = (byte)(var1 >> 16);
        this.array[++this.offset - 1] = (byte)(var1 >> 8);
        this.array[++this.offset - 1] = (byte)var1;
    }

    public int readMedium() {
        this.offset += 3; // L: 220
        return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8);
    }

    public int readInt() {
        this.offset += 4; // L: 225
        return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 24);
    }

    public long readLong() {
        long var1 = (long)this.readInt() & 4294967295L;
        long var3 = (long)this.readInt() & 4294967295L;
        return (var1 << 32) + var3; // L: 232
    }

    public boolean readBoolean() {
        return (this.readUnsignedByte() & 1) == 1;
    }

    public void writeBoolean(boolean var1) {
        this.writeByte(var1 ? 1 : 0);
    }

    boolean isOverFlowed = false;

    public void writePacket(Buffer packetContent, String dataType) {
        if ((offset + packetContent.offset) > (array.length-100)) { //if packet would overflow this buffer (aka the backBuffer), send backbuffer to unreal, then continue;
            try {
                System.out.println("backbuffer has overflowed, sending existing packets before continuing");
                Thread.sleep(200);
                isOverFlowed = true;
                RuneModPlugin.runeModPlugin.alreadyCommunicatedUnreal = false;
                System.out.println("dot4");
                RuneModPlugin.runeModPlugin.communicateWithUnreal();
                isOverFlowed = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        byte packetOpCode = rsDataTypeToOpCode(dataType);
        writeByte(packetOpCode);
        int packetContentLength = packetContent.offset;
        writeMedium(packetContentLength);
        System.arraycopy(packetContent.array, 0, array, offset, packetContentLength);
        offset+=packetContentLength;
    }

    public void reset() {
        offset = 0;
    }


    public static final char[] cp1252AsciiExtension = new char[]{'€', '\u0000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\u0000', 'Ž', '\u0000', '\u0000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\u0000', 'ž', 'Ÿ'};

    public static String decodeStringCp1252(byte[] var0, int var1, int var2) {
        char[] var3 = new char[var2];
        int var4 = 0;

        for (int var5 = 0; var5 < var2; ++var5) {
            int var6 = var0[var5 + var1] & 255;
            if (var6 != 0) {
                if (var6 >= 128 && var6 < 160) {
                    char var7 = cp1252AsciiExtension[var6 - 128];
                    if (var7 == 0) {
                        var7 = '?';
                    }

                    var6 = var7;
                }

                var3[var4++] = (char)var6;
            }
        }

        return new String(var3, 0, var4);
    }

    public String readStringCp1252NullTerminated() {
        int var1 = this.offset;

        while (this.array[++this.offset - 1] != 0) {
        }

        int var2 = this.offset - var1 - 1;
        return var2 == 0 ? "" : decodeStringCp1252(this.array, var1, var2);
    }
}
