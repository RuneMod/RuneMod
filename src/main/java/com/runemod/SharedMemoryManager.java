package com.runemod;


import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.W32APIOptions;
import lombok.SneakyThrows;
import net.runelite.api.GameState;
import net.runelite.client.ui.ContainableFrame;

import javax.swing.*;

import java.awt.*;

import static com.sun.jna.platform.win32.WinBase.INFINITE;
import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

public class SharedMemoryManager
{
	private final MyKernel32 myKernel32;
	private WinNT.HANDLE SharedMemoryMutex;

	public WinNT.HANDLE RuneliteMutex;
	public WinNT.HANDLE UnrealMutex;

	public Pointer SharedMemoryData;
	private WinNT.HANDLE SharedMemoryHandle;
	String SharedMemoryName = "";
	static byte TerminatingPacketOpCode = 1;
	Buffer backBuffer; //used to write packets while shared memory is unavailable
	private RuneModPlugin runeModPlugin;

	int SharedMemorySize_Static = 0;
	int Offset;

	public interface MyKernel32 extends Kernel32
	{
		MyKernel32 INSTANCE = (MyKernel32) Native.loadLibrary("kernel32", MyKernel32.class, W32APIOptions.DEFAULT_OPTIONS);
	}

	public SharedMemoryManager(RuneModPlugin runeModPlugin_) {
		runeModPlugin = runeModPlugin_;
		myKernel32 = MyKernel32.INSTANCE;
	}


/*
	public static void main (String[] args)

	{
		ACCSharedMemory accSharedMemory = new ACCSharedMemory();
		accSharedMemory.test();
	}
*/

	public void setDataLength(int length) {
		setInt(0, length);
	}

	public void setInt(int offset, int anInt)
	{
		SharedMemoryData.setByte(0 + offset, (byte)(anInt >> 24));
		SharedMemoryData.setByte(1 + offset, (byte)(anInt >> 16));
		SharedMemoryData.setByte(2 + offset, (byte)(anInt >> 8));
		SharedMemoryData.setByte(3 + offset, (byte)(anInt));
	}

	public void setMedium(int offset, int anInt)
	{
		SharedMemoryData.setByte(0 + offset, (byte)(anInt >> 16));
		SharedMemoryData.setByte(1 + offset, (byte)(anInt >> 8));
		SharedMemoryData.setByte(2 + offset, (byte)(anInt));
	}

	public int getMedium(int offset)
	{
		return ((SharedMemoryData.getByte(offset+0) & 255) << 16) + (SharedMemoryData.getByte(offset+2) & 255) + ((SharedMemoryData.getByte(offset+1) & 255) << 8);
	}

	public void setByte(int offset, byte anInt)
	{
		SharedMemoryData.setByte(0 + offset, anInt);
	}

	public void AddBytBuffer(Buffer buffer)
	{
		SharedMemoryData.write(0,buffer.array,0,buffer.array.length);
	}

	public void createSharedMemory(String sharedMemoryName, int sharedMemorySize)
	{
		System.out.println("Created Shared Memory: "+sharedMemoryName + "Size: " + (float)sharedMemorySize/1000000.0 + "MB");
		SharedMemoryName = sharedMemoryName;
		SharedMemorySize_Static = sharedMemorySize;

		SharedMemoryHandle = myKernel32.CreateFileMapping(INVALID_HANDLE_VALUE,
			null, PAGE_READWRITE,
			0,
			sharedMemorySize,
			SharedMemoryName);

		if (SharedMemoryHandle == null) return;

		SharedMemoryData = myKernel32.MapViewOfFile(SharedMemoryHandle,
			0x001f,
			0, 0,
			sharedMemorySize);

		if (SharedMemoryData == null) return;

		backBuffer = new Buffer(new byte[10000000]); //10mb backbuffer.

		SharedMemoryData.setInt(0, sharedMemorySize); //first 4 bytes contain sharedMemoryDataSize
	}

