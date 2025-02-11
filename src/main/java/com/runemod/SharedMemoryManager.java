package com.runemod;


import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;

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
	public Pointer SharedMemoryData_ViewPort;
	private WinNT.HANDLE SharedMemoryHandle;
	private WinNT.HANDLE SharedMemoryHandle_ViewPort;
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

	public interface MyUser32 extends User32 {

		public static final MyUser32 MYINSTANCE = (MyUser32) Native.loadLibrary("user32", MyUser32.class, W32APIOptions.UNICODE_OPTIONS);

		/**
		 * Sets a new address for the window procedure (value to be set).
		 */
		public static final int GWLP_WNDPROC = -4;

		/**
		 * Changes an attribute of the specified window
		 * @param   hWnd        A handle to the window
		 * @param   nIndex      The zero-based offset to the value to be set.
		 * @param   callback    The callback function for the value to be set.
		 */
		public int SetWindowLong(WinDef.HWND hWnd, int nIndex, Callback callback);
	}

	User32.HWND hwnd;
	WinDef.RECT rect;
	User32.HDC hdcScreen;
	User32.HDC hdcMem;
	WinGDI.BITMAPINFO bmi;
	User32.HBITMAP hBitmap;
	BufferedImage image;
	Memory pBits;
	int width;
	int height;


	public SharedMemoryManager(RuneModPlugin runeModPlugin_) {
		runeModPlugin = runeModPlugin_;
		myKernel32 = MyKernel32.INSTANCE;
	}

	private WinUser.HHOOK hHook;
	WinUser.HOOKPROC hookFunction = new WinUser.HOOKPROC() {
		public WinUser.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
				if (nCode >= 0 && RuneModHandle != null) {
					User32.INSTANCE.PostMessage(RuneModHandle, wParam.intValue(), wParam, lParam);
					System.out.println("callback codes : " + nCode + " " + wParam.intValue() + " " + lParam.longValue());
				}
			WinDef.LRESULT nextHook = User32.INSTANCE.CallNextHookEx(hHook, nCode, wParam, lParam);
			if(nextHook == null) {
				System.out.println("nextHook is null");
				return nextHook;
			} else {
				return nextHook;
			}
		}
	};

	WinUser.WindowProc windowProc;

	public void setHook() {
/*		windowProc = new WinUser.WindowProc() {
			@Override
			public WinUser.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
				System.out.println("Message received: " + uMsg);
				return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
			}
		};

		runeModPlugin.clientThread.invokeLater(() ->
				{
					WinDef.HWND hWnd = new WinDef.HWND();
					hWnd.setPointer(Native.getWindowPointer(SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas())));
					MyUser32.MYINSTANCE.SetWindowLong(hWnd, WinUser.GWL_WNDPROC, windowProc);
				});*/
/*		if(true) return;
		SwingUtilities.invokeLater(() ->
		{
			JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
			WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame",window.getTitle());
			WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);


			int processThreadId = User32.INSTANCE.GetWindowThreadProcessId(RLHandle, null);

			System.out.println("processThreadId: "+processThreadId);

			hHook = User32.INSTANCE.SetWindowsHookEx(9, hookFunction, hMod, processThreadId);

			if (hHook == null) {
				System.err.println("Failed to set hook: " + Native.getLastError());
			}
		});*/

	}

	public void checkMessages() {


		//User32.INSTANCE.SetWindowLongPtr(RLHandle, WinUser.GWL_WNDPROC, windowProc.getPointer());
/*		SwingUtilities.invokeLater( () -> {
			WinUser.MSG msg = new WinUser.MSG();
			JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
			WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame", window.getTitle());

			while (User32.INSTANCE.GetMessage(msg, RLHandle, 0, 0) != 0) {
				User32.INSTANCE.TranslateMessage(msg);
				User32.INSTANCE.DispatchMessage(msg);

				System.out.println("message: "+msg.message + " lparam: "+msg.lParam + " wParam: "+msg.wParam);
				System.out.println();
				System.out.println();
			}
		});*/

			// Check for specific messages
/*			if (msg.message == WinUser.WM_SIZE) {
				System.out.println("Window resized");
			} else if (msg.message == WinUser.WM_MOVE) {
				System.out.println("Window moved");
			}*/
			// Add more message checks as needed

	}

	public void removeHook() {
		User32.INSTANCE.UnhookWindowsHookEx(hHook);
	}

