package com.runemod;

import com.google.inject.Provides;
import com.runemod.cache.ConfigType;
import com.runemod.cache.IndexType;
import com.runemod.cache.definitions.ObjectDefinition;
import com.runemod.cache.definitions.loaders.ObjectLoader;
import com.runemod.cache.fs.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ModelData;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.rlawt.AWTContext;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static net.runelite.api.Constants.TILE_FLAG_BRIDGE;
import static net.runelite.client.RuneLite.RUNELITE_DIR;


@PluginDescriptor(
		name = "RuneMod",
		enabledByDefault = true,
		description = "Graphics modding tool",
		tags = {"rm", "rune", "mod", "hd", "unreal", "ue4", "graphics"}
)

@Slf4j
public class RuneModPlugin extends Plugin implements DrawCallbacks
{
	public static volatile boolean clientJustConnected;
	private List<Byte> meshDataByteList = new ArrayList<Byte>();
	private List<WallObjectSpawned> spawnedWallObjects = new ArrayList<WallObjectSpawned>();
	private List<Long> spawnedObjectHashes = new ArrayList<Long>();
	private List<AnimationChanged> queuedAnimationChangePackets = new ArrayList<AnimationChanged>();
	HashMap<GameObject, Integer> gameObjectAnimTracker =  new HashMap<GameObject, Integer>();

	private boolean keyEvent = false;
	//public SharedMemoryManager rsclient_ui_pixels = new SharedMemoryManager(this);
	public static SharedMemoryManager sharedmem_rm = null;
	public boolean canvasSizeChanged = false;
	public boolean newRegionLoaded = false;
	public boolean sendTerrain = false;

	private int clientPlane = -1;
	private Set<String> libMeshIDs = new HashSet<>();
	private Set<String> sentInstanceIds = new HashSet<>();
	private Set<Integer> sentSequenceDefIds = new HashSet<>();
	private Set<Integer> sentSkeletonIds = new HashSet<>();
	private Set<Integer> sentFrameIds = new HashSet<>();

	private Set<Integer> hashedEntitys_LastFrame = new HashSet<Integer>();

	public static CacheReader myCacheReader;

	//ExecutorService executorService = Executors.newFixedThreadPool(1);
	ExecutorService executorService2 = null;

	//alt runemodLocation C:\Users\soma.wheelhouse\Documents\Unreal Projects\RuneMod\LaunchGame_Standalone.lnk

	public static RuneMod_Launcher runeModLauncher;

	public static RuneMod_statusUI runeMod_statusUI;

	int clientCycle_unreal = 0;

	@Inject
	private ClientToolbar clientToolbar;

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

	@Inject
	private DrawManager drawManager;

	static RuneModPlugin runeModPlugin;

	@Inject
	private PluginManager pluginManager;


/*	@Inject
	private MouseManager mouseManager;

	net.runelite.client.input.MouseListener rlMouseListener = new net.runelite.client.input.MouseListener() {
		@Override
		public MouseEvent mouseClicked(MouseEvent mouseEvent) {
			return mouseEvent;
		}

		@Override
		public MouseEvent mousePressed(MouseEvent mouseEvent) {
			return mouseEvent;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent mouseEvent) {
			return mouseEvent;
		}

		@Override
		public MouseEvent mouseEntered(MouseEvent mouseEvent) {
			return mouseEvent;
		}

		@Override
		public MouseEvent mouseExited(MouseEvent mouseEvent) {
			return mouseEvent;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent mouseEvent) {
			return mouseEvent;
		}

		@Override
		public MouseEvent mouseMoved(MouseEvent mouseEvent) {
			return mouseEvent;
		}
	};*/

/*	@Subscribe
	public void onFocusChanged(FocusChanged focusChanged)
	{
		if (focusChanged.isFocused() == false)
		{
			maintainRuneModStatusAttachment(true, false);
			return;
		}
		if (focusChanged.isFocused() == true)
		{
			maintainRuneModStatusAttachment(true, true);
			return;
		}
	}*/

	//public Boolean runeModToggled = true;


/*	public void toggleRuneModOverlayOn(){runeModToggled = false; toggleRuneModOverlay();}//set toggled val to opposit because we are abouit to toggle it
	public void toggleRuneModOverlayOff(){runeModToggled = true; toggleRuneModOverlay();} //set toggled val to opposit because we are abouit to toggle it
	public void toggleRuneModOverlay(){
*//*		runeModToggled = !runeModToggled;
		if (runeModToggled) {
			System.out.println("Toggled runemod ON");
			canvasSizeHasChanged(); //updates the ue4 canvas texture size
			Buffer buffer = new Buffer(new byte[20]);
			buffer.writeByte(101);
			buffer.writeShort(client.getCanvas().getLocationOnScreen().x);
			buffer.writeShort(client.getCanvas().getLocationOnScreen().y);
			buffer.writeShort(client.getCanvas().getWidth());
			buffer.writeShort(client.getCanvas().getHeight());
			myRunnableSender.sendBytes(trimmedBufferBytes(buffer),"WindowEvent");
		} else {
			System.out.println("Toggled runemod OFF");
			Buffer buffer = new Buffer(new byte[20]);
			buffer.writeByte(100);
			myRunnableSender.sendBytes(trimmedBufferBytes(buffer),"WindowEvent");
		}*//*
	}*/

	WindowAdapter WindowFocusListener;
	ComponentAdapter componentAdapter0;
	ComponentAdapter componentAdapter1;
	WindowListener windowListener;
	AWTEventListener mouseListener;

	static public boolean runningFromIntelliJ()
	{
		//boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("jdwp") >= 0;
		boolean isDebug = System.getProperty("launcher.version") == null;
		return isDebug;
	}

	public void unRegisterWindowEventListeners() {
		//JFrame window = (JFrame)SwingUtilities.getAncestorOfClass(ContainableFrame.class, client.getCanvas());
/*		JFrame window = (JFrame)SwingUtilities.windowForComponent(client.getCanvas());
		window.removeWindowFocusListener(WindowFocusListener);
		window.removeComponentListener(componentAdapter0);
		window.removeComponentListener(componentAdapter1);
		window.removeWindowListener(windowListener);
		Toolkit.getDefaultToolkit().removeAWTEventListener(mouseListener);*/
	}

	public void registerWindowEventListeners() {
/*		//setup window event listeners

		//JFrame window = (JFrame)SwingUtilities.getAncestorOfClass(ContainableFrame.class, client.getCanvas());
		JFrame window = (JFrame)SwingUtilities.windowForComponent(client.getCanvas());

		window.addWindowFocusListener(WindowFocusListener = new WindowAdapter() {
			public void windowGainedFocus(WindowEvent e) {
				maintainRuneModStatusAttachment(true, true);
			}
		});

		window.addComponentListener(componentAdapter0 = new ComponentAdapter() {
			public void componentMoved(ComponentEvent e) {
				maintainRuneModStatusAttachment(true, true);
			}
		});

		window.addComponentListener(componentAdapter1 = new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				maintainRuneModStatusAttachment(true, true);
			}
		});

		window.addWindowListener(windowListener = new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
					maintainRuneModStatusAttachment(true, true);
			}

			@Override
			public void windowClosing(WindowEvent e) {

			}

			@Override
			public void windowClosed(WindowEvent e) {

			}

			@Override
			public void windowIconified(WindowEvent e) {
					maintainRuneModStatusAttachment(false, true);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
					maintainRuneModStatusAttachment(true, true);
			}

			@Override
			public void windowActivated(WindowEvent e) {
				System.out.println("activated");
					maintainRuneModStatusAttachment(true, true);
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
					maintainRuneModStatusAttachment(true, false);
			}
		});*/


/*		Toolkit.getDefaultToolkit().addAWTEventListener(mouseListener = new AWTEventListener() { //keeps game window on top
			public void eventDispatched(AWTEvent event) {
				if(event instanceof KeyEvent){

				}
				if(event instanceof MouseEvent){
					MouseEvent evt = (MouseEvent)event;
					if(evt.getID() == MouseEvent.MOUSE_PRESSED){
						new Thread(() -> {
							maintainRuneModStatusAttachment(true, true);
						}).start();
					}
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK);*/

/*		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() { //keeps game window on top
			public void eventDispatched(AWTEvent event) {
				if (!config.keepOverlayOnTop()) {return;}
				if(event instanceof KeyEvent){
					KeyEvent evt = (KeyEvent)event;
					System.out.println("keyPressedEvent: ");
					System.out.println("char: " + evt.getKeyChar());
					System.out.println("KeyCode: " + evt.getKeyCode());
					System.out.println("keyLocation: " + evt.getKeyLocation());
					System.out.println("paramString: " + evt.paramString());
				}
				if(event instanceof MouseEvent){
					MouseEvent evt = (MouseEvent)event;
					if(evt.getID() == MouseEvent.MOUSE_PRESSED){
						System.out.println("mousePressed");
						Buffer buffer = new Buffer(new byte[20]);
						buffer.writeByte(9);
						myRunnableSender.sendBytes(trimmedBufferBytes(buffer),"WindowEvent");
					}
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK);

		client.getCanvas().addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent keyEvent)
			{
				System.out.println("keyPressedEvent: ");
				System.out.println("char: " + keyEvent.getKeyChar());
				System.out.println("KeyCode: " + keyEvent.getKeyCode());
				System.out.println("keyLocation: " + keyEvent.getKeyLocation());
				System.out.println("paramString: " + keyEvent.paramString());
				System.out.println("modifiers: " + keyEvent.getModifiersEx());
			}
		});

		client.getCanvas().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				System.out.println("id: "+mouseEvent.getID());
				System.out.println("modifiers: "+mouseEvent.getModifiersEx());
				System.out.println("clickCount: "+mouseEvent.getClickCount());
				System.out.println("button: "+mouseEvent.getButton());
				System.out.println("isPopupTrigger: "+mouseEvent.isPopupTrigger());
			}
		});*/
	}

	int perFramePacketSentTick = -1;
	int sharedMemPixelsUpdatedTick = -1;
	int ticksSincePLuginLoad = 0;

	void maintainRuneModStatusAttachment() {
		return;
/*			if(!client.getCanvas().isShowing()) {return; }
			Point loc = client.getCanvas().getParent().getLocationOnScreen();
			loc.x += 100;
			loc.y -= runeMod_statusUI.frame.getHeight();
			loc.x = (int)clamp(loc.x, 0, 8000);
			loc.y = (int)clamp(loc.y, 0, 8000);
			runeMod_statusUI.frame.setLocation(loc);*/
	}

	void clearRsPixelBuffer() {
		int[] newArray = new int[5000];
		System.arraycopy(client.getBufferProvider().getPixels(), 0, newArray, 0, 3);
	}