	public boolean CreateMutexes() {
		RuneliteMutex = myKernel32.CreateMutex(null,
				true,
				"RuneliteMutex");
		if (RuneliteMutex == null) {
			throw new Win32Exception(myKernel32.GetLastError());
			//return false;
		} else {
			System.out.println("Created RuneliteMutex");
			myKernel32.ReleaseMutex(RuneliteMutex); //release the mutex
		}

		UnrealMutex = myKernel32.CreateMutex(null,
				true,
				"UnrealMutex");
		if (UnrealMutex == null) {
			throw new Win32Exception(myKernel32.GetLastError());
			//return false;
		} else {
			System.out.println("Created UnrealMutex");
			myKernel32.ReleaseMutex(UnrealMutex); //release the mutex
		}



		//System.out.println("creating mutex");
		SharedMemoryMutex = myKernel32.CreateMutex(null,
				true,
				SharedMemoryName+"MUTEX");
		if (SharedMemoryMutex == null) {
			throw new Win32Exception(myKernel32.GetLastError());
			//return false;
		} else {
			//System.out.println("Created Mutex");
			UnlockMutex(); //release the mutex
			return true;
		}
	}

	boolean AwaitUnrealMutex_Locked(long timeOutMs) {
		long awaitStart = System.nanoTime();
		while(true) {
			boolean aquiredLock = myKernel32.WaitForSingleObject(UnrealMutex,0) == 0; //if aquired lock
			if(aquiredLock == false) { //if failed, means is already locked;
				return true;
				//break;
			} else {
				myKernel32.ReleaseMutex(UnrealMutex);
				waitNanos(10000);
				if (System.nanoTime()-awaitStart > timeOutMs*1000000) { //if waited more than timeout
					return false;
				}
			}
		}
	}

	void UnrealMutexTest() {
		int result = myKernel32.WaitForSingleObject(UnrealMutex,0);
		System.out.println(result);
	}

	void AwaitUnrealMutex_UnLocked() {
			myKernel32.WaitForSingleObject(UnrealMutex, INFINITE);
			myKernel32.ReleaseMutex(UnrealMutex);
	}

	void LockRuneliteMutex() {
		myKernel32.WaitForSingleObject(RuneliteMutex, INFINITE); //if aquired lock
	}

	void UnLockRuneliteMutex() {
		myKernel32.ReleaseMutex(RuneliteMutex);
	}

	public Boolean LockMutex()
	{
		if (SharedMemoryMutex!=null)
		{
			int result = myKernel32.WaitForSingleObject(SharedMemoryMutex,10);

			if (result >= 10)
			{
				//System.out.println("Lock mutex timeout");
				return false;
			} else {
				//System.out.println("Acquired MutexLock");
				return true;
			}
		} else {
			//System.out.println("Mutex Isnt Valid");
			return false;
		}
	}

	public void UnlockMutex()
	{
		if (SharedMemoryMutex!=null)
		{
			myKernel32.ReleaseMutex(SharedMemoryMutex);
			//System.out.println("Released MutexLock");
		}
	}


	public void CloseSharedMemory()
	{
		setByte(SharedMemorySize_Static-1, (byte)0); //last byte defines DataType; 2 = unrealdata. 1 = rsdata. on close, set this to 0 so unreal can run without crash
		if (SharedMemoryMutex != null)
		{
			UnlockMutex();
			myKernel32.CloseHandle(SharedMemoryMutex);
			SharedMemoryMutex = null;
		}

		if (UnrealMutex != null)
		{
			myKernel32.ReleaseMutex(UnrealMutex);
			myKernel32.CloseHandle(UnrealMutex);
			UnrealMutex = null;
		}

		if (RuneliteMutex != null)
		{
			myKernel32.ReleaseMutex(RuneliteMutex);
			myKernel32.CloseHandle(RuneliteMutex);
			RuneliteMutex = null;
		}

		if (SharedMemoryData != null)
		{
			myKernel32.UnmapViewOfFile(SharedMemoryData);
			SharedMemoryData = null;
		}

		if (SharedMemoryHandle != null)
		{
			myKernel32.CloseHandle(SharedMemoryHandle);
			SharedMemoryHandle = null;
		}
	}

/*	public void writePacket(byte[] packetContent, String dataType) {
		byte packetOpCode = rsDataTypeToOpCode(dataType);
		setByte(Offset, packetOpCode); //write packetType
		Offset += 1;

		setMedium(Offset, packetContent.length); //write packetLength
		Offset += 3;

		SharedMemoryData.write(Offset, packetContent,0, packetContent.length); //write packetData
		Offset += packetContent.length;
		//System.out.println("written packetContent of length: "+packetContent.length);
	}*/