/*
	public void initScreenCaptureObjects() {
		if(hBitmap!=null) {
			GDI32.INSTANCE.DeleteObject(hBitmap);
			hBitmap = null;
		}

		if(hdcMem!=null) {
			GDI32.INSTANCE.DeleteDC(hdcMem);
			hdcMem = null;
		}

		if(hdcScreen!=null) {
			User32.INSTANCE.ReleaseDC(hwnd, hdcScreen);
			hdcScreen = null;
		}


		JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
		WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame",window.getTitle());

		hwnd = RLHandle;

		hdcScreen = User32.INSTANCE.GetDC(hwnd);
		hdcMem = GDI32.INSTANCE.CreateCompatibleDC(hdcScreen);

		rect = new WinDef.RECT();
		User32.INSTANCE.GetClientRect(hwnd, rect);
		width = rect.right - rect.left;
		height = rect.bottom - rect.top;

		if(width < 1 || height < 1) {System.out.println("not initing screencapture screen cos 0 area"); hwnd = null; return;}

		System.out.println("initScreenCaptureObjects");

		hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcScreen, width, height);
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		// Allocate native memory for pixel data
		pBits = new Memory(width * height * 4);

		bmi = new WinGDI.BITMAPINFO();
		bmi.bmiHeader.biSize = bmi.size();
		bmi.bmiHeader.biWidth = width;
		bmi.bmiHeader.biHeight = -height; // Negative to indicate a top-down DIB
		bmi.bmiHeader.biPlanes = (short) 1;
		bmi.bmiHeader.biBitCount = (short) 32;
		bmi.bmiHeader.biCompression = 0;
		bmi.bmiHeader.biSizeImage = 0;
		bmi.bmiHeader.biXPelsPerMeter = 0;
		bmi.bmiHeader.biYPelsPerMeter = 0;
		bmi.bmiHeader.biClrUsed = 0;
		bmi.bmiHeader.biClrImportant = 0;
	}
*/


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

	public void createSharedMemory_ViewPort(String sharedMemoryName, int sharedMemorySize)
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
	}

	public boolean CreateMutexes() {
		RuneliteMutex = myKernel32.CreateMutex(null,
				true,
				"RuneliteMutexRM");
		if (RuneliteMutex == null) {
			throw new Win32Exception(myKernel32.GetLastError());
			//return false;
		} else {
			System.out.println("Created RuneliteMutex");
			myKernel32.ReleaseMutex(RuneliteMutex); //release the mutex
		}

		UnrealMutex = myKernel32.CreateMutex(null,
				true,
				"UnrealMutexRM");
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
			int val = myKernel32.WaitForSingleObject(UnrealMutex,0); //if aquired lock
			boolean aquiredLock = val == 0;
			if(aquiredLock == false) { //if failed, means is already locked;
				RuneModPlugin.log_Timed_Verbose("AwaitUnrealMutex_Locked val = " + val);
				return true;
				//break;
			} else {
				myKernel32.ReleaseMutex(UnrealMutex);
				waitNanos(40000);
				if (System.nanoTime()-awaitStart > timeOutMs*1000000 || RuneModPlugin.isShutDown) { //if waited more than timeout, or runemod plugin has shutdown, abort  waiting
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
			int val = myKernel32.WaitForSingleObject(UnrealMutex, INFINITE);
			RuneModPlugin.log_Timed_Verbose("AwaitUnrealMutex_UnLocked val = " + val);
			boolean val_b = myKernel32.ReleaseMutex(UnrealMutex);
			RuneModPlugin.log_Timed_Verbose("AwaitUnrealMutex_UnLocked val_b = " + val);
	}

	void LockRuneliteMutex() {
		int val = myKernel32.WaitForSingleObject(RuneliteMutex, INFINITE); //if aquired lock
		RuneModPlugin.log_Timed_Verbose("LockRuneliteMutex val = " + val);
	}

	void UnLockRuneliteMutex() {
		boolean val_b = myKernel32.ReleaseMutex(RuneliteMutex);
		RuneModPlugin.log_Timed_Verbose("UnLockRuneliteMutex val_b = " + val_b);
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
		setByte(SharedMemoryDataSize -1, (byte)0); //last byte defines DataType; 2 = unrealdata. 1 = rsdata. on close, set this to 0 so unreal can run without crash
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

		if (SharedMemoryHandle_ViewPort != null)
		{
			myKernel32.CloseHandle(SharedMemoryHandle_ViewPort);
			SharedMemoryHandle_ViewPort = null;
		}

		if (SharedMemoryData_ViewPort != null)
		{
			myKernel32.UnmapViewOfFile(SharedMemoryData_ViewPort);
			SharedMemoryData_ViewPort = null;
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

/*
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
*/


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
			System.out.println("found runemod window");
			return handle;
		} else {
			System.out.println("runemod window not yet found");
			return null;
		}
	}

	private static WinDef.HWND findWindowByProcessId(int processId) {
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
	}

	public WinDef.HWND getSelfHWND() {
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

	public boolean putRuneModOnTop() {
		if(!RuneModPlugin.unrealIsReady) {return false;}

		if(!runeModPlugin.config.attachRmWindowToRL()) {return false;}

		if(RuneModHandle != null) { }
		else {
			RuneModHandle = findRuneModWindow();
		}
		if (RuneModHandle == null) { return false;}

		System.out.println("putRuneModOnTop");
		//System.out.println("zzzzzz");
		//JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
		//WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame",window.getTitle());

		//Pointer currentLongPtr = User32.INSTANCE.GetWindowLongPtr(RuneModHandle, WinUser.GWL_EXSTYLE).toPointer();
		//Pointer newLongPtr = new Pointer(currentLongPtr.getLong(0) | 0x00000008);
		//User32.INSTANCE.SetWindowLongPtr(RuneModHandle, WinUser.GWL_EXSTYLE, newLongPtr);

		//WinDef.HWND topmost = new WinDef.HWND(new Pointer(-1));
		// Apply the new style
		User32.INSTANCE.SetWindowPos(RuneModHandle, new User32.HWND(new Pointer(-1)), 0, 0, 0, 0, WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE);
		//SetWindowPos(RuneModHandle, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);

		return true;
	}

	public static void setClickThrough(WinDef.HWND hwnd) {
		int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT);
		//User32.INSTANCE.SetLayeredWindowAttributes(hwnd, 0, (byte)255, WinUser.LWA_ALPHA);
	}

	public WinDef.HWND RuneModHandle = null;

	public boolean isChilded = false;
	public boolean ChildRuneModWinToRl() {
		//if(runeModPlugin.client.getGameState().ordinal()<=GameState.LOGIN_SCREEN.ordinal()) { return false; }  //prevents childing while on login screen, because that causes a period where the client becomes unfocused which prevents you typing your user/pass
		//if(!RuneModPlugin.unrealIsReady) {return false;}

		if(!runeModPlugin.config.attachRmWindowToRL()) {return false;}

		if(isChilded) {return true;}

		if(RuneModHandle == null) { RuneModHandle = findRuneModWindow(); }
		if (RuneModHandle == null) { return false; }
/*		User32.INSTANCE.SetWindowLongPtr(RuneModHandle *//*The handle of the window to remove its borders*//*, User32.GWL_STYLE, User32.WS_POPUP);
		User32.INSTANCE.SetWindowLongPtr(RuneModHandle *//*The handle of the window to remove its borders*//*, User32.GWL_STYLE, User32.WS_POPUP, );*/

		System.out.println("ChildingRuneModWindowToRL ...");

		//JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
		//WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame",window.getTitle());
		//WinDef.HWND RLHandle = getSelfHWND();

		//WinDef.HWND mainFrameComponentHandle = new WinDef.HWND(Native.getComponentPointer(SwingUtilities.getAncestorOfClass(Frame.class, runeModPlugin.client.getCanvas())));
		WinDef.HWND mainFrameComponentHandle = new WinDef.HWND(Native.getComponentPointer(runeModPlugin.client.getCanvas().getParent()));

		//setHook();
		//User32.INSTANCE.SetWindowPos(RuneModHandle, null, 0, 0, 100, 100, 0);
		User32.INSTANCE.SetParent(RuneModHandle, mainFrameComponentHandle);
		isChilded = true;
		RuneModPlugin.toggleRuneModLoadingScreen(false);

		//User32.INSTANCE.ShowWindow(RuneModHandle, User32.SW_SHOW); //unreal minimizes itself when it starts, so we show it here

		WinDef.HWND rmControls = User32.INSTANCE.FindWindow(null,"RuneModControls");

		//User32.INSTANCE.SetWindowLongPtr(rmControls, User32.GWL_HWNDPARENT, RuneModHandle.getPointer());
		//bring rm controls to front. if we dotn tdo this, rm controls dont appear at front until we reactivate rl win
		User32.INSTANCE.SetWindowPos(rmControls, User32.INSTANCE.GetWindow(mainFrameComponentHandle, new WinDef.DWORD(User32.GW_HWNDPREV)), 0, 0, 0, 0,  User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_FRAMECHANGED);

		//User32.INSTANCE.SetWindowLongPtr(RuneModHandle, User32.GWL_HWNDPARENT, mainFrameComponentHandle.getPointer());

		//runeModPlugin.client.getCanvas().getParent().setComponentZOrder(runeModPlugin.client.getCanvas(), 1);

/*		WinDef.HWND ui_overlay_handle = new WinDef.HWND(Native.getComponentPointer(runeModPlugin.UI_Overlay));
		setClickThrough(ui_overlay_handle);*/

/*		// Get current window styles
		int currentStyle = User32.INSTANCE.GetWindowLong(mainFrameComponentHandle, WinUser.GWL_STYLE);

		// Remove WS_CLIPCHILDREN flag
		int newStyle = currentStyle & ~WinUser.WS_CLIPCHILDREN;

		// Set the new window style
		User32.INSTANCE.SetWindowLong(mainFrameComponentHandle, WinUser.GWL_STYLE, newStyle);

		// Update the window to reflect changes
		User32.INSTANCE.SetWindowPos(mainFrameComponentHandle, null, 0, 0, 0, 0, WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOZORDER | WinUser.SWP_NOACTIVATE | WinUser.SWP_FRAMECHANGED);

		if (mainFrameComponentHandle != null) {
			int style = User32.INSTANCE.GetWindowLong(mainFrameComponentHandle, User32.GWL_STYLE);
			boolean hasClipChildren = (style & User32.WS_CLIPCHILDREN) != 0;
			System.out.println("Window has WS_CLIPCHILDREN flag: " + hasClipChildren);
		} else {
			System.out.println("Window not found.");
		}*/

/*		Robot  robot = null;
		try {
			robot = new Robot();
			System.out.println("Drawing screenshot to canvas");
			Rectangle screenRect = new Rectangle(runeModPlugin.client.getCanvas().getBounds());
			BufferedImage screenImage = robot.createScreenCapture(screenRect);
			runeModPlugin.client.getCanvas().getGraphics().drawImage(screenImage, 0, 0, null);
		} catch (AWTException awtException) {
			awtException.printStackTrace();
		}

		WinDef.HWND canvasComponentHandle = new WinDef.HWND(Native.getComponentPointer(runeModPlugin.client.getCanvas()));

		WinDef.HWND TopMost = new WinDef.HWND(new Pointer(-1));
		User32.INSTANCE.SetWindowPos(canvasComponentHandle, TopMost, 0, 0, 0, 0, User32.SWP_NOMOVE | User32.SWP_NOSIZE);
		User32.INSTANCE.SetForegroundWindow(canvasComponentHandle);*/

		//RuneModHandle = findRuneModWindow();
		//User32.INSTANCE.ShowWindow(RuneModHandle, User32.SW_MAXIMIZE); //unreal minimizes itself when it starts, so we show it here

/*		WinDef.HWND ModModeWindow = User32.INSTANCE.FindWindow(null,"ModModeTogglerWin");
		User32.INSTANCE.SetWindowLongPtr(ModModeWindow, GWLP_HWNDPARENT, RLHandle.getPointer());*/

		//updateRmWindowTransform();

		//runeModPlugin.maintainRuneModStatusAttachment();
		return true;
	}

	public void UnChildRuneModWinToRl() {

		System.out.println("UnChildingRuneModWindowToRL");
		if(!RuneModPlugin.unrealIsReady) {return;}
		System.out.println("UnChildingRuneModWindowToRL .");
		if(runeModPlugin.config.attachRmWindowToRL()) {return;}
		System.out.println("UnChildingRuneModWindowToRL ..");
		if(!isChilded) {return;}
		System.out.println("UnChildingRuneModWindowToRL ...");
		if(RuneModHandle == null) { RuneModHandle = findRuneModWindow(); }
		if (RuneModHandle == null) { return;}

		System.out.println("UnChildingRuneModWindowToRL ....");

		User32.INSTANCE.SetParent(RuneModHandle, null);
		isChilded = false;
	}

	public boolean isTopMostWindow() {
		JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
		WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame", window.getTitle());
		WinDef.HWND foreGroundWind = User32.INSTANCE.GetForegroundWindow();

		// Debugging output
		//System.out.println("RLHandle: " + RLHandle);
		//System.out.println("Foreground Window: " + foreGroundWind);

		// Check if RLHandle is null
		if (RLHandle == null) {
			System.out.println("Window not found: " + window.getTitle());
			return false;
		}

		if(RuneModHandle == null) { return false; }

		char[] windowText = new char[512];
		User32.INSTANCE.GetWindowText(foreGroundWind, windowText, windowText.length);
		String title = Native.toString(windowText);
		System.out.println("ForeGroundWindow Title: " + title);

		if(foreGroundWind == null) {
			return runeModPlugin.isTopMost;
		}
		// Compare pointers
		boolean isTopMostWindow = RLHandle.getPointer().equals(foreGroundWind.getPointer());
		boolean topmostWindowIsRm_App = foreGroundWind.getPointer().equals(RuneModHandle.getPointer());
		return isTopMostWindow || topmostWindowIsRm_App;
	}



/*	public boolean ChildRuneModStatusToRl() {
		WinDef.HWND RLStatusHandle = User32.INSTANCE.FindWindow(null,"RuneModStatus");
		WinDef.HWND RLHandle = User32.INSTANCE.FindWindow(null,"RuneLite");

		Point loc = runeModPlugin.client.getCanvas().getLocationOnScreen();
		loc.x += 100;
		loc.y -= runeModPlugin.runeMod_statusUI.frame.getHeight();

		User32.INSTANCE.SetWindowPos(RLStatusHandle, RLHandle, loc.x, loc.y, 300, 20,  User32.SWP_NOSIZE|User32.SWP_NOACTIVATE);
		//User32.INSTANCE.SetWindowLongPtr(RLStatusHandle, User32.GWL_HWNDPARENT, RLHandle.getPointer());
		return true;
	}*/

	void checkIsRsData() {
		byte dataType = SharedMemoryData.getByte(SharedMemoryDataSize-1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		if(dataType!=1) {
			System.out.println("is not unreal data. dataType is " + dataType);
		}
	}

	boolean checkIsUnrealData() {
		byte dataType = SharedMemoryData.getByte(SharedMemoryDataSize-1); //last byte defines DataType; 2 = unrealdata. 1 = rsdata
		if(dataType!=2) {
			System.out.println("is not unreal data. dataType is " + dataType);
			return false;
		}
		return true;
	}

	public void updateRmWindowTransform() {
		//User32.INSTANCE.ShowWindow(RuneModHandle, User32.SW_MAXIMIZE); //unreal minimizes itself when it starts, so we show it here
		//if(true) { return; }
		if (!RuneModPlugin.unrealIsReady) { return; }
		if (!runeModPlugin.config.attachRmWindowToRL()) { return; }
		if (!runeModPlugin.client.getCanvas().isShowing()) { return; }

/*		if(runeModPlugin.UI_Overlay != null) {
			runeModPlugin.UI_Overlay.setBounds(runeModPlugin.client.getCanvas().getBounds());
		}*/

		Container parent = runeModPlugin.client.getCanvas().getParent();
		int canvasPosX = parent.getLocationOnScreen().x;
		int canvasPosY = parent.getLocationOnScreen().y;
		int canvasSizeX = parent.getWidth();
		int canvasSizeY = parent.getHeight();

		float dpiScalingFactor = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0f; // 96 DPI is the standard

		// Adjust position and size based on DPI scaling
		canvasPosX = Math.round(canvasPosX * dpiScalingFactor);
		canvasPosY = Math.round(canvasPosY * dpiScalingFactor);
		canvasSizeX = Math.round(canvasSizeX * dpiScalingFactor);
		canvasSizeY = Math.round(canvasSizeY * dpiScalingFactor);

		//temporarily added to test different childing method
		canvasPosX=0;
		canvasPosY=0;

		System.out.println("Updating RuneMod windows. PosX: " + canvasPosX + " posY: " + canvasPosY + " sizeX: " + canvasSizeX + " sizeY: " + canvasSizeY);

		//JFrame window = (JFrame) SwingUtilities.getWindowAncestor(runeModPlugin.client.getCanvas());
		//WinDef.HWND RLHandle = User32.INSTANCE.FindWindow("SunAwtFrame",window.getTitle());

		User32.INSTANCE.SetWindowPos(RuneModHandle, null, canvasPosX, canvasPosY, canvasSizeX, canvasSizeY, 0);
		/*		// Create POINT for the position
		WinDef.POINT point = new WinDef.POINT();
		point.x = canvasPosX;
		point.y = canvasPosY;

		// Create SIZE for the size
		WinUser.SIZE size = new WinUser.SIZE();
		size.cx = canvasSizeX;
		size.cy = canvasSizeY;

		// Call UpdateLayeredWindow
		User32.INSTANCE.UpdateLayeredWindow(RuneModHandle, null, point, size, null, null, 0, null, WinUser.ULW_ALPHA);*/
	}

/*
	public BufferedImage captureScreen() {
		if(hwnd == null) { System.out.println("null hwnd, not capturing screen"); return image; }
		if(width < 1 || height < 1) {System.out.println("not capturing screen cos 0 area"); return image;}
		System.out.println("captureScreen");
*/
/*		Rectangle screenRect = new Rectangle(client.getCanvas().getBounds());
		BufferedImage screenImage = robot.createScreenCapture(screenRect);
		client.getCanvas().getGraphics().drawImage(screenImage, 0, 0, null);*//*


		GDI32.INSTANCE.SelectObject(hdcMem, hBitmap);
		GDI32.INSTANCE.BitBlt(hdcMem, 0, 0, width, height, hdcScreen, 0, 0, 0x00CC0020);


		// Pass the BITMAPINFO structure to GetDIBits
		GDI32.INSTANCE.GetDIBits(hdcMem, hBitmap, 0, height, pBits, bmi, 0);

		// Copy pixel data from native memory to BufferedImage
		pBits.read(0, ((DataBufferInt) image.getRaster().getDataBuffer()).getData(), 0, width * height);

		//GDI32.INSTANCE.DeleteObject(hBitmap);
		//GDI32.INSTANCE.DeleteDC(hdcMem);
		//User32.INSTANCE.ReleaseDC(hwnd, hdcScreen);

		return image;
	}
*/

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
				case 7: //StatusReport
					//RuneModPlugin.runeMod_statusUI.SetStatus_Detail(packet.readStringCp1252NullTerminated());
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
		UnlockMutex();
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