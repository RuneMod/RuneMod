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

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import lombok.SneakyThrows;

import javax.net.ssl.HttpsURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class RuneMod_Launcher
{
	public String rmAppLocation = System.getProperty("user.home") + "\\.runemod\\application\\";
	public String AltRuneModLocation = "";
	public Process runemodApp = null;
	public Process rsUiDisplayer = null;
	boolean AutoLaunch;

	RuneMod_Launcher(String altRuneModLocation, boolean AutoLaunch_)
	{
		AltRuneModLocation = altRuneModLocation;
		AutoLaunch = AutoLaunch_;
	}

	public static void deleteDirectory(File file)
	{
		if (file == null || !file.exists())
		{
			log.debug("The specified path does not exist.");
			return;
		}

		if (file.isDirectory())
		{
			for (File subfile : file.listFiles())
			{
				deleteDirectory(subfile);
			}
		}

		if (file.delete())
		{
			log.debug("Deleted: " + file.getAbsolutePath());
		}
		else
		{
			log.debug("Failed to delete: " + file.getAbsolutePath());
		}
	}

	public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException
	{
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator))
		{
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	public int getLatestAppVersion()
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://files.runemod.net/application/version.txt")).build();
		HttpResponse<String> response = null;
		try
		{
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch (IOException e)
		{
			//e.printStackTrace();
			log.debug("Failed to fetch latest version no");
			return -1;
		}
		catch (InterruptedException e)
		{
			// e.printStackTrace();
			log.debug("Failed to fetch latest version no");
			return -2;
		}
		if (response != null && response.statusCode() == 200)
		{
			int version = Integer.parseInt(response.body());
			return (version);
		}
		else
		{
			return -3;
		}
	}

	public int getLocalAppVersion()
	{
		String content = "-1";
		try
		{
			content = new String(Files.readAllBytes(Paths.get(rmAppLocation + "\\version.txt")));
		}
		catch (IOException e)
		{
			log.debug("Failed to find local version");
			return -1;
		}
		return Integer.parseInt(content);
	}

	@SneakyThrows
	public void SetCurrentAppVersion(int version)
	{
		String version_string = "" + version;
		Files.write(Paths.get(rmAppLocation + "\\version.txt"), version_string.getBytes());
	}

	@SneakyThrows
	public void launch()
	{
		if (AltRuneModLocation.length() > 1)
		{
			LaunchRuneMod(AltRuneModLocation);
			return;
		}

		int latestAppVersion = getLatestAppVersion();
		log.debug("latest version = " + latestAppVersion);
		int currentAppVersion = getLocalAppVersion();
		log.debug("current version = " + currentAppVersion);

		//if the local version is correct but somehow the user is missing the runemod exe file, we download the runemod app files regardless of the local version.
		boolean runeModExeExists = Files.exists(Paths.get(rmAppLocation + "Windows\\RuneMod\\Binaries\\Win64\\RuneMod-Win64-Shipping.exe"));

		if (!runeModExeExists)
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Runemod.exe could not be found, so downloading rm app files", true);
		}
		else
		{
			if (currentAppVersion < 0 || latestAppVersion > currentAppVersion)
			{
				RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Runemod.exe exists, but local version is not up to date, so downloading rm app files", true);
			}
		}


		if (currentAppVersion < 0 || latestAppVersion > currentAppVersion || !runeModExeExists)
		{
			//delete old app folder.
			log.debug("Deleting old rm app at" + rmAppLocation + "Windows");
			File directoryToDelete = new File(rmAppLocation + "Windows");
			deleteDirectory(directoryToDelete);

			Files.createDirectories(Paths.get(rmAppLocation));

			String zipFilePath = rmAppLocation + "Windows.zip";
			if(downloadZip("https://files.runemod.net/application/windows.zip", zipFilePath)) {
				UnzipFile(zipFilePath, rmAppLocation);
				SetCurrentAppVersion(latestAppVersion);
				try
				{
					Files.delete(Paths.get(zipFilePath));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		if (Files.exists(Paths.get(rmAppLocation + "Windows\\RuneMod\\Binaries\\Win64\\RuneMod-Win64-Shipping.exe")))
		{
			LaunchRuneMod(rmAppLocation + "Windows\\RuneMod\\Binaries\\Win64\\RuneMod-Win64-Shipping.exe");
		}
		else
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Launch failed: Runemod.exe could not be found", true);
		}
	}

	public boolean downloadZip(String URL, String filePath)
	{
		log.debug(rmAppLocation);
		RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Starting RuneMod download...", true);
		try
		{

			URL url = new URL(URL);


			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			long fileSize = conn.getContentLengthLong();


			BufferedInputStream bis = new BufferedInputStream(url.openStream());
			FileOutputStream fis = new FileOutputStream(filePath);

			byte[] buffer = new byte[8192];
			int count = 0;

			while ((count = bis.read(buffer, 0, buffer.length)) != -1)
			{
				RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Downloaded: " + (((int) fis.getChannel().size() / 100000) / 10.0f) + " / " + (((int) fileSize / 100000) / 10.0f) + "mb", true);
				fis.write(buffer, 0, count);
			}

			fis.close();
			bis.close();
			return true;

		}
		catch (IOException e)
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("RuneMod Download failed", true);
			e.printStackTrace();
			return false;
		}
	}

	public void UnzipFile(String fileZip, String destPath) throws IOException
	{
		RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Unzipping...", true);

		File destDir = new File(destPath);
		byte[] buffer = new byte[8192];
		ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
		ZipEntry zipEntry = zis.getNextEntry();
		int noBytesDecompressed = -1;
		while (zipEntry != null)
		{
			File newFile = newFile(destDir, zipEntry);
			if (zipEntry.isDirectory())
			{
				if (!newFile.isDirectory() && !newFile.mkdirs())
				{
					throw new IOException("Failed to create directory " + newFile);
				}
			}
			else
			{
				// fix for Windows-created archives
				File parent = newFile.getParentFile();
				if (!parent.isDirectory() && !parent.mkdirs())
				{
					throw new IOException("Failed to create directory " + parent);
				}

				// write file content
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0)
				{
					noBytesDecompressed = noBytesDecompressed + len;

					if (noBytesDecompressed % 1000 == 0)
					{ //set status message ever 1kb
						RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("UnZipped " + noBytesDecompressed / 1000000 + "mb", true);
					}

					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			zipEntry = zis.getNextEntry();
		}

		zis.close();

		RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("UnZipping finished", true);
	}

/*	public void LaunchRsUiDisplayer() throws IOException
	{
		//launch uiDisplayer
		String[] command = {rmAppLocation + "\\ImageCompressor\\RsUiDisplayer.exe"};
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true); // Combine error and output streams

		try
		{
			rsUiDisplayer = processBuilder.start();
			log.debug("Launched RsUiDisplayer");
		}
		catch (IOException e)
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("There was an error launching RsUiDisplayer", true);
			System.err.println("IOException while launching rsUiDisplayer process: " + e.getMessage());
		}
	}*/

	public void LaunchRuneMod(String filePath) throws IOException
	{
		if (AutoLaunch == false)
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Auto launch turned off, will not auto-start runemod.exe. Awaiting manual start of runemod.exe...", true);
			return;
		}

		if (AltRuneModLocation.length() > 1)
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Launching Alt RuneMod.exe...", true);
		}
		else
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("Launching RuneMod.exe...", true);
		}

		log.debug("Launch filePath:" + filePath);


		String[] command = {filePath, "-windowed", "-nosplash", "-ResX=2", "-ResY=2", "-WinX=1", "-WinY=1"};

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true); // Combine error and output streams

		try
		{
			runemodApp = processBuilder.start();
			new Thread(new Runnable() {
				@SneakyThrows
				public void run(){
					while (true) {
						WinDef.HWND handle = SharedMemoryManager.findWindowByPid(runemodApp.pid());
						if (User32.INSTANCE.IsWindow(handle)) {
							User32.INSTANCE.ShowWindow(handle, WinUser.SW_HIDE);
							System.out.println("hiding window after launch");
							break;
						}

						Thread.sleep(20);
					}
				}
			}).start();
		}
		catch (IOException e)
		{
			RuneModPlugin.runeMod_loadingScreen.SetStatus_DetailText("There was an error launching RuneMod", true);
			System.err.println("IOException while launching runemod process: " + e.getMessage());
		}
	}
}