	public void transferBackBufferToSharedMem() {
		int backBufferLength = backBuffer.offset;
		SharedMemoryData.write(0, backBuffer.array,0, backBufferLength);
		Offset+=backBufferLength;
		clearBackBuffer();
	}

	public void clearBackBuffer() {
		backBuffer.reset();
	}

	@SneakyThrows
	public boolean awaitUnrealData() {
		int timeOut = 40000000;

		int unrealTicksBehind = runeModPlugin.client.getGameCycle()-gameCycle_Unreal;
		if(unrealTicksBehind > 50) { //if unreal is somewhat out of sync, dont bother waiting for unreal. Unreal may not get back to use every frame btw, as it skips writing an unreal packet if it cant aquire mutex.
			timeOut = 0;
		}

		long awaitStart = System.nanoTime();
		while (true) {
			//try to aquire lock
			if (LockMutex() == true) {
				if(isUnrealData())
				{
					long waitTimeSinceLastFrame = System.nanoTime()-runeModPlugin.timeCommunicatedWithUnreal;
					long waitTime = System.nanoTime()-awaitStart;
					//System.out.println("Waited " + waitTime/1000000 + "ms for unreal. It has been "+ waitTimeSinceLastFrame/1000000  + "ms since last frame. ");
					return true;
				}
				else
				{
					long waitTimeSinceLastFrame = System.nanoTime()-runeModPlugin.timeCommunicatedWithUnreal;
					long waitTime = System.nanoTime()-awaitStart;
					if(waitTime > timeOut) {
						UnlockMutex();
						//System.out.println("Timedout waiting. Waited " + waitTime/1000000 + "ms for unreal. It has been "+ waitTimeSinceLastFrame/1000000  + "ms since last frame. ");
						return false;
					} else {
						UnlockMutex();
						//System.out.println("Waiting.." + (float)waitTime/1000000 + "ms");
						Thread.sleep(0,100000);
					}
				}
			} else {
				if(System.nanoTime()-awaitStart > timeOut) { //if unable to aquire mutex after soemt ime
					return false;
				}
				Thread.sleep(0,100000);
			}
		}
	}


	void waitNanos(int Nanos) {
		long waitStartNanos = System.nanoTime();

		while (true) {
			if((System.nanoTime())-waitStartNanos > Nanos) {
				break;
			}
		}
		return;
	}

	boolean curRmWindowVisibility = true;

	public void setRuneModVisibility(boolean visibility) {
		if(!runeModPlugin.config.attachRmWindowToRL()) {return;}
		if(curRmWindowVisibility == visibility) {return;}
		if(!runeModWindowsExist()) {return;}

		WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null,"RuneModWin");
		//WinDef.HWND hwnd2 = User32.INSTANCE.FindWindow(null,"RuneModControls");
		if(visibility) {
			User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_SHOWNOACTIVATE);

