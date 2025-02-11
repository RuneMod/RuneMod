package com.runemod;

import com.google.inject.Provides;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ModelData;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.Container;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static net.runelite.api.Constants.CHUNK_SIZE;
import static net.runelite.api.Constants.TILE_FLAG_BRIDGE;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.FileReader;

@PluginDescriptor(
		name = "RuneMod",
		enabledByDefault = true,
		description = "Graphics modding tool",
		tags = {"rm", "rune", "mod", "hd", "graphics", "high", "detail", "graphics", "shaders", "textures", "gpu", "shadows", "lights", "unreal", "ue4"},
		conflicts = {"GPU", "117 HD"}
)

@Slf4j
public class RuneModPlugin extends Plugin implements DrawCallbacks
{
	public static SharedMemoryManager sharedmem_rm = null;

	private int clientPlane = -1;

	private Set<Integer> hashedEntitys_LastFrame = new HashSet<Integer>();

	public static CacheReader myCacheReader;

	public static RuneMod_Launcher runeModLauncher;

	public static RuneMod_statusUI runeMod_statusUI;


	@Inject
	private Notifier notifier;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	public Client client;

	@Inject
	public ClientThread clientThread;

	@Inject
	public RuneModConfig config;

	@Inject
	private KeyManager keyManager;

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


	public static float getCurTimeSeconds() {
		float curT = (float)((System.currentTimeMillis() / 1000.0D)-1736168000D);
		return curT;
	}

	public static void log_Timed_Verbose(String message) {
		//System.out.println("["+getCurTimeSeconds()+"]	"+message);
	}

	int perFramePacketSentTick = -1;
	int sharedMemPixelsUpdatedTick = -1;
	int ticksSincePLuginLoad = 0;

	void clearRsPixelBuffer() {
		int[] newArray = new int[5000];
		System.arraycopy(client.getBufferProvider().getPixels(), 0, newArray, 0, 3);
	}


	int cacheLastUpdatedTick = Integer.MAX_VALUE; //when a cache file was last seen to be changed
	boolean rsCacheIsDownloading = true;
	HashMap<File, Long> lastSeenFileSizes =  new HashMap<File, Long>();

	boolean startedWhileLoggedIn;
	boolean runeModAwaitingRsCacheHashes = false;


	@SneakyThrows
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		if(config.OrbitCamera()) {
			client.setCameraYawTarget(client.getCameraYaw()+1);
		}

		log_Timed_Verbose("onBeforeRender");
		alreadyCommunicatedUnreal = false;

		if(client.getGameState().ordinal() >= GameState.STARTING.ordinal() && client.getTopLevelWorldView()!=null) {
			ticksSincePLuginLoad++;
		}

		if(ticksSincePLuginLoad == 1) {
			startUp_Custom();

			if (client.getGameState().ordinal() > GameState.LOGIN_SCREEN_AUTHENTICATOR.ordinal()) {
				runeMod_statusUI.SetStatus_Detail("To enable RuneMod, you must first logout", true);
				startedWhileLoggedIn = true;

				Thread.sleep(4000);

				SwingUtilities.invokeLater(() ->
				{
					try {
						pluginManager.setPluginEnabled(this, false);
						pluginManager.stopPlugin(this);
					} catch (PluginInstantiationException ex) {
						log.error("error stopping plugin", ex);
					}

					try {
						shutDown();
					} catch (Exception exception) {
						exception.printStackTrace();
					}
				});

			} else {
				runeMod_statusUI.SetStatus_Detail("Starting...", true);
				startedWhileLoggedIn = false;

				runeModLauncher.launch();
			}
		}

		if(startedWhileLoggedIn) {
			return;
		}

			//check if rscache is currently being updated. if not, start runemod launcher
			if(rsCacheIsDownloading == true && ticksSincePLuginLoad > 1 && client.getGameState().ordinal() >= GameState.LOGIN_SCREEN.ordinal() && client.getGameCycle()%20 == 0) {
				String directory = RUNELITE_DIR + "\\jagexcache\\oldschool\\LIVE";
				File[] files = new File(directory).listFiles();
				for(File file : files){
					if(file!=null && file.isFile()) {
						long lastSeenFileSize = lastSeenFileSizes.getOrDefault(file, -1L);
						if (file.length() != lastSeenFileSize) {
							lastSeenFileSizes.put(file, file.length());
							cacheLastUpdatedTick = client.getGameCycle();
							rsCacheIsDownloading = true;
						}
					}
				}

				if(client.getGameCycle() - cacheLastUpdatedTick > 200) {
					System.out.println("RSCache has finished downloading");
					runeMod_statusUI.SetStatus_Detail("Downloaded RS cache", true);
					rsCacheIsDownloading = false;
				} else {
					runeMod_statusUI.SetStatus_Detail("Downloading RS cache...", true);
				}
			}

			if(runeModAwaitingRsCacheHashes && rsCacheIsDownloading == false) { //provide rscache hashes, if runemod is waiting for thjem
				clientThread.invokeAtTickEnd(() -> {
					myCacheReader.provideRsCacheHashes();
				});

				runeModAwaitingRsCacheHashes = false;
			}

			log_Timed_Verbose("_0");
			JFrame window = (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas());
			if (!window.getTitle().equals("RuneLite - RuneLite")) {
				 window.setTitle("RuneLite - RuneLite");
			}

			log_Timed_Verbose("_1");
			if(ticksSincePLuginLoad <= 2 || client.getGameState().ordinal()<GameState.LOGGING_IN.ordinal() || config.RuneModVisibility() == false || config.useTwoRenderers() == true) {//allows us to display logging in... on login screen
					setGpuFlags(0);
					if(client.getDrawCallbacks() == null) {
						communicateWithUnreal("onBeforeRender");
						//clientThread.invokeAtTickEnd(this::communicateWithUnreal); //for times when scenedraw callback isnt available.
					}
			} else {
				clientThread.invokeAtTickEnd(() -> {
					if(isShutDown == false) {
						if(storedGpuFlags!=3) {
							communicateWithUnreal("onBeforeRender_TickEnd"); //here to set rm visibility before gpu flags get set. we do this to prevent momentarily showing unrendered client before rm visibility is set to true;
							setGpuFlags(3);
						}
					}
				});
			}
	}

	void MaintainRuneModAttachment() {

		if(sharedmem_rm == null) {
			return;
		}

		if(!runeModPlugin.config.attachRmWindowToRL()) {
			return;
		}

		if(!unrealIsReady) {
			return;
		}


		sharedmem_rm.ChildRuneModWinToRl();

		if(RmNeedsWindowUpdate()) {
			sharedmem_rm.updateRmWindowTransform();
		}

		if (config.RuneModVisibility() == true) {
			sharedmem_rm.setRuneModVisibility(true);
		} else {
			sharedmem_rm.setRuneModVisibility(false);
		}
	}

	public boolean isRuneliteTopmost() {
		JFrame window = (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas());
		return window.isFocused() && window.isActive();
		//return sharedmem_rm.isTopMostWindow();
	}

	public boolean isRuneliteVisible() {
		JFrame window = (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas());
		boolean isRlVisible = window.isShowing() && (window.getExtendedState() & JFrame.ICONIFIED) == 0;
		return isRlVisible;
	}

	public boolean isTopMost = false;

	//int consecutiveTimeouts = 0;
	boolean alreadyCommunicatedUnreal = false; //whether we have communicated with unreal this frame.
	void communicateWithUnreal(String funcLocation) {
		//System.out.println(System.currentTimeMillis());
		//if(!clientUI.isFocused()) {return;}

		if(ticksSincePLuginLoad < 3) {
			log_Timed_Verbose("ticksSincePLuginLoad < 3"); return; }

		if(isShutDown) {
			log_Timed_Verbose("isShutDown"); return; }

		if(alreadyCommunicatedUnreal) {
			log_Timed_Verbose("Already communicated. cancelled communicateWithUnreal::"+funcLocation); return; }

		alreadyCommunicatedUnreal = true;


		log_Timed_Verbose("communicateWithUnreal::"+funcLocation);


		//sendWindowUpdatePacket();

		SwingUtilities.invokeLater(() -> {
			MaintainRuneModAttachment();
		});


		if (client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal()) { //prevents communicating with unreal before client is loaded.
			return;
		}

		log_Timed_Verbose("Start");

		if(!sharedmem_rm.backBuffer.isOverFlowed) {
			WritePerFramePacket(); //we start writing the perframe apcket before unreal has indidcated its started a new frame, for a small performance optimization
		}

		//long waitForUnrealTimeout = 1000/(consecutiveTimeouts+1);
		//clamp(waitForUnrealTimeout, 30, 500)
		log_Timed_Verbose("0");
		boolean unrealStartedNewFrame = sharedmem_rm.AwaitUnrealMutex_Locked(600000000); //unreal locks it's mutex when it has started frame

		long frameSyncStartTime = System.nanoTime();


		if(!unrealStartedNewFrame) { // if the wait for unreal's frame timedout
			System.out.println("TimedOutWaitingFor unreal");
/*			consecutiveTimeouts++;
			if(consecutiveTimeouts > 0) {
				setUnrealConnectionStatus(false);
			}*/
			return;
		} else {
			//consecutiveTimeouts = 0;
			setUnrealConnectionStatus(true);
		}
		//System.out.println("UnrealMutexLocked");

		//export runeliteframe
		//WritePerFramePacket(); //doing this above, as a small optimization

		sharedmem_rm.startNewRsData();
		sharedmem_rm.transferBackBufferToSharedMem();

		log_Timed_Verbose("1");

		sharedmem_rm.writeTerminatingPacket();
		//System.out.println("ExportedRuneliteFrame");

		sharedmem_rm.LockRuneliteMutex(); //indicates to unreal that runelite has exported it's framedata

		log_Timed_Verbose("2");

		//System.out.println("LockedRuneliteMutex");
		sharedmem_rm.AwaitUnrealMutex_UnLocked(); //unreal sends data back to us, and unlocks it's mutex when it has imported rl frame.
		//System.out.println("Unreal Has Unlocked it's mutex");

		log_Timed_Verbose("3");

		//import unrealdata
		sharedmem_rm.handleUnrealData();

		log_Timed_Verbose("4");

		sharedmem_rm.UnLockRuneliteMutex(); //indicates ruenlite is now busy rendering the current frame and calcing the next frame.

		log_Timed_Verbose("5");
		//long frameSyncWaitTime = System.nanoTime()-frameSyncStartTime;
		//System.out.println("frameSyncWaitTime: "+(float)frameSyncWaitTime/1000000.0 + "ms");
		//System.out.println("commedWithUnreal");
	}

	int storedGpuFlags = -1;
	void setGpuFlags(int flags) {
		if(storedGpuFlags != flags) {
			client.setGpuFlags(flags);
			client.resizeCanvas(); //resize canvas to force rebuild for working alpha channel
			storedGpuFlags = flags;
			client.getCanvas().setIgnoreRepaint(true);
			client.resizeCanvas();
			System.out.println("GPU Flags have been changed to "+flags);
		}
	}

	void setUnrealConnectionStatus(boolean currentValue) {
		if(currentValue) {
			if(unrealIsConnected == false) {
				unrealIsConnected = true;

				sharedmem_rm.clearBackBuffer();

				System.out.println("Unreal Has just Connected");

				//if unreal is connected while we are already logged in, you would need trigger a scene reload.
				//simulateGameEvents(); //decided not to allow connection while loaded for now
				return;
			}
		}

		if(!currentValue) {
			if(client.getGameCycle()-sharedmem_rm.gameCycle_Unreal > 50) { //if unreal is very desynced from rstick
				if(unrealIsConnected == true) { //if unreal is disconnecting just now
					unrealIsConnected = false;

					if(sharedmem_rm.runeModWindowsExist()) {
						runeMod_statusUI.SetStatus_Detail("RuneMod gas just Disconnected or become desynced", true);
					}
				}

				if(sharedmem_rm.backBuffer.offset > 500000) {
					//runeMod_statusUI.SetStatus_Detail("Disconnected..");
					sharedmem_rm.clearBackBuffer();
				}
			}
		}
	}

	boolean unrealIsConnected = false;
	@SneakyThrows
	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane) {
		log_Timed_Verbose("drawScene");
		if(storedGpuFlags <= 0) {
			log_Timed_Verbose("storedGpuFlags <= 0"); return;}
		if(!client.isGpu()){
			log_Timed_Verbose("!client.isGpu()"); return;}
		communicateWithUnreal("drawScene");

		visibleActors.clear();
	}

	Set<Renderable> visibleActors = new HashSet<Renderable>();

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash) {
		if(storedGpuFlags <= 0) {return;}
/*


/*		(RL) plane = bits >> 49 & 3
		id = bits >> 17 & 0xffffffff
		wall = bits >> 16 & 1
		type = bits >> 14 & 3
		scene y = bits >> 7 & 127
		scene x = bits >> 0 & 127

		Type 0 = player, 1 = npc, 2 = game object, 3 = item*/
		//if(client.getGameCycle()%4==0) {
/*		long plane = hash >> 49 & 3;
		//if(plane==client.getPlane()) {
			if(renderable instanceof Model) {
				Model model = (Model) renderable;
				if (model!=null) {
					client.checkClickbox(model, orientation, pitchSin, pitchCos, yawSin, yawCos, x, y, z, hash);
				}
			}
		//}*/

		if(renderable instanceof Player) {
			visibleActors.add(renderable);
		} else {
			if(renderable instanceof NPC) {
				visibleActors.add(renderable);
			} else {
				if(renderable instanceof GraphicsObject) {
					visibleActors.add(renderable);
				} else {
					if(renderable instanceof Projectile) {
						visibleActors.add(renderable);
					}
				}
			}
		}

		long plane = hash >> 49 & 3;
		//if(plane==client.getPlane()) {
			Model model = renderable instanceof Model ? (Model) renderable : renderable.getModel();
			if (model != null)
			{
				// Apply height to renderable from the model
				if (model != renderable)
				{
					renderable.setModelHeight(model.getModelHeight());
				}

				//client.checkClickbox(model, orientation, pitchSin, pitchCos, yawSin, yawCos, x, y, z, hash);
				client.checkClickbox(projection, model, orientation, x, y, z, hash);
			}


		//}
	}

	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ) {
		//GpuPluginDrawCallbacks.drawScenePaint(orientation, pitchSin, pitchCos, yawSin, yawCos, x, y, z, paint, tileZ, tileX, tileY, zoom, centerX, centerY);
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ) {
		//GpuPluginDrawCallbacks.drawSceneModel(orientation, pitchSin, pitchCos, yawSin, yawCos, x, y, z, model, tileZ, tileX, tileY, zoom, centerX, centerY);
	}

	boolean focused_lastFrame = false;

	void overlayColourChanged() {
		Buffer packet = new Buffer(new byte[6]);

		packet.writeInt(overlayColor_LastFrame);

		//extra blank data, for future use
		packet.writeInt(0);

		sharedmem_rm.backBuffer.writePacket(packet, "OverlayColorChanged");

		System.out.println("overlayColourChanged");
	}

	public int overlayColor_LastFrame = 0;
	@SneakyThrows
	@Override
	public void draw(int overlayColor) {
		if(overlayColor_LastFrame!=overlayColor) {
			overlayColor_LastFrame = overlayColor;
			overlayColourChanged();
		}
		//System.out.println("draw at "+System.currentTimeMillis());
/*		if(mouseIsDown) {
			mouseIsDown = false;
			SwingUtilities.invokeAndWait(() -> {
				Robot  robot = null;
				try {
					robot = new Robot();
					System.out.println("Drawing screenshot to canvas");
					Rectangle screenRect = new Rectangle(client.getCanvas().getBounds());
					screenRect.x = rsUiPosX;
					screenRect.y = rsUiPosY;
					BufferedImage screenImage = robot.createScreenCapture(screenRect);
					client.getCanvas().getGraphics().drawImage(screenImage, 0, 0, null);

					SharedMemoryManager.waitNanos(1000000*300);

				} catch (AWTException awtException) {
					awtException.printStackTrace();
				}
			});
		}*/


		log_Timed_Verbose("draw");
		UpdateSharedMemoryUiPixels();
		communicateWithUnreal("Draw");
	}

