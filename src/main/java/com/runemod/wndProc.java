package com.runemod;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.*;

public class wndProc
{
	public interface WndProc extends StdCallLibrary.StdCallCallback
	{
		WinDef.LRESULT callback(WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
	}

	static User32 user32 = User32.INSTANCE;
	static Kernel32 kernel32 = Kernel32.INSTANCE;

	static Pointer originalWndProc = null;
	static WndProc newWndProc;

	public static void installHook(WinDef.HWND hwnd) {
		final int GWL_WNDPROC = -4;

		// Save original WndProc
		BaseTSD.LONG_PTR originalWndProcLong = user32.GetWindowLongPtr(hwnd, GWL_WNDPROC);
		originalWndProc = Pointer.createConstant(originalWndProcLong.longValue());

		newWndProc = new WndProc() {
			@Override
			public WinDef.LRESULT callback(WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
				if(uMsg == 528) {//message code indicates win was clicked
					//mitigates delay issue slightly
					RuneModPlugin.sharedmem_rm.FocusRl();
				}

				switch (uMsg)
				{
					case 0x0007: // WM_SETFOCUS
						System.out.println("Focus gained");
						// Set parent if needed
						break;

					case 0x0008: // WM_KILLFOCUS
						System.out.println("Focus lost");
						// Remove parent if needed
						break;

					//
				}

				if(uMsg!=49797) {
					System.out.println("hook umsg: "+uMsg);
				}

				// Call original WndProc
				return user32.CallWindowProc(originalWndProc, hWnd, uMsg, wParam, lParam);
			}
		};

		// Register custom WndProc
		user32.SetWindowLongPtr(hwnd, GWL_WNDPROC, CallbackReference.getFunctionPointer(newWndProc));
	}
}