			//User32.INSTANCE.ShowWindow(hwnd2, WinUser.SW_SHOWNOACTIVATE);
		} else {
			User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_HIDE);
			//User32.INSTANCE.ShowWindow(hwnd2, WinUser.SW_HIDE);
		}
		curRmWindowVisibility = visibility;
		System.out.println("SetRmWindowVisibility to "+visibility);
	}

	public boolean runeModWindowsExist() {
		WinDef.HWND RuneModWin = User32.INSTANCE.FindWindow(null, "RuneModWin");
		return User32.INSTANCE.IsWindow(RuneModWin);
/*		WinDef.HWND ModModeWindow = User32.INSTANCE.FindWindow(null, "ModModeTogglerWin");
		if(User32.INSTANCE.IsWindow(RuneModWin) && User32.INSTANCE.IsWindow(ModModeWindow)) {
			//System.out.println("runeModWindowsExist = true");
			return true;
		}
		//System.out.println("runeModWindowsExist = false");
		return false;*/
	}

	@SneakyThrows
	void destroyRuneModWin() {
		//if(!runeModPlugin.config.attachRmWindowToRL()) {return;}
		if(!runeModPlugin.config.StartRuneModOnStart()) {return;}
		//WinDef.HWND hwnd = findRuneModWindow();
		//User32.INSTANCE.PostMessage(hwnd, WinUser.WM_CLOSE, null, null);
/*		String process = "\"RuneModWin\"";
		Runtime.getRuntime().exec("taskkill /fi " + process);*/
		User32.INSTANCE.PostMessage(findRuneModWindow(),  0x0010, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
	}

	public WinDef.HWND findRuneModWindow() {
		WinDef.HWND handle = User32.INSTANCE.FindWindow(null,"RuneModWin");
		if(User32.INSTANCE.IsWindow(handle)) {
			System.out.println("found rl window");
			return handle;
		} else {
			System.out.println("Not found rl window");
			return null;
		}
	}

	WinDef.HWND RuneModHandle = null;
	public boolean ChildRuneModWinToRl() {
		if(runeModPlugin.client.getGameState().ordinal()<=GameState.LOGIN_SCREEN.ordinal()) { return false; }  //prevents childing while on login screen, because that causes a period where the client becomes unfocused which prevents you typing your user/pass
		if(RuneModHandle != null) {
			return true;}
		else {
			RuneModHandle = findRuneModWindow();
		}
		if (RuneModHandle == null) { return false;}

		if(!runeModPlugin.config.attachRmWindowToRL()) {return false;}

/*		User32.INSTANCE.SetWindowLongPtr(RuneModHandle *//*The handle of the window to remove its borders*//*, User32.GWL_STYLE, User32.WS_POPUP);
		User32.INSTANCE.SetWindowLongPtr(RuneModHandle *//*The handle of the window to remove its borders*//*, User32.GWL_STYLE, User32.WS_POPUP, );*/

		System.out.println("AttchinRuneModWindowsToRL");

		WinDef.HWND RLHandle = User32.INSTANCE.FindWindow(null,"RuneLite");
		User32.INSTANCE.SetWindowLongPtr(RuneModHandle, User32.GWL_HWNDPARENT, RLHandle.getPointer());

/*		WinDef.HWND ModModeWindow = User32.INSTANCE.FindWindow(null,"ModModeTogglerWin");
		User32.INSTANCE.SetWindowLongPtr(ModModeWindow, GWLP_HWNDPARENT, RLHandle.getPointer());*/

		updateRmWindowTransform();

		//runeModPlugin.maintainRuneModStatusAttachment();
		return true;
	}

/*	public boolean ChildRuneModStatusToRl() {
		WinDef.HWND RLStatusHandle = User32.INSTANCE.FindWindow(null,"RuneModStatus");
		WinDef.HWND RLHandle = User32.INSTANCE.FindWindow(null,"RuneLite");

		Point loc = runeModPlugin.client.getCanvas().getLocationOnScreen();
		loc.x += 100;
		loc.y -= runeModPlugin.runeModLauncher.runeMod_statusUI.frame.getHeight();

		User32.INSTANCE.SetWindowPos(RLStatusHandle, RLHandle, loc.x, loc.y, 300, 20,  User32.SWP_NOSIZE|User32.SWP_NOACTIVATE);
		//User32.INSTANCE.SetWindowLongPtr(RLStatusHandle, User32.GWL_HWNDPARENT, RLHandle.getPointer());
		return true;
	}*/

	public void updateRmWindowTransform() {
		if(!runeModPlugin.config.attachRmWindowToRL()) {return;}
		//if(RuneModHandle == null) {System.out.println("null rmHandle, cant uodate transform");}
		System.out.println("updating RuneMod windows");
		if(!runeModPlugin.client.getCanvas().isShowing()) { return;}
		Container parent = runeModPlugin.client.getCanvas().getParent();
		int canvasPosX = parent.getLocationOnScreen().x;
		int canvasPosY = parent.getLocationOnScreen().y;
		int canvasSizeX = parent.getWidth();
		int canvasSizeY = parent.getHeight();


		//WinDef.HWND HWND_TOP = new WinDef.HWND(new Pointer(0));
		User32.INSTANCE.SetWindowPos(RuneModHandle, null, canvasPosX, canvasPosY, canvasSizeX, canvasSizeY,  0);

		//runeModPlugin.maintainRuneModStatusAttachment();

/*		WinDef.HWND ModModeWindow = User32.INSTANCE.FindWindow(null,"ModModeTogglerWin");
		int SWP_NOSIZE = 0x0001;
		User32.INSTANCE.SetWindowPos(ModModeWindow, null, canvasPosX+(canvasSizeX/2)-16, canvasPosY-29, 0, 0,  SWP_NOSIZE);*/
	}

	public int gameCycle_Unreal = -1;

	public void handleUnrealData() {
		//System.out.println("handling unrealData");
		Offset = 0;
		while (true) {
			byte PacketOpCode = SharedMemoryData.getByte(Offset); //read packet opcode
			//System.out.println("read packetOpCode: " + PacketOpCode);
			Offset +=1;

			//System.out.println("handling unrealData WithOpCode: " + PacketOpCode);

			if(PacketOpCode == TerminatingPacketOpCode) { //indicates end of data
				//System.out.println("terminating Packet Reached. UnrealData Handled.");
				return;
			} //means packetList is finished.

			int packetLength = getMedium(Offset); //read packetLength
			Offset +=3;
			byte[] packetContent = SharedMemoryData.getByteArray(Offset, packetLength); //read packetContent

			Buffer packet = new Buffer(packetContent);
			switch(PacketOpCode) {
				case 0: //WindowEvent
					//System.out.println("recieved WindowEvent packet");
					break;
				case 1: //terminatorPacket
					//System.out.println("recieved terminatorPacket");
					return;
				case 2: //perFramePacket
					//System.out.println("recieved unrealPerFramePacket");
					gameCycle_Unreal = packet.readInt();
					//System.out.println("UnrealsTick: " + gameCycle_Unreal);
					break;
				case 3: //StatusReport
					//System.out.println("recieved unrealStatusReport");
					RuneModPlugin.runeModLauncher.runeMod_statusUI.SetStatus_Detail(packet.readStringCp1252NullTerminated());
					break;
				case 4: //RequestRsCacheData
					//System.out.println("recieved RsCacheData request");
					runeModPlugin.clientThread.invokeAtTickEnd(() -> //invoke at end of tick, because we cannot communicate with unreal at this point in the communiucation process. (tecnically we can, but only if we dont overflow the backbuffer, which we will do when sending cache)
					{
						runeModPlugin.provideRsCacheData();
					});
					break;
				case 5: //RequestRsCacheHashes
					runeModPlugin.clientThread.invokeAtTickEnd(() -> //invoke at end of tick, because we cannot communicate with unreal at this point in the communiucation process. (tecnically we can, but only if we dont overflow the backbuffer, which we will do when sending cache)
					{
						runeModPlugin.myCacheReader.provideRsCacheHashes();
					});
					break;
				case 6: //RequestSceneReload
					System.out.println("recieved unreal RequestSceneReload");
					runeModPlugin.clientThread.invokeAtTickEnd(() ->
					{
						runeModPlugin.reloadUnrealScene();
					});
					break;
				default:
					System.out.println("unhandled unrealDataType at: " + " Offset: "+Offset+" PacketLen: " + packetLength);
					break;
			}

			Offset+=packetLength;
			//System.out.println("read PacketContent of length: "+packetContent.length);
		}
	}

	public void startNewRsData() {
		Offset = 0;
		setByte(SharedMemorySize_Static-1, (byte)1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		//System.out.println("Started new rsData");
	}

	void writeTerminatingPacket() {
		SharedMemoryData.setByte(Offset, TerminatingPacketOpCode);
		Offset +=1;
	}

	public void passRsDataToUnreal() { //writes dataLength metadata, and releases lock
		writeTerminatingPacket();
		UnlockMutex();
		//System.out.println("Finished Rs Data, unreal may now do its thing, will await unreal data.");
	}

	public boolean isUnrealData() {
		if(SharedMemoryData.getByte(SharedMemorySize_Static-1)==(byte)2) {
			return true;
		} else {
			return false;
		}
	}

	@SneakyThrows
	static byte rsDataTypeToOpCode(String dataType) {
		Byte dataTypeByte = 0;
		switch(dataType)
		{
			case "PartialPacket":
				dataTypeByte = 0;
				break;
			case "Terminator":
				dataTypeByte = 1;
				break;
			case "Connection":
				dataTypeByte = 2;
				break;
			case "RsCacheDataProvided":
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
			case "BaseCoordinate":
				dataTypeByte = 28;
				break;
			case "29":
				dataTypeByte = 29;
				break;
			case "SpotAnimDefinition":
				dataTypeByte = 30;
				break;
			case "RsCacheHashesProvided":
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

		return dataTypeByte;

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
}