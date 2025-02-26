package com.runemod;


import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import lombok.SneakyThrows;

import java.awt.*;
import java.lang.management.ManagementFactory;

import static com.sun.jna.platform.win32.WinBase.INFINITE;
import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

public class SharedMemoryManager
{
	public final MyKernel32 myKernel32;
	//private WinNT.HANDLE SharedMemoryMutex;

	public WinNT.HANDLE RuneliteMutex;
	public WinNT.HANDLE UnrealMutex;
	public WinNT.HANDLE BallMutex;

	public WinNT.HANDLE EventRlDataReady;
	public WinNT.HANDLE EventUeDataReady;

	public WinNT.HANDLE EventViewportPixelsReady;

	public Pointer SharedMemoryData;
	//public Pointer SharedMemoryData_ViewPort;
	private WinNT.HANDLE SharedMemoryHandle;
	//private WinNT.HANDLE SharedMemoryHandle_ViewPort;
	String SharedMemoryName = "";
	static byte TerminatingPacketOpCode = 1;
	Buffer backBuffer; //used to write packets while shared memory is unavailable
	private RuneModPlugin runeModPlugin;

	int SharedMemoryDataSize = 0;
	int Offset;

	public interface MyKernel32 extends Kernel32
	{
		MyKernel32 INSTANCE = (MyKernel32) Native.loadLibrary("kernel32", MyKernel32.class, W32APIOptions.DEFAULT_OPTIONS);
	}

	public SharedMemoryManager(RuneModPlugin runeModPlugin_) {
		runeModPlugin = runeModPlugin_;
		myKernel32 = MyKernel32.INSTANCE;
	}

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

	public void createSharedMemory(String sharedMemoryName, int sharedMemorySize)
	{
		System.out.println("Created Shared Memory: "+sharedMemoryName + "Size: " + (float)sharedMemorySize/1000000.0 + "MB");

		SharedMemoryHandle = myKernel32.CreateFileMapping(INVALID_HANDLE_VALUE,
			null, PAGE_READWRITE,
			0,
			sharedMemorySize,
				sharedMemoryName);

		if (SharedMemoryHandle == null) return;

		SharedMemoryData = myKernel32.MapViewOfFile(SharedMemoryHandle,
			0x001f,
			0, 0,
			sharedMemorySize);

		if (SharedMemoryData == null) return;

		SharedMemoryName = sharedMemoryName;
		SharedMemoryDataSize = sharedMemorySize;

		backBuffer = new Buffer(new byte[10000000]); //10mb backbuffer.
		System.out.println("Created backbuffer with Size: " + (float)10000000/1000000.0 + "MB");

		//SharedMemoryData.setInt(0, sharedMemorySize); //first 4 bytes contain sharedMemoryDataSize
	}

/*	public void createSharedMemory_ViewPort(String sharedMemoryName, int sharedMemorySize)
	{
		System.out.println("Created Shared Memory: "+sharedMemoryName + "Size: " + (float)sharedMemorySize/1000000.0 + "MB");
		//SharedMemoryName = sharedMemoryName;
		//SharedMemoryDataSize = sharedMemorySize;

		SharedMemoryHandle_ViewPort = myKernel32.CreateFileMapping(INVALID_HANDLE_VALUE,
				null, PAGE_READWRITE,
				0,
				sharedMemorySize,
				sharedMemoryName);

		if (SharedMemoryHandle_ViewPort == null) return;

		SharedMemoryData_ViewPort = myKernel32.MapViewOfFile(SharedMemoryHandle_ViewPort,
				0x001f,
				0, 0,
				sharedMemorySize);

		if (SharedMemoryData_ViewPort == null) return;
		//SharedMemoryData.setInt(0, sharedMemorySize); //first 4 bytes contain sharedMemoryDataSize
	}*/

	public boolean CreateNamedEvents() {
		EventRlDataReady = Kernel32.INSTANCE.CreateEvent(null, true, false, "Global\\RlDataReadyEvent");
		EventUeDataReady = Kernel32.INSTANCE.CreateEvent(null, true, false, "Global\\UeDataReadyEvent");
		EventViewportPixelsReady = Kernel32.INSTANCE.CreateEvent(null, true, false, "Global\\ViewportPixelsReadyEvent");

		return true;
	}