/*
	private Image screenshot() {

		return image;
	}
*/

	long lastFrameTime = 0;
	@SneakyThrows
	@Override
	public void postDrawScene() {
/*		if(client.getGameCycle()%50 == 0)
				BufferedImage screenImage = sharedmem_rm.captureScreen();
				client.getCanvas().getGraphics().drawImage(screenImage, 0, 0, null);
		}*/
		log_Timed_Verbose("postDrawScene");
		//client.setReplaceCanvasNextFrame(true);
		//GpuPluginDrawCallbacks.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff) {
		//GpuPluginDrawCallbacks.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene) {
		sendBaseCoordinatePacket(scene);


		System.out.println("LoadScene");

		//System.out.println("Loading scene at "+System.currentTimeMillis() + "coord: X "+scene.getBaseX() + " Y "+scene.getBaseY());
		//GpuPluginDrawCallbacks.loadScene(scene);
	}

	@Override
	public void swapScene(Scene scene) {
		System.out.println("SwapScene");
		//System.out.println("Swapping scene at "+System.currentTimeMillis() + "coord: X "+scene.getBaseX() + " Y "+scene.getBaseY());
		//GpuPluginDrawCallbacks.swapScene(scene);
	}

	static boolean isShutDown = false;

	@Override
	protected void shutDown() throws Exception
	{
		isShutDown = true;
		System.out.println("RuneMod plugin shutDown");
		clientThread.invoke(() -> {
			if(runeModLauncher != null) {
				if(runeModLauncher.runemodApp != null) {
					runeModLauncher.runemodApp.destroyForcibly();
				}
			}

			setDefaults();
			//unRegisterWindowEventListeners();
		});
	}

	void setDefaults() {
		clientPlane = -1;

		unrealIsReady = false;

		toggleRuneModLoadingScreen(false);

		runeModLauncher = null;
		myCacheReader = null;

		if(runeMod_statusUI!=null) {
			runeMod_statusUI.close();
			runeMod_statusUI = null;
		}

		ticksSincePLuginLoad = -1;

		if(sharedmem_rm!=null) {
			//sharedmem_rm.destroyRuneModWin();
			sharedmem_rm.CloseSharedMemory();
			sharedmem_rm = null;
		}

		setGpuFlags(0);
		setDrawCallbacks(null);

		client.setUnlockedFps(false);
		client.setUnlockedFpsTarget(50);

		//toggleRuneModLoadingScreen(false);
		// force main buffer provider rebuild to turn off alpha channel
		client.resizeCanvas();

		lastCavansX = 0;
		lastCavansY = 0;
		lastCavansSizeX = 0;
		lastCavansSizeY = 0;

		storedGpuFlags = -1;

		rsCacheIsDownloading = true;
	}

	void setDrawCallbacks(DrawCallbacks drawCallbacks) {
		client.setDrawCallbacks(drawCallbacks);

		if (drawCallbacks==null) {
			System.out.println("Changed DrawCallbacks To Null");
		} else {
			System.out.println("Changed DrawCallbacks");
		}
	}

	public static boolean runemodLoadingScreenVisibility = false;

	public static JPanel RuneModLoadingScreen = new JPanel();
	public static Client client_static;

	public static Container canvasAncestor;
	public static  LayoutManager ogLayout;

	public static boolean unrealIsReady = false;

	public static void toggleRuneModLoadingScreen(Boolean toggled) {
		if(runemodLoadingScreenVisibility == toggled) {return;}
		runemodLoadingScreenVisibility = toggled;
		SwingUtilities.invokeLater(() ->
		{
			//System.out.println("toggling RmLoadingScreen to "+toggled);
			if(toggled) {
				//JPanel window = (JPanel) SwingUtilities.getAncestorOfClass();
				if(canvasAncestor == null) {return;}
				RuneModLoadingScreen.removeAll();
				//RuneModLoadingScreen.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 300));

				ogLayout = canvasAncestor.getLayout();

				canvasAncestor.setLayout(new BorderLayout(0,0));

				RuneModLoadingScreen.setBackground(Color.black);
				RuneModLoadingScreen.add(runeMod_statusUI.labelPanel);
				RuneModLoadingScreen.setSize(canvasAncestor.getSize());
				canvasAncestor.add(RuneModLoadingScreen, BorderLayout.CENTER, 0);
				canvasAncestor.revalidate();
				canvasAncestor.repaint();
			} else {
				if(canvasAncestor == null) {return;}
				canvasAncestor.remove(RuneModLoadingScreen);
				canvasAncestor.setLayout(ogLayout);
				canvasAncestor.revalidate();
				canvasAncestor.repaint();
			}
		});
	}

	public ApplicationSettings appSettings;
	//public Overlay_UI UI_Overlay;
	@SneakyThrows
	void startUp_Custom() {
//		clientUI.requestFocus();

		client_static = client;
		isShutDown = false;

		canvasAncestor = client.getCanvas().getParent(); //is "clientPannel" class

		RuneModPlugin.toggleRuneModLoadingScreen(true);

		configManager.setConfiguration("stretchedmode","keepAspectRatio", true);

		System.out.println("runelitDir: " + RUNELITE_DIR);

		sharedmem_rm.CreateMutexes();

		//start new rsdata. Unreal does not completely block rs (due to timeout), so it is safe to do his.
		{
			sharedmem_rm.LockMutex();
			sharedmem_rm.startNewRsData();
			sharedmem_rm.transferBackBufferToSharedMem();
			sharedmem_rm.passRsDataToUnreal();
		}

		//maintainRuneModStatusAttachment();

		//mapVarbitsToObjDefs(); //make map that shows us which objdef is linked to which varbit index

		//int GpuFlags = DrawCallbacks.GPU | (computeMode == ComputeMode.NONE ? 0 : DrawCallbacks.HILLSKEW);
		client.getTopLevelWorldView().getScene().setDrawDistance(90);
		client.setExpandedMapLoading(1);


		int MaxFps = Integer.parseInt(configManager.getConfiguration("RuneMod","MaxFps"));

		if(MaxFps > 50) {
			client.setUnlockedFps(true);
		} else {
			client.setUnlockedFps(false);
		}
		client.setUnlockedFpsTarget(MaxFps);


		client.getCanvas().setIgnoreRepaint(true);
		setGpuFlags(0);
		setDrawCallbacks(this);

		//registerWindowEventListeners();
	}

	@Override
	protected void startUp() throws IOException {
		clientThread.invoke(() -> {
			setDefaults();

			sharedmem_rm = new SharedMemoryManager(this);
			sharedmem_rm.createSharedMemory("sharedmem_rm", 50000000); //50 mb
			sharedmem_rm.createSharedMemory_ViewPort("sharedmem_rm_viewport", 50000000); //50 mb

			runeModPlugin = this;

			JFrame window = (JFrame) SwingUtilities.getAncestorOfClass(Frame.class, client.getCanvas());
			window.setIgnoreRepaint(true);

			runeMod_statusUI = new RuneMod_statusUI(window, this);


			//overlay.setLocationRelativeTo(runeModPlugin.client.getCanvas());
			//overlay.setVisible(true);

			runeModLauncher =  new RuneMod_Launcher(config.UseAltRuneModLocation() ? config.AltRuneModLocation() : "", config.StartRuneModOnStart());
			myCacheReader = new CacheReader();

			keyManager.registerKeyListener(hotkeyListenerw);
			keyManager.registerKeyListener(hotkeyListenerr);
			keyManager.registerKeyListener(hotkeyListenert);
		});
	}

	public static float signedToUnsigned(byte signedByte) {
		return signedByte & 0xFF; // Masking with 0xFF to get the unsigned value
	}

	// Convert unsigned byte back to signed byte
	public static byte unsignedToSigned(int unsignedByte) {
		if (unsignedByte < 0 || unsignedByte > 255) {
			throw new IllegalArgumentException("Value must be between 0 and 255");
		}
		return (byte) unsignedByte; // Casting to byte
	}

	public static byte multiplyByteAsIfUnsigned(byte value, float multiplier) {
		float multipliedVal = signedToUnsigned(value)*multiplier;
		return unsignedToSigned(Math.round(multipliedVal));
	}

	public void sendTextures() {
		int counter = 0;
			for (int i = 0; i < client.getTextureProvider().getTextures().length; i++) { //sends the textures to unreal to be saved as texture defs and materialdefs
				short texSizeX = 128;
				short texSizeY = 128;
				TextureProvider textureProvider = client.getTextureProvider();
				textureProvider.setBrightness(0.8);
				Texture tex = textureProvider.getTextures()[i];
				int[] pixels = textureProvider.load(i);
				if (tex!=null) {
					if (pixels != null) {
						counter++;
						//System.out.println("pixel len =" + pixels.length);
						Buffer mainBuffer = new Buffer (new byte[4+2+2+4+(texSizeX*texSizeY*4)]);
						mainBuffer.writeShort(i);
						mainBuffer.writeByte (tex.getAnimationDirection());
						mainBuffer.writeByte (tex.getAnimationSpeed());

						mainBuffer.writeShort (texSizeX); //write texSizeX. required by readImage ue4 function
						mainBuffer.writeShort (texSizeY); //write texSizeX. required by readImage ue4 function
						mainBuffer.writeInt((int)texSizeX*(int)texSizeY*4); //write byte array length. required  by readByteArray function in ue4
						//System.out.println("len "+pixels.length);
						boolean hasAlpha = false;

						for (int i0 = 0; i0 < texSizeX*texSizeY; i0++) { //write byte array content
							int pixelValue = pixels[i0];
							byte a = (byte)((pixelValue >> 24) & 0xff);
							if (a != 0){hasAlpha = true;}
						}

						//System.out.println("hasALpha: " + hasAlpha);

						for (int i0 = 0; i0 < texSizeX*texSizeY; i0++) { //write byte array content
							int pixelValue = pixels[i0];
							//byte a = (byte)((pixelValue >> 24) & 0xff);
							byte a = (byte)255;
							byte r = (byte) ((pixelValue >> 16) & 0xff);
							byte g = (byte) ((pixelValue >> 8) & 0xff);
							byte b = (byte)((pixelValue >> 0) & 0xff);

							pixelValue = (int) Math.round(255 - Math.pow(pixelValue / 255.0, 2.2));

							if (r == 0 && b == 0 && g == 0) {
								a = 0;
							}
/*					r = 5;
					g = 5;
					b = 5;
					a = 5;*/
/*							mainBuffer.writeByte(b);
							mainBuffer.writeByte(g);
							mainBuffer.writeByte(r);
							mainBuffer.writeByte(a);*/
							mainBuffer.writeByte(multiplyByteAsIfUnsigned(b, 0.9f));
							mainBuffer.writeByte(multiplyByteAsIfUnsigned(g, 0.9f));
							mainBuffer.writeByte(multiplyByteAsIfUnsigned(r, 0.9f));
							mainBuffer.writeByte(a);
						}
						RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "Texture");
						//myRunnableSender.sendBytes(pixelsBuffer.array,"Texture");
					}
				}
			}
		System.out.println("Sent "+ counter +" Textures");
		//System.out.println("anim speed: "+ tex.getAnimationSpeed());
		//System.out.println("anim direction: "+ tex.getAnimationDirection());
	}

	public void provideRsCacheData() {
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
		mainBuffer.writeInt(789728); //random number. just here to fill the packet.
		RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "RsCacheDataProvided");
		System.out.println("RsCacheData has been provided To Unreal");
	}

	@SneakyThrows
	@Subscribe
	private void onConfigChanged(ConfigChanged event) {
		clientThread.invoke(() ->
		{
			if (event.getGroup().equalsIgnoreCase("RuneMod")) {
				System.out.println("RuneModConfigChanged to " + event.getNewValue());
				if(event.getKey().equalsIgnoreCase("RuneModVisibility")) {
					if(event.getNewValue().equalsIgnoreCase("true")) {
						clientThread.invoke(() -> {
							setDrawCallbacks(this);
						});

					} else {
						clientThread.invoke(() -> {
							setDrawCallbacks(null);
						});
					}
				}

				if(event.getKey().equalsIgnoreCase("useTwoRenderers")) {
					if(event.getNewValue().equalsIgnoreCase("true")) {
						clientThread.invoke(() -> {
							setDrawCallbacks(null);
						});

					} else {
						clientThread.invoke(() -> {
							setDrawCallbacks(this);
						});
					}
				}

				if(event.getKey().equalsIgnoreCase("MaxFps")) {
					int MaxFps = Integer.parseInt(event.getNewValue());

					if(MaxFps > 50) {
						client.setUnlockedFps(true);
					} else {
						client.setUnlockedFps(false);
					}

					client.setUnlockedFpsTarget(MaxFps);
				}

				if(event.getKey().equalsIgnoreCase("attachRmWindowToRL")) {
					if(event.getNewValue().equalsIgnoreCase("true")) {
						sharedmem_rm.ChildRuneModWinToRl();
					} else {
						sharedmem_rm.UnChildRuneModWinFromRl();
					}
				}
			}
		});
	}

	class NpcOverrides_Copy {
		int[] modelIds;

		short[] colorToReplaceWith;

		short[] textureToReplaceWith;

		boolean useLocalPlayer;

		public NpcOverrides_Copy(NpcOverrides overrides) {
			this.modelIds = overrides.getModelIds();
			this.colorToReplaceWith = overrides.getColorToReplaceWith();
			this.textureToReplaceWith = overrides.getTextureToReplaceWith();
			this.useLocalPlayer = overrides.useLocalPlayer();
		}

		public boolean isIdenticalTo(NpcOverrides_Copy other) {
			boolean isEqual = true;
			if(!Arrays.equals(other.modelIds, modelIds)) {
				isEqual = false;
			}
			if(!Arrays.equals(other.colorToReplaceWith, colorToReplaceWith)) {
				isEqual = false;
			}
			if(!Arrays.equals(other.textureToReplaceWith, textureToReplaceWith)) {
				isEqual = false;
			}
			if(!other.useLocalPlayer == useLocalPlayer) {
				isEqual = false;
			}
			return isEqual;
		}
	}

	private HashMap<NPC, NpcOverrides_Copy> npcsWithOverrides_LastFrame =  new HashMap<NPC, NpcOverrides_Copy>();
	//if npc in curfram has overrides, and it didnt in last frame, overridesChanged.
	//if npcsWithOverrides in last frame is missing from npcsWithOverrides in currentFrame, overridesChanged.
	//if npc With Overrides is present in last and current frame, check if the overrides from each frame are equal. if they are not, , overridesChanged.


	private void rgbaIntToColors(int col) {
		int a = (col >> 24) & 0xFF;
		int r = (col >> 16) & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = col & 0xFF;
	}

	private void printUIPixelAtMousePos() {
/*		System.out.println(client.getGraphicsPixelsWidth());
		int arrayPos = client.getMouseCanvasPosition().getX() + (client.getMouseCanvasPosition().getY()*client.getGraphicsPixelsWidth());
		int col = client.getGraphicsPixels()[arrayPos];
		int a = (col >> 24) & 0xFF;
		int r = (col >> 16) & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = col & 0xFF;
		System.out.print("a");
		System.out.print(" "+a+" ");
		System.out.print("r");
		System.out.print(" "+r+" ");
		System.out.print("g");
		System.out.print(" "+g+" ");
		System.out.print("b");
		System.out.print(" "+b+" ");*/
	}

	public Component component = new Component() {
		@Override
		public void addNotify() {
			super.addNotify();
		}
	};



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
		//final GameStateChanged gameStateChanged = new GameStateChanged();
		//gameStateChanged.setGameState(GameState.HOPPING); //causes unreal to clear scene
		//onGameStateChanged(gameStateChanged);
		simulateGameEvents();
	}

	public void simulateGameEvents()
	{
		System.out.println("simulating game-events");

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

/*			for (final ItemContainer itemContainer : client.getItemContainers())
		{
			eventBus.post(new ItemContainerChanged(itemContainer.getId(), itemContainer));
		}*/

		for (NPC npc : client.getCachedNPCs())
		{
			if (npc != null)
			{
				final NpcSpawned npcSpawned = new NpcSpawned(npc);
				onNpcSpawned(npcSpawned);
			}
		}

		for (Player player : client.getCachedPlayers())
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
						if(object instanceof TileObject) {
							if (object.getRenderable()!=null) {
								if(object.getRenderable() instanceof DynamicObject || object.getRenderable() instanceof Model || object.getRenderable() instanceof ModelData) {
									final GameObjectSpawned objectSpawned = new GameObjectSpawned();
									objectSpawned.setTile(tile);
									objectSpawned.setGameObject(object);
									onGameObjectSpawned(objectSpawned);
								} else {
/*									if(object.getRenderable() instanceof Actor) {
										System.out.println("unhandled renderableClass: Actor");
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


	private static final Keybind myKeybindW = new Keybind(KeyEvent.VK_W, InputEvent.SHIFT_DOWN_MASK);
	private final HotkeyListener hotkeyListenerw = new HotkeyListener(() -> myKeybindW)
	{
		@SneakyThrows
		@Override
		public void hotkeyPressed() //send terrain
		{
			clientThread.invokeAtTickEnd(() ->
				{
					//sendTexture_test();
/*					myCacheReader.sendModels();
					myCacheReader.sendKitDefinitions();
					myCacheReader.sendObjectDefinitions();
					myCacheReader.sendItemDefinitions();
					myCacheReader.sendSequenceDefinitions();
					myCacheReader.sendNpcDefinitions();
					myCacheReader.sendSpotAnimations();
					myCacheReader.sendSkeletons();
					myCacheReader.sendFrames();
					myCacheReader.sendTiles();
					sendTextures();
					myCacheReader.sendOverlayDefinitions();
					myCacheReader.sendUnderlayDefinitions();*/
				});

			//System.out.println("sending textures");
			//sendTextures();

/*			clientThread.invoke(() ->
			{
				SpotAnimationDefinition_get(800);
			});*/

/*			for (int i = 0; i < client.getTextureProvider().getTextures().length; i++) { //sends the textures to unreal to be saved as texture defs and materialdefs
				sendTexture(i);
			}*/

			//sendTexture(46);


/*			while(entry != null) {

				System.out.println(entry.getName());
				if (!entry.isDirectory()) {
					// if the entry is a file, extracts it
					System.out.println("===File===");

				} else {
					System.out.println("===Directory===");
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();

			}*/

			//eventQ.postEvent(mouseEvent);



			//client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(),MouseEvent.MOUSE_PRESSED, System.currentTimeMillis() + 10, MouseEvent.BUTTON1, 200,200, 0, false));


			//var event = new MouseEvent(component, 0, 0, 0, 200, 200, 1, false);
			//eventQ.dispatchEvent(event);

/*			client.setPlane(config.VarStrID());

			Tile tile = client.getSelectedSceneTile();
			GameObject[] gameobjects = tile.getGameObjects();
			if (gameobjects.length > 0)
			{
					client.getScene().removeGameObject(gameobjects[0]);

			}*/
			//sendAnimation(config.animSequenceDefID());
			System.out.println("shift+w");
			//byte[] bytes = new byte[3];
			//myRunnableSender.sendBytesTest(bytes, "LibMesh");
/*			int[] playerEquipmentIds = client.getLocalPlayer().getPlayerComposition().getEquipmentIds();
			int[] bodyColors = client.getLocalPlayer().getPlayerComposition().getBodyPartColours();

			for (int i = 0; i < playerEquipmentIds.length; i++)
			{
				System.out.println("equipIndex "+ i +" = " + playerEquipmentIds[i]);
			}

			for (int i = 0; i < bodyColors.length; i++)
			{
				System.out.println("bodyColorIndex "+ i +" = " + bodyColors[i]);
			}*/
			//client.setCameraY(client.getCameraY()+100);
		}
	};


