package com.runemod;

import lombok.SneakyThrows;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


// This class downloads a file from a URL.
class RuneMod_Launcher implements Runnable {
    volatile boolean run = true;
    private boolean firstrun = true;
    public String rmAppLocation = System.getProperty("user.home") + "\\.runemod\\application\\";
    public String AltRuneModLocation = "";
    boolean AutoLaunch;
    public Process runemodApp = null;

    RuneMod_Launcher(String altRuneModLocation, boolean AutoLaunch_) {
        AltRuneModLocation = altRuneModLocation;
        AutoLaunch = AutoLaunch_;
    }

    @SneakyThrows
    @Override
    public void run() {
        if (run) {
            if (firstrun) {
                onStart();
                firstrun = false;
            }
        }
    }


    public int getLatestAppVersion() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://pub-64c85893ea904aedab24caeb10432ae1.r2.dev/application/version.txt")).build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Failed to fetch latest version no");
            return -1;
        } catch (InterruptedException e) {
           // e.printStackTrace();
            System.out.println("Failed to fetch latest version no");
            return -2;
        }
        if (response!=null && response.statusCode() == 200) {
            int version = Integer.parseInt(response.body());
            return (version);
        } else {
            return -3;
        }
    }

    public int getLocalAppVersion() {
        String content = "-1";
        try {
            content = new String(Files.readAllBytes(Paths.get(rmAppLocation +"\\version.txt")));
        } catch (IOException e) {
            System.out.println("Failed to find local version");
            return -1;
        }
        return Integer.parseInt(content);
    }

    @SneakyThrows
    public void SetCurrentAppVersion(int version) {
        String version_string = ""+version;
        Files.write(Paths.get(rmAppLocation +"\\version.txt"), version_string.getBytes());
    }

    public static void deleteDirectory(File file) {
        if (file == null || !file.exists()) {
            System.out.println("The specified path does not exist.");
            return;
        }

        if (file.isDirectory()) {
            for (File subfile : file.listFiles()) {
                deleteDirectory(subfile);
            }
        }

        if (file.delete()) {
            System.out.println("Deleted: " + file.getAbsolutePath());
        } else {
            System.out.println("Failed to delete: " + file.getAbsolutePath());
        }
    }

    @SneakyThrows
    public void onStart() {


        if(AltRuneModLocation.length()>1) {
            LaunchApp(AltRuneModLocation);
            return;
        }

        int latestAppVersion = getLatestAppVersion();
        System.out.println("latest version = " + latestAppVersion);
        int currentAppVersion = getLocalAppVersion();
        System.out.println("current version = " + currentAppVersion);

        //if the local version is correct but somehow the user is missing the runemod exe file, we download the runemod app files regardless of the local version.
        boolean runeModExeExists = Files.exists(Paths.get(rmAppLocation +"Windows\\RuneMod\\Binaries\\Win64\\"+"RuneMod-Win64-Shipping.exe"));

        if(!runeModExeExists) {
            RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Runemod.exe could not found, so downloading rm app files", true);
        } else {
            if(currentAppVersion < 0 || latestAppVersion > currentAppVersion) {
                RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Runemod.exe exists, but local version is not up to date, so downloading rm app files", true);
            }
        }


        if (currentAppVersion < 0 || latestAppVersion > currentAppVersion || !runeModExeExists) {
           //delete old app folder.
            System.out.println("Deleting old rm app at"+ rmAppLocation+"Windows");
            File directoryToDelete = new File(rmAppLocation+"Windows");
            deleteDirectory(directoryToDelete);

            Files.createDirectories(Paths.get(rmAppLocation));

            String zipFilePath = rmAppLocation +"Windows.zip";
            downloadZip("https://pub-64c85893ea904aedab24caeb10432ae1.r2.dev/application/windows.zip", zipFilePath);
            UnzipFile(zipFilePath, rmAppLocation);
            SetCurrentAppVersion(latestAppVersion);
            try {
                Files.delete(Paths.get(zipFilePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(Files.exists(Paths.get(rmAppLocation +"Windows\\RuneMod\\Binaries\\Win64\\"+"RuneMod-Win64-Shipping.exe"))) {
            LaunchApp(rmAppLocation +"Windows\\RuneMod\\Binaries\\Win64\\"+"RuneMod-Win64-Shipping.exe");
        } else  {
            RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Launch failed: Runemod.exe could not be found", true);
        }
    }

    public void downloadZip(String URL, String filePath) {
        System.out.println(rmAppLocation);
        RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Starting RuneMod download...", true);
        try {

            URL url = new URL(URL);



            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            long fileSize = conn.getContentLengthLong();



            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            FileOutputStream fis = new FileOutputStream(filePath);

            byte[] buffer = new byte[8192];
            int count = 0;

            while ((count = bis.read(buffer, 0, buffer.length)) != -1) {
                RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Downloaded: "+(((int)fis.getChannel().size()/100000)/10.0f)+" / " + (((int)fileSize/100000)/10.0f) + "mb", true);
                fis.write(buffer, 0, count);
            }

            fis.close();
            bis.close();

        } catch (IOException e) {
            RuneModPlugin.runeMod_statusUI.SetStatus_Detail("RuneMod Download failed", true);
            e.printStackTrace();
        }
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public void UnzipFile(String fileZip, String destPath) throws IOException {
        RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Unzipping...", true);

        File destDir = new File(destPath);
        byte[] buffer = new byte[8192];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        int noBytesDecompressed = -1;
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    noBytesDecompressed = noBytesDecompressed+len;

                    if(noBytesDecompressed%1000 == 0) { //set status message ever 1kb
                        RuneModPlugin.runeMod_statusUI.SetStatus_Detail("UnZipped "+ noBytesDecompressed/1000000 + "mb", true);
                    }

                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.close();





/*                Path path = Paths.get(app_root).resolve(entry.getName()).normalize();
                if (!path.startsWith(app_root)) {
                    throw new IOException("Invalid ZIP");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    try (OutputStream os = Files.newOutputStream(path)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            noBytesDecompressed = noBytesDecompressed+len;
                            runeMod_statusUI.SetMessage("UnZipped "+ noBytesDecompressed/1000000 + "mb");
                            os.write(buffer, 0, len);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();*/
        RuneModPlugin.runeMod_statusUI.SetStatus_Detail("UnZipping finished", true);
    }

    public void LaunchApp(String filePath) throws IOException {

        if(AutoLaunch == false) { RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Auto launch turned off, will not auto-start runemod.exe. Awaiting manual start of runemod.exe...", true); return;}

        if(AltRuneModLocation.length()>1) {
            RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Launching Alt RuneMod.exe...", true);
        } else {
            RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Launching RuneMod.exe...", true);
        }

/*        File theFile = new File(filePath);

        String folderPath = theFile.getParent();
        String fileName = theFile.getName();*/

        System.out.println("Launch filePath:" + filePath);



        String[] command = {filePath, "-windowed", "-nosplash", "-ResX=2", "-ResY=2", "-WinX=1", "-WinY=1"};

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Combine error and output streams
        runemodApp = processBuilder.start();


        /*        if(AutoLaunch == false) { RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Auto launch turned off, will not auto-start runemod.exe. Awaiting manual start of runemod.exe...", true); return;}

        //String exePath = "C:\\path\\to\\your\\executable.exe";

/*        try {

            //int exitCode = process.waitFor(); // Wait for the process to finish
            //System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }*/

/*        Desktop desktop = Desktop.getDesktop();
        try
        {
            desktop.open(new File(filePath));
            desktop.enableSuddenTermination();
        }
        catch (IOException e)
        {
            RuneModPlugin.runeMod_statusUI.SetStatus_Detail("Failed to execute RuneMod.exe...", true);
            e.printStackTrace();
        }*/

    }

}
