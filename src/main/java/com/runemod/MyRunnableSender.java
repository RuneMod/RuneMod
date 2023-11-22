package com.runemod;

import lombok.SneakyThrows;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MyRunnableSender implements Runnable {
    private final AtomicBoolean listen = new AtomicBoolean();
    private final AtomicBoolean send = new AtomicBoolean();
    AtomicReference atomicString = new AtomicReference("nothing yet");
    private boolean firstrun = true;


    public InetAddress clientIPAddress;
    private int port = 8888;
    private DatagramSocket serverSocket;
    private byte[] sendData = new byte[1024];
    private byte[] receiveData = new byte[1024];
    private String stringToSend;
    DatagramPacket receivePacket = null;
    DatagramPacket sendPacket = null;
    public static volatile boolean clientInitialized = false;
    public static volatile boolean clientConnected = false;
    public byte currentDataType = -1;  //0 = lowLatencyData, 1 = meshBytesData
    private int bufferQueuePos = -1;
    private int maxPacketSize = 60000;

    public void setSendMessage(String string) {
        this.atomicString.set(new AtomicReference(string));
    }

    public void setToFalseSend() {
        this.send.set(false);
    }

    public void setToTrueSend() {
        this.send.set(true);
    }

    @SneakyThrows
    public void onStart() {
        System.out.println("Runnable Sender start attempt");
        serverSocket = new DatagramSocket(port, InetAddress.getByName("127.0.0.101")); //our address from which we are sending stuff

		clientIPAddress = InetAddress.getByName("127.0.0.100"); //where we send stuff to

		serverSocket.setReuseAddress(true);
        receiveData = new byte[1024];
        sendData = new byte[1024];
        serverSocket.setSoTimeout(0);
        serverSocket.setReceiveBufferSize(65507);
        serverSocket.setSendBufferSize(65507);
        firstrun = false;
        System.out.println("server started");
    }

    @SneakyThrows
    public void waitForClientConnection() {
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        clientInitialized = true;
    }

    public int getClientPort() {
        return receivePacket.getPort();
    }

    public InetAddress getClientIP() {
        return receivePacket.getAddress();
    }

    @SneakyThrows
    public void sendMessage(String string) {
        if (clientInitialized) {
            String[] stringsToSendBuffer = splitStringEvery(string, 60000); //chops long string up

            for (int i = 0; i < stringsToSendBuffer.length; i++) { //sends each part of the chopped string. If string has been chopped, adds a character to it so UE4 can recognise it has begun receiving partial strings.
                stringToSend = stringsToSendBuffer[i];
                if (stringsToSendBuffer.length > 1) {
                    stringToSend = "-" + stringToSend;
                    //System.out.println("sending string leng segment " + stringToSend.length());
                }
                sendData = stringToSend.getBytes();
                AddMetaDataToPacket(sendData,"String");
                sendPacket = new DatagramPacket(sendData, sendData.length, clientIPAddress, port);
                serverSocket.send(sendPacket);
            }
        }
    }

    @SneakyThrows
    public void sendSingleByte(byte aSingleByte) { //sends a single byte message to client to indicate what data type it will be receiving
        if (clientInitialized) {
            byte[] singleByte = {aSingleByte};
            sendPacket = new DatagramPacket(singleByte, singleByte.length, clientIPAddress, port);
            serverSocket.send(sendPacket);
            //System.out.println("sent single byte: "+ aSingleByte);
        }
    }

    public void sendSetDataType(String dataType) {
        switch(dataType)
        {
            case "PartialPacket":
                if (currentDataType != 0) {
                    currentDataType = 0;
                    sendSingleByte((byte) 0);
                }
                break;
            case "String":
                if (currentDataType != 1) {
                    currentDataType = 1;
                    sendSingleByte((byte) 1);
                }
                break;
            case "LibMesh":
                if (currentDataType != 2) {
                    currentDataType = 2;
                    sendSingleByte((byte) 2);
                }
                break;
            case "GameObject":
                if (currentDataType != 3) {
                    currentDataType = 3;
                    sendSingleByte((byte) 3);
                }
                break;
            case "TileHeights":
                if (currentDataType != 4) {
                    currentDataType = 4;
                    sendSingleByte((byte) 4);
                }
                break;
            case "ObjectDefinition":
                if (currentDataType != 5) {
                    currentDataType = 5;
                    sendSingleByte((byte) 5);
                }
                break;
            case "Skeleton":
                if (currentDataType != 6) {
                    currentDataType = 6;
                    sendSingleByte((byte) 6);
                }
                break;
            case "SequenceDefinition":
                if (currentDataType != 7) {
                    currentDataType = 7;
                    sendSingleByte((byte) 7);
                }
                break;
            case "AnimationFrame":
                if (currentDataType != 8) {
                    currentDataType = 8;
                    sendSingleByte((byte) 8);
                }
                break;
            case "NpcDefinition":
                if (currentDataType != 9) {
                    currentDataType = 9;
                    sendSingleByte((byte) 9);
                }
                break;
            case "ModelData":
                if (currentDataType != 10) {
                    currentDataType = 10;
                    sendSingleByte((byte) 10);
                }
                break;
            case "ItemDefinition":
                if (currentDataType != 11) {
                    currentDataType = 11;
                    sendSingleByte((byte) 11);
                }
                break;
            case "TerrainLoad":
                if (currentDataType != 12) {
                    currentDataType = 12;
                    sendSingleByte((byte) 12);
                }
                break;
            case "KitDefinition":
                if (currentDataType != 13) {
                    currentDataType = 13;
                    sendSingleByte((byte) 13);
                }
                break;
            case "Textures":
                if (currentDataType != 14) {
                    currentDataType = 14;
                    sendSingleByte((byte) 14);
                }
                break;
            case "TilePaintCols0123RGB":
                if (currentDataType != 15) {
                    currentDataType = 15;
                    sendSingleByte((byte) 15);
                }
                break;
            case "TileOverlayCols0123RGB":
                if (currentDataType != 16) {
                    currentDataType = 16;
                    sendSingleByte((byte) 16);
                }
                break;
			case "PerFramePacket":
				if (currentDataType != 17) {
					currentDataType = 17;
					sendSingleByte((byte) 17);
				}
				break;
			case "ActorSpawn":
				if (currentDataType != 18) {
					currentDataType = 18;
					sendSingleByte((byte) 18);
				}
				break;
			case "ActorDeSpawn":
				if (currentDataType != 19) {
					currentDataType = 19;
					sendSingleByte((byte) 19);
				}
				break;
			case "ActorAnimationChange":
				if (currentDataType != 20) {
					currentDataType = 20;
					sendSingleByte((byte) 20);
				}
				break;
			case "21":
				if (currentDataType != 21) {
					currentDataType = 21;
					sendSingleByte((byte) 21);
				}
				break;
			case "22":
				if (currentDataType != 22) {
					currentDataType = 22;
					sendSingleByte((byte) 22);
				}
				break;
			case "ColourPalette":
				if (currentDataType != 23) {
					currentDataType = 23;
					sendSingleByte((byte) 23);
				}
				break;
			case "GameStateChanged":
				if (currentDataType != 24) {
					currentDataType = 24;
					sendSingleByte((byte) 24);
				}
				break;
			case "CanvasSizeChanged":
				if (currentDataType != 25) {
					currentDataType = 25;
					sendSingleByte((byte) 25);
				}
				break;
			case "PlaneChanged":
				if (currentDataType != 26) {
					currentDataType = 26;
					sendSingleByte((byte) 26);
				}
				break;
            default:
                System.out.println("no match");
                break;
        }
    }
    @SneakyThrows
    public void sendBytes(byte[] bytesToSend, String dataType) {
        if (clientInitialized) {
            bytesToSend = AddMetaDataToPacket(bytesToSend, dataType);
            if (bytesToSend.length > maxPacketSize) { //if bytesToSend exceeds max UDP packetSize
                System.out.println("SendingBUfferedPackets");
                //System.out.println("sending PartialPackets");
                int rangeEnd = 0;
                int rangeStart = -1;
                int noPartialPackets = (int) Math.ceil((bytesToSend.length-1)/maxPacketSize)+1;
                //sendSetDataType("PartialPacket"); //indicate we are currently sending partial packets;
                //System.out.println("BytesLen: "+bytesToSend.length);
               // System.out.println("noPartialPackets : "+noPartialPackets);
                for (int i= 1; i <= noPartialPackets; i++) {
                    rangeStart = (i*maxPacketSize)-(maxPacketSize);
                    if (i == noPartialPackets) { //if on last packet
                        //sendSetDataType(dataType);
                        rangeEnd = bytesToSend.length;
                        //System.out.println("onLastPacket. Len: "+ (rangeEnd-rangeStart));
                    } else {
                        rangeEnd = rangeStart+maxPacketSize;
                    }

                    sendData = (Arrays.copyOfRange(bytesToSend, rangeStart, rangeEnd)); //get range from bytes array
                    //System.out.println("sending "+noPartialPackets+ " PartialPackets. packetlen:  " + sendData.length);
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientIPAddress, port);
                    serverSocket.send(sendPacket);
                    //Thread.sleep(0,10000);
                }
            } else {
                //System.out.println("sendingBytes");
                //sendSetDataType(dataType);
                sendPacket = new DatagramPacket(bytesToSend, bytesToSend.length, clientIPAddress, port);
                serverSocket.send(sendPacket);
            }
        }
    }

    @SneakyThrows
    public void sendBytesTest(byte[] bytesToSend, String dataType) {
            AddMetaDataToPacket(bytesToSend, dataType);
            if (bytesToSend.length > maxPacketSize) { //if bytesToSend exceeds max UDP packetSize
                //System.out.println("sending PartialPackets");
                int rangeEnd = 0;
                int rangeStart = -1;
                int noPartialPackets = (int) Math.ceil((bytesToSend.length-1)/maxPacketSize)+1;
                //sendSetDataType("PartialPacket"); //indicate we are currently sending partial packets;
                //System.out.println("BytesLen: "+bytesToSend.length);
                // System.out.println("noPartialPackets : "+noPartialPackets);
                for (int i= 1; i <= noPartialPackets; i++) {
                    rangeStart = (i*maxPacketSize)-(maxPacketSize);
                    if (i == noPartialPackets) { //if on last packet
                        //sendSetDataType(dataType);
                        rangeEnd = bytesToSend.length;
                        //System.out.println("onLastPacket. Len: "+ (rangeEnd-rangeStart));
                    } else {
                        rangeEnd = rangeStart+maxPacketSize;
                    }

                    sendData = (Arrays.copyOfRange(bytesToSend, rangeStart, rangeEnd)); //get range from bytes array
                    //System.out.println("sending "+noPartialPackets+ " PartialPackets. packetlen:  " + sendData.length);
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientIPAddress, port);
                    serverSocket.send(sendPacket);
                    Thread.sleep(0,10000);
                }
            } else {
                //System.out.println("sendingBytes");
                //sendSetDataType(dataType);
                sendPacket = new DatagramPacket(bytesToSend, bytesToSend.length, clientIPAddress, port);
                serverSocket.send(sendPacket);
            }
    }

    @SneakyThrows
    private byte[] AddMetaDataToPacket(byte[] bytes, String dataType) {
        Byte dataTypeByte = 0;
        switch(dataType)
        {
            case "PartialPacket":
                dataTypeByte = 0;
                break;
            case "String":
                dataTypeByte = 1;
                break;
            case "Connection":
                dataTypeByte = 2;
                break;
            case "GameObject":
                dataTypeByte = 3;
                break;
            case "TileHeights":
                dataTypeByte = 4;
                break;
            case "ObjectDefinition":
                dataTypeByte = 5;
                break;
            case "Skeleton":
                dataTypeByte = 6;
                break;
            case "SequenceDefinition":
                dataTypeByte = 7;
                break;
            case "AnimationFrame":
                dataTypeByte = 8;
                break;
            case "NpcDefinition":
                dataTypeByte = 9;
                break;
            case "ModelData":
                dataTypeByte = 10;
                break;
            case "ItemDefinition":
                dataTypeByte = 11;
                break;
            case "TerrainLoad":
                dataTypeByte = 12;
                break;
            case "KitDefinition":
                dataTypeByte = 13;
                break;
            case "Texture":
                dataTypeByte = 14;
                break;
            case "RegionTiles":
                dataTypeByte = 15;
                break;
            case "TileOverlayCols0123RGB":
                dataTypeByte = 16;
                break;
            case "PerFramePacket":
                dataTypeByte = 17;
                break;
            case "ActorSpawn":
                dataTypeByte = 18;
                break;
            case "ActorDeSpawn":
                dataTypeByte = 19;
                break;
            case "ActorAnimationChange":
                dataTypeByte = 20;
                break;
            case "UnderlayDefinition":
                dataTypeByte = 21;
                break;
            case "OverlayDefinition":
                dataTypeByte = 22;
                break;
            case "ColourPalette":
                dataTypeByte = 23;
                break;
            case "GameStateChanged":
                dataTypeByte = 24;
                break;
            case "CanvasSizeChanged":
                dataTypeByte = 25;
                break;
            case "PlaneChanged":
                dataTypeByte = 26;
                break;
            case "WindowEvent":
                dataTypeByte = 27;
                break;
            case "28":
                dataTypeByte = 28;
                break;
            case "29":
                dataTypeByte = 29;
                break;
            case "SpotAnimDefinition":
                dataTypeByte = 30;
                break;
            case "31":
                dataTypeByte = 31;
                break;
            case "32":
                dataTypeByte = 32;
                break;
            case "Varbit":
                dataTypeByte = 33;
                break;
            case "Varp":
                dataTypeByte = 34;
                break;
            default:
                System.out.println("no match");
                break;
        }
        byte[] MetaData = new byte[5];
        //write dataLength
        int metadDataLength = 5;
        int packetLength = bytes.length+metadDataLength;
        MetaData[0] = (byte)(packetLength >> 24); // L: 84
        MetaData[1] = (byte)(packetLength >> 16); // L: 85
        MetaData[2] = (byte)(packetLength >> 8); // L: 86
        MetaData[3] = (byte)packetLength; // L: 87
        //write dataType
        MetaData[4] = (byte)(dataTypeByte); // L: 84
        //append array to metadata(insert metadata at array start)
        byte[] concatenated = Arrays.copyOf(MetaData, MetaData.length + bytes.length);
        System.arraycopy(bytes, 0, concatenated, MetaData.length, bytes.length);

        return concatenated;

/*        byte[] PacketEndIndicator = new byte[4];
        //write dataLength
        PacketEndIndicator[0] = (byte)(12345678 >> 24); // L: 84
        PacketEndIndicator[1] = (byte)(12345678 >> 16); // L: 85
        PacketEndIndicator[2] = (byte)(12345678 >> 8); // L: 86
        PacketEndIndicator[3] = (byte)12345678; // L: 87
        //append PacketEndIndicator to array end
        byte[] both = Arrays.copyOf(concatenated, concatenated.length + PacketEndIndicator.length);
        System.arraycopy(PacketEndIndicator, 0, both, concatenated.length, PacketEndIndicator.length);

        System.out.println("packet:" );
        for (int i = 0; i < both.length; i++) {
            System.out.println(both[i]);
        }

        return both;*/
    }

    @SneakyThrows
    public void sendLibMeshBytes(byte[] bytz) {
        if (clientInitialized) {
            sendBytes(bytz,"LibMesh");
        }
    }

    @SneakyThrows
    public void sendObjectDefinitionBytes(byte[] bytz) {
        if (clientInitialized) {
            sendBytes(bytz,"ObjectDefinition");
        }
    }

    @SneakyThrows
    public void sendGameObjectBytes(byte[] bytz) {
        if (clientInitialized) {
            sendBytes(bytz, "GameObject");
        }
    }

    public String[] splitStringEvery(String s, int interval) {
        int arrayLength = (int) Math.ceil(((s.length() / (double) interval)));
        String[] result = new String[arrayLength];

        int j = 0;
        int lastIndex = result.length - 1;
        for (int i = 0; i < lastIndex; i++) {
            result[i] = s.substring(j, j + interval);
            j += interval;
        } //Add the last bit
        result[lastIndex] = s.substring(j);
        return result;
    }

    @SneakyThrows
    @Override
    public void run() {
        if (firstrun == true) onStart();
    }
}