/*
skills menu:__________
2
		57
		-1
		20,971,521

	bond pounch:__________
1
		57
		-1
		7602252

	logout:__________
1
		57
		-1
		11927560

	drop item no 5__________
		7
		1007
		4
		9764864

	use slot 5__________
		0
		25
		4
		9764864

	use slot 5 on 6__________
		0
		58
		5
		9764864
*/

	private static final Keybind myKeybindR = new Keybind(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK);
	private final HotkeyListener hotkeyListenerr = new HotkeyListener(() -> myKeybindR)
	{
		@Override
		public void hotkeyPressed()
		{
			//we just sent stuff to unreal, now we wait for unreal to gte back to us
			clientThread.invoke(() ->
			{
				//sharedmem_rm.AwaitUnrealMutex_Locked();
				//System.out.println("UnrealMutexIsLocked");
				//sharedmem_rm.UnrealMutexTest();
				//sharedmem_rm.setRuneModVisibility(true);
				//sharedmem_rm.ChildRuneModWinToRl();
			});

			//client.setCameraY2(client.getCameraY()+100);
		}
	};

	private static final Keybind myKeybindT = new Keybind(KeyEvent.VK_T, InputEvent.ALT_DOWN_MASK);
	private final HotkeyListener hotkeyListenert = new HotkeyListener(() -> myKeybindT)
	{
		@Override
		public void hotkeyPressed()
		{
/*			clientThread.invoke(() ->
			{
				sharedmem_rm.setRuneModVisibility(false);
			});*/
		}
	};


	@Provides
	RuneModConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneModConfig.class);
	}


	@Subscribe
	private void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"drawSpecbarAnyway".equals(event.getEventName()))
		{
			return;
		}

		int[] iStack = client.getIntStack();
		int iStackSize = client.getIntStackSize();
		iStack[iStackSize - 1] = 1;
	}

	@Subscribe
	private void onGameTick(GameTick event) {
/*		client.getCanvas().setVisible(false);
		client.getCanvas().getParent().setVisible(false);
		client.getCanvas().getParent().getParent().setVisible(false);
		client.getCanvas().getParent().getParent().getParent().setVisible(false);
		client.getCanvas().getParent().getParent().getParent().getParent().setVisible(false);*/
		//clientUI.requestFocus();
		//sharedmem_rm.putRuneModOnTop();
/*		var notif = defaultNotification(TrayIcon.MessageType.NONE);
		notify(notif, message);*/
		//notifier.notify("crashTest");
		//clientUI.flashTaskbar();

		//clientUI.requestFocus();//causes crash if rm window is childed

		//clientUI.forceFocus();
/*		Toolkit.getDefaultToolkit().beep();
		if (clientUI.getTrayIcon() != null)
		{
			clientUI.getTrayIcon().displayMessage("title", "message", TrayIcon.MessageType.NONE);
		}*/
/*		switch (notification.getRequestFocus())
		{
			case REQUEST:
				clientUI.requestFocus();
				break;
			case TASKBAR:
				clientUI.flashTaskbar();
				break;
			case FORCE:
				clientUI.forceFocus();
				break;
		}*/
		//getVarbits();

/*		MapDefinition.Tile standingTile = myCacheReader.getTileAtCoordinate(client.getLocalPlayer().getWorldLocation().getX(), client.getLocalPlayer().getWorldLocation().getY(),0);
		int underlay = standingTile.underlayId;
		int overlay = standingTile.overlayId;

		System.out.println("underlay: " + underlay + "  Overlay: " + overlay);*/

		//System.out.println("clientPixelsX = "+client.getGraphicsPixelsWidth());

//		System.out.println("tick ahem");

	}

	private int OrientationToAngles(int orient) {
		int angle = 1;
		switch (orient) {
			case 1:
				angle=0;
				break;
			case 2:
				angle=90;
				break;
			case 4:
				angle=180;
				break;
			case 8:
				angle=270;
				break;
			case 16:
				angle=45;
				break;
			case 32:
				angle=135;
				break;
			case 64:
				angle=225;
				break;
			case 128:
				angle = 315;
				break;
		}
		if (angle == 1) {
			System.out.println("orientToAngles Failed" + orient);
		}
		return angle;
	}

	private	int getObjModelTypeFromFlags(int flags){
		return flags & 63;
	}

	public void resendGameStateChanged() {
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
	private void onGameStateChanged(GameStateChanged event) {
		//clientThread.invoke(() ->
		//{
			lastGameState = curGamestate;
			curGamestate = event.getGameState();
			System.out.print("gameStateChanged to: ");

			byte newEventTypeByte = 0;
			if (curGamestate == GameState.LOGIN_SCREEN) {
				appSettings = loadAppSettings(); //reload appSettings

				if(appSettings != null) { //makes login screen background transparent if animeLoginScreen appsetting is true
					if(appSettings.animateLoginScreen) {
						SpritePixels spritePixels = client.createSpritePixels(new int[0],0,0);
						client.setLoginScreen(spritePixels);
						client.setShouldRenderLoginScreenFire(false);
					} else {
						client.setLoginScreen(null);
					}
				} else {
					System.out.println("appsettings is null");
				}


				//sendVarbits();
				System.out.println("Login SCREEN...");
				baseX = -1;
				baseY = -1;
				//newEventTypeByte = 1;
			} else if (curGamestate == GameState.LOGGING_IN) {

				System.out.println("logging in...");
				//newEventTypeByte = 2;
			} else if (curGamestate == GameState.LOGGED_IN) {

				System.out.println("logged in...");
				//newEventTypeByte = 3;
			} else if (curGamestate == GameState.HOPPING) {
				baseX = -1;
				baseY = -1;

				System.out.println("hopping...");
				//newEventTypeByte = 4;
			} else if (curGamestate == GameState.LOADING) {
				System.out.println("loading...");
				//newEventTypeByte = 5;
			} else if (curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR) {
				System.out.println("Authenticator SCREEN...");
			} else if (curGamestate == GameState.CONNECTION_LOST) {
				System.out.println("ConnectionLost...");
			} else if (curGamestate == GameState.STARTING) {
				System.out.println("STARTING...");
			} else if (curGamestate == GameState.UNKNOWN) {
				System.out.println("UNKNOWN...");
			}
		System.out.println("ordsinal: "+event.getGameState().ordinal());

			Buffer packet = new Buffer(new byte[10]);
			packet.writeByte(event.getGameState().ordinal());
			sharedmem_rm.backBuffer.writePacket(packet, "GameStateChanged");
			//myRunnableSender.sendBytes(new byte[] {newEventTypeByte,0,0},"GameStateChanged");
		//});
	}

	@Subscribe
	private void onGraphicChanged(GraphicChanged graphicChanged)
	{
		//System.out.println("graphic changed to: " + graphicChanged.getActor().getGraphic());
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event) {
/*		if (projectiles.contains(event.getProjectile())) {
			//System.out.println("projectileMoved");
		}  else {
			projectiles.add(event.getProjectile());
			//System.out.println("projectileSpawned with hash:"+ event.getProjectile().getHash());
			//System.out.println("java hash:"+ event.getProjectile().hashCode());
		}*/
/*		if(!config.spawnProjectiles()) {return;}

		Buffer actorSpawnPacket = new Buffer(new byte[30]);

		Projectile projectile = event.getProjectile();

		int hash = event.hashCode();
		short localX = (short)projectile.getX();
		short localY = (short)projectile.getY();
		short localZ = (short)projectile.getZ();
		byte plane = (byte)projectile.getFloor();

		actorSpawnPacket.writeInt(hash);
		actorSpawnPacket.writeShort(localX);
		actorSpawnPacket.writeShort(localY);
		actorSpawnPacket.writeShort(localZ);
		actorSpawnPacket.writeByte(plane);*/

		//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ProjectileMoved");

/*		if (projectile!=null) System.out.println("projectile moved: "+projectile.getId());
		System.out.println(event.hashCode()); //yes, is identifier
		System.out.println("X: "+); //yes
		System.out.println("Y: "+projectile.getY()); //yes
		System.out.println("Z: "+projectile.getZ()); //yes*/


/*		System.out.println("Pos: "+event.getPosition().getX()+", "+event.getPosition().getY());
		System.out.println("Z: "+event.getZ());
		System.out.println("slope: "+projectile.getSlope());
		System.out.println("X1: "+projectile.getX1());
		System.out.println("Y1: "+projectile.getY1());
		System.out.println("Scalar: "+projectile.getScalar());*/
	}

	public void RmVisChanged(boolean visibility)
	{
		clientThread.invoke(() ->
		{
			Buffer packet = new Buffer(new byte[5]);

			packet.writeBoolean(visibility);

			sharedmem_rm.backBuffer.writePacket(packet, "RmVisChanged");
		});
	}

	@Subscribe
	private void onWallObjectDespawned(WallObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			if (!config.spawnGameObjects()) {
				return;
			}

			Tile tile;
			if (event.getTile().getBridge() != null) {
				tile = event.getTile().getBridge();
			} else {
				tile = event.getTile();
			}

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getPlane();
			if (event.getTile().getBridge() != null) {
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
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
			System.out.println("wallobject despawned: " + event.getWallObject().getId());
		});
	}

	@Subscribe
	private void onWallObjectSpawned(WallObjectSpawned event) {
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
				if (!config.spawnGameObjects()) {
					return;
				}
				//clientThread.invokeLater(() ->
				//{
				Tile tile;
				tile = event.getTile();


				Buffer actorSpawnPacket = new Buffer(new byte[100]);
				int tileObjectModelType = getObjModelTypeFromFlags(event.getWallObject().getConfig());
				int var4 = (event.getWallObject().getConfig() - tileObjectModelType) >> 6 & 3;

				int rotation = var4;
				int anint = (rotation * 512);

				int objectOrientationA = anint;
				int objectOrientationB = 1234;
				if (tileObjectModelType == 2) { //if wall is objectType 2. walltype 2 has a model B;
					rotation = (event.getWallObject().getConfig() >> 6) & 3;
					rotation = rotation + 1 & 3;
					anint = (rotation * 512);
					objectOrientationB = anint;
				}
				int objectDefinitionId = event.getWallObject().getId();
				int plane = event.getTile().getRenderLevel();

				int tileX = (event.getWallObject().getX()) / 128;
				int tileY = (event.getWallObject().getY()) / 128;

				byte[][][] tileSettings = client.getTileSettings();

/*			if (plane < Constants.MAX_Z - 1 && (tileSettings[1][tile.getSceneLocation().getX()][tile.getSceneLocation().getY()] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE)
		{
			plane = plane+1;
			plane = plane > 3 ? 3 : plane < 0 ? 0 : plane;
		}*/
				int plane_v2 = plane;
				if (tile.getBridge() != null) {
					plane_v2 = 0;
				}

				//int height = getTileHeight_beforeBridgeCode(client, tile.getLocalLocation(), plane) * -1;
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
				//int tileMinPlane = tile.getPhysicalLevel(); //api missing
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
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
		//});








/*		int objDefID1 = wallObjectspawned.getWallObject().getId();
		loadedObjDefinitions[objDefID1] = client.getObjectDefinition(objDefID1);

		WallObject gameObject = wallObjectspawned.getWallObject();
		Tile tile = wallObjectspawned.getTile();
		WorldPoint tileLocation = tile.getWorldLocation();
		if (gameObject != null) {
			int objDefID = gameObject.getId();
			int orientation = OrientationToAngles(gameObject.getOrientationA());

			LocalPoint location = gameObject.getLocalLocation();
			int tilePosZ = ((Perspective.getHeight(client, location.getX(), location.getY(), tile.getPlane())) * -1);

			Long GameObjectHash = Long.parseLong("" + gameObject.getId() + tile.getPlane()+ tile.getSceneLocation().getX() + tile.getSceneLocation().getY());

			spawnedObjectHashes.add(GameObjectHash);

			ObjectDefinition objDef = loadedObjDefinitions[objDefID];
			int objDefModelIds[] = objDef.getModelIds();

			if (objDefModelIds!= null) {
				UnrealGameObject uGameObject = client.getUGameObjectsMap().get(GameObjectHash);
				if (uGameObject != null) {
					System.out.println("uGameObjectNotNull");
					uGameObject.setWorldPoint(tileLocation);
					uGameObject.setOrientation(orientation);
					uGameObject.setLocalPosX(location.getX());
					uGameObject.setLocalPosY(location.getY());
					uGameObject.setLocalPosZ(tilePosZ);
					if (gameObject.getRenderable2()!= null) {
						uGameObject.setOrientationB(OrientationToAngles(gameObject.getOrientationB()));
					}

					client.getUGameObjectsMap().put(GameObjectHash, uGameObject);

				}
			}
		}*/
	}

	@Subscribe
	private void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			Tile tile;