	public void CloseSharedMemory()
	{
		setByte(SharedMemoryDataSize -1, (byte)0); //last byte defines DataType; 2 = unrealdata. 1 = rsdata. on close, set this to 0 so unreal can run without crash
/*		if (SharedMemoryMutex != null)
		{
			//UnlockMutex();
			myKernel32.CloseHandle(SharedMemoryMutex);
			SharedMemoryMutex = null;
		}*/

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

/*		if (SharedMemoryHandle_ViewPort != null)
		{
			myKernel32.CloseHandle(SharedMemoryHandle_ViewPort);
			SharedMemoryHandle_ViewPort = null;
		}*/

/*		if (SharedMemoryData_ViewPort != null)
		{
			myKernel32.UnmapViewOfFile(SharedMemoryData_ViewPort);
			SharedMemoryData_ViewPort = null;
		}*/
	}

	public void transferBackBufferToSharedMem() {
		int backBufferLength = backBuffer.offset;
		SharedMemoryData.write(0, backBuffer.array,0, backBufferLength);
		Offset+=backBufferLength;
		clearBackBuffer();
	}

	public void clearBackBuffer() {
		backBuffer.reset();
	}


	static void waitNanos(int Nanos) {
		long waitStartNanos = System.nanoTime();

		while (true) {
			if((System.nanoTime())-waitStartNanos > Nanos) {
				break;
			}
		}
		return;
	}

	volatile boolean curRmWindowVisibility = true;


	public void setRuneModVisibility(boolean visibility) {
		if(!RuneModPlugin.unrealIsReady) {return;}
		//if(!runeModPlugin.config.attachRmWindowToRL()) {return;}
		if(curRmWindowVisibility == visibility) {return;}
		//if(!runeModWindowsExist()) {return;}

		//WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null,"RuneModWin");
		//if(hwnd.getPointer() == null) {return;}
		//WinDef.HWND hwnd2 = User32.INSTANCE.FindWindow(null,"RuneModControls");
		System.out.println("SettingRmWindowVisibility to "+visibility);
		curRmWindowVisibility = visibility;
		//runeModPlugin.RmVisChanged(curRmWindowVisibility);
		if(visibility) {
			User32.INSTANCE.ShowWindow(RuneModHandle, WinUser.SW_SHOWNOACTIVATE);
			//User32.INSTANCE.ShowWindow(hwnd2, WinUser.SW_SHOWNOACTIVATE);

			//runeModPlugin.RmVisChanged(visibility);
		} else {
			User32.INSTANCE.ShowWindow(RuneModHandle, WinUser.SW_HIDE);
			//User32.INSTANCE.ShowWindow(hwnd2, WinUser.SW_HIDE);
			//runeModPlugin.RmVisChanged(visibility);
		}
	}

	public WinDef.HWND findRuneModWindow() {
		WinDef.HWND handle = User32.INSTANCE.FindWindow(null,"RuneModWin");
		if(User32.INSTANCE.IsWindow(handle)) {
			System.out.println("found runemod window");
			return handle;
		} else {
			System.out.println("runemod window not yet found");
			return null;
		}
	}

/*	private static WinDef.HWND findWindowByProcessId(int processId) {
		User32 user32 = User32.INSTANCE;
		WinDef.HWND hwnd = user32.GetForegroundWindow();
		user32.EnumWindows((hWnd, arg) -> {
			int[] processIdArray = new int[1];
			IntByReference int_ = new IntByReference();

			user32.GetWindowThreadProcessId(hWnd, int_);
			if (processIdArray[0] == processId) {
				hwnd.setPointer(hWnd.getPointer());
				return false;
			}
			return true; // Continue enumeration
		}, null);
		return hwnd;
	}*/

/*	public WinDef.HWND getSelfHWND() {
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		String pid = processName.split("@")[0]; // Extract PID
		int targetProcessId = Integer.parseInt(pid); // Replace with your target process ID

		WinDef.HWND hwnd = findWindowByProcessId(targetProcessId);
		if (hwnd != null) {
			System.out.println("HWND: " + hwnd);
		} else {
			System.out.println("HWND of self not found.");
		}
		return hwnd;
	}

	public static void setClickThrough(WinDef.HWND hwnd) {
		int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT);
		//User32.INSTANCE.SetLayeredWindowAttributes(hwnd, 0, (byte)255, WinUser.LWA_ALPHA);
	}*/

	public WinDef.HWND RuneModHandle = null;

