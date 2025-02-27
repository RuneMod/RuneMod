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

import com.google.inject.Provides;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.LayoutManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ModelData;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientUI;

import javax.inject.Inject;
import java.awt.Container;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;

import java.util.function.Consumer;

import static net.runelite.api.Constants.CHUNK_SIZE;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileReader;

@PluginDescriptor(
	name = "RuneMod",
	enabledByDefault = true,
	description = "Graphics modifier",
	tags = {"rm", "rune", "mod", "hd", "graphics", "high", "detail", "graphics", "shaders", "textures", "gpu", "shadows", "lights"},
	conflicts = {"GPU", "117 HD"}
)

@Slf4j
public class RuneModPlugin extends Plugin implements DrawCallbacks
{
	public static SharedMemoryManager sharedmem_rm = null;

	public static CacheReader myCacheReader;

	public static RuneMod_Launcher runeModLauncher;

	public static RuneMod_LoadingScreen runeMod_loadingScreen;

	public static Client client_static;

	public static RuneModConfig config_static;

	private int clientPlane = -1; //used to track when plane has changed

	private Set<Integer> hashedEntitys_LastFrame = new HashSet<Integer>(); //used to track spawns/despawnes of entities.

	@Inject
	public Client client;

	@Inject
	public ClientThread clientThread;

	@Inject
	public RuneModConfig config;

	@Inject
	public ClientUI clientUI;

	@Inject
	private ConfigManager configManager;

	static RuneModPlugin runeModPlugin;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private Hooks hooks;


	static public boolean runningFromIntelliJ()
	{
		//boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("jdwp") >= 0;
		boolean isDebug = System.getProperty("launcher.version") == null;
		return isDebug;
	}


	public static float getCurTimeSeconds()
	{
		float curT = (float) (((double) System.currentTimeMillis() / 1000.0D) - 1739538139D);
		return curT;
	}

	public static void log_Timed_Heavy(String message)
	{
		if (config_static.HeavyLogging())
		{
			log.debug("[" + getCurTimeSeconds() + "]	" + message);
		}
	}

	int sharedMemPixelsUpdatedTick = -1; //used to prevent updating sharedMemory twice in the same tick.

	int cacheLastUpdatedTick = Integer.MAX_VALUE; //when a cache file was last seen to be changed.
	boolean rsCacheIsDownloading = true; //used to delay cache reading until cache is fully download.
	HashMap<File, Long> lastSeenCacheFileSizes = new HashMap<File, Long>();

	int ticksSincePluginLoad = 0;

	boolean startedWhileLoggedIn;
	boolean runeModAwaitingRsCacheHashes = false;

	@SneakyThrows
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		log_Timed_Heavy("onBeforeRender");

		alreadyCommunicatedUnreal = false;

		if (client.getGameState().ordinal() >= GameState.STARTING.ordinal() && client.getTopLevelWorldView() != null)
		{
			ticksSincePluginLoad++;
		}


		if (ticksSincePluginLoad == 1)
		{
			startUp_Custom();

			if (client.getGameState().ordinal() > GameState.LOGIN_SCREEN_AUTHENTICATOR.ordinal())
			{
				runeMod_loadingScreen.SetStatus_DetailText("To enable RuneMod, you must first logout", true);
				startedWhileLoggedIn = true;

				Thread.sleep(4000);

				SwingUtilities.invokeLater(() ->
				{
					try
					{
						pluginManager.setPluginEnabled(this, false);
						pluginManager.stopPlugin(this);
					}
					catch (PluginInstantiationException ex)
					{
						log.error("error stopping plugin", ex);
					}

					try
					{
						shutDown();
					}
					catch (Exception exception)
					{
						exception.printStackTrace();
					}
				});

			}
			else
			{
				runeMod_loadingScreen.SetStatus_DetailText("Starting...", true);
				startedWhileLoggedIn = false;

				runeModLauncher.launch();
			}

			myCacheReader.printRevs();
		}

		if (ticksSincePluginLoad <= 1)
		{
			return;
		}


		if (config.OrbitCamera())
		{
			client.setCameraYawTarget(client.getCameraYaw() + 1);
		}

		if (startedWhileLoggedIn)
		{
			return;
		}

		//check if rscache is currently being updated. if not, start runemod launcher.
		if (rsCacheIsDownloading == true && client.getGameState().ordinal() >= GameState.STARTING.ordinal() && client.getGameCycle() % 20 == 0)
		{
			String directory = RUNELITE_DIR + "\\jagexcache\\oldschool\\LIVE";
			File[] files = new File(directory).listFiles();
			for (File file : files)
			{
				if (file != null && file.isFile())
				{
					long lastSeenFileSize = lastSeenCacheFileSizes.getOrDefault(file, -1L);
					if (file.length() != lastSeenFileSize)
					{
						lastSeenCacheFileSizes.put(file, file.length());
						cacheLastUpdatedTick = client.getGameCycle();
						rsCacheIsDownloading = true;
					}
				}
			}

			if (client.getGameCycle() - cacheLastUpdatedTick > 400)
			{
				log.info("RSCache has finished downloading");
				runeMod_loadingScreen.SetStatus_DetailText("Downloaded RS cache", true);
				rsCacheIsDownloading = false;
			}
			else
			{
				runeMod_loadingScreen.SetStatus_DetailText("Downloading RS cache...", true);
			}
		}

		if (runeModAwaitingRsCacheHashes && rsCacheIsDownloading == false)
		{ //provide rscache hashes, if runemod is waiting for them
			clientThread.invokeAtTickEnd(() -> {
				myCacheReader.provideRsCacheHashes();
			});

			runeModAwaitingRsCacheHashes = false;
		}

		log_Timed_Heavy("_0");
		JFrame window = (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas());
		if (!window.getTitle().equals("RuneLite - RuneLite"))
		{
			window.setTitle("RuneLite - RuneLite");
		}

		log_Timed_Heavy("_1");
		if (ticksSincePluginLoad <= 2 || client.getGameState().ordinal() < GameState.LOGGING_IN.ordinal() || config.RuneModVisibility() == false || config.useTwoRenderers() == true)
		{//allows us to display logging in... on login screen
			setGpuFlags(0);
			if (client.getDrawCallbacks() == null)
			{
				communicateWithUnreal("onBeforeRender");
				//clientThread.invokeAtTickEnd(this::communicateWithUnreal); //for times when scenedraw callback isnt available.
			}
		}
		else
		{
			clientThread.invokeAtTickEnd(() -> {
				if (isShutDown == false)
				{
					if (curGpuFlags != 3)
					{
						communicateWithUnreal("onBeforeRender_TickEnd"); //here to set rm visibility before gpu flags get set. we do this to prevent momentarily showing unrendered client before rm visibility is set to true;
						setGpuFlags(3);
					}
				}
			});
		}
	}

	void MaintainRuneModAttachment()
	{
		if (sharedmem_rm == null)
		{
			return;
		}

		if (!runeModPlugin.config.attachRmWindowToRL())
		{
			return;
		}

		if (!unrealIsReady)
		{
			return;
		}


		sharedmem_rm.ChildRuneModWinToRl();

		if (RmNeedsWindowUpdate())
		{
			sharedmem_rm.updateRmWindowTransform();
		}

		sharedmem_rm.setRuneModVisibility(config.RuneModVisibility() == true);
	}

	boolean alreadyCommunicatedUnreal = false; //whether we have communicated with unreal this frame.

	void communicateWithUnreal(String funcLocation)
	{
		if (ticksSincePluginLoad < 3)
		{
			log_Timed_Heavy("ticksSincePluginLoad < 3");
			return;
		}

		if (isShutDown)
		{
			log_Timed_Heavy("isShutDown");
			return;
		}

		if (alreadyCommunicatedUnreal)
		{
			log_Timed_Heavy("Already communicated. cancelled communicateWithUnreal::" + funcLocation);
			return;
		}

		alreadyCommunicatedUnreal = true;


		log_Timed_Heavy("communicateWithUnreal::" + funcLocation);

		SwingUtilities.invokeLater(() -> {
			MaintainRuneModAttachment();
		});


		if (client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal())
		{ //prevents communicating with unreal before client is loaded.
			return;
		}

		log_Timed_Heavy("Start");

		if (!sharedmem_rm.backBuffer.isOverFlowed)
		{
			WritePerFramePacket(); //we start writing the perframe apcket before unreal has indidcated its started a new frame, for a small performance optimization
		}

		log_Timed_Heavy("0");
		//while (true) {

		//wait for ue to signal ueData is ready
		int val = sharedmem_rm.myKernel32.WaitForSingleObject(sharedmem_rm.EventUeDataReady, 100000000);

		log_Timed_Heavy("1 awaitUeDataIsReady = " + val);

		//unsignal EventUeDataReady, as it has just been signalled (according to infinite wait on line above this).
		sharedmem_rm.myKernel32.ResetEvent(sharedmem_rm.EventUeDataReady);

		//read unreal packets
		sharedmem_rm.handleUnrealData();

		//copy buffered rl packets to sharedMem
		sharedmem_rm.startNewRsData();
		sharedmem_rm.transferBackBufferToSharedMem();

		sharedmem_rm.writeTerminatingPacket();

		//signal rldata is ready.
		sharedmem_rm.myKernel32.SetEvent(sharedmem_rm.EventRlDataReady);

		log_Timed_Heavy("2");
	}

	int curGpuFlags = -1; //there is no client.setGpuFlags, so I use this to keep track of them myself.

	void setGpuFlags(int flags)
	{
		if (curGpuFlags != flags)
		{
			client.setGpuFlags(flags);
			client.resizeCanvas(); //resize canvas to force rebuild for working alpha channel
			curGpuFlags = flags;
			client.getCanvas().setIgnoreRepaint(true);
			log.info("GPU Flags have been changed to " + flags);
		}
	}

	@SneakyThrows
	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		log_Timed_Heavy("drawScene");
		if (curGpuFlags <= 0)
		{
			log_Timed_Heavy("storedGpuFlags <= 0");
			return;
		}
		if (!client.isGpu())
		{
			log_Timed_Heavy("!client.isGpu()");
			return;
		}
		communicateWithUnreal("drawScene");

		visibleActors.clear();
	}

	Set<Renderable> visibleActors = new HashSet<Renderable>();

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		if (curGpuFlags <= 0)
		{
			return;
		}