/*			if (event.getTile().getBridge() != null) {
				tile = event.getTile().getBridge();
			} else {*/
			tile = event.getTile();
			//}

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();
/*			if (event.getTile().getBridge() != null) {
				tilePlane++;
			}*/

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getDecorativeObject());


			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
		});
	}

	@Subscribe
	private void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
		if(!config.spawnGameObjects()) {return;}
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
			if (tileObjectModelType == 8) { //if wall is objectType 2. walltype 2 has a model B;
				rotation = (event.getDecorativeObject().getConfig() >> 6) & 3;
				rotation = (rotation + 2 & 3);
				objectOrientationB = (rotation+4) * 512;
				objectOrientationA = ((var4+4)*512);
			}
			int objectDefinitionId = event.getDecorativeObject().getId();
			int plane = tile.getRenderLevel();
/*			if ((client.getTileSettings()[1][tile.getSceneLocation().getX()][tile.getSceneLocation().getY()] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE) //if tileStack IsLinkedBellow
			{
				if (tile.getBridge()==null) { //if this tile used to be on plane0
					plane = plane;
				} else {
					plane++;
				}
			}*/

			int tileX = (event.getDecorativeObject().getX())/128;
			int tileY = (event.getDecorativeObject().getY())/128;

			byte[][][] tileSettings = client.getTileSettings();

