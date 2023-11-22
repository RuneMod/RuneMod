package com.runemod;

import lombok.SneakyThrows;
import net.runelite.api.coords.LocalPoint;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyRunnableReciever implements Runnable {
    private final AtomicBoolean listen = new AtomicBoolean();
    private final AtomicBoolean send = new AtomicBoolean();
    AtomicReference atomicString = new AtomicReference("nothing yet");
    private boolean firstrun = true;

    public RuneModPlugin runemod;
    private int port = 9999;
    private DatagramSocket serverSocketReciever;
    private byte[] sendData =  new byte[1024];
    private byte[] receiveData =  new byte[1024];
    private String stringToSend;
    DatagramPacket receivePacket = null;
    DatagramPacket sendPacket = null;
    public boolean clientInitialized = false;
    public static volatile boolean clientConnected = false;
    public int currentDataType;  //0 = lowLatencyData, 1 = meshBytesData
    private int bufferQueuePos = -1;

    public int curMouseX;
    public int curMouseY;

    public Component clientCanvas;
    public EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();

    volatile LocalPoint mouseLocalPoint_FirstPerson = new LocalPoint(0,0);
    volatile int mouseLocalPoint_Z_FirstPerson = 0;

    public void setSendMessage(String string) {
        this.atomicString.set(new AtomicReference(string));
    }

    public void setToFalseSend() {
        this.send.set(false);
    }

    public void setToTrueSend() {
        this.send.set(true);
    }

    HashMap<Character, Character> keys_shiftkeys = new HashMap<Character, Character>() {{
        put('1', '!');
        put('2', '\'');
        put('3', '£');
        put('4', '$');
        put('5', '%');
        put('6', '^');
        put('7', '&');
        put('8', '*');
        put('9', '(');
        put('0', ')');
        put('`', '¬');
        put('-', '_');
        put('=', '+');
        put('[', '{');
        put(']', '}');
        put('#', '~');
        put(';', ':');
        put('\'', '@');
        put(',', '<');
        put('.', '>');
        put('/', '?');
        put('q', 'Q');
        put('w', 'W');
        put('e', 'E');
        put('r', 'R');
        put('t', 'T');
        put('y', 'Y');
        put('u', 'U');
        put('i', 'I');
        put('o', 'O');
        put('p', 'P');
        put('a', 'A');
        put('s', 'S');
        put('d', 'D');
        put('f', 'F');
        put('g', 'G');
        put('h', 'H');
        put('j', 'J');
        put('k', 'K');
        put('l', 'L');
        put('z', 'Z');
        put('x', 'X');
        put('c', 'C');
        put('v', 'V');
        put('b', 'B');
        put('n', 'N');
        put('m', 'M');
        put(' ', ' ');
    }};

    boolean shiftPressed = false;

    public AtomicInteger unrealTick =  new AtomicInteger();


    @SneakyThrows
    public void onStart() {
        System.out.println("Runnable reciever start attempt");
        serverSocketReciever = new DatagramSocket(port, InetAddress.getByName("127.0.0.200"));

        serverSocketReciever.setReuseAddress(true);

        receiveData = new byte[1024];
        sendData = new byte[1024];
        serverSocketReciever.setSoTimeout(0);
        serverSocketReciever.setReceiveBufferSize(65507);
        serverSocketReciever.setSendBufferSize(65507);
        firstrun = false;
        System.out.println("server started");
    }

    @SneakyThrows
    public void waitForPacket() {
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocketReciever.receive(receivePacket);
    }

    public int getClientPort() {
        return receivePacket.getPort();
    }

    public InetAddress getClientIP() {
        return receivePacket.getAddress();
    }

    @SneakyThrows
    @Override
    public void run() {
        while (true) {
            if (firstrun == true)onStart();

            //System.out.println("waiting for client connection");
            waitForPacket();

            byte[] bytes = receivePacket.getData();

            int opCode = bytes[0];

            switch (opCode) {
                case 1:
                    System.out.println("Client Connected");
                    MyRunnableSender.clientInitialized = true;
                    MyRunnableSender.clientConnected = true;
                    runemod.clientJustConnected = true;

                    receiveData = new byte[1024];
                    sendData = new byte[1024];
                    break;
                case 2:
                    System.out.println("Client Disconnected");
                    MyRunnableSender.clientInitialized = false;
                    MyRunnableSender.clientConnected = false;
                    runemod.clientJustConnected = false;

                    receiveData = new byte[1024];
                    sendData = new byte[1024];
                    break;
                case 3:
                    byte keyState0 = bytes[1];
                    char keyChar0 = (char)bytes[2];
                    byte specialKey = bytes[3];
                    int keyCode0 = 0;
                    switch (specialKey) {
                        case 0:
                            keyCode0 = keyChar0;
                            keyChar0 = keyChar0;
                            break;
                        case 1:
                            keyCode0 = KeyEvent.VK_ENTER;
                            keyChar0 = KeyEvent.CHAR_UNDEFINED;
                            break;
                        case 2:
                            keyCode0 = KeyEvent.VK_TAB;
                            keyChar0 = KeyEvent.CHAR_UNDEFINED;
                            break;
                        case 3:
                            keyCode0 = KeyEvent.VK_BACK_SPACE;
                            keyChar0 = KeyEvent.CHAR_UNDEFINED;
                            break;
                        case 4:
                            keyCode0 = KeyEvent.VK_SHIFT;
                            keyChar0 = KeyEvent.CHAR_UNDEFINED;
                            break;
                        case 5:
                            keyCode0 = KeyEvent.VK_SHIFT;
                            keyChar0 = KeyEvent.CHAR_UNDEFINED;
                            break;
                        case 6:
                            keyCode0 = KeyEvent.VK_CAPS_LOCK;
                            keyChar0 = KeyEvent.CHAR_UNDEFINED;
                            return;
                            //break;
                        case 7:
                            keyCode0 = KeyEvent.VK_ESCAPE;
                            keyChar0 = KeyEvent.CHAR_UNDEFINED;
                            break;
                    }
                    System.out.println("key press recieved");
                    System.out.println("keycode:"+keyChar0);
                    char character = (char)keyChar0;


                    //character = Character.toLowerCase((char)keyChar0);

                    System.out.println("char:"+character);
                    KeyEvent keyEvent;
                    switch (keyState0) {
                        case 0: //key release
                            keyEvent = new KeyEvent(clientCanvas,KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,keyCode0, keyChar0);
                            eventQueue.postEvent(keyEvent);

                            if (keyCode0 == KeyEvent.VK_SHIFT) {shiftPressed = false;}
                            break;
                        case 1: //key press
                            keyEvent = new KeyEvent(clientCanvas,KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,keyCode0, keyChar0);
                            eventQueue.postEvent(keyEvent);

                            if (keyCode0 == KeyEvent.VK_SHIFT) {shiftPressed = true; break;}

                            if(keys_shiftkeys.containsKey(keyChar0)){ //if key is typable text
                                if (shiftPressed) {
                                    keyEvent = new KeyEvent(clientCanvas,KeyEvent.KEY_TYPED,System.currentTimeMillis(),0,KeyEvent.VK_UNDEFINED, keys_shiftkeys.get(keyChar0));
                                }else {
                                    keyEvent = new KeyEvent(clientCanvas,KeyEvent.KEY_TYPED,System.currentTimeMillis(),0,KeyEvent.VK_UNDEFINED, keyChar0);
                                }
                                eventQueue.postEvent(keyEvent);
                            }
                            break;
                    }
                    break;
                case 4:// Mouse Scroll
                    int WheelRotation = bytes[1]-1;
                    MouseWheelEvent mouseWheelEvent = new MouseWheelEvent(clientCanvas, MouseEvent.MOUSE_WHEEL ,System.currentTimeMillis(), 0, curMouseX, curMouseY, 0, false, 0, WheelRotation*3, WheelRotation );
                    eventQueue.postEvent(mouseWheelEvent);
                    break;
                case 5://Mouse Button
                    byte keyState = bytes[1];
                    byte keyChar = bytes[2];

                    int modifiers = -1;

                    switch (keyChar) {
                        case 1:
                            modifiers = 1024;
                            break;
                        case 2:
                            modifiers = 2048;
                            break;
                        case 3:
                            modifiers = 4096;
                            break;
                    }

                    if (keyState == 1){//pressed
                        MouseEvent mouseEventClick = new MouseEvent(clientCanvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), modifiers, curMouseX,curMouseY, 1, false, keyChar);
                        eventQueue.postEvent(mouseEventClick);
                    }

                    if (keyState == 0){//released
                        MouseEvent mouseEventClick = new MouseEvent(clientCanvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), modifiers, curMouseX,curMouseY, 1, false, keyChar);
                        eventQueue.postEvent(mouseEventClick);
                    }
                    break;
                case 6: //mouse Pos
                    curMouseX = (bytes[2] & 255) + ((bytes[1] & 255) << 8);
                    curMouseY = (bytes[4] & 255) + ((bytes[3] & 255) << 8);
                    MouseEvent mouseMoveEvent = new MouseEvent(clientCanvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, curMouseX,curMouseY, 0, false, 0);
                    eventQueue.postEvent(mouseMoveEvent); //temp disabled for firstperson testing
                    break;
                case 7: //unreal cycle
                    int UnrealClientCycle = ((bytes[2] & 255) << 16) + (bytes[4] & 255) + ((bytes[3] & 255) << 8) + ((bytes[1] & 255) << 24);
                    //System.out.println(UnrealClientCycle);
                    unrealTick.set(UnrealClientCycle);
                    break;
                case 8: //firstPersonViewCursorPos. not yet implemented on unreal side
                    int x = (bytes[2] & 255) + ((bytes[1] & 255) << 8);
                    int y = (bytes[4] & 255) + ((bytes[3] & 255) << 8);
                    int z = (bytes[6] & 255) + ((bytes[5] & 255) << 8);
                    mouseLocalPoint_FirstPerson = new LocalPoint(x,y);
                    mouseLocalPoint_Z_FirstPerson = z;
                    System.out.println("localX = " + x + " localY = "+y);
                    break;
            }

            //String sentence = new String(receivePacket.getData());
            //System.out.println("RECEIVED opcode: " + opCode);
			if (opCode == 2) { //disconnection indication

			} else {
				if (opCode == 1) { //connect indication

				}
			}
        }


    }
}