	int cacheLastUpdatedTick = Integer.MAX_VALUE; //when a cache file was last seen to be changed
	boolean rsCacheIsUpdating = true;
	HashMap<File, Long> lastSeenFileSizes =  new HashMap<File, Long>();

	boolean startedWhileLoggedIn;
	@SneakyThrows
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		alreadyCommunicatedUnreal = false;

		if(client.getGameState().ordinal() >= GameState.LOGIN_SCREEN.ordinal()) {
			ticksSincePLuginLoad++;
		}

		if(ticksSincePLuginLoad == 1) {
			startUp_Custom();
			if (client.getGameState().ordinal() > GameState.LOGIN_SCREEN_AUTHENTICATOR.ordinal()) {
				runeMod_statusUI.SetStatus_Detail("To enable RuneMod, you must first logout");
				startedWhileLoggedIn = true;
			} else {
				runeMod_statusUI.SetStatus_Detail("Starting...");
				startedWhileLoggedIn = false;
			}
		}

		if (startedWhileLoggedIn) { //if were logged in, we auto stop the plugin after the user has had a moment to read the message
			System.out.println("Not logged out, so not starting rm . Will stop plugin soon");
			if (ticksSincePLuginLoad == 50) {
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
			}
			return;
		}

		//check if rscache is currently being updated. if not, start runemod launcher
		if(rsCacheIsUpdating == true && ticksSincePLuginLoad > 1 && client.getGameCycle()%50 == 0) {
			String directory = RUNELITE_DIR + "\\jagexcache\\oldschool\\LIVE";
			File[] files = new File(directory).listFiles();
			for(File file : files){
				if(file!=null && file.isFile()) {
					long lastSeenFileSize = lastSeenFileSizes.getOrDefault(file, -1L);
					if (file.length() != lastSeenFileSize) {
						lastSeenFileSizes.put(file, file.length());
						cacheLastUpdatedTick = client.getGameCycle();
						rsCacheIsUpdating = true;
					}
				}
			}

			if(client.getGameCycle() - cacheLastUpdatedTick > 100) {
				System.out.println("RSCache has finished downloading");
				runeMod_statusUI.SetStatus_Detail("Downloaded RS cache");
				rsCacheIsUpdating = false;

				executorService2 = Executors.newFixedThreadPool(1);

				executorService2.execute(runeModLauncher);
			} else {
				runeMod_statusUI.SetStatus_Detail("Downloading RS cache...");
			}
		}

		JFrame window = (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas());
		if (!window.getTitle().equals("RuneLite")) {
			 window.setTitle("RuneLite");
		}

/*		if(unrealIsConnected && client.getGameState().ordinal()>=GameState.LOGGING_IN.ordinal() && lastGameState.ordinal()>=GameState.LOGIN_SCREEN.ordinal()) {
			setGpuFlags(3);
			setDrawCallbacks(this);
		} else {
			//if(client.getGameState().ordinal()<GameState.LOGGING_IN.ordinal()  || !unrealIsConnected) {
				setGpuFlags(0);
				setDrawCallbacks(null);
			//}
		}*/

		//clientThread.invokeAtTickEnd(() -> {
			if(ticksSincePLuginLoad <= 2 || client.getGameState().ordinal()<GameState.LOGGING_IN.ordinal() || config.RuneModVisibility() == false) {//allows us to display logging in... on login screen
					setGpuFlags(0);
					if(client.getDrawCallbacks() == null) {
						communicateWithUnreal();
						//clientThread.invokeAtTickEnd(this::communicateWithUnreal); //for times when scenedraw callback isnt available.
					}
			} else {
				clientThread.invokeAtTickEnd(() -> {
					if(isShutDown == false) {
						setGpuFlags(3);
					}
				});
			}




/*			if (curGamestate == GameState.LOGIN_SCREEN) {
				if(sharedmem_rm.runeModWindowsExist()) {
					sharedmem_rm.setRuneModVisibility(false);
				}
			} else {
				if (curGamestate == GameState.LOGGED_IN) {
					if(sharedmem_rm.runeModWindowsExist()) {
						sharedmem_rm.setRuneModVisibility(true);
					}
				}
			}*/

/*		clientThread.invoke(() -> {
			if (client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal()) {
				System.out.println("IsAutoStarted");
				return false;
			}

			// you are now at the login screen or higher
			System.out.println("IsManuallyStarted");
			return true;
		});

		if(client.getGameState() == GameState.LOGIN_SCREEN && client.getGameState()!= curGamestate || client.getGameCycle() > 1000) { //the first loginscreen gamestate is not recieved (for some reason), so we trigger it ourselves like this.

		}*/
		//order is: onBeforeRender, drawScene, PostDrawScene, Draw().
		//System.out.println("onBeforeRender(). Order: "+ orderDebug++);
/*		//first person test
		if(client.getGameCycle() > 1200 ) {
			Point mousePos = Perspective.localToCanvas(client, myRunnableReciever.mouseLocalPoint_FirstPerson,myRunnableReciever.mouseLocalPoint_Z_FirstPerson);
			if(mousePos!=null) {
				MouseEvent mouseMoveEvent = new MouseEvent(myRunnableReciever.clientCanvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, mousePos.getX(), mousePos.getY(), 0, false, 0);
				myRunnableReciever.eventQueue.postEvent(mouseMoveEvent);
			}
		}*/

/*		if (clientJustConnected) {
			//if (client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR) {
			//	toggleRuneModOverlayOff();;
			//} else {
			//}

			clientPlane = client.getPlane();
			sendPlaneChanged();

			clientJustConnected = false;
			//myRunnableSender.clientConnected = false;
		}*/