/*			if (plane < Constants.MAX_Z - 1 && (tileSettings[1][tile.getSceneLocation().getX()][tile.getSceneLocation().getY()] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE)
			{
				plane = plane-1;
				plane = plane > 3 ? 3 : plane < 0 ? 0 : plane;
			}*/

			int plane_v2 = plane;
			if (tile.getBridge()!=null) {
				plane_v2 = 0;
			}

			//int height = getTileHeight_beforeBridgeCode(client, tile.getLocalLocation(), plane) * -1;
			int height = event.getDecorativeObject().getZ()*-1;

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			//int tileMinPlane = tile.getPhysicalLevel(); //api missing
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
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
		});
	}

	public int getTileHeight_beforeBridgeCode(Client client, LocalPoint point, int plane)
	{
		int LOCAL_COORD_BITS = 7;
		int LOCAL_TILE_SIZE = 1 << LOCAL_COORD_BITS;

		int sceneX = point.getSceneX();
		int sceneY = point.getSceneY();
		if (sceneX >= 0 && sceneY >= 0 && sceneX < Constants.SCENE_SIZE && sceneY < Constants.SCENE_SIZE)
		{
			byte[][][] tileSettings = client.getTileSettings();
			int[][][] tileHeights = client.getTileHeights();

			int z1 = plane;
			if (plane < Constants.MAX_Z - 1 && (tileSettings[plane][sceneX][sceneY] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE)
			{
				z1 = plane + 1;
			}

			int x = point.getX() & (LOCAL_TILE_SIZE - 1);
			int y = point.getY() & (LOCAL_TILE_SIZE - 1);
			int var8 = x * tileHeights[z1][sceneX + 1][sceneY] + (LOCAL_TILE_SIZE - x) * tileHeights[z1][sceneX][sceneY] >> LOCAL_COORD_BITS;
			int var9 = tileHeights[z1][sceneX][sceneY + 1] * (LOCAL_TILE_SIZE - x) + x * tileHeights[z1][sceneX + 1][sceneY + 1] >> LOCAL_COORD_BITS;
			return (LOCAL_TILE_SIZE - y) * var8 + y * var9 >> LOCAL_COORD_BITS;
		}

		return 0;
	}

/*	public int getTileHeight_beforeBridgeCode(Client client, Point point, int plane)
	{
		int LOCAL_COORD_BITS = 7;
		int LOCAL_TILE_SIZE = 1 << LOCAL_COORD_BITS;

		int sceneX = point.getX();
		int sceneY = point.getY();

		if (sceneX >= 0 && sceneY >= 0 && sceneX < Constants.SCENE_SIZE && sceneY < Constants.SCENE_SIZE)
		{
			byte[][][] tileSettings = client.getTileSettings();
			int[][][] tileHeights = client.getTileHeights();
			int AverageHeight = tileHeights[plane][sceneX][sceneY] + tileHeights[plane][sceneX+1][sceneY+1] + tileHeights[plane][sceneX+1][sceneY] + tileHeights[plane][sceneX][sceneY+1] >> 2;
			return AverageHeight;

*//*			int z1 = plane;
*//**//*			if (plane < Constants.MAX_Z - 1 && (tileSettings[plane][sceneX][sceneY] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE)
			{
				z1 = plane + 1;
			}*//**//*

			int x = point.getX() & (LOCAL_TILE_SIZE - 1);
			int y = point.getY() & (LOCAL_TILE_SIZE - 1);
			int var8 = x * tileHeights[z1][sceneX + 1][sceneY] + (LOCAL_TILE_SIZE - x) * tileHeights[z1][sceneX][sceneY] >> LOCAL_COORD_BITS;
			int var9 = tileHeights[z1][sceneX][sceneY + 1] * (LOCAL_TILE_SIZE - x) + x * tileHeights[z1][sceneX + 1][sceneY + 1] >> LOCAL_COORD_BITS;
			return (LOCAL_TILE_SIZE - y) * var8 + y * var9 >> LOCAL_COORD_BITS;*//*
		}

		return 0;
	}*/

	public static boolean wasInScene(int baseX, int baseY, int x, int y)
	{
		int maxX = baseX + Perspective.SCENE_SIZE;
		int maxY = baseY + Perspective.SCENE_SIZE;

		return x >= baseX && x < maxX && y >= baseY && y < maxY;
	}


	//private HashMap<Integer, Integer> VarbitObjDef_Map = new HashMap<Integer, Integer>();
	//private HashMap<Integer, Integer> VarpObjDef_Map = new HashMap<Integer, Integer>();

/*	private void mapVarbitsToObjDefs() {
		int objDefCount = myCacheReader.getCacheFiles(IndexType.CONFIGS, ConfigType.OBJECT.getId()).size();
		//System.out.println(objDefCount+" ObjDefs");
		for (int i = 0; i < objDefCount; i++) {
			ObjectComposition objDef = client.getObjectDefinition(i);
			if (objDef!= null) {
				if (objDef.getVarbitId()!=-1) {
					//System.out.println("stored objdef varbit");
					VarbitObjDef_Map.put(objDef.getVarbitId(), i);
				}
				if (objDef.getVarPlayerId()!=-1) {
					//System.out.println("stored objdef varp");
					VarpObjDef_Map.put(objDef.getVarPlayerId(), i);
				}
			}
		}
	}*/

/*	private void mapVarbitsToNpcDefs() {
		int npcDefCount = myCacheReader.getCacheFiles(IndexType.CONFIGS, ConfigType.NPC.getId()).size();
		//System.out.println(objDefCount+" ObjDefs");
		for (int i = 0; i < npcDefCount; i++) {
			NPCComposition npcDef = client.getNpcDefinition(i);
			if (npcDef!= null) {
				if (npcDef.getVarbitId()!=-1) {
					//System.out.println("stored objdef varbit");
					VarbitNpcDef_Map.put(objDef.getVarbitId(), i);
				}
				if (npcDef.getVarPlayerId()!=-1) {
					//System.out.println("stored objdef varp");
					VarpNpcDef_Map.put(objDef.getVarPlayerId(), i);
				}
			}
		}
	}*/

	private HashMap<Integer, Integer> imposterToOriginal_Map = new HashMap<Integer, Integer>();
	@Subscribe
	private void onVarbitChanged(VarbitChanged event) {

		//for VarbitChanged func in unreal, we send: varType. varId. varValue. custom0.
		//clientThread.invoke(() ->
		//{
			//System.out.println("varbitChanged");
			Buffer buffer = new Buffer(new byte[12]);

			if (event.getVarbitId() != -1) {
				//System.out.println("varbit "+ event.getVarbitId()+"cahnged to "+event.getValue());
				buffer.writeInt(event.getVarbitId());
				buffer.writeInt(event.getValue());
				//int objDefId = VarbitObjDef_Map.getOrDefault(event.getVarbitId(), -1);
				//if (objDefId != -1) {
					//buffer.writeInt(objDefId);
					sharedmem_rm.backBuffer.writePacket(buffer, "Varbit");

					//System.out.println("objDef: " + objDefId + " Imposter Changed to index " + event.getValue());
				//}
			} else {
				//System.out.println("varP "+ event.getVarbitId()+"cahnged to "+event.getValue());
				buffer.writeInt(event.getVarpId());
				buffer.writeInt(event.getValue());
				//int objDefId = VarpObjDef_Map.getOrDefault(event.getVarpId(), -1);
				//if (objDefId != -1) {
					//buffer.writeInt(objDefId);
					sharedmem_rm.backBuffer.writePacket(buffer, "Varp");

					//System.out.println("objDef: " + objDefId + " Imposter Changed to index " + event.getValue());
				//}
			}
		//});
	}

	public void sendVarbits() {
		HashMap<Integer, Integer> varbits = new HashMap<Integer, Integer>();
		System.out.println("Sending varbits");
		int VARBITS_ARCHIVE_ID = 14;

		//Buffer buffer = new Buffer(new byte[128000]);

		//clientThread.invoke(() ->
		//{
			IndexDataBase indexVarbits = client.getIndexConfig();
			final int[] varbitIds = indexVarbits.getFileIds(VARBITS_ARCHIVE_ID);
			for (int id : varbitIds)
			{
				VarbitChanged varbitChangedEvent = new VarbitChanged();
				varbitChangedEvent.setVarbitId(id);
				varbitChangedEvent.setValue(client.getVarbitValue(id));
				onVarbitChanged(varbitChangedEvent);
			}
		//});
	}

/*	public void getVarbits() {
		HashMap<Integer, Integer> varbits = new HashMap<Integer, Integer>();
		int VARBITS_ARCHIVE_ID = 14;

		Buffer buffer = new Buffer(new byte[128000]);

		clientThread.invoke(() ->
		{
			IndexDataBase indexVarbits = client.getIndexConfig();
			final int[] varbitIds = indexVarbits.getFileIds(VARBITS_ARCHIVE_ID);
			for (int id : varbitIds)
			{
				VarbitComposition varbit = client.getVarbit(id);
				if (varbit != null)
				{
					int varBitVal = client.getVarbitValue(id);
					System.out.println("varbit: "+id+" has val: "+varBitVal);
					buffer.writeInt(id); //write varbit id
					buffer.writeInt(id); //write varbit value
					//varbits.put(varbit.getIndex(), id);
				}
			}
		});
	}*/

	private List<RuneLiteObject> getRlObjects()
	{
		List<RuneLiteObject> rlObjects = new ArrayList<>();
		if(client.getScene() == null || client.getScene().getTiles() == null) {return rlObjects;}
		for (int z = 0; z < Constants.MAX_Z; ++z) {
			for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
				for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
					Tile tile = client.getScene().getTiles()[z][x][y];
					if(tile!=null) {
						GameObject[] gameObjects = tile.getGameObjects();
						for (GameObject gameObject : gameObjects)
						{
							if (gameObject != null && gameObject.getRenderable() instanceof RuneLiteObject)
							{
								RuneLiteObject runeLiteObject = (RuneLiteObject) gameObject.getRenderable();
								if(runeLiteObject!=null) {
									rlObjects.add(runeLiteObject);
									System.out.println("Found RuneLiteObject");
								}
							}
						}
					}
				}
			}
		}
		return rlObjects;
	}

	private Map<Long, DynamicObject> getAnimatedGameObjects()
	{
		Map<Long, DynamicObject> allGameObjects = new HashMap<>();
		if(client.getScene() == null || client.getScene().getTiles() == null) {return allGameObjects;}
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
									if(gameObject.getRenderable() != null) {
										if(gameObject.getRenderable() instanceof DynamicObject) {

											DynamicObject dynamicObject = (DynamicObject)gameObject.getRenderable();
											if(dynamicObject.getAnimation()!= null) {

												allGameObjects.put(getTag_Unique(gameObject), dynamicObject);
/*												if(gameObject.getId() == 5589 ) {
													System.out.println("animFrameCycle = " + dynamicObject.getAnimCycle());
												}*/
											}
										}
									}
								}
							}
						}

						GroundObject groundObject = tile.getGroundObject();
						if (groundObject != null) // && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()
						{
							if(groundObject.getRenderable() != null) {
								if(groundObject.getRenderable() instanceof DynamicObject) {
									DynamicObject dynamicObject = (DynamicObject)groundObject.getRenderable();
									if(dynamicObject.getAnimation()!= null) {
										allGameObjects.put(getTag_Unique(groundObject), dynamicObject);
/*												if(gameObject.getId() == 5589 ) {
												System.out.println("animFrameCycle = " + dynamicObject.getAnimCycle());
											}*/
									}
								}
							}
						}

						DecorativeObject decorativeObject = tile.getDecorativeObject();
						if (decorativeObject != null) // && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()
						{
							if(decorativeObject.getRenderable() != null) {
								if(decorativeObject.getRenderable() instanceof DynamicObject) {
									DynamicObject dynamicObject = (DynamicObject)decorativeObject.getRenderable();
									if(dynamicObject.getAnimation()!= null) {
										allGameObjects.put(getTag_Unique(decorativeObject), dynamicObject);
/*												if(gameObject.getId() == 5589 ) {
												System.out.println("animFrameCycle = " + dynamicObject.getAnimCycle());
											}*/
									}
								}
							}
						}

						WallObject wallbject =  tile.getWallObject();
						if (wallbject != null) // && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()
						{
							if(wallbject.getRenderable1() != null) {
								if(wallbject.getRenderable1() instanceof DynamicObject) {
									DynamicObject dynamicObject = (DynamicObject)wallbject.getRenderable1();

									if(dynamicObject.getAnimation()!= null) {
										allGameObjects.put(getTag_Unique(wallbject), dynamicObject);
										//System.out.println("wall object:" + wallbject.getId() + " has anim " + dynamicObject.getAnimation().getId());
/*												if(gameObject.getId() == 5589 ) {
												System.out.println("animFrameCycle = " + dynamicObject.getAnimCycle());
											}*/
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
			if (!config.spawnGameObjects()) {
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

			if(objectDefinitionId == 12166) {
				System.out.println("12166 spawned");
			}

			if(objectDefinitionId == 12144) {
				System.out.println("12144 spawned");
			}


			if(objectDefinitionId == 144) {
				System.out.println("gate GO spawned (id 144)");
			}




			int plane = tile.getRenderLevel();
			int tileX = event.getGameObject().getSceneMinLocation().getX();
			int tileY = event.getGameObject().getSceneMinLocation().getY();

			int plane_v2 = plane;
			if (tile.getBridge()!=null) {
				plane_v2 = 0;
			}
			//int height = getTileHeight_beforeBridgeCode(client, tile.getLocalLocation(), plane) * -1;
			int height = event.getGameObject().getZ() * -1;


			long tag = getTag_Unique(event.getGameObject());
			int cycleStart = 0;
			int frame = 0;
			if (event.getGameObject().getRenderable() instanceof DynamicObject) {
				DynamicObject dynamicObject = (DynamicObject) event.getGameObject().getRenderable();
				//cycleStart = dynamicObject.getAnimCycleCount(); //api missing
				cycleStart = 0;
				//frame = dynamicObject.getAnimFrame(); //api missing
				frame = 0;
			}
			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			//int tileMinPlane = tile.getPhysicalLevel();  //api missing
			int tileMinPlane = tile.getPlane();
			actorSpawnPacket.writeByte(tileMinPlane);
			actorSpawnPacket.writeShort(height);
			actorSpawnPacket.writeLong(tag);
			actorSpawnPacket.writeShort(cycleStart);
			actorSpawnPacket.writeShort(frame);
			LocalPoint min_localPoint = new LocalPoint((event.getGameObject().getSceneMinLocation().getX()*128)+64,(event.getGameObject().getSceneMinLocation().getY()*128)+64);
			int offsetX = event.getGameObject().getX()-min_localPoint.getX(); //explanation: centreX-MinX = offsetX;
			int offsetY = event.getGameObject().getY()-min_localPoint.getY(); //explanation: centreY-MinY = offsetY;
			actorSpawnPacket.writeShort(offsetX);
			actorSpawnPacket.writeShort(offsetY);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");


	/*		Tile tile = gameObjectSpawned.getTile();
			WorldPoint tilePos = tile.getWorldLocation();
			int tilePosZ = ((Perspective.getTileHeight(client, tile.getLocalLocation(), tile.getPlane())) * -1);

			int tileLocalHeight = ((Perspective.getTileHeight(client, tile.getLocalLocation(), tile.getPlane())) * -1);

			int dist = playerWorldPos.distanceTo(tilePos);

	//		if (dist < 15) {
				GameObject gameObject = gameObjectSpawned.getGameObject();
					Model gameObjectModel = gameObject.getModel();
					if (gameObjectModel!=null) {
						if (gameObjectModel.getTrianglesCount() > 0) {
							if (gameObject.getSceneLocation().getY() == tilePos.getY() && gameObject.getSceneLocation().getX() == tilePos.getX()) { //this location check to to prevent getting the same object twice when it is scaled up over multiple squares
							}
							int orientation = (gameObject.getOrientation().getAngle() / 512) * 90;
							int scale = gameObjectModel.getXYZMag();
							int libMeshID = gameObject.getId();
							int libMeshSubID = orientation;
							LocalPoint location = gameObject.getLocalLocation();
							int  locationX = location.getX();
							int  locationY = location.getY();
							String IDAndSubID = libMeshID+"_"+libMeshSubID;
							instanceID = ("" + libMeshID + libMeshSubID + locationX + locationY + tilePosZ);


							if (libMeshIDs.contains(libMeshID) == false) {
								//myRunnable.sendMessage("2_" + ID + "_" + orientation + "_" + GetModelVertData(gameObjectModel));
								sendLibMeshBytes(gameObjectModel, libMeshID, libMeshSubID, orientation);
								libMeshIDs.add(IDAndSubID);
							}

							if (instanceIDs.contains(instanceID) == false) {
								//myRunnable.sendMessage("3_" + ID + "_" + tileCoords + "_" + orientation);
								sendGameObjectBytes(libMeshID,libMeshSubID,locationX,locationY,tilePosZ,orientation,scale,0,0,0);
								instanceIDs.add(instanceID);
							}
						}
					}

			}*/
		});
	}

	@Subscribe
	private void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			Tile tile;
/*			if (event.getTile().getBridge() != null) {
				tile = event.getTile().getBridge();
			} else {*/
			tile = event.getTile();
			//}

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();
/*			if (event.getTile().getBridge() != null) {
				tilePlane++;
			}*/

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGroundObject());


			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
		});
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
			Tile tile;
/*			if (event.getTile().getBridge() != null) {
				tile = event.getTile().getBridge();
			} else {*/
				tile = event.getTile();
			//}

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();
/*			if (event.getTile().getBridge() != null) {
				tilePlane++;
			}*/

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGameObject());


			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeByte(tileX);
			actorSpawnPacket.writeByte(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
		});
	}

