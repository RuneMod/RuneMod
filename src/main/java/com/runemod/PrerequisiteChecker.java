package com.runemod;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class PrerequisiteChecker {

	// Minimum required version: 14.38.33130.0
	static final VersionInfo MIN_REDIST_VERSION = new VersionInfo(14, 38, 33130, 0);

	static class VersionInfo {
		int Major, Minor, Build, Rev;

		public VersionInfo(int major, int minor, int build, int rev) {
			this.Major = major;
			this.Minor = minor;
			this.Build = build;
			this.Rev = rev;
		}

		public boolean isValid(VersionInfo min) {
			if (Major > min.Major) return true;
			if (Major == min.Major && Minor > min.Minor) return true;
			if (Major == min.Major && Minor == min.Minor && Build > min.Build) return true;
			if (Major == min.Major && Minor == min.Minor && Build == min.Build && Rev >= min.Rev) return true;
			return false;
		}

		@Override
		public String toString() {
			return Major + "." + Minor + "." + Build + "." + Rev;
		}
	}

	public static boolean canLoadDll(String name) {
		try {
			System.loadLibrary(name.replace(".dll", ""));
			return true;
		} catch (UnsatisfiedLinkError e) {
			return false;
		}
	}

	public static VersionInfo getRedistVersionFromRegistry() {
		try {
			String key = "SOFTWARE\\Microsoft\\VisualStudio\\14.0\\VC\\Runtimes\\x64";
			int major = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, key, "Major");
			int minor = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, key, "Minor");
			int build = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, key, "Bld");
			int rev = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, key, "Rbld");
			return new VersionInfo(major, minor, build, rev);
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean isVCRedistInstalled() {
		boolean dllsLoaded = canLoadDll("msvcp140_2.dll") && canLoadDll("vcruntime140_1.dll");

		if (dllsLoaded) return true;

		VersionInfo regVersion = getRedistVersionFromRegistry();
		if (regVersion != null && regVersion.isValid(MIN_REDIST_VERSION)) {
			// Might be stale, try loading the DLLs again
			return canLoadDll("msvcp140_2.dll") && canLoadDll("vcruntime140_1.dll");
		}

		return false;
	}

	static void openWebpage(String uri) {
		try {
			Desktop.getDesktop().browse(new URI(uri));
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	static void createMissingPreReqsPopup() {
		JOptionPane optionPane = new JOptionPane(
			"Missing Unreal Engine Pre-requisites. Please install them and try again",
			JOptionPane.INFORMATION_MESSAGE);

		JButton downloadButton = new JButton("Get the installer here");
		downloadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.printf(e.getActionCommand());
				openWebpage(DivergentStuff.PreReqsFileUrl);
			}
		});

		optionPane.setOptions(new Object[]{downloadButton});
		JDialog dialog = optionPane.createDialog("RuneMod Setup");

		dialog.setVisible(true);
	}
}