/*		(RL) plane = bits >> 49 & 3
		id = bits >> 17 & 0xffffffff
		wall = bits >> 16 & 1
		type = bits >> 14 & 3
		scene y = bits >> 7 & 127
		scene x = bits >> 0 & 127

		Type 0 = player, 1 = npc, 2 = game object, 3 = item*/

		if (renderable instanceof Player)
		{
			visibleActors.add(renderable);
		}
		else
		{
			if (renderable instanceof NPC)
			{
				visibleActors.add(renderable);
			}
			else
			{
				if (renderable instanceof GraphicsObject)
				{
					visibleActors.add(renderable);
				}
				else
				{
					if (renderable instanceof Projectile)
					{
						visibleActors.add(renderable);
					}
				}
			}
		}

		Model model = renderable instanceof Model ? (Model) renderable : renderable.getModel();
		if (model != null)
		{
			// Apply height to renderable from the model
			if (model != renderable)
			{
				renderable.setModelHeight(model.getModelHeight());
			}

			client.checkClickbox(projection, model, orientation, x, y, z, hash);
		}
	}

	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ)
	{
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
	}

	void overlayColourChanged()
	{
		Buffer packet = new Buffer(new byte[8]);

		packet.writeInt(overlayColor_LastFrame);

		//extra blank data, for future use
		packet.writeInt(0);

		sharedmem_rm.backBuffer.writePacket(packet, "OverlayColorChanged");

		log.debug("overlayColourChanged");
	}

	public int overlayColor_LastFrame = 0;

	@SneakyThrows
	@Override
	public void draw(int overlayColor)
	{
		if (overlayColor_LastFrame != overlayColor)
		{
			overlayColor_LastFrame = overlayColor;
			overlayColourChanged();
		}

		log_Timed_Heavy("draw");
		UpdateSharedMemoryUiPixels();
		communicateWithUnreal("Draw");
	}

	@SneakyThrows
	@Override
	public void postDrawScene()
	{
		log_Timed_Heavy("postDrawScene");
	}

	@Override
	public void animate(Texture texture, int diff)
	{
	}

	@Override
	public void loadScene(Scene scene)
	{
		sendBaseCoordinatePacket(scene);
		log.debug("LoadScene");
	}

	@Override
	public void swapScene(Scene scene)
	{
		log.debug("SwapScene");
	}

	static boolean isShutDown = false;

	@Override
	protected void shutDown() throws Exception
	{
		log.info("RuneMod is stopping");
		isShutDown = true;
		clientThread.invoke(() -> {
			if (runeModLauncher != null)
			{
				if (runeModLauncher.runemodApp != null)
				{
					runeModLauncher.runemodApp.destroyForcibly();
				}
			}

			setDefaults();
		});
	}

	void setDefaults()
	{
		clientPlane = -1;

		unrealIsReady = false;

		toggleRuneModLoadingScreen(false);

		runeModLauncher = null;
		myCacheReader = null;

		if (runeMod_loadingScreen != null)
		{
			runeMod_loadingScreen.close();
			runeMod_loadingScreen = null;
		}

		if (sharedmem_rm != null)
		{
			//sharedmem_rm.destroyRuneModWin();
			sharedmem_rm.CloseSharedMemory();
			sharedmem_rm = null;
		}

		setGpuFlags(0);
		setDrawCallbacks(null);

		client.setUnlockedFps(false);
		client.setUnlockedFpsTarget(50);

		lastCavansX = 0;
		lastCavansY = 0;
		lastCavansSizeX = 0;
		lastCavansSizeY = 0;

		curGpuFlags = -1;

		rsCacheIsDownloading = true;
	}

	void setDrawCallbacks(DrawCallbacks drawCallbacks)
	{
		client.setDrawCallbacks(drawCallbacks);

		if (drawCallbacks == null)
		{
			log.info("Changed DrawCallbacks To Null");
		}
		else
		{
			log.info("Changed DrawCallbacks");
		}
	}

	public static boolean runemodLoadingScreenVisibility = false;

	public static JPanel RuneModLoadingScreenPanel = new JPanel();

	public static Container canvasAncestor;
	public static LayoutManager ogLayout;

	public static boolean unrealIsReady = false;

	public static void toggleRuneModLoadingScreen(Boolean toggled)
	{
		if (runemodLoadingScreenVisibility == toggled)
		{
			return;
		}
		runemodLoadingScreenVisibility = toggled;
		log.debug("toggling RmLoadingScreen to " + toggled);
		SwingUtilities.invokeLater(() ->
		{
			if (toggled)
			{
				//JPanel window = (JPanel) SwingUtilities.getAncestorOfClass();
				if (canvasAncestor == null)
				{
					return;
				}
				//RuneModLoadingScreenPanel.removeAll();
				//RuneModLoadingScreen.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 300));

				ogLayout = canvasAncestor.getLayout();

				//canvasAncestor.setLayout(new BorderLayout(0, 0));

				RuneModLoadingScreenPanel.setBackground(Color.black);
				RuneModLoadingScreenPanel.add(runeMod_loadingScreen.labelPanel);
				RuneModLoadingScreenPanel.setSize(canvasAncestor.getSize());
				canvasAncestor.add(RuneModLoadingScreenPanel, BorderLayout.CENTER, 0);
				canvasAncestor.revalidate();
				canvasAncestor.repaint();
			}
			else
			{
				if (canvasAncestor == null)
				{
					return;
				}
				canvasAncestor.remove(RuneModLoadingScreenPanel);
				canvasAncestor.setLayout(ogLayout);
				canvasAncestor.revalidate();
				canvasAncestor.repaint();
			}
		});
	}

	public ApplicationSettings appSettings;

	@SneakyThrows
	void startUp_Custom()
	{
		setDefaults();

		runeModPlugin = this;

		sharedmem_rm = new SharedMemoryManager(this);
		sharedmem_rm.createSharedMemory("sharedmem_rm", 50000000); //50 mb
		sharedmem_rm.CreateNamedEvents();

		JFrame window = (JFrame) SwingUtilities.getAncestorOfClass(Frame.class, client.getCanvas());
		//window.setIgnoreRepaint(true); //doing this could save a bit of performance however it causes visual issues in runelite.

		runeMod_loadingScreen = new RuneMod_LoadingScreen(window, this);

		runeModLauncher = new RuneMod_Launcher(config.UseAltRuneModLocation() ? config.AltRuneModLocation() : "", config.StartRuneModOnStart());
		myCacheReader = new CacheReader();

		isShutDown = false;

		canvasAncestor = client.getCanvas().getParent(); //is "clientPannel" class

		RuneModPlugin.toggleRuneModLoadingScreen(true);

		configManager.setConfiguration("stretchedmode", "keepAspectRatio", true); //We need keepAspectRatio enabled, because runemod does not support nonuniform canvas scaling.

		log.info("runelitDir: " + RUNELITE_DIR);

		{
			sharedmem_rm.startNewRsData();
			sharedmem_rm.transferBackBufferToSharedMem();
			sharedmem_rm.passRsDataToUnreal();
			sharedmem_rm.myKernel32.SetEvent(sharedmem_rm.EventRlDataReady); //signal rl  data is ready. either ue or rl has to signal it is ready for the back and forth communication to begin
		}

		//int GpuFlags = DrawCallbacks.GPU | (computeMode == ComputeMode.NONE ? 0 : DrawCallbacks.HILLSKEW);
		client.getTopLevelWorldView().getScene().setDrawDistance(90);
		client.setExpandedMapLoading(1);

		client.setUnlockedFps(config.MaxFps() > 50);
		client.setUnlockedFpsTarget(config.MaxFps());

		setGpuFlags(0);
		setDrawCallbacks(this);
	}

	public String getBetaKeys()
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://pub-64c85893ea904aedab24caeb10432ae1.r2.dev/beta_keys_hashed.txt")).build();
		HttpResponse<String> response = null;
		try
		{
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch (IOException e)
		{
			//e.printStackTrace();
			log.debug("Failed to fetch beta keys");
			return "-1";
		}
		catch (InterruptedException e)
		{
			// e.printStackTrace();
			log.debug("Failed to fetch beta keys");
			return "-2";
		}
		if (response != null && response.statusCode() == 200)
		{
			return (response).body();
		}
		else
		{
			return "-3";
		}
	}

	public boolean checkBetaKey()
	{
		boolean isValidKey = false;

		if (config.BetaTesterKey().length() > 30)
		{
			String hashedBetaTesterKey = keyGen.hashKey(config.BetaTesterKey());
			String keys = getBetaKeys();
			isValidKey = keys.contains(hashedBetaTesterKey);
		}


		if (!isValidKey)
		{
			SwingUtilities.invokeLater(() ->
			{
				log.debug("invalid BetaTesterKey, stopping plugin");
				try
				{
					pluginManager.setPluginEnabled(this, false);
					pluginManager.stopPlugin(this);
				}
				catch (PluginInstantiationException ex)
				{
				}

				try
				{
					shutDown();
				}
				catch (Exception exception)
				{
					exception.printStackTrace();
				}
			});
		}
		else
		{
			log.debug("beta key is valid");
		}

		return isValidKey;
	}

	@Override
	protected void startUp() throws IOException
	{
		client_static = client;
		config_static = config;

		if (config.DebugLogging())
		{
			((Logger) LoggerFactory.getLogger("com.runemod")).setLevel(Level.DEBUG);
		}
		else
		{
			((Logger) LoggerFactory.getLogger("com.runemod")).setLevel(Level.INFO);
		}

		log.info("Starting RuneMod plugin");

		checkBetaKey();
		ticksSincePluginLoad = -1;
	}

	public static float signedToUnsigned(byte signedByte)
	{
		return signedByte & 0xFF; // Masking with 0xFF to get the unsigned value
	}

	// Convert unsigned byte back to signed byte
	public static byte unsignedToSigned(int unsignedByte)
	{
		if (unsignedByte < 0 || unsignedByte > 255)
		{
			throw new IllegalArgumentException("Value must be between 0 and 255");
		}
		return (byte) unsignedByte; // Casting to byte
	}

	public static byte multiplyByteAsIfUnsigned(byte value, float multiplier)
	{
		float multipliedVal = signedToUnsigned(value) * multiplier;
		return unsignedToSigned(Math.round(multipliedVal));
	}

	public void sendTextures()
	{
		int counter = 0;
		for (int i = 0; i < client.getTextureProvider().getTextures().length; i++)
		{ //sends the textures to unreal to be saved as texture defs and materialdefs
			short texSizeX = 128;
			short texSizeY = 128;
			TextureProvider textureProvider = client.getTextureProvider();
			textureProvider.setBrightness(0.8);
			Texture tex = textureProvider.getTextures()[i];
			int[] pixels = textureProvider.load(i);
			if (tex != null)
			{
				if (pixels != null)
				{
					counter++;
					Buffer mainBuffer = new Buffer(new byte[4 + 2 + 2 + 4 + (texSizeX * texSizeY * 4)]);
					mainBuffer.writeShort(i);
					mainBuffer.writeByte(tex.getAnimationDirection());
					mainBuffer.writeByte(tex.getAnimationSpeed());

					mainBuffer.writeShort(texSizeX);
					mainBuffer.writeShort(texSizeY);
					mainBuffer.writeInt((int) texSizeX * (int) texSizeY * 4); //write byte array length. required  by readByteArray function in ue4

					for (int i0 = 0; i0 < texSizeX * texSizeY; i0++)
					{ //write byte array content
						int pixelValue = pixels[i0];
						byte a = (byte) 255;
						byte r = (byte) ((pixelValue >> 16) & 0xff);
						byte g = (byte) ((pixelValue >> 8) & 0xff);
						byte b = (byte) ((pixelValue >> 0) & 0xff);

						if (r == 0 && b == 0 && g == 0)
						{
							a = 0;
						}

						mainBuffer.writeByte(multiplyByteAsIfUnsigned(b, 0.9f));
						mainBuffer.writeByte(multiplyByteAsIfUnsigned(g, 0.9f));
						mainBuffer.writeByte(multiplyByteAsIfUnsigned(r, 0.9f));
						mainBuffer.writeByte(a);
					}
					RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "Texture");
				}
			}
		}
		log.debug("Sent " + counter + " Textures");
	}

	public void provideRsCacheData()
	{
		myCacheReader.sendObjectDefinitions();
		myCacheReader.sendModels();
		myCacheReader.sendKitDefinitions();
		myCacheReader.sendItemDefinitions();
		myCacheReader.sendSequenceDefinitions();
		myCacheReader.sendNpcDefinitions();
		myCacheReader.sendSpotAnimations();
		myCacheReader.sendSkeletons();
		myCacheReader.sendFrames();
		myCacheReader.sendTiles();
		sendTextures();
		myCacheReader.sendOverlayDefinitions();
		myCacheReader.sendUnderlayDefinitions();

		Buffer mainBuffer = new Buffer(new byte[10]);
		mainBuffer.writeInt(789728); //put this random number in packet content because im not sure it is ok to send empty packets.
		RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "RsCacheDataProvided");
		log.debug("RsCacheData has been provided To Unreal");
	}

	@SneakyThrows
	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		clientThread.invoke(() ->
		{
			if (event.getGroup().equalsIgnoreCase("RuneMod"))
			{
				log.debug("RuneModConfig " + event.getKey() + " Changed to " + event.getNewValue());
				if (event.getKey().equalsIgnoreCase("RuneModVisibility"))
				{
					if (config.RuneModVisibility())
					{
						clientThread.invoke(() -> {
							setDrawCallbacks(this);
						});

					}
					else
					{
						clientThread.invoke(() -> {
							setDrawCallbacks(null);
						});
					}
				}

				if (event.getKey().equalsIgnoreCase("useTwoRenderers"))
				{
					if (config.useTwoRenderers())
					{
						clientThread.invoke(() -> {
							setDrawCallbacks(null);
						});

					}
					else
					{
						clientThread.invoke(() -> {
							setDrawCallbacks(this);
						});
					}
				}

				if (event.getKey().equalsIgnoreCase("DebugLogging"))
				{
					if (config.DebugLogging())
					{
						((Logger) LoggerFactory.getLogger("com.runemod")).setLevel(Level.DEBUG);
					}
					else
					{
						((Logger) LoggerFactory.getLogger("com.runemod")).setLevel(Level.INFO);
					}
				}

				if (event.getKey().equalsIgnoreCase("MaxFps"))
				{
					int MaxFps = config.MaxFps();

					client.setUnlockedFps(MaxFps > 50);

					client.setUnlockedFpsTarget(MaxFps);
				}

				if (event.getKey().equalsIgnoreCase("attachRmWindowToRL"))
				{
					if (config.attachRmWindowToRL())
					{
						sharedmem_rm.ChildRuneModWinToRl();
					}
					else
					{
						sharedmem_rm.UnChildRuneModWinFromRl();
					}
				}
			}
		});
	}

	//not currently used, is part of wip code to handle npcOverrides.
	class NpcOverrides_Copy
	{
		int[] modelIds;

		short[] colorToReplaceWith;

		short[] textureToReplaceWith;

		boolean useLocalPlayer;

		public NpcOverrides_Copy(NpcOverrides overrides)
		{
			this.modelIds = overrides.getModelIds();
			this.colorToReplaceWith = overrides.getColorToReplaceWith();
			this.textureToReplaceWith = overrides.getTextureToReplaceWith();
			this.useLocalPlayer = overrides.useLocalPlayer();
		}

		public boolean isIdenticalTo(NpcOverrides_Copy other)
		{
			boolean isEqual = Arrays.equals(other.modelIds, modelIds);
			if (!Arrays.equals(other.colorToReplaceWith, colorToReplaceWith))
			{
				isEqual = false;
			}
			if (!Arrays.equals(other.textureToReplaceWith, textureToReplaceWith))
			{
				isEqual = false;
			}
			if (!other.useLocalPlayer == useLocalPlayer)
			{
				isEqual = false;
			}
			return isEqual;
		}
	}

	private final HashMap<NPC, NpcOverrides_Copy> npcsWithOverrides_LastFrame = new HashMap<NPC, NpcOverrides_Copy>();
	//if npc in curfram has overrides, and it didnt in last frame, overridesChanged.
	//if npcsWithOverrides in last frame is missing from npcsWithOverrides in currentFrame, overridesChanged.
	//if npc With Overrides is present in last and current frame, check if the overrides from each frame are equal. if they are not, , overridesChanged.


	private void argbIntToColorChannels(int col)
	{
		int a = (col >> 24) & 0xFF;
		int r = (col >> 16) & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = col & 0xFF;
	}


	private void forEachTile(Consumer<Tile> consumer)
	{
		final Scene scene = client.getScene();
		final Tile[][][] tiles = scene.getTiles();

		for (int z = 0; z < Constants.MAX_Z; ++z)
		{
			for (int x = 0; x < Constants.SCENE_SIZE; ++x)
			{
				for (int y = 0; y < Constants.SCENE_SIZE; ++y)
				{
					Tile tile = tiles[z][x][y];

					if (tile == null)
					{
						continue;
					}

					consumer.accept(tile);

					if (tile.getBridge() != null)
					{
						consumer.accept(tile.getBridge());
					}
				}
			}
		}
	}

	public void reloadUnrealScene()
	{
		simulateGameEvents();
	}

	public void simulateGameEvents()
	{
		log.debug("simulating game-events");

		if (client.getGameState() != GameState.LOGGED_IN) //if not logged in, just resend game state
		{
			final GameStateChanged gameStateChanged = new GameStateChanged();
			gameStateChanged.setGameState(client.getGameState());
			onGameStateChanged(gameStateChanged);
			return;
		}

		sendBaseCoordinatePacket();

		final GameStateChanged gameStateChanged = new GameStateChanged();
		gameStateChanged.setGameState(client.getGameState());
		onGameStateChanged(gameStateChanged);

		WritePerFramePacket(); //we start writing the perframe apcket before unreal has indidcated its started a new frame, for a small performance optimization

		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null)
			{
				final NpcSpawned npcSpawned = new NpcSpawned(npc);
				onNpcSpawned(npcSpawned);
			}
		}

		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null)
			{
				final PlayerSpawned playerSpawned = new PlayerSpawned(player);
				onPlayerSpawned(playerSpawned);
			}
		}

		forEachTile((tile) ->
		{
			WallObject wallObject = tile.getWallObject();
			if (wallObject != null)
			{
				final WallObjectSpawned objectSpawned = new WallObjectSpawned();
				objectSpawned.setTile(tile);
				objectSpawned.setWallObject(wallObject);
				onWallObjectSpawned(objectSpawned);
			}

			DecorativeObject decorativeObject = tile.getDecorativeObject();
			if (decorativeObject != null)
			{
				final DecorativeObjectSpawned objectSpawned = new DecorativeObjectSpawned();
				objectSpawned.setTile(tile);
				objectSpawned.setDecorativeObject(decorativeObject);
				onDecorativeObjectSpawned(objectSpawned);
			}

			GroundObject groundObject = tile.getGroundObject();
			if (groundObject != null)
			{
				final GroundObjectSpawned objectSpawned = new GroundObjectSpawned();
				objectSpawned.setTile(tile);
				objectSpawned.setGroundObject(groundObject);
				onGroundObjectSpawned(objectSpawned);
			}

			for (GameObject object : tile.getGameObjects())
			{
				if (object != null)
				{
					if (object.getSceneMinLocation().equals(tile.getSceneLocation()))
					{
						if (object instanceof TileObject)
						{
							if (object.getRenderable() != null)
							{
								if (object.getRenderable() instanceof DynamicObject || object.getRenderable() instanceof Model || object.getRenderable() instanceof ModelData)
								{
									final GameObjectSpawned objectSpawned = new GameObjectSpawned();
									objectSpawned.setTile(tile);
									objectSpawned.setGameObject(object);
									onGameObjectSpawned(objectSpawned);
								}
								else
								{
/*									if(object.getRenderable() instanceof Actor) {
										log.debug("unhandled renderableClass: Actor");
									}*/
								}
							}
						}
					}
				}
			}

			ItemLayer itemLayer = tile.getItemLayer();
			if (itemLayer != null)
			{
				Node current = itemLayer.getTop();
				while (current instanceof TileItem)
				{
					final TileItem item = (TileItem) current;

					current = current.getNext();

					final ItemSpawned itemSpawned = new ItemSpawned(tile, item);
					onItemSpawned(itemSpawned);
				}
			}
		});
	}

	@Provides
	RuneModConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneModConfig.class);
	}

	private int OrientationToAngles(int orient)
	{
		int angle = 1;
		switch (orient)
		{
			case 1:
				angle = 0;
				break;
			case 2:
				angle = 90;
				break;
			case 4:
				angle = 180;
				break;
			case 8:
				angle = 270;
				break;
			case 16:
				angle = 45;
				break;
			case 32:
				angle = 135;
				break;
			case 64:
				angle = 225;
				break;
			case 128:
				angle = 315;
				break;
		}
		if (angle == 1)
		{
			log.debug("orientToAngles Failed" + orient);
		}
		return angle;
	}

	private int getObjModelTypeFromFlags(int flags)
	{
		return flags & 63;
	}

	public void resendGameStateChanged()
	{
		clientThread.invokeAtTickEnd(() ->
		{
			final GameStateChanged gameStateChanged = new GameStateChanged();
			gameStateChanged.setGameState(client.getGameState());
			onGameStateChanged(gameStateChanged);
		});
	}

	GameState curGamestate = GameState.STARTING;
	GameState lastGameState = GameState.STARTING;

	@SneakyThrows
	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (ticksSincePluginLoad <= 1)
		{
			return;
		}
		lastGameState = curGamestate;
		curGamestate = event.getGameState();
		log.debug("gameStateChanged to: ");

		if (curGamestate == GameState.LOGIN_SCREEN)
		{
			appSettings = loadAppSettings(); //load appSettings file

			if (appSettings != null)
			{ //makes login screen background transparent if animeLoginScreen appsetting is true
				if (appSettings.animateLoginScreen)
				{
					SpritePixels spritePixels = client.createSpritePixels(new int[0], 0, 0);
					client.setLoginScreen(spritePixels);
					client.setShouldRenderLoginScreenFire(false);
				}
				else
				{
					client.setLoginScreen(null);
				}
			}
			else
			{
				log.debug("appsettings is null");
			}

			log.debug("Login SCREEN...");
			baseX = -1;
			baseY = -1;
		}
		else if (curGamestate == GameState.LOGGING_IN)
		{

			log.debug("logging in...");
		}
		else if (curGamestate == GameState.LOGGED_IN)
		{
			log.debug("logged in...");
		}
		else if (curGamestate == GameState.HOPPING)
		{
			baseX = -1;
			baseY = -1;

			log.debug("hopping...");
		}
		else if (curGamestate == GameState.LOADING)
		{
			log.debug("loading...");
		}
		else if (curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{
			log.debug("Authenticator SCREEN...");
		}
		else if (curGamestate == GameState.CONNECTION_LOST)
		{
			log.debug("ConnectionLost...");
		}
		else if (curGamestate == GameState.STARTING)
		{
			log.debug("STARTING...");
		}
		else if (curGamestate == GameState.UNKNOWN)
		{
			log.debug("UNKNOWN...");
		}
		log.debug("ordsinal: " + event.getGameState().ordinal());

		Buffer packet = new Buffer(new byte[10]);
		packet.writeByte(event.getGameState().ordinal());
		sharedmem_rm.backBuffer.writePacket(packet, "GameStateChanged");
	}

	@Subscribe
	private void onWallObjectDespawned(WallObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			if (!config.spawnGameObjects())
			{
				return;
			}

			Tile tile;
			if (event.getTile().getBridge() != null)
			{
				tile = event.getTile().getBridge();
			}
			else
			{
				tile = event.getTile();
			}

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getPlane();
			if (event.getTile().getBridge() != null)
			{
				tilePlane++;
			}

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getWallObject());
			actorSpawnPacket.writeByte(5); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			log.debug("wallobject despawned: " + event.getWallObject().getId());
		});
	}

	@Subscribe
	private void onWallObjectSpawned(WallObjectSpawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			if (!config.spawnGameObjects())
			{
				return;
			}
			Tile tile;
			tile = event.getTile();


			Buffer actorSpawnPacket = new Buffer(new byte[100]);
			int tileObjectModelType = getObjModelTypeFromFlags(event.getWallObject().getConfig());
			int var4 = (event.getWallObject().getConfig() - tileObjectModelType) >> 6 & 3;

			int rotation = var4;
			int anint = (rotation * 512);

			int objectOrientationA = anint;
			int objectOrientationB = 1234;
			if (tileObjectModelType == 2)
			{ //if wall is objectType 2. walltype 2 has a model B;
				rotation = (event.getWallObject().getConfig() >> 6) & 3;
				rotation = rotation + 1 & 3;
				anint = (rotation * 512);
				objectOrientationB = anint;
			}
			int objectDefinitionId = event.getWallObject().getId();
			int plane = event.getTile().getRenderLevel();

			int tileX = (event.getWallObject().getX()) / 128;
			int tileY = (event.getWallObject().getY()) / 128;

			int height = event.getWallObject().getZ() * -1;

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			int tileMinPlane = tile.getPlane();
			actorSpawnPacket.writeByte(tileMinPlane);
			actorSpawnPacket.writeShort(height);
			long tag = getTag_Unique(event.getWallObject());
			actorSpawnPacket.writeLong(tag);
			int cycleStart = 0;
			int frame = 0;
			actorSpawnPacket.writeShort(cycleStart);
			actorSpawnPacket.writeShort(frame);
			int offsetX = 0;
			int offsetY = 0;
			actorSpawnPacket.writeShort(offsetX);
			actorSpawnPacket.writeShort(offsetY);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		});
	}

	@Subscribe
	private void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			Tile tile;
			tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getDecorativeObject());

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		});
	}

	@Subscribe
	private void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		if (!config.spawnGameObjects())
		{
			return;
		}
		clientThread.invokeAtTickEnd(() ->
		{
			Tile tile;
			tile = event.getTile();


			Buffer actorSpawnPacket = new Buffer(new byte[100]);
			int tileObjectModelType = getObjModelTypeFromFlags(event.getDecorativeObject().getConfig());
			int var4 = (event.getDecorativeObject().getConfig()) >> 6 & 3;


			int rotation = var4;
			int anint = (rotation * 512);

			int objectOrientationA = anint;
			int objectOrientationB = 1234;
			if (tileObjectModelType == 8)
			{ //if wall is objectType 2. walltype 2 has a model B;
				rotation = (event.getDecorativeObject().getConfig() >> 6) & 3;
				rotation = (rotation + 2 & 3);
				objectOrientationB = (rotation + 4) * 512;
				objectOrientationA = ((var4 + 4) * 512);
			}
			int objectDefinitionId = event.getDecorativeObject().getId();
			int plane = tile.getRenderLevel();

			int tileX = (event.getDecorativeObject().getX()) / 128;
			int tileY = (event.getDecorativeObject().getY()) / 128;

			int height = event.getDecorativeObject().getZ() * -1;

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			int tileMinPlane = tile.getPlane();
			actorSpawnPacket.writeByte(tileMinPlane);
			actorSpawnPacket.writeShort(height);
			long tag = getTag_Unique(event.getDecorativeObject());
			actorSpawnPacket.writeLong(tag);
			int cycleStart = 0;
			int frame = 0;
			actorSpawnPacket.writeShort(cycleStart);
			actorSpawnPacket.writeShort(frame);
			int offsetX = event.getDecorativeObject().getXOffset();
			int offsetY = event.getDecorativeObject().getYOffset();
			actorSpawnPacket.writeShort(offsetX);
			actorSpawnPacket.writeShort(offsetY);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		});
	}

	@Subscribe
	private void onVarbitChanged(VarbitChanged event)
	{
		//in VarbitChanged func in unreal, we read: varType. varId. varValue. custom0.
		Buffer buffer = new Buffer(new byte[12]);

		if (event.getVarbitId() != -1)
		{
			//log.debug("varbit "+ event.getVarbitId()+"cahnged to "+event.getValue());
			buffer.writeInt(event.getVarbitId());
			buffer.writeInt(event.getValue());
			sharedmem_rm.backBuffer.writePacket(buffer, "Varbit");
		}
		else
		{
			//log.debug("varP "+ event.getVarbitId()+"cahnged to "+event.getValue());
			buffer.writeInt(event.getVarpId());
			buffer.writeInt(event.getValue());
			sharedmem_rm.backBuffer.writePacket(buffer, "Varp");
		}
	}

	private Map<Long, DynamicObject> getAnimatedGameObjects()
	{
		Map<Long, DynamicObject> allGameObjects = new HashMap<>();
		if (client.getScene() == null || client.getScene().getTiles() == null)
		{
			return allGameObjects;
		}
		for (int z = 0; z < Constants.MAX_Z; ++z)
		{
			for (int x = 0; x < Constants.SCENE_SIZE; ++x)
			{
				for (int y = 0; y < Constants.SCENE_SIZE; ++y)
				{
					Tile tile = client.getScene().getTiles()[z][x][y];
					if (tile != null)
					{
						GameObject[] gameObjects = tile.getGameObjects();
						if (gameObjects != null)
						{
							for (GameObject gameObject : gameObjects)
							{
								if (gameObject != null) // && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()
								{
									if (gameObject.getRenderable() != null)
									{
										if (gameObject.getRenderable() instanceof DynamicObject)
										{

											DynamicObject dynamicObject = (DynamicObject) gameObject.getRenderable();
											if (dynamicObject.getAnimation() != null)
											{
												allGameObjects.put(getTag_Unique(gameObject), dynamicObject);
											}
										}
									}
								}
							}
						}

						GroundObject groundObject = tile.getGroundObject();
						if (groundObject != null) // && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()
						{
							if (groundObject.getRenderable() != null)
							{
								if (groundObject.getRenderable() instanceof DynamicObject)
								{
									DynamicObject dynamicObject = (DynamicObject) groundObject.getRenderable();
									if (dynamicObject.getAnimation() != null)
									{
										allGameObjects.put(getTag_Unique(groundObject), dynamicObject);
									}
								}
							}
						}

						DecorativeObject decorativeObject = tile.getDecorativeObject();
						if (decorativeObject != null) // && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()
						{
							if (decorativeObject.getRenderable() != null)
							{
								if (decorativeObject.getRenderable() instanceof DynamicObject)
								{
									DynamicObject dynamicObject = (DynamicObject) decorativeObject.getRenderable();
									if (dynamicObject.getAnimation() != null)
									{
										allGameObjects.put(getTag_Unique(decorativeObject), dynamicObject);
									}
								}
							}
						}

						WallObject wallbject = tile.getWallObject();
						if (wallbject != null)
						{
							if (wallbject.getRenderable1() != null)
							{
								if (wallbject.getRenderable1() instanceof DynamicObject)
								{
									DynamicObject dynamicObject = (DynamicObject) wallbject.getRenderable1();

									if (dynamicObject.getAnimation() != null)
									{
										allGameObjects.put(getTag_Unique(wallbject), dynamicObject);
									}
								}
							}
						}
					}
				}
			}
		}
		return allGameObjects;
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			if (!config.spawnGameObjects())
			{
				return;
			}

			client.getObjectDefinition(event.getGameObject().getConfig()).getName();

			int isBridgeTile = 0;
			Tile tile;
			tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);
			int tileObjectModelType = getObjModelTypeFromFlags(event.getGameObject().getConfig());
			int var4 = ((event.getGameObject().getConfig()) >> 6) & 3;
			int objectOrientationA = event.getGameObject().getOrientation();
			int objectOrientationB = 65535;
			int objectDefinitionId = event.getGameObject().getId();//note when a gameobject spawns, the id relates to the untransformed/original objectdef.

			int plane = tile.getRenderLevel();
			int tileX = event.getGameObject().getSceneMinLocation().getX();
			int tileY = event.getGameObject().getSceneMinLocation().getY();

			int height = event.getGameObject().getZ() * -1;

			long tag = getTag_Unique(event.getGameObject());
			int cycleStart = 0;
			int frame = 0;
			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			int tileMinPlane = tile.getPlane();
			actorSpawnPacket.writeByte(tileMinPlane);
			actorSpawnPacket.writeShort(height);
			actorSpawnPacket.writeLong(tag);
			actorSpawnPacket.writeShort(cycleStart);
			actorSpawnPacket.writeShort(frame);
			LocalPoint min_localPoint = new LocalPoint((event.getGameObject().getSceneMinLocation().getX() * 128) + 64, (event.getGameObject().getSceneMinLocation().getY() * 128) + 64);
			int offsetX = event.getGameObject().getX() - min_localPoint.getX(); //explanation: centreX-MinX = offsetX;
			int offsetY = event.getGameObject().getY() - min_localPoint.getY(); //explanation: centreY-MinY = offsetY;
			actorSpawnPacket.writeShort(offsetX);
			actorSpawnPacket.writeShort(offsetY);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		});
	}

	@Subscribe
	private void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGroundObject());

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		});
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGameObject());


			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		});
	}

	long getTag_Unique(TileObject tileObject)
	{
		long tag = tileObject.getHash();
		int tileOIbjType = 0;
		if (tileObject instanceof GroundObject)
		{
			tileOIbjType = 0;
		}
		if (tileObject instanceof GameObject)
		{
			tileOIbjType = 1;
		}
		if (tileObject instanceof DecorativeObject)
		{
			tileOIbjType = 2;
		}
		if (tileObject instanceof WallObject)
		{
			tileOIbjType = 3;
		}

		tag = (tag << 2) | (tileOIbjType & 3);// append tileObjType to make tag unique.
		return tag;
	}

	@Subscribe
	private void onGroundObjectSpawned(GroundObjectSpawned event)
	{ //GroundObject is aka a FloorDecoration
		if (!config.spawnGameObjects())
		{
			return;
		}
		clientThread.invokeAtTickEnd(() ->
		{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);
			int tileObjectModelType = getObjModelTypeFromFlags(event.getGroundObject().getConfig());
			int var4 = (event.getGroundObject().getConfig() - tileObjectModelType) >> 6 & 3;
			int objectOrientationA = var4 * 512;
			int objectOrientationB = -1;
			int objectDefinitionId = event.getGroundObject().getId();
			int plane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();

			int height = event.getGroundObject().getZ() * -1;
			long tag = getTag_Unique(event.getGroundObject());

			int cycleStart = 0;
			int frame = 0;
			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);

			int tileMinPlane = tile.getPlane();

			actorSpawnPacket.writeByte(tileMinPlane);
			actorSpawnPacket.writeShort(height);
			actorSpawnPacket.writeLong(tag);
			actorSpawnPacket.writeShort(cycleStart);
			actorSpawnPacket.writeShort(frame);
			int offsetX = 0;
			int offsetY = 0;
			actorSpawnPacket.writeShort(offsetX);
			actorSpawnPacket.writeShort(offsetY);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		});
	}