/*	void makeTag(int sceneX *//*0-104*//*, int sceneY *//*0-104*//*, int plane *//*0-3*//*, int id *//*uint16*//*, int config*//*8 bits*//*) {
		tileObject.id
	}*/

	long getTag_Unique(TileObject tileObject) {
		long tag = tileObject.getHash();
		int tileOIbjType = 0;
		if(tileObject instanceof GroundObject) {
			tileOIbjType = 0;
		}
		if(tileObject instanceof GameObject) {
			tileOIbjType = 1;
		}
		if(tileObject instanceof DecorativeObject) {
			tileOIbjType = 2;
		}
		if(tileObject instanceof WallObject) {
			tileOIbjType = 3;
		}

		tag = (tag << 2) | (tileOIbjType & 3);// append tileObjType to make tag unique.
		return tag;
	}

	@Subscribe
	private void onGroundObjectSpawned(GroundObjectSpawned event) { //GroundObject is aka FloorDecoration
		if(!config.spawnGameObjects()) {return;}
		clientThread.invokeAtTickEnd(() ->
		{
			int isBridgeTile = 0;
			Tile tile;
			tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);
			int tileObjectModelType = getObjModelTypeFromFlags(event.getGroundObject().getConfig());
			int var4 = (event.getGroundObject().getConfig() - tileObjectModelType) >> 6 & 3;
			int objectOrientationA = var4 * 512;
			int objectOrientationB = -1;
			int objectDefinitionId = event.getGroundObject().getId();
			int plane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			//int height = getTileHeight_beforeBridgeCode(client, tile.getLocalLocation(), plane) * -1;
			//System.out.println(height);
			int height = event.getGroundObject().getZ()*-1;
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

			//int tileMinPlane = tile.getPhysicalLevel(); //api missing
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
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
		});