		//SendPerFramePacket();
	}

	long timeCommunicatedWithUnreal = 0;

	void MaintainRuneModAttachment() {
		maintainRuneModStatusAttachment();

		if(sharedmem_rm == null) {
			return;
		}

		if(!runeModPlugin.config.attachRmWindowToRL()) {return;}

		sharedmem_rm.ChildRuneModWinToRl();

		//if(sharedmem_rm.ChildRuneModWinToRl()) { //if RuneMod win exists and is childed to rl


		if (client.getGameState().ordinal()>=GameState.LOGGING_IN.ordinal() && config.RuneModVisibility() == true) {
			sharedmem_rm.setRuneModVisibility(true);
		} else {
			sharedmem_rm.setRuneModVisibility(false);
		}

		if(RmNeedsWindowUpdate()) {
			sharedmem_rm.updateRmWindowTransform();
		}
	}

	public static long clamp(long val, long min, long max) {
		return Math.max(min, Math.min(max, val));
	}

	int consecutiveTimeouts = 0;
	boolean alreadyCommunicatedUnreal = false; //whether we have communicated with unreal this frame.
	void communicateWithUnreal() {
		if(isShutDown) { return; }

		if(alreadyCommunicatedUnreal) { return; }

		alreadyCommunicatedUnreal = true;

		new Thread(this::MaintainRuneModAttachment).start();

		if (client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal()) { //prevents communicating with unreal before client is loaded.
			return;
		}

		if(!sharedmem_rm.backBuffer.isOverFlowed) {
			WritePerFramePacket(); //we start writing the perframe apcket before unreal has indidcated its started a new frame, for a small performance optimization
		}

		long waitForUnrealTimeout = 1000/(consecutiveTimeouts+1);
		boolean unrealStartedNewFrame = sharedmem_rm.AwaitUnrealMutex_Locked(clamp(waitForUnrealTimeout, 60, 500)); //unreal locks it's mutex when it has started frame

		long frameSyncStartTime = System.nanoTime();

		if(!unrealStartedNewFrame) { // if the wait for unreal's frame timedout
			//System.out.println("TimedOutWaitingFor unreal");
			consecutiveTimeouts++;
			if(consecutiveTimeouts > 10) {
				setUnrealConnectionStatus(false);
			}
			return;
		} else {
			consecutiveTimeouts = 0;
			setUnrealConnectionStatus(true);
		}
		//System.out.println("UnrealMutexLocked");

		//export runeliteframe
		//WritePerFramePacket(); //doing this above, as a small optimization

		sharedmem_rm.startNewRsData();
		sharedmem_rm.transferBackBufferToSharedMem();

		sharedmem_rm.writeTerminatingPacket();
		//System.out.println("ExportedRuneliteFrame");

		sharedmem_rm.LockRuneliteMutex(); //indicates to unreal that runelite has exported it's framedata

		//System.out.println("LockedRuneliteMutex");
		sharedmem_rm.AwaitUnrealMutex_UnLocked(); //unreal sends data back to us, and unlocks it's mutex when it has imported rl frame.
		//System.out.println("Unreal Has Unlocked it's mutex");

		//import unrealdata
		sharedmem_rm.handleUnrealData();


		sharedmem_rm.UnLockRuneliteMutex(); //indicates ruenlite is now busy rendering the current frame and calcing the next frame.

		//long frameSyncWaitTime = System.nanoTime()-frameSyncStartTime;
		//System.out.println("frameSyncWaitTime: "+(float)frameSyncWaitTime/1000000.0 + "ms");
		//System.out.println("commedWithUnreal");
	}

	int orderDebug = 0;

	enum ComputeMode
	{
		NONE,
		OPENGL,
		OPENCL
	}
	ComputeMode computeMode = ComputeMode.OPENGL;

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
				//if unreal connected while we were already logged in, trigger a reload rs scene.
				simulateGameEvents();

				//runeMod_statusUI.SetStatus_Detail("Connected");
				return;
			}
		}

		if(!currentValue) {
			if(client.getGameCycle()-sharedmem_rm.gameCycle_Unreal > 50) { //if unreal is very desynced from rstick
				if(unrealIsConnected == true) { //if unreal is disconnecting just now
					unrealIsConnected = false;

					System.out.println("Unreal Has just Disconnected");

					if(sharedmem_rm.runeModWindowsExist()) {
						runeMod_statusUI.SetStatus_Detail("RuneMod is not connected.");
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
		if(storedGpuFlags <= 0) {return;}
		if(!client.isGpu()){return;}
		communicateWithUnreal();
		//System.out.println("drawScene");






/*		boolean unrealDataArrived = sharedmem_rm.awaitUnrealData(); //waits until unrealdata exists, then locks mutex. it has a timeout of 20ms.
		if (unrealDataArrived) { //if unrealdata was found
			sharedmem_rm.UnlockMutex();
		}*/
		//Thread.sleep(5);
	}



	@Override
	public void draw(Renderable renderable, int orientation, int pitchSin, int pitchCos, int yawSin, int yawCos, int x, int y, int z, long hash) {
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
				client.checkClickbox(model, orientation, pitchSin, pitchCos, yawSin, yawCos, x, y, z, hash);
			}
		//}
	}

	@Override
	public void drawScenePaint(int orientation, int pitchSin, int pitchCos, int yawSin, int yawCos, int x, int y, int z, SceneTilePaint paint, int tileZ, int tileX, int tileY, int zoom, int centerX, int centerY) {
		//GpuPluginDrawCallbacks.drawScenePaint(orientation, pitchSin, pitchCos, yawSin, yawCos, x, y, z, paint, tileZ, tileX, tileY, zoom, centerX, centerY);
	}

	@Override
	public void drawSceneModel(int orientation, int pitchSin, int pitchCos, int yawSin, int yawCos, int x, int y, int z, SceneTileModel model, int tileZ, int tileX, int tileY, int zoom, int centerX, int centerY) {
		//GpuPluginDrawCallbacks.drawSceneModel(orientation, pitchSin, pitchCos, yawSin, yawCos, x, y, z, model, tileZ, tileX, tileY, zoom, centerX, centerY);
	}

	@Override
	public void draw(int overlayColor) {
		//communicateWithUnreal();
		UpdateSharedMemoryUiPixels();
		clientThread.invokeAtTickEnd(this::communicateWithUnreal);
/*		BufferedImage image = new BufferedImage(client.getBufferProvider().getWidth(), client.getBufferProvider().getHeight(), BufferedImage.TYPE_INT_RGB);

		System.arraycopy(client.getBufferProvider().getPixels(), 0, ((DataBufferInt) image.getRaster().getDataBuffer()).getData(), 0, client.getBufferProvider().getPixels().length-1);

		client.getCanvas().getGraphics().drawImage(image,0,0, (ImageObserver)null);
		client.getCanvas().getGraphics().dispose();*/
		//if(storedGpuFlags == 0) {return;}
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
		//client.setReplaceCanvasNextFrame(true);
		//GpuPluginDrawCallbacks.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff) {
		//GpuPluginDrawCallbacks.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene) {
		clientThread.invokeAtTickEnd(() -> {
			sendBaseCoordinatePacket(scene);
		});

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

	boolean isShutDown = false;

	@Override
	protected void shutDown() throws Exception
	{
		System.out.println("RuneMod plugin shutDown");
		isShutDown = true;
		clientThread.invoke(() -> {

			//unRegisterWindowEventListeners();

			runeModLauncher = null;
			myCacheReader = null;

			if(runeMod_statusUI!=null) {
				runeMod_statusUI.close();
				runeMod_statusUI = null;
			}

			if(executorService2!=null) {
				executorService2.shutdown();
				executorService2 = null;
			}

			ticksSincePLuginLoad = -1;

			if(sharedmem_rm!=null) {
				sharedmem_rm.destroyRuneModWin();
				sharedmem_rm.CloseSharedMemory();
				sharedmem_rm = null;
			}


			setGpuFlags(0);
			setDrawCallbacks(null);
			client.setUnlockedFps(false);

			toggleRuneModLoadingScreen(false);
			// force main buffer provider rebuild to turn off alpha channel
			client.resizeCanvas();

			lastCavansX = 0;
			lastCavansY = 0;
			lastCavansSizeX = 0;
			lastCavansSizeY = 0;

			storedGpuFlags = -1;

			rsCacheIsUpdating = true;
		});
	}

	void setDrawCallbacks(DrawCallbacks drawCallbacks) {
		client.setDrawCallbacks(drawCallbacks);

		if (drawCallbacks==null) {
			System.out.println("Changed DrawCallbacks To Null");
		} else {
			System.out.println("Changed DrawCallbacks");
		}
	}


	public static JPanel RuneModLoadingScreen = new JPanel();
	public static Container canvasAncestor;
	public static Client client_static;

	public static void toggleRuneModLoadingScreen(Boolean toggled) {
		SwingUtilities.invokeLater(() ->
		{
			if(toggled) {
				//JPanel window = (JPanel) SwingUtilities.getAncestorOfClass();
				if(canvasAncestor == null) {return;}
				RuneModLoadingScreen.removeAll();
				RuneModLoadingScreen.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 300));
				RuneModLoadingScreen.setBackground(Color.black);
				RuneModLoadingScreen.add(runeMod_statusUI.labelPanel);
				RuneModLoadingScreen.setSize(canvasAncestor.getSize());
				canvasAncestor.add(RuneModLoadingScreen, BorderLayout.CENTER, 0);
				RuneModLoadingScreen.revalidate();
				RuneModLoadingScreen.repaint();
			} else {
				if(canvasAncestor == null) {return;}
				canvasAncestor.remove(RuneModLoadingScreen);
			}
		});
	}

	@SneakyThrows
	void startUp_Custom() {
		client_static = client;
		isShutDown = false;

		canvasAncestor = client.getCanvas().getParent(); //is "clientPannel" class

		RuneModPlugin.toggleRuneModLoadingScreen(true);

		configManager.setConfiguration("stretchedmode","keepAspectRatio", true);

		for (Plugin plugin : pluginManager.getPlugins()) {
			if (plugin.getName().equalsIgnoreCase("GPU")) {
				if (pluginManager.isPluginEnabled(plugin)) {
					SwingUtilities.invokeAndWait(() ->
					{
						try {
							pluginManager.setPluginEnabled(plugin, false);
							pluginManager.stopPlugin(plugin);
						} catch (PluginInstantiationException e) {
							e.printStackTrace();
						}
						System.out.println("Disabled the GpuPlugin, as it is not compatible with runemod");
					});
				}
			}
		}

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

		registerWindowEventListeners();


		//int GpuFlags = DrawCallbacks.GPU | (computeMode == ComputeMode.NONE ? 0 : DrawCallbacks.HILLSKEW);
		client.getScene().setDrawDistance(50);
		client.setUnlockedFps(false);
		client.getCanvas().setIgnoreRepaint(true);
		setGpuFlags(0);
		setDrawCallbacks(this);

/*		ObjectLoader loader = new ObjectLoader();

		Storage storage = myCacheReader.store.getStorage();
		Index index = myCacheReader.store.getIndex(IndexType.CONFIGS);
		Archive archive = index.getArchive(ConfigType.OBJECT.getId());

		byte[] archiveData = storage.loadArchive(archive);
		ArchiveFiles files = archive.getFiles(archiveData);

		ObjectDefinition def = loader.load(f.getFileId(), f.getContents());

		for (FSFile f : files.getFiles())
		{
			ObjectDefinition def = loader.load(f.getFileId(), f.getContents());
			objects.put(f.getFileId(), def);
		}*/


/*			Window window = SwingUtilities.getWindowAncestor(client.getCanvas());
			JFrame frame = (JFrame) window;
			System.out.println("menuBarLocation: " + frame.getTitle());
			frame.setComponentZOrder(runeMod_statusUI.StatusDetail, 1);*/
			//return true;
		//});
	}

	@Override
	protected void startUp() throws IOException {
		sharedmem_rm = new SharedMemoryManager(this);
		sharedmem_rm.createSharedMemory("sharedmem_rm", 50000000); //50 mb

		runeModPlugin = this;

		JFrame window = (JFrame) SwingUtilities.getAncestorOfClass(Frame.class, client.getCanvas());
		runeMod_statusUI = new RuneMod_statusUI(window);

		runeModLauncher =  new RuneMod_Launcher(config.AltRuneModLocation(), config.StartRuneModOnStart());
		myCacheReader = new CacheReader();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();


		keyManager.registerKeyListener(hotkeyListenerq);
		keyManager.registerKeyListener(hotkeyListenerw);
		keyManager.registerKeyListener(hotkeyListenerr);
		keyManager.registerKeyListener(hotkeyListenert);
	}


	//block disabled due to missing api
/*	private byte[] runGetSkeletonBytes (int id) {
		IndexDataBase var0 = client.getSequenceDefinition_animationsArchive();
		System.out.println("Animation frame Count: "+ var0.getFileIds().length);

		IndexDataBase var1 = client.getSequenceDefinition_skeletonsArchive();
		System.out.println("skeleton Count: "+ var1.getGroupCount());

		IndexDataBase var2 = client.getSequenceDefinition_archive();
		System.out.println("Sequence Count: "+ var2.getGroupCount());

		int count = 0;
		for (int i = 0; i < var2.getFileCounts().length; i++) {
			count = count+ var2.getFileCounts()[i];
		}
		System.out.println("Sequence file file Count: "+ count);
		//return (getSkeletonBytes(var0, var1, id, false));
		return null;
	}*/

	public static byte[] insertIDToByteArray(byte[] bytes, int id) {
		//byte[] concatenated = new byte[bytes.length+4];
		byte[] id_temp = new byte[4];
		id_temp[0] = (byte)(id >> 24); // L: 84
		id_temp[1] = (byte)(id >> 16); // L: 85
		id_temp[2] = (byte)(id >> 8); // L: 86
		id_temp[3] = (byte)id; // L: 87

		byte[] concatenated = Arrays.copyOf(id_temp, id_temp.length + bytes.length);
		System.arraycopy(bytes, 0, concatenated, id_temp.length, bytes.length);

		return concatenated;
	}

	public byte[] addToArray(final byte[] source, final Byte element) {
		final byte[] destination = new byte[source.length + 1];
		System.arraycopy(source, 0, destination, 0, source.length);
		destination[source.length] = element;
		return destination;
	}

	//block disabled due to missing api
	/*private byte[] getFrameBytes(int frameId)
	{
		int framesGroupID = frameId >> 16;

		int frameFileId = frameId;
		frameFileId &= 65535;

		int var3 = framesGroupID; //framesGroupID
		Frames framesObjects = client.getFrames(var3);
		if (framesObjects == null) {
			return null;
		}

		IndexDataBase animArchive = client.getSequenceDefinition_animationsArchive(); //var1

		int[] var7 = animArchive.getFileIds(var3); //array of files in group

		for (int var8 = 0; var8 < var7.length; ++var8)
		{ //for each of the files in the group
			if (var7[var8] == frameFileId)
			{
				System.out.println("frameIdx_OldMethod: "+ (frameId & 0xFFFF) + " frameIdx_NewMethod: " + var8);
				byte[] var9 = animArchive.getConfigData(var3, var7[var8]);//animationFrameBytes
				return var9;
			}
		}
		return null;
	}*/

/*	private byte[] getFrameBytes(int frameId) //old bad methof of gettoing frame bytes
	{
		IndexDataBase animArchive = client.getSequenceDefinition_animationsArchive(); //var1

		int frameIdx = frameId & 0xFFFF;
		int framesGroupID = frameId >> 16;

		int[] frameFileIds = animArchive.getFileIds(framesGroupID); // L: 38
		byte[] bytes_AnimationFrame = animArchive.getConfigData(framesGroupID, frameFileIds[frameIdx]); // L: 40

		return bytes_AnimationFrame;
	}*/

	//block disabled due to missing api
/*	private byte[] getSkeletonBytesForAnimFrame(byte[] animFrameBytes) {
		int skeletonId = (animFrameBytes[0] & 255) << 8 | animFrameBytes[1] & 255;
		IndexDataBase skeletonArchive = client.getSequenceDefinition_skeletonsArchive(); //var2
		byte[] bytes_Skeleton = skeletonArchive.loadData(skeletonId, 0); // L: 54
		return bytes_Skeleton;
	}*/

	//block disabled due to missing api
/*	private void sendAnimation (int sequenceDefID) {
		boolean var3 = false;

		byte[] bytes_sequenceDefinition = client.getSequenceDefinition_archive().getConfigData(12, sequenceDefID); // L: 37

		if (bytes_sequenceDefinition!= null) {
			System.out.println("sending animSequence: " + sequenceDefID);
			if (!sentSequenceDefIds.contains(sequenceDefID)) {
				bytes_sequenceDefinition = insertIDToByteArray(bytes_sequenceDefinition, sequenceDefID); // inserts bytes to define id

				myRunnableSender.sendBytes(bytes_sequenceDefinition,"SequenceDefinition");

				//sentSequenceDefIds.add(sequenceDefID);
			System.out.println("sent sequence def id: " + sequenceDefID);
			for (int i1 = 0; i1 < bytes_sequenceDefinition.length; i1++) {
				System.out.println(bytes_sequenceDefinition[i1]);
			}

			}


			IndexDataBase animArchive = client.getSequenceDefinition_animationsArchive();
			IndexDataBase skeletonArchive = client.getSequenceDefinition_skeletonsArchive();

			//byte[] bytes_SeqDef = sequenceDefArchive.getConfigData(12, sequenceDefID); // send seqDefBytes

			SequenceDefinition sequenceDef = client.getSequenceDefinition(sequenceDefID);
			int[] sequenceFrameIds = sequenceDef.getFrameIDs();

			//System.out.println("bitshifted frameid is: "+ framesGroupID);

			//System.out.println("animSequence frameCount: " + sequenceDef.getFrameIDs().length);

			//NodeDeque var5 = client.createNodeDeque(); // L: 35

			//System.out.println("frameFIleIDs len: "+ frameFileIds.length);

			for (int i = 0; i < sequenceDef.getFrameIDs().length; ++i)
			{ // L: 39

				int frame = i;
				int packed = frame ^ Integer.MIN_VALUE;
				int interval = packed >> 16;
				frame = packed & 0xFFFF;
				int frameId = sequenceFrameIds[frame];
				int frameIdx = frameId & 0xFFFF;
				int framesGroupID = frameId >> 16;

				int[] frameFileIds = animArchive.getFileIds(framesGroupID); // L: 38
				if (frameIdx >= frameFileIds.length) { continue; }
				byte[] bytes_AnimationFrame = animArchive.getConfigData(framesGroupID, frameFileIds[frameIdx]); // L: 40
				Skeleton skeleton = null; // L: 41
				int skeletonId = (bytes_AnimationFrame[0] & 255) << 8 | bytes_AnimationFrame[1] & 255; // L: 42

				for (Skeleton skeletonLast = (Skeleton) var5.last(); skeletonLast != null; skeletonLast = (Skeleton) var5.previous()) //if skeleton != null do the loop
				{ // L: 43 44 49
					if (skeletonId == skeletonLast.id())
					{ // L: 45
						skeleton = skeletonLast; // L: 46
						break;
					}
				}

				if (!sentSkeletonIds.contains(skeletonId))
				{
					byte[] bytes_Skeleton;
					if (var3) { // L: 53
						bytes_Skeleton = skeletonArchive.getFile(0, skeletonId);
					}
					else {
						bytes_Skeleton = skeletonArchive.getFile(skeletonId, 0); // L: 54
					}

					//send skeleton bytes
					//skeleton = client.createSkeleton(skeletonId, bytes_Skeleton); // L: 55
					bytes_Skeleton = insertIDToByteArray(bytes_Skeleton, skeletonId); // inserts bytes to define id
					myRunnableSender.sendBytes(bytes_Skeleton,"Skeleton");
					sentSkeletonIds.add(skeletonId);
				}


				if (!sentFrameIds.contains(frameId))
				{
					byte[] frameBytes = getFrameBytes(frameId);
					if (frameBytes!= null) {
						int skeletonId = (frameBytes[0] & 255) << 8 | frameBytes[1] & 255; // L: 42

						if (!sentSkeletonIds.contains(skeletonId))
						{
							byte[] bytes_Skeleton;
							bytes_Skeleton = skeletonArchive.loadData(skeletonId, 0);
							if (bytes_Skeleton!=null)
								bytes_Skeleton = insertIDToByteArray(bytes_Skeleton, skeletonId); // inserts bytes to define id
							myRunnableSender.sendBytes(bytes_Skeleton,"Skeleton");
							sentSkeletonIds.add(skeletonId);
						}
						frameBytes = insertIDToByteArray(frameBytes, frameId); // inserts bytes to define id
						myRunnableSender.sendBytes(frameBytes,"AnimationFrame");
						sentFrameIds.add(frameId);
					}
					//send animationFrame bytes
					//System.out.println("sent frame id: " + frameId);
				}
				//frames[frameFileIds[i]] = client.createAnimationFrame(bytes_AnimationFrame, skeleton); // L: 58
			}
		}
	}*/

/*	public void sendSpotAnimation(int i) {
		byte[] spotAnimDefBytes = myCacheReader.GetCacheFileBytes(IndexType.CONFIGS, ConfigType.SPOTANIM.getId(), i);
		if (spotAnimDefBytes != null) {
			spotAnimDefBytes = insertIDToByteArray(spotAnimDefBytes, i);
			myRunnableSender.sendBytes(spotAnimDefBytes,"SpotAnimDefinition");
			System.out.println("sentSpotAnim: " + i);
		}
	}*/

/*	public void sendSpotAnimations() {
		int spotAnimDefCount = myCacheReader.GetFileCount(IndexType.CONFIGS, ConfigType.SPOTANIM.getId());
		for (int i = 0; i < spotAnimDefCount; i++) {
			sendSpotAnimation(i);
		}
	}*/

	public void sendTextures() {
		int counter = 0;
			for (int i = 0; i < client.getTextureProvider().getTextures().length; i++) { //sends the textures to unreal to be saved as texture defs and materialdefs
				short texSizeX = 128;
				short texSizeY = 128;
				TextureProvider textureProvider = client.getTextureProvider();
				Texture tex = textureProvider.getTextures()[i];
				int[] pixels = textureProvider.load(i);
				if (tex!=null) {
					if (pixels != null) {
						counter++;
						//System.out.println("pixel len =" + pixels.length);
						Buffer mainBuffer = new Buffer (new byte[3+2+2+4+(texSizeX*texSizeY*4)]);
						mainBuffer.writeByte (i);
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
							if (r == 0 && b == 0 && g == 0) {
								a = 0;
							}
/*					r = 5;
					g = 5;
					b = 5;
					a = 5;*/
							mainBuffer.writeByte(b);
							mainBuffer.writeByte(g);
							mainBuffer.writeByte(r);
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
			}
			configManager.setConfiguration("RuneMod","UpdateDefaultModToLatest", false);
		});
	}

	private static final Keybind myKeybindQ = new Keybind(KeyEvent.VK_Q, InputEvent.ALT_DOWN_MASK);
	private final HotkeyListener hotkeyListenerq = new HotkeyListener(() -> myKeybindQ)
	{
		//block disabled due to missing api
		@Override
		public void hotkeyPressed() //print animation
		{
			clientThread.invoke(() ->
			{
				//myCacheReader.sendColourPallette();

				//sendTextures();
/*				for (int x = 40; x < 60; x++) {
					for (int y = 40; y < 60; y++) {
						int regionID = x << 8 | y;
						myCacheReader.sendTilesInRegion(regionID);
					}
				}*/
				//myCacheReader.sendTilesInRegion(12850);

/*				Tile targetTile = client.getSelectedSceneTile();
				int tileHeightFromCache = myCacheReader.getTileHeightAtCoordinate(targetTile.getWorldLocation().getX(), targetTile.getWorldLocation().getY(), targetTile.getWorldLocation().getPlane());
				int tileHeightFromClient = client.getTileHeights()[targetTile.getPlane()][targetTile.getSceneLocation().getX()][targetTile.getSceneLocation().getY()];
				System.out.println("tileHeightFromCache:" + tileHeightFromCache);
				System.out.println("tileHeightFromClient:" + tileHeightFromClient);*/

/*				for (int regionID = 0; regionID < Short.MAX_VALUE; regionID++) {
					System.out.println("sending region"+regionID);
					myCacheReader.sendTilesInRegion(regionID);
				}*/

/*				myCacheReader.sendOverlayDefinitions();
				myCacheReader.sendUnderlayDefinitions();
				myCacheReader.sendTilesInRegion(12850);
				myCacheReader.sendTilesInRegion(12851);
				myCacheReader.sendTilesInRegion(12595);
				myCacheReader.sendTilesInRegion(12594);
				myCacheReader.sendTilesInRegion(12593);
				myCacheReader.sendTilesInRegion(12849);
				myCacheReader.sendTilesInRegion(13105);
				myCacheReader.sendTilesInRegion(13106);
				myCacheReader.sendTilesInRegion(13107);*/
				//myCacheReader.sendAllMapDefinitions();
				//myCacheReader.sendUnderlayDefinitions();
				//myCacheReader.sendOverlayDefinitions();
				//int[] regions = client.getMapRegions();
				//myCacheReader.sendTilesInRegion(12850);
/*				for (int i = 0; i< regions.length; i++) {
					myCacheReader.sendTilesInRegion(regions[i]);
				}*/
			});

//			client.invokeMenuAction(-1, 36569105, 57, 1, "Look up name", "", 206, 261);
//			KeyEvent kvPressed = new KeyEvent(client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER);
//			KeyEvent kvReleased = new KeyEvent(client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER);



			//sends all spot animation cache bytes
/*			clientThread.invoke(() ->
			{
				int spotAnimDefCount = myCacheReader.GetFileCount(IndexType.CONFIGS, ConfigType.SPOTANIM.getId());
				System.out.println("spotAnimDefCount: " + spotAnimDefCount);
				for (int i = 0; i < spotAnimDefCount; i++) {
					byte[] spotAnimDefBytes = myCacheReader.GetCacheFileBytes(IndexType.CONFIGS, ConfigType.SPOTANIM.getId(), i);
					if (spotAnimDefBytes != null) {
						spotAnimDefBytes = insertIDToByteArray(spotAnimDefBytes, i);
						myRunnableSender.sendBytes(spotAnimDefBytes,"SpotAnimDefinition");
						System.out.println("sentSpotAnim: " + i);
					}
				}
			});*/
/*
			int texturesCount = client.getTextureProvider().getTextures().length;
			System.out.println("Textures: " + texturesCount);
			for (int i = 0; i < texturesCount; i++) {
				sendTexture(i);
			}

			int sequenceDefCount = client.getSequenceDefinition_archive().getGroupFileCount(12);
			System.out.println("sequenceDefCount: " + sequenceDefCount);
			for (int i = 0; i < sequenceDefCount; i++) {
				sendAnimation(i);
			}

			int objDefModelDataCount = client.getObjectDefinition_modelsArchive().getGroupCount();
			System.out.println("objDefModelDataCount: " + objDefModelDataCount);
			for (int i = 0; i < objDefModelDataCount; i++) {
				byte[] modelDataBytes = client.getObjectDefinition_modelsArchive().getConfigData(i & 65535, 0);
				if (modelDataBytes != null) {
					modelDataBytes = insertIDToByteArray(modelDataBytes, i);
					myRunnableSender.sendBytes(modelDataBytes,"ModelData");
				}
			}

			int npcDefCount = client.getNpcDefinition_archive().getGroupFileCount(9);
			System.out.println("npcDefCount: " + npcDefCount);
			for (int i = 0; i < npcDefCount; i++) {
				byte[] npcDefinitionBytes = client.getNpcDefinition_archive().getConfigData(9, i);
				if (npcDefinitionBytes != null) {
					npcDefinitionBytes = insertIDToByteArray(npcDefinitionBytes, i);
					myRunnableSender.sendBytes(npcDefinitionBytes,"NpcDefinition");
				}
			}

			int objectDefinitionCount = client.getObjectDefinition_archive().getGroupFileCount(6);
			System.out.println("objectDefCount: " + objectDefinitionCount);
			for (int i = 0; i < objectDefinitionCount; i++) {
				byte[] objectDefinitionBytes = client.getObjectDefinition_archive().getConfigData(6, i);
				if (objectDefinitionBytes != null) {
					objectDefinitionBytes = insertIDToByteArray(objectDefinitionBytes, i);
					myRunnableSender.sendBytes(objectDefinitionBytes,"ObjectDefinition");
				}
			}

			int itemDefinitionCount = client.getItemDefinition_archive().getGroupFileCount(10);
			System.out.println("itemDefCount: " + itemDefinitionCount);
			for (int i = 0; i < itemDefinitionCount; i++) {
				byte[] itemDefinitionBytes = client.getItemDefinition_archive().getConfigData(10, i);
				if (itemDefinitionBytes != null) {
					itemDefinitionBytes = insertIDToByteArray(itemDefinitionBytes, i);
					myRunnableSender.sendBytes(itemDefinitionBytes,"ItemDefinition");
				}
			}

			int kitDefinitionCount = client.getKitDefinition_archive().getGroupFileCount(3);
			System.out.println("kitDefCount: " + kitDefinitionCount);
			for (int i = 0; i < kitDefinitionCount; i++) {
				byte[] kitDefinitionBytes = client.getKitDefinition_archive().getConfigData(3, i);
				if (kitDefinitionBytes != null) {
					kitDefinitionBytes = insertIDToByteArray(kitDefinitionBytes, i);
					myRunnableSender.sendBytes(kitDefinitionBytes,"KitDefinition");
				}
			}

			System.out.println("pressed");
			for (int i = 0; i < 60000; i++) {
				//int modelID = 9637+counter; // capes
				//int modelID = 640 + counter; //spears wall
				//int modelID = 14200 + counter; //soft edged man man with hard edged shackles
			}*/

		}
	};

/*
	private void sendTerrainData() {
		if(!config.spawnTerrain()) {return;}
		//SendLowLatencyData();
		Scene scene = client.getScene();

		int tileHeights [][][] = client.getTileHeights();
		byte tileSettings [][][] = client.getTileSettings();

		int tileHeightsBufferSize = 43264;
		int tileSettingsBufferSize = 43264;
		int tileColorsBufferSize = 43264*4;
		int tileTextureIdsBufferSize = 43264;
		int tileRotationsBufferSize = 43264;
		int tileOverlayColorsBufferSize = 43264*4;
		int tileOverlayShapesBufferSize = 43264;
		int tileOverlayTextureIdsBufferSize = 43264;
		int flatTileBufferSize = 43264;
		int tileShouldDrawBufferSize = 43264;
		int tileOriginalPlaneBufferSize = 43264;

		int terrainDataBufferSize = 4+tileHeightsBufferSize+
				tileSettingsBufferSize+
				tileColorsBufferSize+
				tileTextureIdsBufferSize+
				tileRotationsBufferSize+
				tileOverlayColorsBufferSize+
				tileOverlayShapesBufferSize+
				tileOverlayTextureIdsBufferSize+
				flatTileBufferSize+
				tileShouldDrawBufferSize+
				tileOriginalPlaneBufferSize;

		int noTerrainDataTypes = 9;

		Buffer terrainDataBuffer = new Buffer(new byte[terrainDataBufferSize]);

		for (int z = 0; z < 4; z++)
		{
			for (int y = 0; y < 104; y++)
			{
				for (int x = 0; x < 104; x++)
				{
					boolean isBridgeTile = false;
					boolean isLinkedTile = false;
					int tileOriginalPlane = 0;
					int isAboveBridge = 0;
					if (z == 2 &&  z < Constants.MAX_Z - 1 && (client.getTileSettings()[z-1][x][y] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE) { //if we are above a bridge tile
						isAboveBridge = 1;
					}

					if (z < Constants.MAX_Z - 1 && (tileSettings[z][x][y] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE && z == 1)
					{
						isBridgeTile = true;
					}

					Tile tile = null;

					if (client.getScene().getTiles()[z][x][y]!= null)
					{
						tile = scene.getTiles()[z][x][y];
						tileOriginalPlane = tile.getRenderLevel();

					}

					if (client.getScene().getTiles()[z][x][y]!= null || isBridgeTile)
					{
						if (isBridgeTile)
						{ // if is bridge tile, we get the tiles colors from the tile bellow. We also eed to check Ne color of the bellow tile, to know if the bridge tile is supposed to be rendered/visible.
							tile = scene.getTiles()[z][x][y];
							if (scene.getTiles()[z - 1][x][y] != null)
							{
								if (scene.getTiles()[z - 1][x][y].getSceneTilePaint() != null)
								{
									if (scene.getTiles()[z - 1][x][y].getSceneTilePaint().getNeColor() != 12345678)
									{
										tile = scene.getTiles()[z - 1][x][y];
									}
								}
							}
						}
						else
						{
							if (scene.getTiles()[z][x][y].getBridge() != null)
							{ //if this tile is bellow a bridge, we get colors from Linked_Bellow tile (getBridge is getLinkedBellow)
								tile = scene.getTiles()[z][x][y].getBridge();
								isLinkedTile = true;
							}
							else
							{
								tile = scene.getTiles()[z][x][y]; //normal Tile
							}
						}
					}

					int tileHeight =  (tileHeights[z+isAboveBridge][x][y]/8)*-1;
					int tileSetting = tileSettings[z][x][y];
					int tileCol = 0; //12345678
					int tileTextureId = -1;
					int tileRotation = -1;
					int tileOverlayColor = 0; //12345678
					int tileOverlayTextureId = -1;
					int tileOverlayShape = 0;
					int tileIsFlat = 0;
					int tileIsdDraw = -1;

					if (tile!= null)
					{
						if (tile.getSceneTilePaint() != null)
						{
							if (tile.getSceneTilePaint().getNeColor() == 12345678 || tile.getSceneTilePaint().getSwColor() == 12345678) {
								tileIsdDraw = 0;
							} else {
								tileIsdDraw = 1;
							}

							tileCol = tile.getSceneTilePaint().getRBG();
							tileTextureId = tile.getSceneTilePaint().getTexture();
						}

						SceneTileModel tileModel = tile.getSceneTileModel();
						if (tileModel != null)
						{
							if ((tileModel.getModelUnderlay()== 12345678 || tileModel.getModelOverlay()==12345678) || tileIsdDraw == 0) {
								tileIsdDraw = 0;
							} else {
								tileIsdDraw = 1;
							}
							tileRotation = tileModel.getRotation();
							tileOverlayColor = tileModel.getModelOverlay();
							tileCol = tileModel.getModelUnderlay();

*/
/*								if (z < 3) {
									if (tileSettings[tile.getPlane()+1] [tile.getSceneLocation().getX()] [tile.getSceneLocation().getY()] == 8) { //if tile above has flag 8, tilemodel is used on plane bellow? maybe
										Tile tileAbove = scene.getTiles()[tile.getPlane()+1] [tile.getSceneLocation().getX()] [tile.getSceneLocation().getY()];
										if (tileAbove!= null) {
											SceneTileModel tileModelAbove = tileAbove.getSceneTileModel();
											if (tileAbove != null)
											{
												if (tileModelAbove!= null)
												{
													tileCol = tileModelAbove.getModelOverlay();
													int[] tileModelTextureIds = tileModel.getTriangleTextureId();
													if (tileModelTextureIds != null)
													{ //find textureIDForOverlay
														for (int i = 0; i < tileModelTextureIds.length; i++)
														{
															if (tileModelTextureIds[i] > 0)
															{
																tileOverlayTextureId = tileModelTextureIds[i];
																break;
															}
														}
													}
												}
											}
										}
									}
								}*//*


							int[] tileModelTextureIds = tileModel.getTriangleTextureId();
							if (tileModelTextureIds != null)
							{ //find textureIDForOverlay
								for (int i = 0; i < tileModelTextureIds.length; i++)
								{
									if (tileModelTextureIds[i] > 0)
									{
										tileOverlayTextureId = tileModelTextureIds[i];
										break;
									}
								}
							}

							tileOverlayShape = tileModel.getShape();
							//if (tileModel.getIsFlat()) tileIsFlat = 1; //apimissing
						}


					}

					if (tileIsdDraw == -1){tileIsdDraw = 0;}

					terrainDataBuffer.writeByte(tileHeight); // tileHeightsBuffer
					terrainDataBuffer.writeByte(tileSetting); // tileSettingsBuffer
					terrainDataBuffer.writeInt(tileCol); // tileColorsBuffer
					terrainDataBuffer.writeByte(tileTextureId); // tileTextureIdsBuffer
					terrainDataBuffer.writeByte(tileRotation); // tileRotationsBuffer
					terrainDataBuffer.writeInt(tileOverlayColor); //tileOverlayColorsBuffer
					terrainDataBuffer.writeByte(tileOverlayTextureId); //tileOverlayTextureIdsBuffer
					terrainDataBuffer.writeByte(tileOverlayShape); //tileOverlayShapesBuffer
					terrainDataBuffer.writeByte(tileIsFlat); //flatTileBuffer
					terrainDataBuffer.writeByte(tileIsdDraw); //flatTileBuffer
					terrainDataBuffer.writeByte(tileOriginalPlane); //flatTileBuffer
				}
			}
		}
		rsclient_terrain_shared_memory.setDataLength(terrainDataBuffer.array.length); //write data length
		rsclient_terrain_shared_memory.SharedMemoryData.write(4,terrainDataBuffer.array,0,terrainDataBuffer.array.length);
		myRunnableSender.sendBytes(new byte[3], "TerrainLoad");
		//rsclient_terrain_shared_memory.SharedMemoryData.read();
	}
*/

	private void rgbaIntToColors(int col) {
		int a = (col >> 24) & 0xFF;
		int r = (col >> 16) & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = col & 0xFF;
	}

	//@SneakyThrows
	private void createSharedMemory () {
/*		ByteBuffer buf = ByteBuffer.allocateDirect(10);
		directMemoryBuffer = new DirectMemoryBuffer(buf,0,10);
		for (int i = 0; i < 10; i++) {
			buf.put((byte)i);
		}
		System.out.println("made new DirectByteBuffer at address: "+directMemoryBuffer.getAddress());*/
	}


/*	private void sendUiPixels() {
		int bufferLen = client.getGraphicsPixelsWidth()*client.getGraphicsPixelsHeight() + 4;
		Buffer uiByteBuffer = new Buffer(new byte[bufferLen]);
		uiByteBuffer.writeInt(bufferLen);

		for (int i = 0; i < client.getGraphicsPixels().length; i++) {
			int col = client.getGraphicsPixels()[i];
			byte a = (byte)((col >> 24) & 0xFF);
			byte r = (byte)((col >> 16) & 0xFF);
			byte g = (byte)((col >> 8) & 0xFF);
			byte b = (byte)(col & 0xFF);
			uiByteBuffer.writeByte(a);
			uiByteBuffer.writeByte(r);
			uiByteBuffer.writeByte(g);
			uiByteBuffer.writeByte(b);
		}
	}*/

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
		if (client.getGameState() != GameState.LOGGED_IN)
		{
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
					myCacheReader.sendModels();
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
					myCacheReader.sendUnderlayDefinitions();
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
			//downloadZip("https://runemod.net/dotrunemod/runemod_master.zip", "runemod_master.zip");

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


	GameState curGamestate = GameState.STARTING;
	GameState lastGameState = GameState.STARTING;
	@SneakyThrows
	@Subscribe
	private void onGameStateChanged(GameStateChanged event) {
		//clientThread.invokeAtTickEnd(() ->
		//{
			lastGameState = curGamestate;
			curGamestate = event.getGameState();

			byte newEventTypeByte = 0;
			if (curGamestate == GameState.LOGIN_SCREEN) {
				System.out.println("Login SCREEN...");
				baseX = -1;
				baseY = -1;
				newEventTypeByte = 1;
			} else if (curGamestate == GameState.LOGGING_IN) {
				//SendPerFramePacket();
				System.out.println("logging in...");
				newEventTypeByte = 2;
			} else if (curGamestate == GameState.LOGGED_IN) {
				//SendPerFramePacket();
				System.out.println("logged in...");
				newEventTypeByte = 3;
			} else if (curGamestate == GameState.HOPPING) {
				baseX = -1;
				baseY = -1;
				//SendPerFramePacket();
				System.out.println("hopping...");
				newEventTypeByte = 4;
			} else if (curGamestate == GameState.LOADING) {
				//SendPerFramePacket();
				System.out.println("loading...");
				newEventTypeByte = 5;
				newRegionLoaded = true;
			} else if (curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR) {

			} else if (curGamestate == GameState.CONNECTION_LOST) {
				System.out.println("ConnectionLost...");
			}

			if (curGamestate == GameState.STARTING) {

			}
			if (curGamestate == GameState.UNKNOWN) {

			}

			Buffer packet = new Buffer(new byte[10]);
			packet.writeByte(newEventTypeByte);
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
			long tag = event.getWallObject().getHash();
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
				long tag = event.getWallObject().getHash();
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
			long tag = event.getDecorativeObject().getHash();
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


	private HashMap<Integer, Integer> VarbitObjDef_Map = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> VarpObjDef_Map = new HashMap<Integer, Integer>();

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

/*	@Subscribe
	private void onVarbitChanged(VarbitChanged event) {

		//for VarbitChanged func in unreal, we send: varType. varId. varValue. custom0.

		//System.out.println("varbitChanged");
		Buffer buffer = new Buffer(new byte[20]);

		if (event.getVarbitId()!=-1) {
			buffer.writeInt(event.getVarbitId());
			buffer.writeInt(event.getValue());
			buffer.writeInt(VarbitObjDef_Map.getOrDefault(event.getVarbitId(),-1));
			myRunnableSender.sendBytes(buffer.array, "Varbit");

			if (VarbitObjDef_Map.containsKey(event.getVarbitId())) {
				System.out.println("objDef: " +VarpObjDef_Map.getOrDefault(event.getVarpId(),-1)+ " Imposter Changed");
			}
		} else {
			buffer.writeInt(event.getVarpId());
			buffer.writeInt(event.getValue());
			buffer.writeInt(VarpObjDef_Map.getOrDefault(event.getVarpId(),-1));
			myRunnableSender.sendBytes(buffer.array, "Varp");
			if (VarpObjDef_Map.containsKey(event.getVarpId())) {
				System.out.println("objDef: " +VarpObjDef_Map.getOrDefault(event.getVarpId(),-1)+ " Imposter Changed");
			}
		}
	}*/

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
									System.out.println("Found RuneLiteObject wirth id: "+runeLiteObject.getId());
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
												allGameObjects.put(gameObject.getHash(), dynamicObject);
/*												if(gameObject.getId() == 5589 ) {
													System.out.println("animFrameCycle = " + dynamicObject.getAnimCycle());
												}*/
											}
										}
									}
								}
							}

							DecorativeObject gameObject =  tile.getDecorativeObject();

							if (gameObject != null) // && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()
							{
								if(gameObject.getRenderable() != null) {
									if(gameObject.getRenderable() instanceof DynamicObject) {
										DynamicObject dynamicObject = (DynamicObject)gameObject.getRenderable();
										if(dynamicObject.getAnimation()!= null) {
											allGameObjects.put(gameObject.getHash(), dynamicObject);
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
											allGameObjects.put(wallbject.getHash(), dynamicObject);
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
			int objectDefinitionId = event.getGameObject().getId();
			if(objectDefinitionId == 144) {
				System.out.println("gate GO spawned (id 144)");
			}

			if (objectDefinitionId == 34818) {
				System.out.println("animated alter version spawned");
			}
			if (objectDefinitionId == 30373) {
				System.out.println("unAnimated alter version spawned");
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


			long tag = event.getGameObject().getHash();
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
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		{
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
			long tag = event.getGameObject().getHash();

			if (event.getGameObject().getId() == 34818) {
				System.out.println("animated alter version DeSpawned");
			}
			if (event.getGameObject().getId() == 30373) {
				System.out.println("unAnimated alter version DeSpawned");
			}

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
			long tag = event.getGroundObject().getHash();
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
			int height = Perspective.getTileHeight(client, event.getTile().getLocalLocation(), client.getPlane()) * -1;
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
		if (!config.spawnNPCs()) { return; }
		clientThread.invokeLater(() -> {
			clientThread.invokeLater(() -> {
				clientThread.invokeLater(() -> {
					clientThread.invokeLater(() -> {
						clientThread.invokeLater(() -> {
							clientThread.invokeLater(() -> {
								Buffer actorSpawnPacket = new Buffer(new byte[100]);

								int instanceId = event.getNpc().getIndex();
								int definitionId = event.getNpc().getId();
								actorSpawnPacket.writeByte(1); //write npc data type
								actorSpawnPacket.writeShort(instanceId);
								actorSpawnPacket.writeShort(definitionId);
								System.out.println("NPC Spawn kashjkasd");
								sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
								//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
							});
						});
					});
				});
			});
		});
	}

	@Subscribe
	private void onNpcChanged(NpcChanged event) {
		if (!config.spawnNPCs()) { return; }
		clientThread.invokeLater(() -> {
			clientThread.invokeLater(() -> {
				clientThread.invokeLater(() -> {
					clientThread.invokeLater(() -> {
						clientThread.invokeLater(() -> {
							clientThread.invokeLater(() -> {
								Buffer actorSpawnPacket = new Buffer(new byte[100]);

								int instanceId = event.getNpc().getIndex();
								int definitionId = event.getNpc().getId();
								actorSpawnPacket.writeByte(1); //write npc data type
								actorSpawnPacket.writeShort(instanceId);
								actorSpawnPacket.writeShort(definitionId);
								System.out.println("NPC Change kashjkasd");
								sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
								//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorSpawn");
							});
						});
					});
				});
			});
		});
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event) {
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		int instanceId = event.getNpc().getIndex();
		actorSpawnPacket.writeByte(1); //write npc data type
		actorSpawnPacket.writeShort(instanceId);

		//write bogus packet so len is more than 1

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		//myRunnableSender.sendBytes(trimmedBufferBytes(actorSpawnPacket), "ActorDeSpawn");
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
		if (player.getPlayerComposition().isFemale()) isFemale = 1;
		actorSpawnPacket.writeByte(isFemale); //isFemale

		//temp actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID
		actorSpawnPacket.writeInt(-1); //npcTransformID //temp

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
		if (player.getPlayerComposition().isFemale()) isFemale = 1;
		actorSpawnPacket.writeByte(isFemale); //isFemale

		//temp actorSpawnPacket.writeInt(player.getPlayerComposition().getTransformedNpcId()); //npcTransformID
		actorSpawnPacket.writeInt(-1); //npcTransformID //temp

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
		canvasSizeChanged = true;
	}

	private void sendPlaneChanged() {
		System.out.println("PlaneChanged");
		Buffer buffer = new Buffer(new byte[4]);
		buffer.writeByte(clientPlane);
		//myRunnableSender.sendBytes(buffer.array, "PlaneChanged");
	}

/*
	@Subscribe
	private void onDrawFinished_Ui(DrawFinished_Ui event)
	{
		//SendPerFramePacket();
	}
*/

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

	public SpotAnimationDefinition SpotAnimationDefinition_get(int id) {
		IndexDataBase SpotAnimDefArchive = client.getIndex(2);

		byte[] bytes = SpotAnimDefArchive.loadData(13, id);

		//decode(buffer)
		Buffer var1 = new Buffer(bytes);

		SpotAnimationDefinition spotAnimDef = new SpotAnimationDefinition();
		spotAnimDef.decode(var1);

		return spotAnimDef;

/*		SpotAnimationDefinition var1 = (SpotAnimationDefinition)SpotAnimationDefinition.SpotAnimationDefinition_cached.get((long)var0); // L: 37
		if (var1 != null) { // L: 38
			return var1;
		} else {
			byte[] var2 = SpotAnimationDefinition.SpotAnimationDefinition_archive.takeFile(13, var0); // L: 39
			var1 = new SpotAnimationDefinition(); // L: 40
			var1.id = var0; // L: 41
			if (var2 != null) { // L: 42
				var1.decode(new Buffer(var2));
			}

			SpotAnimationDefinition.SpotAnimationDefinition_cached.put(var1, (long)var0); // L: 43
			return var1; // L: 44*/
	}

	private void UpdateSharedMemoryUiPixels() {
		if(sharedmem_rm == null) {return;}
		if(sharedMemPixelsUpdatedTick == client.getGameCycle()) {return;}
		if(!client.getCanvas().isShowing()) {return;}
		sharedMemPixelsUpdatedTick = client.getGameCycle();
		final BufferProvider bufferProvider = client.getBufferProvider();
		//rsclient_ui_pixels.setDataLength(pixels.length*4); //write data length
		//int viewX = client.getCanvas().getSize().width;
		//int viewY = client.getCanvas().getSize().height;
		//rsclient_ui_pixels_shared_memory.setInt_BigEndian(4,viewX); //write data length
		//rsclient_ui_pixels_shared_memory.setInt_BigEndian(8,viewY); //write data length
		//rsclient_ui_pixels.SharedMemoryData.write(0, pixels,0, pixels.length); //write pixel data

		int bufferWidth = bufferProvider.getWidth();
		int bufferHeight = bufferProvider.getHeight();

		sharedmem_rm.setInt(30000000, bufferWidth);
		sharedmem_rm.setInt(30000005, bufferHeight);

		float ratioX = 1;
		float ratioY = 1;
		if (client.isStretchedEnabled()) {
			ratioX = (float)client.getStretchedDimensions().width/(float)bufferProvider.getWidth();
			ratioY = (float)client.getStretchedDimensions().height/(float)bufferProvider.getHeight();
		}

		//top left position of ui in rl window.
		float rsUiPosOffsetX = client.getCanvas().getParent().getLocationOnScreen().x-client.getCanvas().getLocationOnScreen().x;
		rsUiPosOffsetX*=-1;
		float rsUiPosOffsetY = client.getCanvas().getParent().getLocationOnScreen().y-client.getCanvas().getLocationOnScreen().y;
		rsUiPosOffsetY*=-1;
		sharedmem_rm.setInt(30000010, (int)rsUiPosOffsetX);
		sharedmem_rm.setInt(30000015, (int)rsUiPosOffsetY);

		//3d viewport size
		float View3dSizeX = client.getViewportWidth();
		View3dSizeX*=ratioX;
		float View3dSizeY = client.getViewportHeight();
		View3dSizeY*=ratioY;
		sharedmem_rm.setInt(30000020, (int)View3dSizeX);
		sharedmem_rm.setInt(30000025, (int)View3dSizeY);

		//System.out.println(""+client.getStretchedDimensions().getWidth() + " " + client.getStretchedDimensions().getHeight());

		//top left position of 3d viewport in rl window.
		float View3dOffsetX = client.getViewportXOffset();
		View3dOffsetX*=ratioX;
		View3dOffsetX+=rsUiPosOffsetX;
		float View3dOffsetY = client.getViewportYOffset();
		View3dOffsetY*=ratioY;
		View3dOffsetY+=rsUiPosOffsetY;
		sharedmem_rm.setInt(30000030, (int)View3dOffsetX);
		sharedmem_rm.setInt(30000035, (int)View3dOffsetY);

		//Size of ui. Can differ from buffer size because of stretchmode.
		float canvasSizeX = client.getCanvasWidth();
		canvasSizeX*=ratioX;
		float canvasSizeY = client.getCanvasHeight();
		canvasSizeY*=ratioY;
		sharedmem_rm.setInt(30000040, (int)canvasSizeX);
		sharedmem_rm.setInt(30000045, (int)canvasSizeY);

		sharedmem_rm.SharedMemoryData.write(30000050, bufferProvider.getPixels(),0, bufferProvider.getHeight()*bufferProvider.getWidth());
	}

	int baseX = 0;
	int baseY = 0;

	private void sendBaseCoordinatePacket(Scene scene) { //send Base Coordinate if needed
		//if(client.getBaseX()!= baseX || client.getBaseY()!= baseY) {

			WorldPoint baseCoord = getSceneBase(scene, client.getPlane());

			baseX = baseCoord.getX();
			baseY = baseCoord.getY();

			Buffer packet = new Buffer(new byte[20]);
			packet.writeShort(baseX);
			packet.writeShort(baseY);
			packet.writeByte(client.getPlane());

			sharedmem_rm.backBuffer.writePacket(packet, "BaseCoordinate");

		//}
	}

	private void sendBaseCoordinatePacket() { //send Base Coordinate if needed
		//if(client.getBaseX()!= baseX || client.getBaseY()!= baseY) {

		WorldPoint baseCoord = getSceneBase(client.getScene(), client.getPlane());

		baseX = baseCoord.getX();
		baseY = baseCoord.getY();

		Buffer packet = new Buffer(new byte[20]);
		packet.writeShort(baseX);
		packet.writeShort(baseY);
		packet.writeByte(client.getPlane());

		sharedmem_rm.backBuffer.writePacket(packet, "BaseCoordinate");
		//}
	}

	/**
	 * Returns the south-west coordinate of the scene in world space, after resolving instance template chunks to their
	 * original world coordinates. If the scene is instanced, the base coordinates are computed from the center chunk.
	 *
	 * @param scene to get the south-west coordinate for
	 * @param plane to use when resolving instance template chunks
	 * @return the south-western coordinate of the scene in world space
	 */
	public static WorldPoint getSceneBase(Scene scene, int plane)
	{
		System.out.println("getSceneBase");
		int baseX = scene.getBaseX();
		int baseY = scene.getBaseY();

		if (scene.isInstance())
		{
			System.out.println("IsInstancedArea");
			// Assume the player is loaded into the center chunk, and calculate the world space position of the lower
			// left corner of the scene, assuming well-behaved template chunks are used to create the instance.
			int chunkX = 6, chunkY = 6;
			int chunk = scene.getInstanceTemplateChunks()[plane][chunkX][chunkY];
			if (chunk == -1)
			{
				// If the center chunk is invalid, pick any valid chunk and hope for the best
				int[][] chunks = scene.getInstanceTemplateChunks()[plane];
				outer:
				for (chunkX = 0; chunkX < chunks.length; chunkX++)
				{
					for (chunkY = 0; chunkY < chunks[chunkX].length; chunkY++)
					{
						chunk = chunks[chunkX][chunkY];
						if (chunk != -1)
						{
							break outer;
						}
					}
				}
			}

			// Extract chunk coordinates
			baseX = chunk >> 14 & 0x3FF;
			baseY = chunk >> 3 & 0x7FF;
			// Shift to what would be the lower left corner chunk if the template chunks were contiguous on the map
			baseX -= chunkX;
			baseY -= chunkY;
			// Transform to world coordinates
			baseX <<= 3;
			baseY <<= 3;
		}

		return new WorldPoint(baseX, baseY, plane);
	}

	private void WritePerFramePacket() {
		client.isInInstancedRegion();
		client.getScene().getInstanceTemplateChunks();

		perFramePacketSentTick = client.getGameCycle();
		//System.out.println("canvasSizeChanged"+viewWidth+" X "+viewHeight);
		if (client.getPlane()!= clientPlane) {
			clientPlane = client.getPlane();
			sendPlaneChanged();
		}

/*		if (client.getLocalPlayer() == null) {
			//System.out.println("not logged in yet");
			return;
		}*/

		Set<Integer> hashedEntitys_ThisFrame = new HashSet<Integer>();

		byte[] bytes = new byte[20000];
		Buffer perFramePacket = new Buffer(bytes);

		int camX = client.getCameraX();
		int camY = client.getCameraY();
		int camZ = client.getCameraZ();
		int camYaw = client.getCameraYaw();
		int camPitch = client.getCameraPitch();
		int camZoom = client.getScale();
		int maxVisiblePlane = client.getSceneMaxPlane(); //missing api
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
		perFramePacket.writeByte(maxVisiblePlane);
		perFramePacket.writeInt(clientCycle);
		perFramePacket.writeShort(canvasWidth);
		perFramePacket.writeShort(canvasHeight);


		List<NPC> npcs = client.getNpcs();

		int npcCount = npcs.size();
		if(!config.spawnNPCs()) { npcCount = 0;}
		perFramePacket.writeShort(npcCount);
		if (npcCount > 0) {
			for (int i = 0; i < npcCount; i++ ) {
				NPC npc = npcs.get(i);
				int npcInstanceId = npc.getIndex();
				int npcX = npc.getLocalLocation().getX();
				int npcY = npc.getLocalLocation().getY();
				int offset = ((npc.getComposition().getSize()-1)*128)/2; //offset is required for npc's who are > 1 tile wide
				LocalPoint LocationToSampleHeightFrom = new LocalPoint(npc.getLocalLocation().getX() + offset, npc.getLocalLocation().getY() + offset);
				int npcHeight = Perspective.getTileHeight(client, LocationToSampleHeightFrom, client.getPlane())*-1;
				int npcOrientation = npc.getCurrentOrientation();

				int npcAnimationId = -1;
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
					}/*else
					if (npc.getGraphic() != -1) {
						npcAnimation = npc.getGraphic();
						npcAnimationFrame = npc.getSpotAnimFrame();
						//temp npcAnimationFrameCycle = npc.getSpotAnimationFrameCycle();
					}*/
				}

				int spotAnimationId = -1;
				int spotAnimationFrame = -1;
				int spotAnimationFrameCycle = -1;

				for(ActorSpotAnim spotAnim : npc.getSpotAnims()) {
					spotAnimationFrame = spotAnim.getFrame();
					spotAnimationId = spotAnim.getId();
				}

				perFramePacket.writeInt(npcInstanceId);
				perFramePacket.writeShort(npcX);
				perFramePacket.writeShort(npcY);
				perFramePacket.writeShort(npcHeight);
				perFramePacket.writeShort(npcOrientation);
				perFramePacket.writeInt((config.spawnAnimations() ? npcAnimationId : -1));
				perFramePacket.writeShort(npcAnimationFrame);
				perFramePacket.writeShort(npcAnimationFrameCycle);

				perFramePacket.writeInt(spotAnimationId);
				perFramePacket.writeShort(spotAnimationFrame);
				perFramePacket.writeShort(spotAnimationFrameCycle);
			}
		}

		List<Player> players = client.getPlayers();

		int playerCount = players.size();
		if(!config.spawnPlayers()) { playerCount = 0;}
		perFramePacket.writeShort(playerCount);
		if (playerCount > 0) {
			for (int i = 0; i < playerCount; i++ ) {
				Player player = players.get(i);
				int playerInstanceId =player.getId();

				int playerX = player.getLocalLocation().getX();
				int playerY = player.getLocalLocation().getY();
				int playerHeight = Perspective.getTileHeight(client, player.getLocalLocation(), client.getPlane())*-1;
				int playerOrientation = player.getCurrentOrientation();
/*				if (playerInstanceId == client.getLocalPlayerIndex()) {
					System.out.println(playerOrientation);
				}*/
				int animationId = -1;
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
					}/*else
					if (player.getGraphic() != -1) {
						animation = player.getGraphic();
						animationFrame = player.getSpotAnimFrame();
						//temp animationFrameCycle = player.getSpotAnimationFrameCycle();
					}*/
				}

				int spotAnimationId = -1;
				int spotAnimationFrame = -1;
				int spotAnimationFrameCycle = -1;
				int spotAnimationHeight = 0;
				int sceneId = -1;
				Boolean newSpotAnimSpawned = false;
				if (config.spawnPlayerGFX()) {
					for(ActorSpotAnim spotAnim : player.getSpotAnims()) {
						spotAnimationFrame = spotAnim.getFrame();
						spotAnimationId = spotAnim.getId();
						spotAnimationHeight = spotAnim.getHeight();

						sceneId = spotAnim.hashCode();

						if(!hashedEntitys_LastFrame.contains(sceneId)) {
							System.out.println("new SpotAnimSpawned. id: "+sceneId);
							newSpotAnimSpawned = true;
						}

						hashedEntitys_ThisFrame.add(spotAnim.hashCode());
					}
				}

				perFramePacket.writeShort(playerInstanceId);
				perFramePacket.writeShort(playerX);
				perFramePacket.writeShort(playerY);
				perFramePacket.writeShort(playerHeight);
				perFramePacket.writeShort(playerOrientation);
				perFramePacket.writeInt((config.spawnAnimations() ? animationId : -1));
				perFramePacket.writeShort(animationFrame);
				perFramePacket.writeShort(animationFrameCycle);

				perFramePacket.writeInt(spotAnimationId);
				perFramePacket.writeShort(spotAnimationFrame);
				perFramePacket.writeShort(spotAnimationFrameCycle);
				perFramePacket.writeShort(spotAnimationHeight);
				perFramePacket.writeInt(sceneId);
				perFramePacket.writeBoolean(newSpotAnimSpawned);
			}
		}
		//local player;
		int LocalPlayerIndex = -1;
		if(client.getLocalPlayer()!=null) {LocalPlayerIndex = client.getLocalPlayer().getId();}
		perFramePacket.writeShort(LocalPlayerIndex);


		int noGraphicsObjects = 0;
		if (config.spawnStaticGFX()) {
			for (GraphicsObject graphicsObject : client.getGraphicsObjects()) {
				noGraphicsObjects++;
				if(!hashedEntitys_LastFrame.contains(graphicsObject.hashCode())) {
					System.out.println("graphicsObjSpawn. id: "+graphicsObject.getId());
				}
				hashedEntitys_ThisFrame.add(graphicsObject.hashCode());
			}
		}

		perFramePacket.writeShort(noGraphicsObjects);
		if (noGraphicsObjects>0) {
			for (GraphicsObject graphicsObject : client.getGraphicsObjects())
			{
				if (graphicsObject instanceof RuneLiteObject) {
					System.out.println("encountered runeliteObject");
				}
				int sceneId = graphicsObject.hashCode();
				perFramePacket.writeInt(sceneId);
				short sceneX = (short)graphicsObject.getLocation().getSceneX();
				perFramePacket.writeShort(sceneX);
				short sceneY = (short)graphicsObject.getLocation().getSceneY();
				perFramePacket.writeShort(sceneY);
				short spotAnimId = (short)graphicsObject.getId();
				perFramePacket.writeShort(spotAnimId);
				short animimationFrameIdx = (short)graphicsObject.getAnimationFrame();
				perFramePacket.writeShort(animimationFrameIdx);
				short animimationFrameCycle = (short)-1;
				perFramePacket.writeShort(animimationFrameCycle);
			}
		}




		int noProjectiles = 0;

		if (config.spawnProjectiles()) {
			for (Projectile projectile : client.getProjectiles())
			{
				noProjectiles++;
				hashedEntitys_ThisFrame.add(projectile.hashCode());
			}
		}

		if(config.spawnProjectiles() && noProjectiles > 0) {
			perFramePacket.writeShort(noProjectiles);

			for (Projectile projectile : client.getProjectiles())
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
				Z_target+=(Perspective.getTileHeight(client, new LocalPoint(localX_target, localY_target), client.getPlane())*-1);
				perFramePacket.writeShort(Z_target);

/*				System.out.println("z = "+Z);
				System.out.println("ztarget = " + Z_target);*/

/*				System.out.println("Height: "+projectile.getHeight());
				System.out.println("endHeight: "+projectile.getEndHeight()); //endHeight,relativeToTerrain?
				System.out.println("startHeight: "+projectile.getStartHeight());
				System.out.println("Z: "+projectile.getZ());
				System.out.println("ModelHeight: "+projectile.getModelHeight()); //(offset?)*/

				short spotAnimId = (short)projectile.getId();
				perFramePacket.writeShort(spotAnimId);
				short animimationFrameIdx = (short)projectile.getAnimationFrame();
				perFramePacket.writeShort(animimationFrameIdx);
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
			Map<Long, DynamicObject> gameObjects = getAnimatedGameObjects();
			int NoGameObjects = gameObjects.size();
			perFramePacket.writeInt(NoGameObjects);

			for (Map.Entry<Long, DynamicObject> entry : gameObjects.entrySet()) {
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

		//myRunnableSender.sendBytes(perFramePacket.array, "PerFramePacket");

/*		if (canvasSizeChanged) {
			canvasSizeHasChanged();
		}*/


/*		int worldX = client.getLocalPlayer().getSceneLocation().getX();
		int worldY = client.getLocalPlayer().getSceneLocation().getY();
		int worldZ = client.getLocalPlayer().getWorldLocation().getPlane();

		int camX = client.getCameraX();
		int camY = client.getCameraY();
		int camZ = client.getCameraZ();
		int camYaw = client.getCameraYaw();
		int camPitch = client.getCameraPitch();
		int camZoom = client.getScale();
		int viewWidth = client.getViewportWidth();
		int viewHeight = client.getViewportHeight();
		int viewX = client.getCanvas().getLocationOnScreen().x;
		int viewY = client.getCanvas().getLocationOnScreen().y;

		int playerLocalX = client.getLocalPlayer().getLocalLocation().getX();
		int playerLocalY = client.getLocalPlayer().getLocalLocation().getY();
		int playerZ = Perspective.getTileHeight(client, client.getLocalPlayer().getLocalLocation(), client.getPlane());

		int windowIsFocused = 0;
		if (clientUI.isFocused()) windowIsFocused = 1;


		stringToSend = "0_"+client.getBaseX() + "," + client.getBaseY() + "," + "0" + "_" + playerLocalX + "," + playerLocalY + "," + playerZ + "_" + "0" + "," + camPitch + "," + camYaw + "_" + camX + "," + camY + "," + camZ+"_"+camZoom;
		myRunnableSender.sendMessage(stringToSend);

		lastFrameWorldX = worldX;
		lastFrameWorldY = worldY;
		lastFrameWorldZ = worldZ;*/
	}

	void hashedEntityDespawned(int SceneId) {
			Buffer actorSpawnPacket = new Buffer(new byte[20]);

			actorSpawnPacket.writeByte(6); //write hashedEntity data type
			actorSpawnPacket.writeInt(SceneId);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	void attachUnrealToRl() {
		clientThread.invoke(() -> {
			System.out.println("sending attch command to ue" + client.getCanvas().getLocationOnScreen());
			//sharedmem_rm.ChildRuneModWinToRl();

/*			Buffer buffer = new Buffer(new byte[20]);
			buffer.writeByte(1);
			buffer.writeShort(client.getCanvas().getLocationOnScreen().x);
			buffer.writeShort(client.getCanvas().getLocationOnScreen().y);
			buffer.writeShort(client.getCanvas().getWidth());
			buffer.writeShort(client.getCanvas().getHeight());
			sharedmem_rm.backBuffer.writePacket(buffer, "WindowEvent");*/
		});
	}

	int windowMoved_LastTickCalled = 0;
	@SneakyThrows
	void windowMoved() {
/*		clientThread.invoke(() -> {
			windowMoved_LastTickCalled = client.getGameCycle();
			System.out.println("WindowMoved" + client.getCanvas().getLocationOnScreen());
			sharedmem_rm.updateRmWindow();

*//*			Buffer buffer = new Buffer(new byte[20]);
			buffer.writeByte(0);
			buffer.writeShort(client.getCanvas().getLocationOnScreen().x);
			buffer.writeShort(client.getCanvas().getLocationOnScreen().y);
			sharedmem_rm.backBuffer.writePacket(buffer, "WindowEvent");*//*
		});*/
	}

	@SneakyThrows
	void canvasSizeHasChanged() {
/*		clientThread.invoke(() -> {
			System.out.println("CanvasSizeChanged" + client.getCanvas().getLocationOnScreen());
			sharedmem_rm.updateRmWindow();
*//*
			UpdateSharedMemoryUiPixels();
			Buffer canvasSizeBuffer = new Buffer(new byte[4]);
			canvasSizeBuffer.writeShort(client.getCanvas().getWidth());
			canvasSizeBuffer.writeShort(client.getCanvas().getHeight());
			sharedmem_rm.backBuffer.writePacket(canvasSizeBuffer, "CanvasSizeChanged");
			canvasSizeChanged = false;*//*
		});*/
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
		int curCavansX = client.getCanvas().getLocationOnScreen().x+client.getViewportXOffset();
		int curCavansY = client.getCanvas().getLocationOnScreen().y+client.getViewportYOffset();
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
}


