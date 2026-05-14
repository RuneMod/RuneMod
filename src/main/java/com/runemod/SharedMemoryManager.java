/*
 * Copyright (c) 2025, RuneFace <RuneFace@proton.me>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.runemod;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.SwingUtilities;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

@Slf4j
public class SharedMemoryManager
{
	static byte TerminatingPacketOpCode = 1;
	public final MyKernel32 myKernel32;
	public final MyUser32 myUser32;
	public WinNT.HANDLE EventRlDataReady;
	public WinNT.HANDLE EventUeDataReady;
	public WinNT.HANDLE EventViewportPixelsReady;
	public Pointer SharedMemoryData;
	public WinDef.HWND RuneModHandle = null;
	public WinDef.HWND RsUiDisplayerHandle = null;
	public WinDef.HWND RuneLiteGameHandle = null;
	public WinDef.HWND RuneLiteRootHandle = null;
	Window rlRootWin = null;

	public boolean RmWinIsChilded = false;
	public int gameCycle_Unreal = -1;
	String SharedMemoryName = "";
	Buffer backBuffer; //used to write packets while shared memory is unavailable
	int SharedMemoryDataSize = 0;
	int Offset;
	boolean curRmWindowVisibility = false;
	private WinNT.HANDLE SharedMemoryHandle;
	private final RuneModPlugin runeModPlugin;

	public SharedMemoryManager(RuneModPlugin runeModPlugin_)
	{
		runeModPlugin = runeModPlugin_;
		myKernel32 = MyKernel32.INSTANCE;
		myUser32 = MyUser32.INSTANCE;
	}

	public static WinDef.HWND findWindowByPid(long targetPid) {
		final WinDef.HWND[] found = new WinDef.HWND[1];

		User32.INSTANCE.EnumWindows((hWnd, data) -> {
			if (!User32.INSTANCE.IsWindowVisible(hWnd)) return true;

			IntByReference pidRef = new IntByReference();
			User32.INSTANCE.GetWindowThreadProcessId(hWnd, pidRef);

			if ((long) pidRef.getValue() == targetPid) {
				char[] title = new char[512];
				User32.INSTANCE.GetWindowText(hWnd, title, 512);
				String windowTitle = Native.toString(title);
				log.debug("Found window: " + windowTitle);
				found[0] = hWnd;
				return false; // stop
			}

			return true;
		}, null);

		return found[0];
	}

/*	public static boolean canLoadDll(String dllName) {
		MyKernel32 kernel32 = MyKernel32.INSTANCE;

		// Use the wide-character version (for proper Unicode handling)
		WinDef.HMODULE handle = kernel32.LoadLibraryW(new WString(dllName));
		if (handle != null) {
			kernel32.FreeLibrary(handle);
			return true;
		} else {
			int err = kernel32.GetLastError();
			System.err.println("LoadLibrary failed for " + dllName + " (error code " + err + ")");
			return false;
		}
	}*/

	@SneakyThrows
	static byte rsDataTypeToOpCode(String dataType)
	{
		Byte dataTypeByte = 0;
		switch (dataType)
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
			case "Modifier":
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
			case "RsOptions":
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
			case "SkeletalFrameSet": //"TileOverlayCols0123RGB":
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
			case "ActorOverridesChanged":
				dataTypeByte = 23;
				break;
			case "GameStateChanged":
				dataTypeByte = 24;
				break;
			case "RmUi":
				dataTypeByte = 25;
				break;
			case "PlaneChanged":
				dataTypeByte = 26;
				break;
			case "HiddenChunks":
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
			case "WorldVisibility":
				dataTypeByte = 35;
				break;
			case "WorldViewUpdate":
				dataTypeByte = 36;
				break;
			case "SpawnModel":
				dataTypeByte = 37;
				break;
			default:
				log.debug("no opcode match for packet '"+dataType+"'");
				break;
		}

		return dataTypeByte;
	}

	public void setInt(int offset, int anInt)
	{
		SharedMemoryData.setByte(0 + offset, (byte) (anInt >> 24));
		SharedMemoryData.setByte(1 + offset, (byte) (anInt >> 16));
		SharedMemoryData.setByte(2 + offset, (byte) (anInt >> 8));
		SharedMemoryData.setByte(3 + offset, (byte) (anInt));
	}

	public void setLong(int offset, long value)
	{
		SharedMemoryData.setByte(0 + offset, (byte) (value >> 56));
		SharedMemoryData.setByte(1 + offset, (byte) (value >> 48));
		SharedMemoryData.setByte(2 + offset, (byte) (value >> 40));
		SharedMemoryData.setByte(3 + offset, (byte) (value >> 32));
		SharedMemoryData.setByte(4 + offset, (byte) (value >> 24));
		SharedMemoryData.setByte(5 + offset, (byte) (value >> 16));
		SharedMemoryData.setByte(6 + offset, (byte) (value >> 8));
		SharedMemoryData.setByte(7 + offset, (byte) (value));
	}

	public void setLongLE(int offset, long value) //little endian
	{
		SharedMemoryData.setByte(0 + offset, (byte) (value));
		SharedMemoryData.setByte(1 + offset, (byte) (value >> 8));
		SharedMemoryData.setByte(2 + offset, (byte) (value >> 16));
		SharedMemoryData.setByte(3 + offset, (byte) (value >> 24));
		SharedMemoryData.setByte(4 + offset, (byte) (value >> 32));
		SharedMemoryData.setByte(5 + offset, (byte) (value >> 40));
		SharedMemoryData.setByte(6 + offset, (byte) (value >> 48));
		SharedMemoryData.setByte(7 + offset, (byte) (value >> 56));
	}

	public long getLongLE(int offset)
	{
		return ((long)(SharedMemoryData.getByte(0 + offset) & 0xFF)) |
			((long)(SharedMemoryData.getByte(1 + offset) & 0xFF) << 8) |
			((long)(SharedMemoryData.getByte(2 + offset) & 0xFF) << 16) |
			((long)(SharedMemoryData.getByte(3 + offset) & 0xFF) << 24) |
			((long)(SharedMemoryData.getByte(4 + offset) & 0xFF) << 32) |
			((long)(SharedMemoryData.getByte(5 + offset) & 0xFF) << 40) |
			((long)(SharedMemoryData.getByte(6 + offset) & 0xFF) << 48) |
			((long)(SharedMemoryData.getByte(7 + offset) & 0xFF) << 56);
	}

	public void setMedium(int offset, int anInt)
	{
		SharedMemoryData.setByte(0 + offset, (byte) (anInt >> 16));
		SharedMemoryData.setByte(1 + offset, (byte) (anInt >> 8));
		SharedMemoryData.setByte(2 + offset, (byte) (anInt));
	}

	public int getMedium(int offset)
	{
		return ((SharedMemoryData.getByte(offset + 0) & 255) << 16) + (SharedMemoryData.getByte(offset + 2) & 255) + ((SharedMemoryData.getByte(offset + 1) & 255) << 8);
	}

	public void setByte(int offset, byte anInt)
	{
		SharedMemoryData.setByte(0 + offset, anInt);
	}

	public void createSharedMemory(String sharedMemoryName, int sharedMemorySize)
	{
		log.debug("Creating Shared Memory: " + sharedMemoryName + "Size: " + (float) sharedMemorySize / 1000000.0 + "MB");

		SharedMemoryHandle = myKernel32.CreateFileMapping(INVALID_HANDLE_VALUE,
			null, PAGE_READWRITE,
			0,
			sharedMemorySize,
			sharedMemoryName);

		if (SharedMemoryHandle == null)
		{
			return;
		}

		SharedMemoryData = myKernel32.MapViewOfFile(SharedMemoryHandle,
			0x001f,
			0, 0,
			sharedMemorySize);

		if (SharedMemoryData == null)
		{
			return;
		}

		SharedMemoryName = sharedMemoryName;
		SharedMemoryDataSize = sharedMemorySize;


		backBuffer = new Buffer(new byte[20000000]); //20mb backbuffer.
		log.debug("Created backbuffer with Size: " + (float) 20000000 / 1000000.0 + "MB");
	}

	public boolean CreateNamedEvents()
	{
		EventRlDataReady = Kernel32.INSTANCE.CreateEvent(null, true, false, "Global\\RlDataReadyEvent");
		EventUeDataReady = Kernel32.INSTANCE.CreateEvent(null, true, false, "Global\\UeDataReadyEvent");
		EventViewportPixelsReady = Kernel32.INSTANCE.CreateEvent(null, true, false, "Global\\ViewportPixelsReadyEvent");

		return true;
	}

	public void CloseSharedMemory()
	{
		setByte(SharedMemoryDataSize - 1, (byte) 0); //last byte defines DataType; 2 = unrealdata. 1 = rsdata. setting it to 0 because it is now neither, since it is closed.

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

	public void transferBackBufferToSharedMem()
	{
		int backBufferLength = backBuffer.offset;
		SharedMemoryData.write(0, backBuffer.array, 0, backBufferLength);
		Offset += backBufferLength;
		clearBackBuffer();

	}

	public static boolean RuneModWinExists_ByName() {
		WinDef.HWND handle = User32.INSTANCE.FindWindow(null, "RuneModWin");
		return User32.INSTANCE.IsWindow(handle);
	}

	public void clearBackBuffer()
	{
		backBuffer.reset();
	}

	public void FocusRl() {
		User32.INSTANCE.SetFocus(RuneLiteRootHandle);
	}

	public void setRuneModVisibility(boolean visibility)
	{
		if (!RuneModPlugin.unrealIsReady)
		{
			return;
		}
		if (curRmWindowVisibility == visibility)
		{
			return;
		}

		log.debug("SettingRmWindowVisibility to " + visibility);
		curRmWindowVisibility = visibility;
		if (visibility)
		{
			User32.INSTANCE.ShowWindow(RuneModHandle, WinUser.SW_SHOWNOACTIVATE);
		}
		else
		{
			User32.INSTANCE.ShowWindow(RuneModHandle, WinUser.SW_HIDE);
		}
	}

	public boolean isRuneModHandleValid()
	{
		return User32.INSTANCE.IsWindow(RuneModHandle);
	}

	public WinDef.HWND findRuneModWindow()
	{
		long hwndVal = getLongLE(30000050); //read runemod handle from shared memory

		Pointer hwndPointer = new Pointer(hwndVal);
		WinDef.HWND handle = new WinDef.HWND(hwndPointer);

		//WinDef.HWND handle = User32.INSTANCE.FindWindow(null, "RuneModWin");
		if (User32.INSTANCE.IsWindow(handle))
		{
			log.debug("found runemod window");
			return handle;
		}
		else
		{
			log.debug("runemod window not yet found");
			return null;
		}
	}

	public static Window getRootWindow(Component component) {
		Window window = SwingUtilities.getWindowAncestor(component);
		if (window == null) {
			return null;
		}

		while (window.getOwner() instanceof Window) {
			window = (Window) window.getOwner();
		}

		return window;
	}

	public void write_Rl_hwnd_ToSharedMem()
	{
		//if(RuneModHandle == null && runeModPlugin.client.getCanvas().isShowing()) {
			//Component window = SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas().getParent().getParent());

			//rlWinHandle = new WinDef.HWND(Native.getComponentPointer(window));
			RuneLiteGameHandle = new WinDef.HWND(Native.getComponentPointer(runeModPlugin.client.getCanvas().getParent()));
			long hwndVal = Pointer.nativeValue(RuneLiteGameHandle.getPointer());
			setLongLE(30000070, hwndVal);
		//}
	}

	public WinDef.HWND findRsUiDisplayerWindow()
	{
		long hwndVal = getLongLE(30000060); //read handle from shared memory

		Pointer hwndPointer = new Pointer(hwndVal);
		WinDef.HWND handle = new WinDef.HWND(hwndPointer);

		if (User32.INSTANCE.IsWindow(handle))
		{
			//log.debug("found rsUiDisplayer window");
			return handle;
		}
		else
		{
			//log.debug("rsUiDisplayer window not yet found");
			return null;
		}
	}

/*	public void ChildUiWinToRl()
	{
		if(RsUiDisplayerHandle == null) {
			RsUiDisplayerHandle = findRsUiDisplayerWindow();

			JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
			WinDef.HWND rlWinHandle = new WinDef.HWND(Native.getComponentPointer(window));
			User32.INSTANCE.SetParent(RsUiDisplayerHandle, rlWinHandle);
		}
	}*/

	public static Dimension getLargestMonitorResolution() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices();

		Dimension largest = new Dimension(0, 0);
		for (GraphicsDevice screen : screens) {
			DisplayMode dm = screen.getDisplayMode();
			int width = dm.getWidth();
			int height = dm.getHeight();
			if (width * height > largest.width * largest.height) {
				largest.setSize(width, height);
			}
		}

		return largest;
	}

	public boolean ChildRuneModWinToRl()
	{
		if (!runeModPlugin.config.attachRmWindowToRL())
		{
			return false;
		}

		if (RmWinIsChilded)
		{
			return true;
		}

		if (RuneModHandle == null)
		{
			RuneModHandle = findRuneModWindow();
		}

		if (RuneModHandle == null)
		{
			return false;
		}

		log.debug("ChildingRuneModWindowToRL ...");
		rlRootWin = SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas().getParent().getParent());
		RuneLiteRootHandle = new WinDef.HWND(Native.getComponentPointer(rlRootWin));


		{ //removes runemod icon from taskbar
			int WS_EX_APPWINDOW = 0x00040000;
			int WS_EX_TOOLWINDOW = 0x00000080;
			int exStyle = User32.INSTANCE.GetWindowLong(RuneModHandle, WinUser.GWL_EXSTYLE);
			exStyle &= ~WS_EX_APPWINDOW;
			exStyle |= WS_EX_TOOLWINDOW;
			User32.INSTANCE.SetWindowLong(RuneModHandle, WinUser.GWL_EXSTYLE, exStyle);
		}

		User32.INSTANCE.SetWindowLongPtr(RuneModHandle, User32.GWL_HWNDPARENT, RuneLiteRootHandle.getPointer());
		//wndProc.installHook(RuneLiteRootHandle);


		//Dimension screenSize = getLargestMonitorResolution();
		//User32.INSTANCE.SetWindowPos(findRsUiDisplayerWindow(), null, 0, 0, screenSize.width, screenSize.height, User32.SWP_NOACTIVATE | User32.SWP_NOZORDER | User32.SWP_NOREDRAW | User32.SWP_NOCOPYBITS);

		//ensure runemod window is infront of rl window.
		User32.INSTANCE.SetWindowPos(RuneModHandle, User32.INSTANCE.GetWindow(RuneLiteRootHandle, new WinDef.DWORD(WinUser.GW_HWNDPREV)), 0, 0, 0, 0, User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_NOACTIVATE);


		RmWinIsChilded = true;

		RuneModPlugin.toggleRuneModLoadingScreen(false);
		//FocusRl(); //ensure runemod zorder is automatically correct by updating it.
		return true;
	}

	public void UnChildRuneModWinFromRl()
	{
		log.debug("UnChildingRuneModWindowToRL");
		if (!RuneModPlugin.unrealIsReady)
		{
			return;
		}
		log.debug("UnChildingRuneModWindowToRL .");
		if (runeModPlugin.config.attachRmWindowToRL())
		{
			return;
		}
		log.debug("UnChildingRuneModWindowToRL ..");
		if (!RmWinIsChilded)
		{
			return;
		}
		log.debug("UnChildingRuneModWindowToRL ...");
		if (RuneModHandle == null)
		{
			RuneModHandle = findRuneModWindow();
		}
		if (RuneModHandle == null)
		{
			return;
		}

		log.debug("UnChildingRuneModWindowToRL ....");

		User32.INSTANCE.SetParent(RuneModHandle, null);
		RmWinIsChilded = false;
	}

	void checkIsRsData()
	{
		byte dataType = SharedMemoryData.getByte(SharedMemoryDataSize - 1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		if (dataType != 1)
		{
			log.debug("is not unreal data. dataType is " + dataType);
		}
	}

	boolean checkIsUnrealData()
	{
		byte dataType = SharedMemoryData.getByte(SharedMemoryDataSize - 1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		//log.debug("is not unreal data. dataType is " + dataType);
		return dataType == 2;
	}

	WinDef.RECT r_prevFrame = new WinDef.RECT();
	public void updateRmWindowTransform()
	{
		if (!RuneModPlugin.unrealIsReady)
		{
			return;
		}
		if (!runeModPlugin.config.attachRmWindowToRL())
		{
			return;
		}
		if (!runeModPlugin.client.getCanvas().isShowing())
		{
			return;
		}


		if (runeModPlugin.client.getCanvas() == null)
		{
			log.debug("Null canvas, wont update rmWindow");
			return;
		}
		if (!runeModPlugin.client.getCanvas().isShowing())
		{
			log.debug("Cant Update RmWindow because rl canvas isnt showing");
			return;
		}

		//position and size runemod window.
		SwingUtilities.invokeLater(() ->
		{
			WinDef.RECT r = new WinDef.RECT();
			User32.INSTANCE.GetWindowRect(RuneLiteGameHandle, r);

			Point sizePrevFrame = new Point(r_prevFrame.right-r_prevFrame.left, r_prevFrame.bottom-r_prevFrame.top);
			Point sizeCureFrame = new Point(r.right-r.left, r.bottom-r.top);

			if(r_prevFrame.bottom!=r.bottom || r_prevFrame.right!=r.right || r_prevFrame.top!=r.top || r_prevFrame.left!=r.left) { //if rect pos has changed, update unreal window
				int flags = 0;
				boolean sizeChanged = !sizePrevFrame.equals(sizeCureFrame);
				if(sizeChanged) {
					log.debug("viewport Size And Maybe Pos Changed");
					flags = User32.SWP_NOACTIVATE | User32.SWP_NOZORDER;
				} else {
					log.debug("viewport Pos Changed");
					flags  = User32.SWP_NOACTIVATE | User32.SWP_NOREDRAW | User32.SWP_NOCOPYBITS | User32.SWP_NOZORDER | User32.SWP_NOSENDCHANGING | User32.SWP_DEFERERASE;
				}

				User32.INSTANCE.SetWindowPos(RuneModHandle, null, r.left, r.top,
					r.right - r.left, r.bottom - r.top,
					flags);
				r_prevFrame = r;
			}
		});

/*		if (runeModPlugin.baseOffsetX != runeModPlugin.lastBaseOffsetX || runeModPlugin.baseOffsetY != runeModPlugin.lastBaseOffsetY*//* || canvas2DSizeX != lastCanvas2DSizeX || canvas2DSizeY != lastCanvas2DSizeY || View3dOffsetX != lastView3DX || View3dOffsetY != lastView3dY || View3dSizeX != lastView3dSizeX || View3dSizeY != lastView3dSizeY*//*)
		{
			*//*
			lastView3DX = View3dOffsetX;
			lastView3dY = View3dOffsetY;
			lastView3dSizeX = View3dSizeX;
			lastView3dSizeY = View3dSizeY;
			lastCanvas2DSizeX = canvas2DSizeX;
			lastCanvas2DSizeY = canvas2DSizeY;
			*//*

			RsUiDisplayerHandle = findRsUiDisplayerWindow();
			if(RsUiDisplayerHandle!=null) {
				log.debug("updating rsuidisplayer pos");
				User32.INSTANCE.SetWindowPos(RsUiDisplayerHandle, null, runeModPlugin.baseOffsetX, runeModPlugin.baseOffsetY, runeModPlugin.canvas2DSizeX, runeModPlugin.canvas2DSizeY, User32.SWP_NOSIZE | User32.SWP_NOACTIVATE | User32.SWP_NOREDRAW | User32.SWP_NOCOPYBITS);

				runeModPlugin.lastBaseOffsetX = runeModPlugin.baseOffsetX;
				runeModPlugin.lastBaseOffsetY = runeModPlugin.baseOffsetY;
			} else {
				log.debug("null rsuidisplayer handle");
			}

			return;
		}*/
	}

	public void handleUnrealData()
	{
		Offset = 0;
		if (!checkIsUnrealData())
		{
			log.debug("isNotUnrealData");
			return;
		} //check, just incase something has gone wrong. if its not unreal data, we dont want to read it.
		while (true)
		{
			byte PacketOpCode = SharedMemoryData.getByte(Offset); //read packet opcode
			//log.debug("read packetOpCode: " + PacketOpCode);
			Offset += 1;

			//log.debug("handling unrealData WithOpCode: " + PacketOpCode);

			if (PacketOpCode == TerminatingPacketOpCode)
			{ //indicates end of data
				//log.debug("terminating Packet Reached. UnrealData Handled.");
				return;
			}

			int packetLength = getMedium(Offset); //read packetLength
			Offset += 3;
			byte[] packetContent = SharedMemoryData.getByteArray(Offset, packetLength); //read packetContent

			Buffer packet = new Buffer(packetContent);
			switch (PacketOpCode)
			{
				case 0: //WindowEvent
					//log.debug("recieved WindowEvent packet");
					break;
				case 1: //terminatorPacket
					//log.debug("recieved terminatorPacket");
					return;
				case 2: //perFramePacket
					//log.debug("recieved unrealPerFramePacket");
					gameCycle_Unreal = packet.readInt();
					//log.debug("UnrealsTick: " + gameCycle_Unreal);
					break;
				case 3: //StatusReport
					String string = packet.readStringCp1252NullTerminated();
					RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText(string, true);
					break;
				case 4: //RequestRsCacheData
					//log.debug("recieved RsCacheData request");
					log.debug("rm says it is awaiting RsCacheData, sending it next tick.");
					runeModPlugin.clientThread.invokeAtTickEnd(() -> //invoke at end of tick, because we cannot communicate with unreal at this point in the communiucation process. (tecnically we can, but only if we dont overflow the backbuffer, which we will do when sending cache)
					{
						runeModPlugin.provideRsCacheData();
					});
					break;
				case 5: //RequestRsCacheHashes
					log.debug("rm says it is awaiting rscacheHashes, sending them next tick.");
					runeModPlugin.clientThread.invokeAtTickEnd(() -> //invoke at end of tick, because we cannot communicate with unreal at this point in the communiucation process. (tecnically we can, but only if we dont overflow the backbuffer, which we will do when sending cache)
					{
						runeModPlugin.runeModAwaitingRsCacheHashes = true;
					});
					break;
				case 6: //RequestSceneReload
					log.debug("recieved unreal RequestSceneReload");
					runeModPlugin.clientThread.invokeAtTickEnd(() ->
					{
						runeModPlugin.reloadUnrealScene();
					});
					break;
				case 7: //AppSettingChanged
					log.debug("recieved unreal AppSettings packet");
					int numElements = packet.readInt();
					for(int i = 0; i < numElements; i++) {
						String changedAppSetting = packet.readStringCp1252NullTerminated();
						runeModPlugin.appSettingChanged(changedAppSetting);
					}
					break;
				default:
					log.debug("unhandled unrealDataType at: " + " Offset: " + Offset + " PacketLen: " + packetLength);
					break;
			}

			Offset += packetLength;
			//log.debug("read PacketContent of length: "+packetContent.length);
		}
	}

	public void startNewRsData()
	{
		Offset = 0;
		setByte(SharedMemoryDataSize - 1, (byte) 1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		//log.debug("Started new rsData");
	}

	void writeTerminatingPacket()
	{
		SharedMemoryData.setByte(Offset, TerminatingPacketOpCode);
		Offset += 1;
	}

	public void passRsDataToUnreal()
	{ //writes dataLength metadata, and releases lock
		writeTerminatingPacket();
		//UnlockMutex();
		//log.debug("Finished Rs Data, unreal may now do its thing, will await unreal data.");
	}

	public boolean isUnrealData()
	{
		return SharedMemoryData.getByte(SharedMemoryDataSize - 1) == (byte) 2;
	}

	public void SetTimeBeginPeriod() {
		myKernel32.timeBeginPeriod(1);
	}

	public interface MyKernel32 extends Kernel32
	{
		MyKernel32 INSTANCE = Native.loadLibrary("kernel32", MyKernel32.class, W32APIOptions.DEFAULT_OPTIONS);
		int timeBeginPeriod(int period);
		int timeEndPeriod(int period);
	}

	public interface MyUser32 extends Library
	{
		MyUser32 INSTANCE = Native.loadLibrary("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean ScreenToClient(WinDef.HWND hWnd, WinDef.POINT lpPoint);
	}
}