/*		counter1 = counter1+1;
		SendLowLatencyData();
		int objDefId = groundObjectSpawned.getGroundObject().getId();
		loadedObjDefinitions[objDefId] = client.getObjectDefinition(objDefId);

		GroundObject gameObject = groundObjectSpawned.getGroundObject();
		Tile tile = groundObjectSpawned.getTile();
		WorldPoint tileLocation = tile.getWorldLocation();
		if (gameObject != null) {

			short[] recolorFrom = client.getObjectDefinition(objDefId).recolorFrom();
			short[] recolorTo= client.getObjectDefinition(objDefId).recolorTo();


			if (recolorFrom != null) {
				if (recolorFrom.length > 0 && objDefId == 1394 ) {
					System.out.println("recolorFrom : [0] "+recolorFrom[0]);

					System.out.println("recolorTo : [0] "+recolorTo[0]);
				}
			}

			int objDefID = gameObject.getId();
			LocalPoint location = gameObject.getLocalLocation();
			int tilePosZ = ((Perspective.getHeight(client, location.getX(), location.getY(), tile.getPlane())) * -1);

			Long GameObjectHash = Long.parseLong("" + gameObject.getId() + tile.getPlane()+ tile.getSceneLocation().getX() + tile.getSceneLocation().getY());

			spawnedObjectHashes.add(GameObjectHash);

			ObjectDefinition objDef = loadedObjDefinitions[objDefID];
			int objDefModelIds[] = objDef.getModelIds();

			if (objDefModelIds!= null) {
				UnrealGameObject uGameObject = client.getUGameObjectsMap().get(GameObjectHash);
				if (uGameObject != null) {
					if (objDefId == 3635) {
						System.out.println("de scale: "+uGameObject.getScaleX() + " "+  uGameObject.getScaleY() + " "+ uGameObject.getScaleHeight());
					}

					System.out.println("uGameObjectNotNull");
					uGameObject.setWorldPoint(tileLocation);
					//uGameObject.setOrientation(orientation);
					uGameObject.setLocalPosX(location.getX());
					uGameObject.setLocalPosY(location.getY());
					uGameObject.setLocalPosZ(tilePosZ);
					client.getUGameObjectsMap().put(GameObjectHash, uGameObject);

				} else {
					System.out.println( "null UgameObjectWith Defid: " + objDefID);
				}
			}
		}*/
	}



	public static final byte[] intToByteArray(int value) {
		return new byte[] {(byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value};
	}

	public static byte[] intListToByteArray(List<Integer> intList) {
		byte[] bytes = new	byte[intList.size()*4]; //since byte encoding is 24 bit, there are 4 bytes for each value //+1 is to acount for trailing byte
		int byteCounter = 0;

		for (int i = 0; i < intList.size(); i++) { //-1 is to account for trailing byte
			int value = intList.get(i);
			bytes[byteCounter+0] = ((byte)(value >>> 24));
			bytes[byteCounter+1] = ((byte)(value >>> 16));
			bytes[byteCounter+2] = ((byte)(value >>> 8));
			bytes[byteCounter+3] = ((byte)value);
			byteCounter = byteCounter + 4;
		}
		return bytes;
	}

	public static byte[] trimmedBufferBytes(Buffer buffer) {
		return Arrays.copyOfRange(buffer.array, 0, buffer.offset);
	}

	public HashMap<Actor, Integer>  moveMentSequenceFrames  = new HashMap<Actor, Integer>();

	public int getLastFrameMovementSequence(Actor actor) {
		if (moveMentSequenceFrames.containsKey(actor)) {
			return moveMentSequenceFrames.get(actor);
		} else return 999;
	}
	public void setLastFrameMovementSequence(Actor actor, int sequenceId) {
		moveMentSequenceFrames.put(actor, sequenceId);
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event) {
		//int actorId = event.getActor().getRSInteracting();
/*		clientThread.invokeLater(() ->
		{
			clientThread.invokeLater(() ->
			{
				Buffer actorAnimChangePacket = new Buffer(new byte[20]);

				int newAnimationID = -1;
				int actorID = -1;
				int actorType = -1;

				int animation = event.getActor().getAnimation();

				if ((event.getActor() instanceof NPC))
				{
					final NPC npc = (NPC) event.getActor();

					//set spot, pose or normal sequence depending on which is not -1;
					if (npc.getAnimation()!= -1) {
						newAnimationID = npc.getAnimation();
					}else {
						if (npc.getPoseAnimation() != -1) {
							if(npc.getPoseAnimation() !=  getLastFrameMovementSequence(npc)) {
								newAnimationID = npc.getPoseAnimation();
								setLastFrameMovementSequence(npc, npc.getPoseAnimation());
							} else {
								return;
							}
						}else
						if (npc.getGraphic() != -1) {
							newAnimationID = npc.getGraphic();
						} else {
							return;
						}
					}
					setLastFrameMovementSequence(npc, newAnimationID);

					//System.out.println("animationchange to: " + newAnimationID);

					actorID = npc.getIndex();
					actorType = 1;

					actorAnimChangePacket.writeShort(newAnimationID);//write sequenceDef id;
					actorAnimChangePacket.writeShort(actorID);//write actor id;
					actorAnimChangePacket.writeByte(actorType); //write actor type. 1 = npc;
					//Util.sleep(1);
					myRunnableSender.sendBytes(trimmedBufferBytes(actorAnimChangePacket), "ActorAnimationChange");
				} else {
					if ((event.getActor() instanceof Player))
					{
						final Player player = (Player) event.getActor();

						//set spot, pose or normal sequence depending on which is not -1;
						if (player.getAnimation()!= -1) {
							newAnimationID = player.getAnimation();
						}else {
							if (player.getPoseAnimation() != -1) {
								if(player.getPoseAnimation() !=  getLastFrameMovementSequence(player)) {
									newAnimationID = player.getPoseAnimation();
									setLastFrameMovementSequence(player, player.getPoseAnimation());
								} else {
									return;
								}
							}else
							if (player.getGraphic() != -1) {
								newAnimationID = player.getGraphic();
							} else {
								return;
							}
						}
						setLastFrameMovementSequence(player, newAnimationID);

						//System.out.println("animationchange to: " + newAnimationID);

						actorID = player.getPlayerId();
						actorType = 2;

						actorAnimChangePacket.writeShort(newAnimationID);//write sequenceDef id;
						actorAnimChangePacket.writeShort(actorID);//write actor id;
						actorAnimChangePacket.writeByte(actorType); //write actor type. 1 = npc;

						myRunnableSender.sendBytes(trimmedBufferBytes(actorAnimChangePacket), "ActorAnimationChange");
					}
				}
			});
		});*/
	}

	@Subscribe
	private void onItemSpawned(ItemSpawned event)
	{
		//event.getItem().getModel().getModelHeight()
		if(!config.spawnItems()) {return;}
		clientThread.invokeAtTickEnd(() ->
		{
			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = event.getTile().getPlane();
			if (event.getTile().getBridge()!=null) {
				tilePlane++;
			}

			int tileX = event.getTile().getSceneLocation().getX();
			int tileY = event.getTile().getSceneLocation().getY();
			//short height = (short)(event.getItem().getModelHeight());
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
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
		});
	}
	@Subscribe
	private void onItemDespawned(ItemDespawned event)
	{
		clientThread.invokeAtTickEnd(() ->
		{
			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = event.getTile().getPlane();
			if (event.getTile().getBridge()!=null) {
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
		//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event) {
		if(event.getNpc() == null) { return; }
		boolean shouldDraw = hooks.draw(event.getNpc(), false);
		if(!shouldDraw) { return; }

/*		if(event.getNpc().getComposition()!=null && (event.getNpc().getId() == 12668 || event.getNpc().getId() == 12669)) {
			System.out.println("npc id "+event.getNpc().getId() + " has configs: ");
			for (int i = 0; i < event.getNpc().getComposition().getConfigs().length; i++) {
				System.out.println(event.getNpc().getComposition().getConfigs()[i]);
			}
		}*/
		if(event.getNpc().getName().contains("Dark wizard")) {
			//System.out.println("nightmare id "+event.getNpc().getId()+" has spawned");
			System.out.println("dark wizard id "+event.getNpc().getId()+" has spawned");
			NPCComposition npcDef = client.getNpcDefinition(event.getNpc().getId());
			for (int model : npcDef.getModels()) {
				System.out.println(model);
			}
		}

		if (!config.spawnNPCs()) { return; }
		//clientThread.invokeAtTickEnd(() -> {
			//clientThread.invokeLater(() -> {
				//clientThread.invokeLater(() -> {
					//clientThread.invokeLater(() -> {
						//clientThread.invokeLater(() -> {
							//clientThread.invokeLater(() -> {
								Buffer actorSpawnPacket = new Buffer(new byte[100]);

								int instanceId = event.getNpc().getIndex();
								int definitionId = event.getNpc().getId();
								actorSpawnPacket.writeByte(1); //write npc data type
								actorSpawnPacket.writeShort(instanceId);
								actorSpawnPacket.writeShort(definitionId);
								//System.out.println("NPC Spawn kashjkasd");
								sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
								//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
							//});
						//});
					//});
				//});
			//});
		//});
	}

	@Subscribe
	private void onNpcChanged(NpcChanged event) {
		if(event.getNpc() == null) { return; }
/*		System.out.println("NPC Chnaged Event");

		if(event.getNpc()!=null && event.getNpc().getComposition()!=null && event.getOld()!=null) {
			System.out.println("npcChanged. old comp was: "+event.getOld()+" new comp is: "+event.getNpc().getComposition());
		}*/

		if (!config.spawnNPCs()) { return; }

		if(event.getNpc().getName().contains("Nightmare")) {
			System.out.println("nightmare id "+event.getNpc().getId()+" has changed");
		}

		NpcDespawned despawnEvent = new NpcDespawned(event.getNpc());
		onNpcDespawned(despawnEvent);

		NpcSpawned spawnEvent = new NpcSpawned(event.getNpc());
		onNpcSpawned(spawnEvent);

		//clientThread.invokeLater(() -> {
			//clientThread.invokeLater(() -> {
				//clientThread.invokeLater(() -> {
					//clientThread.invokeLater(() -> {
						//clientThread.invokeLater(() -> {
							//clientThread.invokeLater(() -> {
/*								Buffer actorSpawnPacket = new Buffer(new byte[100]);

								int instanceId = event.getNpc().getIndex();
								int definitionId = event.getNpc().getId();
								actorSpawnPacket.writeByte(1); //write npc data type
								actorSpawnPacket.writeShort(instanceId);
								actorSpawnPacket.writeShort(definitionId);
								System.out.println("NPC Change kashjkasd");
								sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");*/
								//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
							//});
						//});
					//});
				//});
			//});
		//});
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event) {
		//clientThread.invokeAtTickEnd(() -> {
			if(event.getNpc() == null) { return; }
			if(event.getNpc().getName().contains("Nightmare")) {
				System.out.println("nightmare id "+event.getNpc().getId()+" has despawned");
			}
			System.out.println();
			Buffer actorSpawnPacket = new Buffer(new byte[100]);
			int instanceId = event.getNpc().getIndex();
			actorSpawnPacket.writeByte(1); //write npc data type
			actorSpawnPacket.writeShort(instanceId);

			//write bogus packet so len is more than 1

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
		//});
	}

	@Subscribe
	private void onPlayerChanged(PlayerChanged event) {
		if (!config.spawnPlayers()) { return; }
		Player player = event.getPlayer();
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		actorSpawnPacket.writeByte(2); //write player data type

		int InstanceId = event.getPlayer().getId();
		actorSpawnPacket.writeShort(InstanceId);

		byte isLocalPlayer = (client.getLocalPlayer().getId() == player.getId()) ? (byte)1 : (byte)0;
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
		if (player.getPlayerComposition().getGender() == 1) isFemale = 1;
		actorSpawnPacket.writeByte(isFemale); //isFemale

		//temp actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID
		actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID //temp
		//player.getPlayerComposition().setTransformedNpcId();

		//System.out.println("player change id: "+InstanceId);
		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
	}

/*	@Subscribe
	private void onPlayerCompositionChanged(PlayerCompositionChanged event) {
		if (!config.spawnPlayers()) { return; }
		Player player = event.getPlayer();
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		actorSpawnPacket.writeByte(2); //write player data type

		int InstanceId = event.getPlayer().getPlayerId();
		actorSpawnPacket.writeShort(InstanceId);

		byte isLocalPlayer = (client.getLocalPlayer().getId() == player.getId()) ? (byte)1 : (byte)0;
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
		if (player.getPlayerComposition().isFemale()) isFemale = 1;
		actorSpawnPacket.writeByte(isFemale); //isFemale

		//temp actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID
		actorSpawnPacket.writeInt(-1); //npcTransformID //temp

		System.out.println("player compositionChange. id: "+InstanceId);
		myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
	}*/


	private Field getField(Object instance, String method) {
		Class<?> clazz = instance.getClass();

		while (clazz != null && clazz.getSuperclass() != clazz) {
			try {
				Field field = clazz.getDeclaredField(method);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ex) {}

			clazz = clazz.getSuperclass();
		}
		throw new RuntimeException("Method '" + method + "' not found in class '" + instance.getClass() + "'");
	}

	@Subscribe
	private void onPlayerSpawned(PlayerSpawned event) {
		if (!config.spawnPlayers()) { return; }

		Player player = event.getPlayer();


		boolean shouldDraw = hooks.draw(player, false);
		if(!shouldDraw && player!=client.getLocalPlayer()) { return; } //we must allow spawning local player even when is hidden, cos camera is attached to that.

		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		actorSpawnPacket.writeByte(2); //write player data type

		int InstanceId = event.getPlayer().getId();
		actorSpawnPacket.writeShort(InstanceId);

		byte isLocalPlayer = (client.getLocalPlayer().getId() == player.getId()) ? (byte)1 : (byte)0;
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
		if (player.getPlayerComposition().getGender() == 1) isFemale = 1;
		actorSpawnPacket.writeByte(isFemale); //isFemale

		//temp actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID
		actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID //temp

		System.out.println("player spawn. id: "+InstanceId);
		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
	}

	@Subscribe
	private void onPlayerDespawned(PlayerDespawned event) {
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		int instanceId = event.getPlayer().getId();
		actorSpawnPacket.writeByte(2); //write player data type
		actorSpawnPacket.writeShort(instanceId);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
	}

	@Subscribe
	private void onCanvasSizeChanged(CanvasSizeChanged event) {
/*		if(sharedmem_rm!=null) {
			sharedmem_rm.initScreenCaptureObjects();
		}*/
	}

	private void sendPlaneChanged() {
		System.out.println("PlaneChanged");
		Buffer buffer = new Buffer(new byte[4]);
		buffer.writeByte(clientPlane);
		sharedmem_rm.backBuffer.writePacket(buffer, "PlaneChanged");
		//myRunnableSender.sendBytes(buffer.array, "PlaneChanged");
	}


	@Subscribe
	private void onClientTick(ClientTick event)
	{
		//allows us to send up to date perframepacket, containing important baselocation update, before object spawn packets are written.
/*		if (newRegionLoaded) {
			//communicateWithUnreal();
			//sendBaseCoordinatePacket();
			System.out.println("newRegionLoaded = true, baseCoordX: "+client.getBaseX());
			newRegionLoaded = false;
		}*/
	}

	public byte[] SpotAnimationModel_get(int Id) {
		IndexDataBase SpotAnimModelArchive = client.getIndex(7);
		byte[] bytes = SpotAnimModelArchive.loadData(Id, 0); //loadData(ArchiveId, FileId). For modeldata, file id is always 0.
		return bytes;
	}

	volatile int rsUiPosOffsetX = 0;
	volatile int rsUiPosOffsetY = 0;
	volatile int rsUiPosX = 0;
	volatile int rsUiPosY = 0;

	private void UpdateUiPosOffsets() { //sets the pos offsets, using swing thread, to avoid crash.
		float dpiScalingFactor = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0f; // 96 DPI is the standard

		rsUiPosOffsetX =  Math.round(dpiScalingFactor*(client.getCanvas().getParent().getLocationOnScreen().x-client.getCanvas().getLocationOnScreen().x)*-1);
		rsUiPosOffsetY =  Math.round(dpiScalingFactor*(client.getCanvas().getParent().getLocationOnScreen().y-client.getCanvas().getLocationOnScreen().y)*-1);
		rsUiPosX = client.getCanvas().getLocationOnScreen().x;
		rsUiPosY = client.getCanvas().getLocationOnScreen().y;
		//rsUiPosOffsetX*=dpiScalingFactor;
		//rsUiPosOffsetY*=dpiScalingFactor;
	}


	private void UpdateSharedMemoryUiPixels() {
		log_Timed_Verbose("_UpdateSharedMemoryUiPixels_0");
		if(client.getDrawCallbacks() == null) {return;}
		if(sharedmem_rm == null) {return;}
		if(sharedMemPixelsUpdatedTick == client.getGameCycle()) {return;}
		if(!client.getCanvas().isShowing()) {return;}
		sharedMemPixelsUpdatedTick = client.getGameCycle();
		final BufferProvider bufferProvider = client.getBufferProvider();

		int bufferWidth = bufferProvider.getWidth();
		int bufferHeight = bufferProvider.getHeight();

		sharedmem_rm.setInt(30000000, bufferWidth);
		sharedmem_rm.setInt(30000005, bufferHeight);

		float dpiScalingFactor = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0f; // 96 DPI is the standard

		float ratioX = 1;
		float ratioY = 1;

		if (client.isStretchedEnabled()) {
			ratioX = (float)client.getStretchedDimensions().width/(float)bufferProvider.getWidth();
			ratioY = (float)client.getStretchedDimensions().height/(float)bufferProvider.getHeight();
		}

		SwingUtilities.invokeLater(this::UpdateUiPosOffsets); //this code is run on swing thread to avoid crash.
		sharedmem_rm.setInt(30000010, rsUiPosOffsetX);
		sharedmem_rm.setInt(30000015, rsUiPosOffsetY);

		//3d viewport size
		float View3dSizeX = client.getViewportWidth();
		boolean onLoginScreen = client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN || client.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR;
		View3dSizeX*=dpiScalingFactor;
		View3dSizeX*=ratioX;
		if(onLoginScreen) { //when we first arrive on login screen, viewport is 0 width because it is uninitialized. In such cases, I am using the canvas dimensions instead.
			View3dSizeX = client.getCanvas().getParent().getWidth();
		}

		float View3dSizeY = client.getViewportHeight();
		View3dSizeY*=dpiScalingFactor;
		View3dSizeY*=ratioY;
		if(onLoginScreen) { //when we first arrive on login screen, viewport is 0 width because it is uninitialized. In such cases, I am using the canvas dimensions instead.
			View3dSizeY = client.getCanvas().getParent().getHeight();
		}

		sharedmem_rm.setInt(30000020,  Math.round(View3dSizeX));
		sharedmem_rm.setInt(30000025, Math.round(View3dSizeY));

		//top left position of 3d viewport in rl window.
		float View3dOffsetX = client.getViewportXOffset();
		View3dOffsetX*=dpiScalingFactor;
		View3dOffsetX*=ratioX;
		View3dOffsetX+=rsUiPosOffsetX;
		if(onLoginScreen) {
			View3dOffsetX = 0;
		}

		float View3dOffsetY = client.getViewportYOffset();
		View3dOffsetY*=dpiScalingFactor;
		View3dOffsetY*=ratioY;
		View3dOffsetY+=rsUiPosOffsetY;
		if(onLoginScreen) {
			View3dOffsetY = 0;
		}

		sharedmem_rm.setInt(30000030, Math.round(View3dOffsetX));
		sharedmem_rm.setInt(30000035, Math.round(View3dOffsetY));

		//Size of ui. Can differ from buffer size because of stretchmode.
		float canvasSizeX = client.getCanvasWidth();
		canvasSizeX*=dpiScalingFactor;
		canvasSizeX*=ratioX;
		float canvasSizeY = client.getCanvasHeight();
		canvasSizeY*=dpiScalingFactor;
		canvasSizeY*=ratioY;
		sharedmem_rm.setInt(30000040, Math.round(canvasSizeX));
		sharedmem_rm.setInt(30000045, Math.round(canvasSizeY));

		sharedmem_rm.SharedMemoryData.write(30000050, bufferProvider.getPixels(),0, bufferProvider.getHeight()*bufferProvider.getWidth());
		log_Timed_Verbose("_UpdateSharedMemoryUiPixels_1");
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

	private static WorldPoint getSourceTileCoord(int[][][] instanceTemplateChunks, int sceneX, int sceneY, int plane)
	{
/*		// get position in the scene
		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();*/

		// get chunk from scene
		int chunkX = sceneX / CHUNK_SIZE;
		int chunkY = sceneY / CHUNK_SIZE;

		// get the template chunk for the chunk
		int templateChunk = instanceTemplateChunks[plane][chunkX][chunkY];

		int rotation = templateChunk >> 1 & 0x3;
		int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
		int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
		int templateChunkPlane = templateChunk >> 24 & 0x3;

		// calculate world point of the template
		int x = templateChunkX + (sceneX & (CHUNK_SIZE - 1));
		int y = templateChunkY + (sceneY & (CHUNK_SIZE - 1));

		// create and rotate point back to 0, to match with template
		return rotate(new WorldPoint(x, y, templateChunkPlane), 4 - rotation);
	}

	private void sendInstancedAreaState(Scene scene) {
		//instanceChunkTemplates = new int[4][13][13]; //scene.getInstanceTemplateChunks() always returns array of this size.

		int ByteArray1DSize = (4*13*13)*4;
		Buffer packet = new Buffer(new byte[ByteArray1DSize+10]);

		boolean isInstancedArea = scene.isInstance();
		packet.writeBoolean(isInstancedArea);

		if(isInstancedArea) {
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

		//send the packet
		sharedmem_rm.backBuffer.writePacket(packet, "InstancedAreaState");


		//in ue4, read template chunks
		//

		/*			//parameters relating to the source chunks
					int rotation = chunkData >> 1 & 0x3;
					int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
					int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
					int plane = chunkData >> 24 & 0x3;*/
	}

	private void sendBaseCoordinatePacket(Scene scene) { //send Base Coordinate if needed
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

	private void sendBaseCoordinatePacket() { //send Base Coordinate if needed
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

	private void WritePerFramePacket() {
		//client.isInInstancedRegion();
		//client.getScene().getInstanceTemplateChunks();

		if(client.getGameState() == GameState.LOGIN_SCREEN || curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR) {
			if (client.getGameCycle()%20 == 0) {
				//constantly resize canvas in order to force it to repaint, so as to keep transparency
				SwingUtilities.invokeLater(() -> {client.resizeCanvas();});
			}
		}

		if(client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN || curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR) { //dont send perframe packet while on login screen because doing so would interfere with animated login screen
			return;
		}

		perFramePacketSentTick = client.getGameCycle();
		//System.out.println("canvasSizeChanged"+viewWidth+" X "+viewHeight);
		if (client.getTopLevelWorldView().getPlane() != clientPlane) {
			clientPlane = client.getTopLevelWorldView().getPlane();
			sendPlaneChanged();
		}

/*		if (client.getLocalPlayer() == null) {
			//System.out.println("not logged in yet");
			return;
		}*/

		Set<Integer> hashedEntitys_ThisFrame = new HashSet<Integer>();

		byte[] bytes = new byte[80000];
		Buffer perFramePacket = new Buffer(bytes);

		int camX = client.getCameraX();
		int camY = client.getCameraY();
		int camZ = client.getCameraZ();
		int camYaw = client.getCameraYaw();
		int camPitch = client.getCameraPitch();
		int camZoom = client.getScale();
		//int maxVisiblePlane = 3; //used to be client.getSceneMaxPlane(), but thats gone now.
		boolean removeRoofs = client.getVarbitValue(12378) > 0;
		int clientCycle = client.getGameCycle();
		short canvasWidth = (short)client.getViewportWidth();
		short canvasHeight = (short)client.getViewportHeight();
		//int playerLocalX = client.getLocalPlayer().getLocalLocation().getX();
		//int playerLocalY = client.getLocalPlayer().getLocalLocation().getY();
		//int playerZ = Perspective.getTileHeight(client, client.getLocalPlayer().getLocalLocation(), client.getPlane());


		//stringToSend = "0_"+client.getBaseX() + "," + client.getBaseY() + "," + "0" + "_" + playerLocalX + "," + playerLocalY + "," + playerZ + "_" + "0" + "," + camPitch + "," + camYaw + "_" + camX + "," + camY + "," + camZ+"_"+camZoom;


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
		for (NPC npc : npcs) {
			if(npc != null) {
				npcCount++;
			}
		}

		if(!config.spawnNPCs()) { npcCount = 0;}
		perFramePacket.writeShort(npcCount);
		if (npcCount > 0) {
			for (NPC npc : npcs) {
				if(npc == null) {continue;}
				int npcInstanceId = npc.getIndex();
				int npcX = npc.getLocalLocation().getX();
				int npcY = npc.getLocalLocation().getY();
				int offset = ((npc.getComposition().getSize()-1)*128)/2; //offset is required for npc's who are > 1 tile wide
				//LocalPoint LocationToSampleHeightFrom = new LocalPoint(npc.getLocalLocation().getX() + offset, npc.getLocalLocation().getY() + offset);
				//int npcHeight = Perspective.getTileHeight(client, LocationToSampleHeightFrom, client.getTopLevelWorldView().getPlane())*-1;
				int npcHeight = Perspective.getTileHeight(client, npc.getLocalLocation(), client.getTopLevelWorldView().getPlane())*-1;
				int npcOrientation = npc.getCurrentOrientation();

/*				int npcAnimationId = -1;
				int npcAnimationFrame = -1;
				int npcAnimationFrameCycle = -1;


				//set animation ints depending on which animation type is not null (spot, pose or normal)
				if (npc.getAnimation()!= -1) {
					npcAnimationId = npc.getAnimation();
					npcAnimationFrame = npc.getAnimationFrame();
					//npcAnimationFrameCycle = npc.getActionFrameCycle();//missing api
				}else {
					if (npc.getPoseAnimation() != -1) {
						npcAnimationId = npc.getPoseAnimation();
						npcAnimationFrame = npc.getPoseAnimationFrame();
						//npcAnimationFrameCycle = npc.getPoseFrameCycle();//missing api
					}*//*else
					if (npc.getGraphic() != -1) {
						npcAnimation = npc.getGraphic();
						npcAnimationFrame = npc.getSpotAnimFrame();
						//temp npcAnimationFrameCycle = npc.getSpotAnimationFrameCycle();
					}*//*
				}*/


				int animation = (config.spawnAnimations() ? npc.getAnimation() : -1);
				int poseAnimation = (config.spawnAnimations() ? npc.getPoseAnimation() : -1);

				int animFrame = npc.getAnimationFrame();
				int poseAnimFrame = npc.getPoseAnimationFrame();

				boolean shouldDraw = visibleActors.contains(npc) && hooks.draw(npc, false);

				if(!shouldDraw) {
					animFrame = -2; //-2 causes entity to be hidden
					poseAnimFrame = -2;
					//animation = -1;
					//poseAnimation = -1;
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
				if (!config.spawnNpcGFX()|| !shouldDraw) {
					perFramePacket.writeByte(numActorSpotAnims);
				} else {
					for(ActorSpotAnim spotAnim : npc.getSpotAnims()) {
						numActorSpotAnims++;
					}
					perFramePacket.writeByte(numActorSpotAnims);

					if(numActorSpotAnims > 0) {
						for(ActorSpotAnim spotAnim : npc.getSpotAnims()) {
							int spotAnimationFrame = spotAnim.getFrame();
							int spotAnimationId = spotAnim.getId();
							int spotAnimationHeight = spotAnim.getHeight();
							int sceneId = spotAnim.hashCode();

							perFramePacket.writeInt(spotAnimationId);
							perFramePacket.writeShort(spotAnimationFrame);
							perFramePacket.writeShort(spotAnimationHeight);
							perFramePacket.writeInt(sceneId);

							hashedEntitys_ThisFrame.add(spotAnim.hashCode()); //We do this so that despawn events can despawn the spotAnimActor
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
		for (Player player : players) {
			if(player != null) {
				playerCount++;
			}
		}
		if(!config.spawnPlayers()) { playerCount = 0;}
		perFramePacket.writeShort(playerCount);
		if (playerCount > 0) {
			for (Player player : players) {
				if(player == null) {continue;}
				//Player player = players.byIndex(i);
				int playerInstanceId = player.getId();
				int playerX = player.getLocalLocation().getX();
				int playerY = player.getLocalLocation().getY();
				int playerHeight = Perspective.getTileHeight(client, player.getLocalLocation(), client.getTopLevelWorldView().getPlane())*-1;
				int playerOrientation = player.getCurrentOrientation();
/*				if (playerInstanceId == client.getLocalPlayerIndex()) {
					System.out.println(playerOrientation);
				}*/
/*				int animationId = -1;
				int animationFrame = -1;
				int animationFrameCycle = -1;

				//set animation ints depending on which animation type is not null (spot, pose or normal)
				if (player.getAnimation()!= -1) {
					animationId = player.getAnimation();
					animationFrame = player.getAnimationFrame();
					//animationFrameCycle = player.getActionFrameCycle();//missing api
				}else {
					if (player.getPoseAnimation() != -1) {
						animationId = player.getPoseAnimation();
						animationFrame = player.getPoseAnimationFrame();
						//animationFrameCycle = player.getPoseFrameCycle();//missing api
					}*//*else
					if (player.getGraphic() != -1) {
						animation = player.getGraphic();
						animationFrame = player.getSpotAnimFrame();
						//temp animationFrameCycle = player.getSpotAnimationFrameCycle();
					}*//*
				}*/

				int animation = (config.spawnAnimations() ? player.getAnimation() : -1);
				int poseAnimation = (config.spawnAnimations() ? player.getPoseAnimation() : -1);

				int animFrame = player.getAnimationFrame();
				int poseAnimFrame = player.getPoseAnimationFrame();

				boolean shouldDraw = visibleActors.contains(player) && hooks.draw(player, false);

				if(!shouldDraw) {
					animFrame = -2;
					poseAnimFrame = -2;
					//animation = -1;
					//poseAnimation = -1;
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
				if (!config.spawnPlayerGFX() || !shouldDraw) {
					perFramePacket.writeByte(numActorSpotAnims);
				} else {
					for(ActorSpotAnim spotAnim : player.getSpotAnims()) {
						numActorSpotAnims++;
					}
					perFramePacket.writeByte(numActorSpotAnims);

					if(numActorSpotAnims > 0) {
						for(ActorSpotAnim spotAnim : player.getSpotAnims()) {
							int spotAnimationFrame = spotAnim.getFrame();
							int spotAnimationId = spotAnim.getId();
							int spotAnimationHeight = spotAnim.getHeight();
							int sceneId = spotAnim.hashCode();

							perFramePacket.writeInt(spotAnimationId);
							perFramePacket.writeShort(spotAnimationFrame);
							perFramePacket.writeShort(spotAnimationHeight);
							perFramePacket.writeInt(sceneId);

							hashedEntitys_ThisFrame.add(spotAnim.hashCode()); //We do this so that despawn events can despawn the spotAnimActor
						}
					}
				}
			}
		}
		//local player;
		int LocalPlayerIndex = -1;
		if(client.getLocalPlayer()!=null) {LocalPlayerIndex = client.getLocalPlayer().getId();}
		perFramePacket.writeShort(LocalPlayerIndex);


		int noGraphicsObjects = 0;
		if (config.spawnStaticGFX()) {
			for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects()) {
				noGraphicsObjects++;
				if(!hashedEntitys_LastFrame.contains(graphicsObject.hashCode())) {
					System.out.println("graphicsObjSpawn. id: "+graphicsObject.getId());
				}
				hashedEntitys_ThisFrame.add(graphicsObject.hashCode());
			}
		}

		perFramePacket.writeShort(noGraphicsObjects);
		if (noGraphicsObjects>0) {
			for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects())
			{
				boolean shouldDraw = hooks.draw(graphicsObject, false);
				if (graphicsObject instanceof RuneLiteObject) {
					System.out.println("encountered runeliteObject");
				}

				if(clientCycle < graphicsObject.getStartCycle()) { //graphicsObj should not draw before it's startCycle.
					shouldDraw = false;
				}

				int sceneId = graphicsObject.hashCode();
				perFramePacket.writeInt(sceneId);
				short localX = (short)graphicsObject.getLocation().getX();
				perFramePacket.writeShort(localX);
				short localY = (short)graphicsObject.getLocation().getY();
				perFramePacket.writeShort(localY);
				short spotAnimId = (short)graphicsObject.getId();
				perFramePacket.writeShort(spotAnimId);
				short animimationFrameIdx = (short)graphicsObject.getAnimationFrame();

				if(!shouldDraw) {
					animimationFrameIdx = -2;
				}

				perFramePacket.writeShort(animimationFrameIdx);

				short Z = (short)((graphicsObject.getZ()*-1)); //not sure if getStartHeight is correct/helping things.

				perFramePacket.writeShort(Z);
			}
		}




		int noProjectiles = 0;
		if (config.spawnProjectiles()) {
			for (Projectile projectile : client.getTopLevelWorldView().getProjectiles())
			{
				noProjectiles++;
				hashedEntitys_ThisFrame.add(projectile.hashCode());
			}
		}

		if(config.spawnProjectiles() && noProjectiles > 0) {
			perFramePacket.writeShort(noProjectiles);

			for (Projectile projectile : client.getTopLevelWorldView().getProjectiles())
			{
				int sceneId = projectile.hashCode();
				perFramePacket.writeInt(sceneId);
				short localX = (short)projectile.getX();
				perFramePacket.writeShort(localX);
				short localY = (short)projectile.getY();
				perFramePacket.writeShort(localY);
				short Z = (short)((projectile.getZ()*-1)); //not sure if getStartHeight is correct/helping things.
				perFramePacket.writeShort(Z);

				short localX_target = (short)projectile.getTarget().getX();
				perFramePacket.writeShort(localX_target);
				short localY_target = (short)projectile.getTarget().getY();
				perFramePacket.writeShort(localY_target);
				short Z_target = (short)(projectile.getEndHeight());
				Z_target+=(Perspective.getTileHeight(client, new LocalPoint(localX_target, localY_target), client.getTopLevelWorldView().getPlane())*-1);
				perFramePacket.writeShort(Z_target);

/*				System.out.println("Height: "+projectile.getHeight());
				System.out.println("endHeight: "+projectile.getEndHeight()); //endHeight,relativeToTerrain?
				System.out.println("startHeight: "+projectile.getStartHeight());
				System.out.println("Z: "+projectile.getZ());
				System.out.println("ModelHeight: "+projectile.getModelHeight()); //(offset?)*/
				short spotAnimId = (short)projectile.getId();
				perFramePacket.writeShort(spotAnimId);
				short animationFrameIdx = (short)projectile.getAnimationFrame();

/*				if(!visibleActors.contains(projectile)) {
				System.out.println("projectile visible");
				}else {
					System.out.println("projectile invisible");
				}*/

				boolean shouldDraw = visibleActors.contains(projectile) && hooks.draw(projectile, false);

				if(!shouldDraw) {
					//System.out.println("shoudlDraw = false");
					animationFrameIdx = -2; //-1 anim mean hide entity
				} else {
					//System.out.println("shoudlDraw = true");
				}

				perFramePacket.writeShort(animationFrameIdx);
				short animimationFrameCycle = (short)-1;
				perFramePacket.writeShort(animimationFrameCycle);
				short remainingCycles = (short)projectile.getRemainingCycles();
				perFramePacket.writeShort(remainingCycles);
			}
		} else {
			perFramePacket.writeShort(0);
		}

		//Map<GameObject,Tile> GameObjAnimChanges = checkForGameObjectAnimationChanges();
		//int noGameObjAnimChanges = GameObjAnimChanges.size();

		if(config.spawnAnimations() && config.spawnGameObjects()) {
			Map<Long, DynamicObject> tileObjects = getAnimatedGameObjects();
			int NoTileObjects = tileObjects.size();
			perFramePacket.writeInt(NoTileObjects);

			for (Map.Entry<Long, DynamicObject> entry : tileObjects.entrySet()) {
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
		} else {
			int NoGameObjects = 0;
			perFramePacket.writeInt(NoGameObjects);
		}

		//getRlObjects();

		if(hashedEntitys_LastFrame !=null) {
			for (Integer lastFrameHashedEntity : hashedEntitys_LastFrame) {
				if(hashedEntitys_ThisFrame.contains(lastFrameHashedEntity) == false) { //if lats frames entity is not present this frame, means it has despawned.
					System.out.println("hashedEntityDespawned. Entity " + lastFrameHashedEntity);
					hashedEntityDespawned(lastFrameHashedEntity);
				}
			}
		}
		hashedEntitys_LastFrame = hashedEntitys_ThisFrame;

		sharedmem_rm.backBuffer.writePacket(perFramePacket, "PerFramePacket");
	}

	void hashedEntityDespawned(int SceneId) {
			Buffer actorSpawnPacket = new Buffer(new byte[20]);

			actorSpawnPacket.writeByte(6); //write hashedEntity data type
			actorSpawnPacket.writeInt(SceneId);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	int lastCavansX = 0;
	int lastCavansY = 0;
	int lastCavansSizeX = 0;
	int lastCavansSizeY = 0;
	boolean RmNeedsWindowUpdate() {
		if(client.getCanvas() == null) { return false; }
		if(!client.getCanvas().isShowing()) {
			System.out.println("Cant Update RmWindow because rl canvas isnt showing");
			return false;
		}
		int curCavansX = rsUiPosX+client.getViewportXOffset();
		int curCavansY = rsUiPosY+client.getViewportYOffset();

		int curCavansSizeX = client.getViewportWidth();
		int curCavansSizeY = client.getViewportHeight();
		if(curCavansX!=lastCavansX||curCavansY!=lastCavansY||curCavansSizeX!=lastCavansSizeX||curCavansSizeY!=lastCavansSizeY) {
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
		int r = (BGR >> 16) & 0xFF/255;
		int g = (BGR >> 8) & 0xFF/255;
		int b = BGR & 0xFF/255;
	}

	public ApplicationSettings loadAppSettings() {
		String jsonFileLocation = System.getProperty("user.home") + "\\.runemod\\AppSettings.json";

		Gson gson = new GsonBuilder().create();
		try (BufferedReader reader = new BufferedReader(new FileReader(jsonFileLocation))) {
			return gson.fromJson(reader, ApplicationSettings.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}