/*	@Subscribe
	private void onAnimationChanged(AnimationChanged event) {

	}*/

	@Subscribe
	private void onItemSpawned(ItemSpawned event)
	{
		if (!config.spawnItems())
		{
			return;
		}
		clientThread.invokeAtTickEnd(() ->
		{
			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = event.getTile().getPlane();
			if (event.getTile().getBridge() != null)
			{
				tilePlane++;
			}

			int tileX = event.getTile().getSceneLocation().getX();
			int tileY = event.getTile().getSceneLocation().getY();
			int height = Perspective.getTileHeight(client, event.getTile().getLocalLocation(), client.getTopLevelWorldView().getPlane()) * -1;
			height += event.getTile().getItemLayer().getHeight();
			int itemDefinitionId = event.getItem().getId();
			int itemQuantity = event.getItem().getQuantity();
			actorSpawnPacket.writeByte(3); //write tileItem data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeShort(height);
			actorSpawnPacket.writeShort(itemDefinitionId);
			actorSpawnPacket.writeShort(itemQuantity);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		});
	}

	@Subscribe
	private void onItemDespawned(ItemDespawned event)
	{
		clientThread.invokeAtTickEnd(() ->
		{
			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = event.getTile().getPlane();
			if (event.getTile().getBridge() != null)
			{
				tilePlane++;
			}

			int tileX = event.getTile().getSceneLocation().getX();
			int tileY = event.getTile().getSceneLocation().getY();
			int itemDefinitionId = event.getItem().getId();
			actorSpawnPacket.writeByte(3); //write tileItem data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeShort(itemDefinitionId);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		});
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (event.getNpc() == null)
		{
			return;
		}
		boolean shouldDraw = hooks.draw(event.getNpc(), false);
		if (!shouldDraw)
		{
			return;
		}

		if (!config.spawnNPCs())
		{
			return;
		}

		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		int instanceId = event.getNpc().getIndex();
		int definitionId = event.getNpc().getId();
		actorSpawnPacket.writeByte(1); //write npc data type
		actorSpawnPacket.writeShort(instanceId);
		actorSpawnPacket.writeShort(definitionId);
		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
	}

	@Subscribe
	private void onNpcChanged(NpcChanged event)
	{
		if (event.getNpc() == null)
		{
			return;
		}

		if (!config.spawnNPCs())
		{
			return;
		}

		NpcDespawned despawnEvent = new NpcDespawned(event.getNpc());
		onNpcDespawned(despawnEvent);

		NpcSpawned spawnEvent = new NpcSpawned(event.getNpc());
		onNpcSpawned(spawnEvent);
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == null)
		{
			return;
		}

		Buffer actorSpawnPacket = new Buffer(new byte[100]);
		int instanceId = event.getNpc().getIndex();
		actorSpawnPacket.writeByte(1); //write npc data type
		actorSpawnPacket.writeShort(instanceId);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	@Subscribe
	private void onPlayerChanged(PlayerChanged event)
	{
		if (!config.spawnPlayers())
		{
			return;
		}
		Player player = event.getPlayer();
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		actorSpawnPacket.writeByte(2); //write player data type

		int InstanceId = event.getPlayer().getId();
		actorSpawnPacket.writeShort(InstanceId);

		byte isLocalPlayer = (client.getLocalPlayer().getId() == player.getId()) ? (byte) 1 : (byte) 0;
		actorSpawnPacket.writeByte(isLocalPlayer);

		int[] equipmentIds = player.getPlayerComposition().getEquipmentIds();
		for (int i = 0; i < 12; i++)//equipment
		{
			actorSpawnPacket.writeInt(equipmentIds[i]);
		}
		int[] bodyColors = player.getPlayerComposition().getColors();
		for (int i = 0; i < 5; i++) //bodyColors
		{
			actorSpawnPacket.writeByte(bodyColors[i]);
		}
		byte isFemale = 0;
		if (player.getPlayerComposition().getGender() == 1)
		{
			isFemale = 1;
		}
		actorSpawnPacket.writeByte(isFemale); //isFemale

		actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
	}

	private Field getField(Object instance, String method)
	{
		Class<?> clazz = instance.getClass();

		while (clazz != null && clazz.getSuperclass() != clazz)
		{
			try
			{
				Field field = clazz.getDeclaredField(method);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException ex)
			{
			}

			clazz = clazz.getSuperclass();
		}
		throw new RuntimeException("Method '" + method + "' not found in class '" + instance.getClass() + "'");
	}

	@Subscribe
	private void onPlayerSpawned(PlayerSpawned event)
	{
		if (!config.spawnPlayers())
		{
			return;
		}

		Player player = event.getPlayer();


		boolean shouldDraw = hooks.draw(player, false);
		if (!shouldDraw && player != client.getLocalPlayer())
		{
			return;
		} //we must allow spawning local player even when is hidden, cos camera is attached to that.

		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		actorSpawnPacket.writeByte(2); //write player data type

		int InstanceId = event.getPlayer().getId();
		actorSpawnPacket.writeShort(InstanceId);

		byte isLocalPlayer = (client.getLocalPlayer().getId() == player.getId()) ? (byte) 1 : (byte) 0;
		actorSpawnPacket.writeByte(isLocalPlayer);

		int[] equipmentIds = player.getPlayerComposition().getEquipmentIds();
		for (int i = 0; i < 12; i++)//equipment
		{
			actorSpawnPacket.writeInt(equipmentIds[i]);
		}

		int[] bodyColors = player.getPlayerComposition().getColors();
		for (int i = 0; i < 5; i++) //bodyColors
		{
			actorSpawnPacket.writeByte(bodyColors[i]);
		}
		byte isFemale = 0;
		if (player.getPlayerComposition().getGender() == 1)
		{
			isFemale = 1;
		}
		actorSpawnPacket.writeByte(isFemale); //isFemale

		actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID //temp

		log.debug("player spawn. id: " + InstanceId);
		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
	}

	@Subscribe
	private void onPlayerDespawned(PlayerDespawned event)
	{
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		int instanceId = event.getPlayer().getId();
		actorSpawnPacket.writeByte(2); //write player data type
		actorSpawnPacket.writeShort(instanceId);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	private void sendPlaneChanged()
	{
		log.debug("PlaneChanged");
		Buffer buffer = new Buffer(new byte[4]);
		buffer.writeByte(clientPlane);
		sharedmem_rm.backBuffer.writePacket(buffer, "PlaneChanged");
	}

	public byte[] SpotAnimationModel_get(int Id)
	{
		IndexDataBase SpotAnimModelArchive = client.getIndex(7);
		byte[] bytes = SpotAnimModelArchive.loadData(Id, 0); //loadData(ArchiveId, FileId). For modeldata, file id is always 0.
		return bytes;
	}

	volatile int rsUiPosOffsetX = 0;
	volatile int rsUiPosOffsetY = 0;
	volatile int rsUiPosX = 0;
	volatile int rsUiPosY = 0;

	float getDpiScalingFactor()
	{
		final GraphicsConfiguration graphicsConfiguration = clientUI.getGraphicsConfiguration();
		final AffineTransform t = graphicsConfiguration.getDefaultTransform();
		return (float) t.getScaleX();
	}

	private void UpdateUiPosOffsets()
	{ //sets the pos offsets, using swing thread, to avoid crash.
		float dpiScalingFactor = getDpiScalingFactor();

		rsUiPosOffsetX = Math.round(dpiScalingFactor * (client.getCanvas().getParent().getLocationOnScreen().x - client.getCanvas().getLocationOnScreen().x) * -1);
		rsUiPosOffsetY = Math.round(dpiScalingFactor * (client.getCanvas().getParent().getLocationOnScreen().y - client.getCanvas().getLocationOnScreen().y) * -1);
		rsUiPosX = client.getCanvas().getLocationOnScreen().x;
		rsUiPosY = client.getCanvas().getLocationOnScreen().y;
	}

	private void UpdateSharedMemoryUiPixels()
	{
		sharedmem_rm.myKernel32.ResetEvent(sharedmem_rm.EventViewportPixelsReady);
		log_Timed_Heavy("_UpdateSharedMemoryUiPixels_0");
		if (client.getDrawCallbacks() == null)
		{
			return;
		}
		if (sharedmem_rm == null)
		{
			return;
		}
		if (sharedMemPixelsUpdatedTick == client.getGameCycle())
		{
			return;
		}
		if (!client.getCanvas().isShowing())
		{
			return;
		}
		sharedMemPixelsUpdatedTick = client.getGameCycle();
		final BufferProvider bufferProvider = client.getBufferProvider();

		int bufferWidth = bufferProvider.getWidth();
		int bufferHeight = bufferProvider.getHeight();

		sharedmem_rm.setInt(30000000, bufferWidth);
		sharedmem_rm.setInt(30000005, bufferHeight);

		float dpiScalingFactor = getDpiScalingFactor(); // 96 DPI is the standard

		float ratioX = 1;
		float ratioY = 1;

		if (client.isStretchedEnabled())
		{
			ratioX = (float) client.getStretchedDimensions().width / (float) bufferProvider.getWidth();
			ratioY = (float) client.getStretchedDimensions().height / (float) bufferProvider.getHeight();
		}

		SwingUtilities.invokeLater(this::UpdateUiPosOffsets); //this code is run on swing thread to avoid crash.
		sharedmem_rm.setInt(30000010, rsUiPosOffsetX);
		sharedmem_rm.setInt(30000015, rsUiPosOffsetY);


		//3d viewport size
		float View3dSizeX = client.getViewportWidth();
		boolean onLoginScreen = client.getGameState() == GameState.STARTING || client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN || client.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR;
		View3dSizeX *= dpiScalingFactor;
		View3dSizeX *= ratioX;
		if (onLoginScreen)
		{ //when we first arrive on login screen, viewport is 0 width because it is uninitialized. In such cases, I am using the canvas dimensions instead.
			View3dSizeX = client.getCanvas().getParent().getWidth() * dpiScalingFactor;
		}

		float View3dSizeY = client.getViewportHeight();
		View3dSizeY *= dpiScalingFactor;
		View3dSizeY *= ratioY;
		if (onLoginScreen)
		{ //when we first arrive on login screen, viewport is 0 width because it is uninitialized. In such cases, I am using the canvas dimensions instead.
			View3dSizeY = client.getCanvas().getParent().getHeight() * dpiScalingFactor;
		}

		sharedmem_rm.setInt(30000020, Math.round(View3dSizeX));
		sharedmem_rm.setInt(30000025, Math.round(View3dSizeY));

		//top left position of 3d viewport in rl window.
		float View3dOffsetX = client.getViewportXOffset();
		View3dOffsetX *= dpiScalingFactor;
		View3dOffsetX *= ratioX;
		View3dOffsetX += rsUiPosOffsetX;
		if (onLoginScreen)
		{
			View3dOffsetX = 0;
		}

		float View3dOffsetY = client.getViewportYOffset();
		View3dOffsetY *= dpiScalingFactor;
		View3dOffsetY *= ratioY;
		View3dOffsetY += rsUiPosOffsetY;
		if (onLoginScreen)
		{
			View3dOffsetY = 0;
		}

		sharedmem_rm.setInt(30000030, Math.round(View3dOffsetX));
		sharedmem_rm.setInt(30000035, Math.round(View3dOffsetY));

		//Size of ui. Can differ from buffer size because of stretchmode.
		float canvasSizeX = client.getCanvasWidth();
		canvasSizeX *= dpiScalingFactor;
		canvasSizeX *= ratioX;
		float canvasSizeY = client.getCanvasHeight();
		canvasSizeY *= dpiScalingFactor;
		canvasSizeY *= ratioY;
		sharedmem_rm.setInt(30000040, Math.round(canvasSizeX));
		sharedmem_rm.setInt(30000045, Math.round(canvasSizeY));

		sharedmem_rm.SharedMemoryData.write(30000060, bufferProvider.getPixels(), 0, bufferProvider.getHeight() * bufferProvider.getWidth());
		sharedmem_rm.myKernel32.SetEvent(sharedmem_rm.EventViewportPixelsReady);
		log_Timed_Heavy("_UpdateSharedMemoryUiPixels_1");
	}

	int baseX = 0;
	int baseY = 0;

	private static WorldPoint rotate(WorldPoint point, int rotation)
	{
		int chunkX = point.getX() & ~(CHUNK_SIZE - 1);
		int chunkY = point.getY() & ~(CHUNK_SIZE - 1);
		int x = point.getX() & (CHUNK_SIZE - 1);
		int y = point.getY() & (CHUNK_SIZE - 1);
		switch (rotation)
		{
			case 1:
				return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
			case 2:
				return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
			case 3:
				return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
		}
		return point;
	}

	private void sendInstancedAreaState(Scene scene)
	{
		int ByteArray1DSize = (4 * 13 * 13) * 4;
		Buffer packet = new Buffer(new byte[ByteArray1DSize + 10]);

		boolean isInstancedArea = scene.isInstance();
		packet.writeBoolean(isInstancedArea);

		if (isInstancedArea)
		{
			int[][][] instanceTemplateChunks = scene.getInstanceTemplateChunks();

			//writeThe 3d array to a packet.
			for (int z = 0; z < 4; z++)
			{
				for (int x = 0; x < 13; ++x)
				{
					for (int y = 0; y < 13; ++y)
					{
						int chunkData = instanceTemplateChunks[z][x][y];
						packet.writeInt(chunkData);
					}
				}
			}
		}

		sharedmem_rm.backBuffer.writePacket(packet, "InstancedAreaState");
	}

	private void sendBaseCoordinatePacket(Scene scene)
	{ //send Base Coordinate if needed
		clientThread.invoke(() -> {
			sendInstancedAreaState(scene);

			baseX = scene.getBaseX();
			baseY = scene.getBaseY();

			Buffer packet = new Buffer(new byte[20]);
			packet.writeShort(baseX);
			packet.writeShort(baseY);
			packet.writeByte(client.getTopLevelWorldView().getPlane());
			sharedmem_rm.backBuffer.writePacket(packet, "BaseCoordinate");
		});
	}

	private void sendBaseCoordinatePacket()
	{ //send Base Coordinate if needed
		sendInstancedAreaState(client.getTopLevelWorldView().getScene());

		Scene scene = client.getTopLevelWorldView().getScene();

		baseX = scene.getBaseX();
		baseY = scene.getBaseY();

		Buffer packet = new Buffer(new byte[20]);
		packet.writeShort(baseX);
		packet.writeShort(baseY);
		packet.writeByte(client.getTopLevelWorldView().getPlane());

		sharedmem_rm.backBuffer.writePacket(packet, "BaseCoordinate");
	}

	private void WritePerFramePacket()
	{
		if (client.getGameState() == GameState.LOGIN_SCREEN || curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{
			if (client.getGameCycle() % 20 == 0)
			{
				//bodge. constantly resizes canvas in order to force it to repaint. This prevents the loss of transparency on the loginscreens background pixels, caused by going to the world sel;ect screen.
				client.resizeCanvas();
			}
		}

		if (client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN || curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{ //dont send perframe packet while on login screen because doing so would interfere with the animated login screen's camera.
			return;
		}

		if (client.getTopLevelWorldView().getPlane() != clientPlane)
		{
			clientPlane = client.getTopLevelWorldView().getPlane();
			sendPlaneChanged();
		}

		Set<Integer> hashedEntitys_ThisFrame = new HashSet<Integer>();

		byte[] bytes = new byte[80000];
		Buffer perFramePacket = new Buffer(bytes);

		int camX = client.getCameraX();
		int camY = client.getCameraY();
		int camZ = client.getCameraZ();
		int camYaw = client.getCameraYaw();
		int camPitch = client.getCameraPitch();
		int camZoom = client.getScale();
		boolean removeRoofs = client.getVarbitValue(12378) > 0;
		int clientCycle = client.getGameCycle();
		short canvasWidth = (short) client.getViewportWidth();
		short canvasHeight = (short) client.getViewportHeight();

		perFramePacket.writeShort(camX);
		perFramePacket.writeShort(camY);
		perFramePacket.writeShort(camZ);
		perFramePacket.writeShort(camYaw);
		perFramePacket.writeShort(camPitch);
		perFramePacket.writeShort(camZoom);
		perFramePacket.writeBoolean(removeRoofs);
		perFramePacket.writeInt(clientCycle);
		perFramePacket.writeShort(canvasWidth);
		perFramePacket.writeShort(canvasHeight);

		var npcs = client.getTopLevelWorldView().npcs();

		int npcCount = 0;
		for (NPC npc : npcs)
		{
			if (npc != null)
			{
				npcCount++;
			}
		}

		if (!config.spawnNPCs())
		{
			npcCount = 0;
		}
		perFramePacket.writeShort(npcCount);
		if (npcCount > 0)
		{
			for (NPC npc : npcs)
			{
				if (npc == null)
				{
					continue;
				}
				int npcInstanceId = npc.getIndex();
				int npcX = npc.getLocalLocation().getX();
				int npcY = npc.getLocalLocation().getY();
				int npcHeight = Perspective.getTileHeight(client, npc.getLocalLocation(), client.getTopLevelWorldView().getPlane()) * -1;
				int npcOrientation = npc.getCurrentOrientation();

				int animation = (config.spawnAnimations() ? npc.getAnimation() : -1);
				int poseAnimation = (config.spawnAnimations() ? npc.getPoseAnimation() : -1);

				int animFrame = npc.getAnimationFrame();
				int poseAnimFrame = npc.getPoseAnimationFrame();

				boolean shouldDraw = visibleActors.contains(npc) && hooks.draw(npc, false);

				if (!shouldDraw)
				{
					animFrame = -2; //-2 causes entity to be hidden in rm
					poseAnimFrame = -2;
				}

				perFramePacket.writeInt(npcInstanceId);
				perFramePacket.writeShort(npcX);
				perFramePacket.writeShort(npcY);
				perFramePacket.writeShort(npcHeight);
				perFramePacket.writeShort(npcOrientation);

				perFramePacket.writeInt(animation);
				perFramePacket.writeShort(animFrame);

				perFramePacket.writeInt(poseAnimation);
				perFramePacket.writeShort(poseAnimFrame);

				int numActorSpotAnims = 0;
				if (!config.spawnNpcGFX() || !shouldDraw)
				{
					perFramePacket.writeByte(numActorSpotAnims);
				}
				else
				{
					for (ActorSpotAnim spotAnim : npc.getSpotAnims())
					{
						numActorSpotAnims++;
					}
					perFramePacket.writeByte(numActorSpotAnims);

					if (numActorSpotAnims > 0)
					{
						for (ActorSpotAnim spotAnim : npc.getSpotAnims())
						{
							int spotAnimationFrame = spotAnim.getFrame();
							int spotAnimationId = spotAnim.getId();
							int spotAnimationHeight = spotAnim.getHeight();
							int sceneId = spotAnim.hashCode();

							perFramePacket.writeInt(spotAnimationId);
							perFramePacket.writeShort(spotAnimationFrame);
							perFramePacket.writeShort(spotAnimationHeight);
							perFramePacket.writeInt(sceneId);

							hashedEntitys_ThisFrame.add(spotAnim.hashCode()); //We do this so that we can later detect when the spotanim is despawned.
						}
					}
				}

				//started writing npc overrides. maybe should make something to detect when overrides are changed and fire a npcOverridesChanged event?
/*				boolean hasNpcOverrides = npc.getModelOverrides()!=null;
				perFramePacket.writeBoolean(hasNpcOverrides);
				if(npc.getModelOverrides() != null) {
					perFramePacket.writeInt_Array(npc.getModelOverrides().getModelIds(), npc.getModelOverrides().getModelIds().length);
					perFramePacket.writeShort_Array(npc.getModelOverrides().getColorToReplaceWith(), npc.getModelOverrides().getColorToReplaceWith().length);
					perFramePacket.writeShort_Array(npc.getModelOverrides().getTextureToReplaceWith(), npc.getModelOverrides().getTextureToReplaceWith().length);
					perFramePacket.writeBoolean(npc.getModelOverrides().useLocalPlayer());
				}*/
			}
		}

		var players = client.getTopLevelWorldView().players();

		int playerCount = 0;
		for (Player player : players)
		{
			if (player != null)
			{
				playerCount++;
			}
		}
		if (!config.spawnPlayers())
		{
			playerCount = 0;
		}
		perFramePacket.writeShort(playerCount);
		if (playerCount > 0)
		{
			for (Player player : players)
			{
				if (player == null)
				{
					continue;
				}
				//Player player = players.byIndex(i);
				int playerInstanceId = player.getId();
				int playerX = player.getLocalLocation().getX();
				int playerY = player.getLocalLocation().getY();
				int playerHeight = Perspective.getTileHeight(client, player.getLocalLocation(), client.getTopLevelWorldView().getPlane()) * -1;
				int playerOrientation = player.getCurrentOrientation();

				int animation = (config.spawnAnimations() ? player.getAnimation() : -1);
				int poseAnimation = (config.spawnAnimations() ? player.getPoseAnimation() : -1);

				int animFrame = player.getAnimationFrame();
				int poseAnimFrame = player.getPoseAnimationFrame();

				boolean shouldDraw = visibleActors.contains(player) && hooks.draw(player, false);

				if (!shouldDraw)
				{
					animFrame = -2; //-2 causes entity to be hidden in rm
					poseAnimFrame = -2;
				}

				perFramePacket.writeShort(playerInstanceId);
				perFramePacket.writeShort(playerX);
				perFramePacket.writeShort(playerY);
				perFramePacket.writeShort(playerHeight);
				perFramePacket.writeShort(playerOrientation);

				perFramePacket.writeInt(animation);

				perFramePacket.writeShort(animFrame);
				perFramePacket.writeInt(poseAnimation);
				perFramePacket.writeShort(poseAnimFrame);


				int numActorSpotAnims = 0;
				if (!config.spawnPlayerGFX() || !shouldDraw)
				{
					perFramePacket.writeByte(numActorSpotAnims);
				}
				else
				{
					for (ActorSpotAnim spotAnim : player.getSpotAnims())
					{
						numActorSpotAnims++;
					}
					perFramePacket.writeByte(numActorSpotAnims);

					if (numActorSpotAnims > 0)
					{
						for (ActorSpotAnim spotAnim : player.getSpotAnims())
						{
							int spotAnimationFrame = spotAnim.getFrame();
							int spotAnimationId = spotAnim.getId();
							int spotAnimationHeight = spotAnim.getHeight();
							int sceneId = spotAnim.hashCode();

							perFramePacket.writeInt(spotAnimationId);
							perFramePacket.writeShort(spotAnimationFrame);
							perFramePacket.writeShort(spotAnimationHeight);
							perFramePacket.writeInt(sceneId);

							hashedEntitys_ThisFrame.add(spotAnim.hashCode()); //We do this so that we can later detect when the spotanim is despawned.
						}
					}
				}
			}
		}

		int LocalPlayerIndex = -1;
		if (client.getLocalPlayer() != null)
		{
			LocalPlayerIndex = client.getLocalPlayer().getId();
		}
		perFramePacket.writeShort(LocalPlayerIndex);


		int noGraphicsObjects = 0;
		if (config.spawnStaticGFX())
		{
			for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects())
			{
				noGraphicsObjects++;
				if (!hashedEntitys_LastFrame.contains(graphicsObject.hashCode()))
				{
					log.debug("graphicsObjSpawn. id: " + graphicsObject.getId());
				}
				hashedEntitys_ThisFrame.add(graphicsObject.hashCode());
			}
		}

		perFramePacket.writeShort(noGraphicsObjects);
		if (noGraphicsObjects > 0)
		{
			for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects())
			{
				boolean shouldDraw = hooks.draw(graphicsObject, false);
				if (graphicsObject instanceof RuneLiteObject)
				{
					log.debug("encountered runeliteObject");
				}

				if (clientCycle < graphicsObject.getStartCycle())
				{ //graphicsObj should not draw before it's startCycle.
					shouldDraw = false;
				}

				int sceneId = graphicsObject.hashCode();
				perFramePacket.writeInt(sceneId);
				short localX = (short) graphicsObject.getLocation().getX();
				perFramePacket.writeShort(localX);
				short localY = (short) graphicsObject.getLocation().getY();
				perFramePacket.writeShort(localY);
				short spotAnimId = (short) graphicsObject.getId();
				perFramePacket.writeShort(spotAnimId);
				short animimationFrameIdx = (short) graphicsObject.getAnimationFrame();

				if (!shouldDraw)
				{
					animimationFrameIdx = -2;
				}

				perFramePacket.writeShort(animimationFrameIdx);

				short Z = (short) ((graphicsObject.getZ() * -1)); //not sure if getStartHeight is correct/helping things.

				perFramePacket.writeShort(Z);
			}
		}


		int noProjectiles = 0;
		if (config.spawnProjectiles())
		{
			for (Projectile projectile : client.getTopLevelWorldView().getProjectiles())
			{
				noProjectiles++;
				hashedEntitys_ThisFrame.add(projectile.hashCode());
			}
		}

		if (config.spawnProjectiles() && noProjectiles > 0)
		{
			perFramePacket.writeShort(noProjectiles);

			for (Projectile projectile : client.getTopLevelWorldView().getProjectiles())
			{
				int sceneId = projectile.hashCode();
				perFramePacket.writeInt(sceneId);
				short localX = (short) projectile.getX();
				perFramePacket.writeShort(localX);
				short localY = (short) projectile.getY();
				perFramePacket.writeShort(localY);
				short Z = (short) ((projectile.getZ() * -1)); //not sure if getStartHeight is correct/helping things.
				perFramePacket.writeShort(Z);

				short localX_target = (short) projectile.getTarget().getX();
				perFramePacket.writeShort(localX_target);
				short localY_target = (short) projectile.getTarget().getY();
				perFramePacket.writeShort(localY_target);
				short Z_target = (short) (projectile.getEndHeight());
				Z_target += (Perspective.getTileHeight(client, new LocalPoint(localX_target, localY_target), client.getTopLevelWorldView().getPlane()) * -1);
				perFramePacket.writeShort(Z_target);

				short spotAnimId = (short) projectile.getId();
				perFramePacket.writeShort(spotAnimId);
				short animationFrameIdx = (short) projectile.getAnimationFrame();

				boolean shouldDraw = visibleActors.contains(projectile) && hooks.draw(projectile, false);

				if (!shouldDraw)
				{
					animationFrameIdx = -2; //-2 causes entity to be hidden in rm
				}

				perFramePacket.writeShort(animationFrameIdx);
				short animimationFrameCycle = (short) -1;
				perFramePacket.writeShort(animimationFrameCycle);
				short remainingCycles = (short) projectile.getRemainingCycles();
				perFramePacket.writeShort(remainingCycles);
			}
		}
		else
		{
			perFramePacket.writeShort(0);
		}

		if (config.spawnAnimations() && config.spawnGameObjects())
		{
			Map<Long, DynamicObject> tileObjects = getAnimatedGameObjects();
			int NoTileObjects = tileObjects.size();
			perFramePacket.writeInt(NoTileObjects);

			for (Map.Entry<Long, DynamicObject> entry : tileObjects.entrySet())
			{
				DynamicObject dynamicObject = entry.getValue();
				long tag = entry.getKey();
				perFramePacket.writeLong(tag);
				Animation anim = dynamicObject.getAnimation();
				int animationId = anim.getId();
				perFramePacket.writeInt(animationId);
				int animationFrame = dynamicObject.getAnimFrame();
				perFramePacket.writeShort(animationFrame);
				int animationCycle = dynamicObject.getAnimCycle();
				perFramePacket.writeInt(animationCycle);
			}
		}
		else
		{
			int NoGameObjects = 0;
			perFramePacket.writeInt(NoGameObjects);
		}

		if (hashedEntitys_LastFrame != null)
		{
			for (Integer lastFrameHashedEntity : hashedEntitys_LastFrame)
			{
				if (hashedEntitys_ThisFrame.contains(lastFrameHashedEntity) == false)
				{ //if lats frames entity is not present this frame, means it has despawned.
					log.debug("hashedEntityDespawned. Entity " + lastFrameHashedEntity);
					hashedEntityDespawned(lastFrameHashedEntity);
				}
			}
		}
		hashedEntitys_LastFrame = hashedEntitys_ThisFrame;

		sharedmem_rm.backBuffer.writePacket(perFramePacket, "PerFramePacket");
	}

	void hashedEntityDespawned(int SceneId)
	{
		Buffer actorSpawnPacket = new Buffer(new byte[20]);

		actorSpawnPacket.writeByte(6); //write hashedEntity data type
		actorSpawnPacket.writeInt(SceneId);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	int lastCavansX = 0;
	int lastCavansY = 0;
	int lastCavansSizeX = 0;
	int lastCavansSizeY = 0;

	boolean RmNeedsWindowUpdate()
	{
		if (client.getCanvas() == null)
		{
			return false;
		}
		if (!client.getCanvas().isShowing())
		{
			log.debug("Cant Update RmWindow because rl canvas isnt showing");
			return false;
		}
		int curCavansX = rsUiPosX + client.getViewportXOffset();
		int curCavansY = rsUiPosY + client.getViewportYOffset();

		int curCavansSizeX = client.getViewportWidth();
		int curCavansSizeY = client.getViewportHeight();
		if (curCavansX != lastCavansX || curCavansY != lastCavansY || curCavansSizeX != lastCavansSizeX || curCavansSizeY != lastCavansSizeY)
		{
			lastCavansX = curCavansX;
			lastCavansY = curCavansY;
			lastCavansSizeX = curCavansSizeX;
			lastCavansSizeY = curCavansSizeY;
			return true;
		}
		return false;
	}

	private static Color rs2hsbToColor(int hsb)
	{
		int decode_hue = (hsb >> 10) & 0x3f;
		int decode_saturation = (hsb >> 7) & 0x07;
		int decode_brightness = (hsb & 0x7f);
		return Color.getHSBColor((float) decode_hue / 63, (float) decode_saturation / 7, (float) decode_brightness / 127);
	}

	private static void BGRToCol(int BGR)
	{
		int r = (BGR >> 16) & 0xFF / 255;
		int g = (BGR >> 8) & 0xFF / 255;
		int b = BGR & 0xFF / 255;
	}

	public ApplicationSettings loadAppSettings()
	{
		String jsonFileLocation = System.getProperty("user.home") + "\\.runemod\\AppSettings.json";

		Gson gson = new GsonBuilder().create();
		try (BufferedReader reader = new BufferedReader(new FileReader(jsonFileLocation)))
		{
			return gson.fromJson(reader, ApplicationSettings.class);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}