	public boolean RmWinIsChilded = false;
	public boolean ChildRuneModWinToRl() {
		if(!runeModPlugin.config.attachRmWindowToRL()) {return false;}

		if(RmWinIsChilded) {return true;}

		if(RuneModHandle == null) { RuneModHandle = findRuneModWindow(); }
		if (RuneModHandle == null) { return false; }

		System.out.println("ChildingRuneModWindowToRL ...");

		//WinDef.HWND mainFrameComponentHandle = new WinDef.HWND(Native.getComponentPointer(SwingUtilities.getAncestorOfClass(Frame.class, runeModPlugin.client.getCanvas())));
		WinDef.HWND mainFrameComponentHandle = new WinDef.HWND(Native.getComponentPointer(runeModPlugin.client.getCanvas().getParent()));

		User32.INSTANCE.SetParent(RuneModHandle, mainFrameComponentHandle);
		RmWinIsChilded = true;

		RuneModPlugin.toggleRuneModLoadingScreen(false);

		WinDef.HWND rmControls = User32.INSTANCE.FindWindow(null,"RuneModControls");

		//User32.INSTANCE.SetWindowLongPtr(rmControls, User32.GWL_HWNDPARENT, RuneModHandle.getPointer());
		//bring rm controls to front. if we dotn tdo this, rm controls dont appear at front until we reactivate rl win
		User32.INSTANCE.SetWindowPos(rmControls, User32.INSTANCE.GetWindow(mainFrameComponentHandle, new WinDef.DWORD(User32.GW_HWNDPREV)), 0, 0, 0, 0, User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_FRAMECHANGED);

		return true;
	}

	public void UnChildRuneModWinFromRl() {
		System.out.println("UnChildingRuneModWindowToRL");
		if(!RuneModPlugin.unrealIsReady) {return;}
		System.out.println("UnChildingRuneModWindowToRL .");
		if(runeModPlugin.config.attachRmWindowToRL()) {return;}
		System.out.println("UnChildingRuneModWindowToRL ..");
		if(!RmWinIsChilded) {return;}
		System.out.println("UnChildingRuneModWindowToRL ...");
		if(RuneModHandle == null) { RuneModHandle = findRuneModWindow(); }
		if (RuneModHandle == null) { return;}

		System.out.println("UnChildingRuneModWindowToRL ....");

		User32.INSTANCE.SetParent(RuneModHandle, null);
		RmWinIsChilded = false;
	}

	void checkIsRsData() {
		byte dataType = SharedMemoryData.getByte(SharedMemoryDataSize-1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		if(dataType!=1) {
			System.out.println("is not unreal data. dataType is " + dataType);
		}
	}

	boolean checkIsUnrealData() {
		byte dataType = SharedMemoryData.getByte(SharedMemoryDataSize-1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		if(dataType!=2) {
			//System.out.println("is not unreal data. dataType is " + dataType);
			return false;
		}
		return true;
	}

	public void updateRmWindowTransform() {
		if (!RuneModPlugin.unrealIsReady) { return; }
		if (!runeModPlugin.config.attachRmWindowToRL()) { return; }
		if (!runeModPlugin.client.getCanvas().isShowing()) { return; }

		Container parent = runeModPlugin.client.getCanvas().getParent();
		int canvasPosX = parent.getLocationOnScreen().x;
		int canvasPosY = parent.getLocationOnScreen().y;
		int canvasSizeX = parent.getWidth();
		int canvasSizeY = parent.getHeight();

		float dpiScalingFactor = runeModPlugin.getDpiScalingFactor(); // 96 DPI is the standard

		// Adjust position and size based on DPI scaling
		//canvasPosX = Math.round(canvasPosX * dpiScalingFactor);
		//canvasPosY = Math.round(canvasPosY * dpiScalingFactor);
		canvasSizeX = Math.round(canvasSizeX * dpiScalingFactor);
		canvasSizeY = Math.round(canvasSizeY * dpiScalingFactor);

		//since we have childed unreal window to rl, pos is always 0
		canvasPosX=0;
		canvasPosY=0;

		System.out.println("Updating RuneMod windows. PosX: " + canvasPosX + " posY: " + canvasPosY + " sizeX: " + canvasSizeX + " sizeY: " + canvasSizeY);

		//JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
		//WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame",window.getTitle());
		User32.INSTANCE.SetWindowPos(RuneModHandle, null, canvasPosX, canvasPosY, canvasSizeX, canvasSizeY, User32.SWP_NOACTIVATE);
		User32.INSTANCE.SetWindowPos(RuneModHandle, null, canvasPosX, canvasPosY, canvasSizeX, canvasSizeY, User32.SWP_NOACTIVATE);
	}

	public int gameCycle_Unreal = -1;

	public void handleUnrealData() {
		//System.out.println("handling unrealData");
		Offset = 0;
		if(!checkIsUnrealData()) {System.out.println("isNotUnrealData"); return;} //check, just incase something has gone wrong. if its not unreal data, we dont want to read it.

		while (true) {
			byte PacketOpCode = SharedMemoryData.getByte(Offset); //read packet opcode
			//System.out.println("read packetOpCode: " + PacketOpCode);
			Offset +=1;

			//System.out.println("handling unrealData WithOpCode: " + PacketOpCode);

			if(PacketOpCode == TerminatingPacketOpCode) { //indicates end of data
				//System.out.println("terminating Packet Reached. UnrealData Handled.");
				return;
			}

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

/*					int ViewSizeX = packet.readInt();
					int ViewSizeY = packet.readInt();
					byte[] pixels = packet.readByte_Array();

					System.out.println("pixelsLen:" +pixels.length);

					BufferedImage image = new BufferedImage(ViewSizeX, ViewSizeY, BufferedImage.TYPE_4BYTE_ABGR);
					image.getRaster().setDataElements(0, 0, ViewSizeX, ViewSizeY, pixels);
					runeModPlugin.client.getCanvas().getGraphics().drawImage(image,0,0, null);*/

					//System.out.println("UnrealsTick: " + gameCycle_Unreal);
					break;
				case 3: //StatusReport
					String string = packet.readStringCp1252NullTerminated();
					RuneModPlugin.runeMod_statusUI.SetStatus_Detail(string, true);
					break;
				case 4: //RequestRsCacheData
					//System.out.println("recieved RsCacheData request");
					System.out.println("rm says it is awaiting RsCacheData, sending it next tick.");
					runeModPlugin.clientThread.invokeAtTickEnd(() -> //invoke at end of tick, because we cannot communicate with unreal at this point in the communiucation process. (tecnically we can, but only if we dont overflow the backbuffer, which we will do when sending cache)
					{
						runeModPlugin.provideRsCacheData();
					});
					break;
				case 5: //RequestRsCacheHashes
					System.out.println("rm says it is awaiting rscacheHashes, sending them next tick.");
					runeModPlugin.clientThread.invokeAtTickEnd(() -> //invoke at end of tick, because we cannot communicate with unreal at this point in the communiucation process. (tecnically we can, but only if we dont overflow the backbuffer, which we will do when sending cache)
					{
						runeModPlugin.runeModAwaitingRsCacheHashes = true;
					});
					break;
				case 6: //RequestSceneReload
					System.out.println("recieved unreal RequestSceneReload");
					runeModPlugin.clientThread.invokeAtTickEnd(() ->
					{
						runeModPlugin.reloadUnrealScene();
					});
					break;
				case 7: //unused?
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
		setByte(SharedMemoryDataSize -1, (byte)1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		//System.out.println("Started new rsData");
	}

	void writeTerminatingPacket() {
		SharedMemoryData.setByte(Offset, TerminatingPacketOpCode);
		Offset +=1;
	}

	public void passRsDataToUnreal() { //writes dataLength metadata, and releases lock
		writeTerminatingPacket();
		//UnlockMutex();
		//System.out.println("Finished Rs Data, unreal may now do its thing, will await unreal data.");
	}

	public boolean isUnrealData() {
		if(SharedMemoryData.getByte(SharedMemoryDataSize -1)==(byte)2) {
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
			case "WindowUpdate":
				dataTypeByte = 25;
				break;
			case "PlaneChanged":
				dataTypeByte = 26;
				break;
			case "RmVisChanged":
				dataTypeByte = 27;
				break;
			case "BaseCoordinate":
				dataTypeByte = 28;
				break;
			case "OverlayColorChanged":
				dataTypeByte = 29;
				break;
			case "SpotAnimDefinition":
				dataTypeByte = 30;
				break;
			case "RsCacheHashesProvided":
				dataTypeByte = 31;
				break;
			case "InstancedAreaState":
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
	}
}