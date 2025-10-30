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
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ModelData;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientUI;

import javax.inject.Inject;
import java.awt.Container;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.OSType;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;

import java.util.function.Consumer;

import static com.runemod.DivergentStuff.*;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;

@PluginDescriptor(
	name = "RuneMod",
	enabledByDefault = true,
	description = "A graphics plugin",
	tags = {"rm", "rune", "mod", "hd", "graphics", "high", "detail", "graphics", "shaders", "textures", "gpu", "shadows", "lights"},
	conflicts = {"GPU", "117 HD", "GPU (experimental)"}
)

@Slf4j
public class RuneModPlugin extends Plugin implements DrawCallbacks
{
	public enum ClientType {
		UNKNOWN,
		RUNELITE,
		ALORA,
	}

	public enum DataType {
		boolean_,
		boolean_arr_,

		byte_,
		byte_arr_,

		integer_,
		integer_arr_,

		integer64_,
		integer64_arr_,

		string_,
		string_arr_,

		integer_hsv_,
		integer_hsv_arr_,
	}

	public enum RsCache0 {
		ItemDefinitions,
		ObjectDefinitions,
		KitDefinitions,
		AnimationFrames,
		Skeletons,
		SequenceDefinitions,
		NpcDefinitions,
		ModelData,
		Sound,
		Texture,
		Entity,
		UnDefined,
		Tile,
		MaterialDefinition,
		SpotAnimationDefinition,
		WeatherDefinition,
		WeatherTile,
		UnderlayDefinition,
		OverlayDefinition,
		LightDefinition,
		ShapeDefinition,
		PlayerComposition,
		SkeletalFrames,
		Num,
	}

	DivergentStuff divergentStuff = null;

	public static SharedMemoryManager sharedmem_rm = null;

	public static JFrame window = null;

	public static CacheReader myCacheReader;

	public static RuneMod_Launcher runeModLauncher;

	public static RuneMod_LoadingScreen runeMod_loadingScreen;

	public static Client client_static;

	public static RuneModConfig config_static;
	public static boolean runemodLoadingScreenVisibility = false;
	public static JPanel RuneModLoadingScreenPanel;
	public static Container canvasAncestor;
	public static boolean unrealIsReady = false;
	static RuneModPlugin runeModPlugin;
	static boolean isShutDown = false;

	private String[] disAllowedDynamicSpawns_Names = {"obstacle pipe", "rail", "stile", "forest", "fence", "rocks", "shortcut", "low wall", "sparkling pool"};
	private Set<Integer> disAllowedDynamicSpawns = new HashSet<>(); //objdefs in here are not allowed to spawn/despawn except during loading. We have these in order to prevent things like stiles becoming invisible due to being incorporated into the player model. Its bodge, but its the best we can do as we cant tell whether a objdef has been put in a player model, in rl api.
	boolean initedDisallowedDynamicSpawns = false;

	public Set<Long> taggedTileObjects = new HashSet<>();

	public int overlayColor_LastFrame = 0;
	public ApplicationSettings appSettings;
	int sharedMemPixelsUpdatedTick = -1; //used to prevent updating sharedMemory twice in the same tick.

	int ticksSincePluginLoad = 0;

	boolean startedWhileLoggedIn;
	boolean runeModAwaitingRsCacheHashes = false;
	boolean alreadyCommunicatedUnreal = false; //whether we have communicated with unreal this frame.
	int curGpuFlags = -1; //there is no client.setGpuFlags, so I use this to keep track of them myself.
	//int GpuFlagsEnableNo = 17;//DrawCallbacks.GPU | DrawCallbacks.ZBUF
	//int GpuFlagsEnableNo = 3;
	int GpuFlagsEnableNo = 1;
	Set<Renderable> visibleActors = new HashSet<Renderable>();
	Set<WorldPoint> activeChunks = new HashSet<WorldPoint>();

	GameState curGamestate = GameState.STARTING;
	GameState lastGameState = GameState.STARTING;
	volatile int rsUiPosX = 0;
	volatile int rsUiPosY = 0;
	volatile int canvas2DSizeX = 0;
	volatile int canvas2DSizeY = 0;
	volatile int baseOffsetX = 0;
	volatile int baseOffsetY = 0;

	boolean isInstanced;
	int baseX = 0;
	int baseY = 0;

	int lastView3DX = -1;
	int lastView3dY = -1;
	int lastView3dSizeX = -1;
	int lastView3dSizeY = -1;
	int lastCanvas2DSizeX = -1;
	int lastCanvas2DSizeY = -1;
	int lastBaseOffsetX = -1;
	int lastBaseOffsetY = -1;

	private int clientPlane_prevFrame = -1; //used to track when plane has changed
	private Set<Integer> hashedEntitys_LastFrame = new HashSet<Integer>(); //used to track spawns/despawnes of entities.

	boolean mappedMaskedAnims = false;
	//int[] knownMaskedAnimIds = {7592, 7593, 7949, 7950, 7951, 7952, 7957, 7960, 8059, 8123, 8124, 8125, 8126, 8127, 8234, 8235, 8236, 8237, 8238, 8241, 8242, 8243, 8244, 8245, 8248, 8249, 8250, 8251, 8252, 8255, 8256, 8257, 8258};
	int[] knownMaskedAnimIds = {}; //did rl do away with animation masking. seems like it.
	HashMap<Integer, Integer> ObbedAnim_deobedAnim_Map = new HashMap<>();
	Field field_animation = null;

	@Inject
	public Client client;

	@Inject
	private EventBus eventBus;

	@Inject
	public ClientThread clientThread;

	@Inject
	public RuneModConfig config;

	@Inject
	public ClientUI clientUI;


	@Inject
	private DrawManager drawManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private Hooks hooks;

	@Inject
	public Gson gson;

	public static float getCurTimeSeconds()
	{
		float curT = (float) (((double) System.currentTimeMillis() / 1000.0D) - (1739538139D));
		return curT;
	}

	public static void log_Timed_Heavy(String message)
	{
		if (config_static.HeavyLogging())
		{
			//log.debug("[" + getCurTimeSeconds() + "]	" + message);
			log.debug("[" + (System.currentTimeMillis() - 1745838397221L) + "]	" + message);
		}
	}

	long timeLastInteracted = Long.MAX_VALUE;

	MouseListener mouseListener = new MouseAdapter()
	{
		@Override
		public void mousePressed(MouseEvent mouseEvent)
		{
			setMaxFps(config.MaxFps());
			timeLastInteracted = System.currentTimeMillis();
			//resizes canvas in order to force it to repaint. This prevents the loss of transparency on the loginscreens background pixels, caused by going to the world select screen.
			if (client.getGameState() == GameState.LOGIN_SCREEN || curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR)
			{
				clientThread.invokeAtTickEnd(() ->
				{
					log.debug("resizing canvas because mouse pressed");
					client.resizeCanvas();
				});
			}
		}

		@Override
		public void mouseEntered (MouseEvent mouseEvent)
		{
			setMaxFps(config.MaxFps());
			timeLastInteracted = System.currentTimeMillis();
		}
	};

	KeyListener keyListener = new KeyAdapter()
	{
		@Override
		public void keyPressed(KeyEvent KeyEvent)
		{
			setMaxFps(config.MaxFps());
			timeLastInteracted = System.currentTimeMillis();
		}
	};

	public void registerMouseListener()
	{
		client.getCanvas().addMouseListener(mouseListener);
		client.getCanvas().addKeyListener(keyListener);
	}

	public void unRegisterMouseListener()
	{
		client.getCanvas().removeMouseListener(mouseListener);
		client.getCanvas().removeKeyListener(keyListener);
	}

	public static void toggleRuneModLoadingScreen(Boolean toggled)
	{
		if (runemodLoadingScreenVisibility == toggled)
		{
			return;
		}
		runemodLoadingScreenVisibility = toggled;
		log.debug("toggling RmLoadingScreen to " + toggled);
		//SwingUtilities.invokeLater(() -> //perhaps causing some shut-down related issue sometimes where the loading screen doesnt toggle?
		//{
			if (canvasAncestor == null)
			{
				return;
			}

			JRootPane root = SwingUtilities.getRootPane(canvasAncestor);
			JComponent glass = (JComponent) root.getGlassPane();

			if (toggled)
			{
				RuneModLoadingScreenPanel = new JPanel();
				BorderLayout ogLayout = (BorderLayout) canvasAncestor.getLayout();
				BorderLayout copyLayout = new BorderLayout(ogLayout.getHgap(), ogLayout.getVgap());

				RuneModLoadingScreenPanel.setOpaque(false); //required so as not to cover whole client in black
				RuneModLoadingScreenPanel.add(runeMod_loadingScreen);

				glass.setOpaque(false);

				glass.setLayout(copyLayout);
				glass.add(RuneModLoadingScreenPanel);
				glass.setVisible(true); //temporarily disabled loading screen visibility for debug purposes

				if (window != null) {
					window.invalidate();
					window.validate();
					window.repaint();
				}
			}
			else
			{
				glass.setVisible(false);
				glass.remove(RuneModLoadingScreenPanel);
				RuneModLoadingScreenPanel = null;
				glass.revalidate();
				glass.repaint();
				root.revalidate();
				root.repaint();
			}
		//});
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

	public void muteLoginScreenMusic(boolean mute)
	{
		//if(true) {
			//return;
		//}

		javax.sound.sampled.Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		for (int i = 0; i < mixers.length; i++)
		{
			Mixer.Info mixerInfo = mixers[i];
			// System.out.println("Mixer Name:" + mixerInfo.getName());
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			Line.Info[] lineinfos = mixer.getTargetLineInfo();
			for (Line.Info lineinfo : lineinfos)
			{
				// System.out.println("line:" + lineinfo);
				Line line = null;
				try
				{
					line = mixer.getLine(lineinfo);
					if (line != null)
					{
						line.open();
						if (line.isControlSupported(BooleanControl.Type.MUTE))
						{
							BooleanControl bc = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
							if (bc != null)
							{
								//System.out.println(line.getLineInfo().toString());
								if (line.getLineInfo().toString().contains("SPEAKER target"))
								{
									bc.setValue(mute); // true to mute the line, false to unmute
									// Implement logic to manage audio settings or mute state
								}
							}
						}
					}
				}
				catch (LineUnavailableException e)
				{
					// e.printStackTrace();
				}
				finally
				{
					if (line != null && line.isOpen())
					{
						line.close(); // Ensure the line is closed after use
					}
				}
			}
		}
	}

	int getAnimation_Unmasked(NPC npc)
	{
		return npc.getAnimation();
/*		try
		{
			if (field_animation == null)
			{
				return npc.getAnimation();
			}

			int animationVal_Obbed = field_animation.getInt(npc); //obfuscated anim val
			Integer deobbedVal = ObbedAnim_deobedAnim_Map.get(animationVal_Obbed);
			if (deobbedVal != null)
			{
				//System.out.println("unmasked anim "+deobbedVal);
				return deobbedVal;
			}
			else
			{
				return npc.getAnimation();
			}
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
			return npc.getAnimation();
		}*/
	}

	@SneakyThrows
	void mapObfuscatedAnimValues()
	{

	}

	int loggedInForNoServerTicks = 0; //used to disallow certain dynamic spawns while logged in, which we do as a way to prevent objects such as stiles despawning when you hop them.


	private static final int CHUNK_SIZE = Constants.CHUNK_SIZE; //8

	public Tile getExtendedSceneTileFromWorldPoint(WorldPoint worldPoint)
	{
		if (client == null || worldPoint == null)
		{
			return null;
		}

		int plane = worldPoint.getPlane();
		if (plane < 0 || plane >= 4)
		{
			return null;
		}

		WorldPoint base = new WorldPoint(client.getBaseX(), client.getBaseY(), worldPoint.getPlane());
		if (base == null)
		{
			return null;
		}

		// Convert world coordinates to extended scene coordinates
		int sceneX_extended = worldPoint.getX() - base.getX() + SCENE_OFFSET;
		int sceneY_extended = worldPoint.getY() - base.getY() + SCENE_OFFSET;


		Tile[][][] tiles = client.getScene().getExtendedTiles();
		int arrXLen = tiles[0].length;
		int arrYLen = tiles[0][0].length;
		if (sceneX_extended < 0 || sceneX_extended >= arrXLen || sceneY_extended < 0 || sceneY_extended >= arrYLen)
		{
			return null;
		}

		return tiles[plane][sceneX_extended][sceneY_extended];
	}

	public void simulateSpawnEventsForChunk(WorldPoint chunkBase)
	{
		log.debug("spawning extended tiles for chunk:  " + chunkBase);

		int baseX = chunkBase.getX();
		int baseY = chunkBase.getY();

		eventIsSimulation = true;
		//for each tile in chunk
		for (int dx = 0; dx < CHUNK_SIZE; dx++)
		{
			for (int dy = 0; dy < CHUNK_SIZE; dy++)
			{
				for (int plane = 0; plane < 4; plane++)
				{
					WorldPoint tileWorldPoint = new WorldPoint(baseX + dx, baseY + dy, plane);
					Tile tile = getExtendedSceneTileFromWorldPoint(tileWorldPoint);
					simulateTilObjectSpawns(tile);
				}
			}
		}
		eventIsSimulation = false;
	}

	int SUBREGION_SIZE = 16;

	public void simulateSpawnEventsForSubRegion(WorldPoint subRegionBase)
	{
		log.debug("spawning extended tile objects for chunk:  " + subRegionBase);

		int baseX = subRegionBase.getX();
		int baseY = subRegionBase.getY();

		eventIsSimulation = true;
		//for each tile in chunk
		for (int dx = 0; dx < SUBREGION_SIZE; dx++)
		{
			for (int dy = 0; dy < SUBREGION_SIZE; dy++)
			{
				for (int plane = 0; plane < 4; plane++)
				{
					WorldPoint tileWorldPoint = new WorldPoint(baseX + dx, baseY + dy, plane);
					Tile tile = getExtendedSceneTileFromWorldPoint(tileWorldPoint);
					simulateTilObjectSpawns(tile);
				}
			}
		}
		eventIsSimulation = false;
	}

	void send_DespawnChunk_Packet(WorldPoint chunkBase) { //generally just used to despawn extended chunks.
		//System.out.println("despawning chunk at:  " + chunkBase);
		if (!activeChunks.contains(chunkBase))
		{
			return;
		}
		activeChunks.remove(chunkBase);
		Buffer actorSpawnPacket = new Buffer(new byte[20]);
		actorSpawnPacket.writeByte(7); //write chunk data type

		actorSpawnPacket.writeShort(chunkBase.getX());
		actorSpawnPacket.writeShort(chunkBase.getY());
		actorSpawnPacket.writeByte(0);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	boolean send_SpawnChunk_Packet(WorldPoint chunkBase)
	{ //generally just used to despawn extended chunks.
		if (activeChunks.contains(chunkBase))
		{
			return false;
		}

		if(chunkBase.getX()%16!=0 || chunkBase.getY()%16!=0) {return false;} //we only care about subregion, which are every 16 tiles

		//System.out.println("spawning chunk at:  " + chunkBase);
		activeChunks.add(chunkBase);
		Buffer actorSpawnPacket = new Buffer(new byte[20]);
		actorSpawnPacket.writeByte(7); //write chunk data type

		actorSpawnPacket.writeShort(chunkBase.getX());
		actorSpawnPacket.writeShort(chunkBase.getY());
		actorSpawnPacket.writeByte(0);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		return true;
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if(config.reduceFpsWhenIdle()) {
			if(storedMaxFps != 50 && client.getTickCount()%6 == 0) {
				if(System.currentTimeMillis() - timeLastInteracted > 5000) { //reduce fps over time if window appears to be inactive/unused. Prevents overwoking pc when rs is idle.
					log.debug("using lower fps lock because afk");
					setMaxFps((int)((float)client.getFPS()*0.8));
				}
			}
		}

		//spawn stuff on extended tiles. does not yet handle despawn

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			loggedInForNoServerTicks++;
		}
		else
		{
			loggedInForNoServerTicks = 0;
		}
	}

	void processExtendedChunkSpawnTask()
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		//ArrayList<WorldPoint> spawnedChunks = new ArrayList();
		WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();

		for (int dx = -config.ExtraChunksLoadDistance(); dx <= config.ExtraChunksLoadDistance(); dx++)
		{
			for (int dy = -config.ExtraChunksLoadDistance(); dy <= config.ExtraChunksLoadDistance(); dy++)
			{
				int playerChunkX = playerLoc.getX() / CHUNK_SIZE;
				int playerChunkY = playerLoc.getY() / CHUNK_SIZE;

				int chunkX = playerChunkX + dx;
				int chunkY = playerChunkY + dy;

				WorldPoint chunkBase = new WorldPoint(chunkX * CHUNK_SIZE, chunkY * CHUNK_SIZE, 0);

				int chunkBaseX = chunkBase.getX();
				int chunkBaseY = chunkBase.getY();

				int sceneX = chunkBaseX - client.getBaseX();
				int sceneY = chunkBaseY - client.getBaseY();

				boolean isInMainScene = (sceneX >= 0 - 8 && sceneX < 104 && sceneY >= 0 - 8 && sceneY < 104);

				if (!isInMainScene)
				{
					//simulateSpawnEventsForChunk(chunkBase);
					boolean spawnedChunk = send_SpawnChunk_Packet(chunkBase);
					if (spawnedChunk)
					{
						return;
					}
					else
					{
						continue;
					}
					//spawnedChunks.add(chunkBase);
					//if(spawnedChunks.size() >= 4) {
					//activeExtendedChunks.addAll(spawnedChunks);
					//}
				}
			}
		}
		//activeExtendedChunks.addAll(spawnedChunks);
	}

	void processExtendedChunkDespawnTask() //find chunks that need to be despawned and despawns them.
	{
		if (activeChunks.isEmpty())
		{
			return;
		}
		WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();

		WorldPoint[] activeChunksArr = activeChunks.toArray(WorldPoint[]::new);
		for (WorldPoint chunkBase : activeChunksArr)
		{
			//if(chunkBase.getX()%16!=0 || chunkBase.getY()%16!=0) {continue;}

			int sceneX = chunkBase.getX() - client.getBaseX();
			int sceneY = chunkBase.getY() - client.getBaseY();
			boolean isInMainScene = (sceneX >= 0 - 8 && sceneX < 104 && sceneY >= 0 - 8 && sceneY < 104);

			int playerChunkX = playerLoc.getX() / CHUNK_SIZE;
			int playerChunkY = playerLoc.getY() / CHUNK_SIZE;

			int chunkX = chunkBase.getX() / CHUNK_SIZE;
			int chunkY = chunkBase.getY() / CHUNK_SIZE;
			boolean isInRange = Math.abs(playerChunkX - chunkX) <= config.ExtraChunksLoadDistance() && Math.abs(playerChunkY - chunkY) <= config.ExtraChunksLoadDistance();

			if (!isInRange && !isInMainScene)
			{
				send_DespawnChunk_Packet(chunkBase);
				//DespawnedChunks.add(chunkBase);
				return;
			}
		}

		//activeChunks.removeAll(DespawnedChunks);
	}

	int ticksSinceLoadScene = 0;

	boolean isCacheFullyLoaded = false;

	private void collectVisibleJLabels(Component component, List<JLabel> labels)
	{
		if (!component.isVisible())
		{
			return;
		}

		if (component instanceof JLabel)
		{
			labels.add((JLabel)component);
		} else {
			if (component instanceof Container)
			{
				for (Component child : ((Container)component).getComponents())
				{
					collectVisibleJLabels(child, labels);
				}
			}
		}
	}

	private boolean isRuneModSettingsVisible(Component component)
	{
		if (component == null || !component.isVisible())
		{
			return false;
		}

		// Check current component
		if (component instanceof JLabel)
		{
			if ("Settings (RuneMod)".equals(((JLabel)component).getText()))
			{
				return true;
			}
		}

		// Recurse into children
		if (component instanceof Container)
		{
			for (Component child : ((Container)component).getComponents())
			{
				if (isRuneModSettingsVisible(child))
				{
					return true;
				}
			}
		}

		// Not found in this branch
		return false;
	}

	void RmUiPacket_SettingsWindow() {
		Buffer packet = new Buffer(new byte[8]);

		packet.writeByte(1); //UiType: SettingsWindow
		packet.writeBoolean(isRuneModSettingsOpen_PrevFrame); //isUiOpen

		sharedmem_rm.backBuffer.writePacket(packet, "RmUi");
	}

	boolean isRuneModSettingsOpen_PrevFrame = false;
	Canvas canvas_prevFrame;

	long gameCycle_50fps;//usefull when using gamecycle with modulo as a way to time things. gameCycle would be inaccurate because it can increase at more than 50fps if user has changed fps value.

	@SneakyThrows
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		log_Timed_Heavy("onBeforeRender");

		gameCycle_50fps = System.currentTimeMillis()/20;

		alreadyCommunicatedUnreal = false;

		if (client.getGameState().ordinal() >= GameState.STARTING.ordinal() && isPastModeChooser()) //changed for alora so that runemod does not start until we are past settings screen.
		{
			ticksSincePluginLoad++;
		}

		ticksSinceLoadScene++;

		if (ticksSincePluginLoad == 1)
		{
			startUp_Custom();

			if (startedWhileLoggedIn)
			{
				return;
			}

			runeMod_loadingScreen.SetStatus_DetailText("Starting...", true);

			runeModLauncher.launch();
		}

		if (ticksSincePluginLoad <= 1)
		{
			return;
		}

		if (startedWhileLoggedIn)
		{
			return;
		}

		if (config.OrbitCamera())
		{
			client.setCameraYawTarget(client.getCameraYaw() + 1);
		}

		mapObfuscatedAnimValues();

		//check if rscache is currently being updated.
		if (!isCacheFullyLoaded && client.getGameState().ordinal() >= GameState.LOGIN_SCREEN.ordinal() && client.getGameCycle() % 20 == 0)
		{
			isCacheFullyLoaded = divergentStuff.isCacheFullyLoaded();

			if (isCacheFullyLoaded)
			{
				log.debug("RSCache has finished downloading");
				runeMod_loadingScreen.SetStatus_DetailText("Downloaded RS cache", true);
				myCacheReader.initCacheStore(); //reload cache store, because new indexes may have been downloaded since it was initialized
			}
			else
			{
				runeMod_loadingScreen.SetStatus_DetailText("Downloading RS cache...", true);
			}
		}

		if (isCacheFullyLoaded && runeModAwaitingRsCacheHashes)
		{ //provide rscache hashes, if runemod is waiting for them
			runeModAwaitingRsCacheHashes = false;
			clientThread.invokeAtTickEnd(() -> {
				myCacheReader.provideRsCacheHashes();
			});
		}

/*		if(canvas_prevFrame!=client.getCanvas()) {
			if(config.RuneModVisibility() == true) {
				sharedmem_rm.SetWindowZOrders();
			}
			canvas_prevFrame = client.getCanvas();
		}*/

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
					if (curGpuFlags != GpuFlagsEnableNo)
					{
						communicateWithUnreal("onBeforeRender_TickEnd"); //here to set rm visibility before gpu flags get set. we do this to prevent momentarily showing unrendered client before rm visibility is set to true;
						setGpuFlags(GpuFlagsEnableNo);
					}
				}
			});
		}

		if((window.getExtendedState() & window.ICONIFIED) != 0) { //reduces cpu usage and gpu usage when minimized
			if(client.getGameCycle()%10 == 0) {
				log.debug("WindowIsMinimized, sleeping...");
			}

			Thread.sleep(60);
		}

		clientThread.invokeAtTickEnd(() -> {
			if (ticksSinceLoadScene > 300 && !isInstanced)
			{
				if (gameCycle_50fps % 10 == 0)
				{
					processExtendedChunkSpawnTask();

				}
				if ((gameCycle_50fps-4) % 10 == 0) //staggers despawn so it happens a few frames after spawn task
				{
					processExtendedChunkDespawnTask();
				}
			}
		});


		if(client.getLocalPlayer()!=null){
			calcWorldVisibility();
		}

		//}

		drawManager.processDrawComplete(this::screenshot);

		//check if settings panel is open for runemod. if yes, open the settings window in runemod.
/*		if(unrealIsReady && ticksSincePluginLoad%2 == 0)
		{
			SwingUtilities.invokeLater(() ->
			{
				//long timeBefore = System.nanoTime();
				boolean isRuneModSettingsOpen = isRuneModSettingsVisible(client.getCanvas().getParent().getParent().getParent());
				//long timeAfter = System.nanoTime();
				//System.out.println("timeTaken: "+(timeAfter-timeBefore));
				if(isRuneModSettingsOpen_PrevFrame!=isRuneModSettingsOpen) {
					log.debug("RmSettings Open = "+isRuneModSettingsOpen);
					clientThread.invoke(() -> {
						isRuneModSettingsOpen_PrevFrame = isRuneModSettingsOpen;
						RmUiPacket_SettingsWindow();
					});
				}
			});
		}*/
	}

	private Image screenshot()
	{
		log.debug("RuneMod Screenshot");

		Robot robot = null;
		try
		{
			robot = new Robot();
		}
		catch (AWTException e)
		{
			e.printStackTrace();
		}

		Component c = client.getCanvas().getParent();

		// Get viewport size and absolute position
		Rectangle rect = c.getBounds();
		java.awt.Point loc = c.getLocationOnScreen();
		rect.setLocation(loc);

		// Capture the full screen
		BufferedImage image = robot.createScreenCapture(rect);

		return image;
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

		//no longer needed because uidisplayer and runemod window size is always max monitor size.

		sharedmem_rm.updateRmWindowTransform();


		sharedmem_rm.setRuneModVisibility(config.RuneModVisibility() == true);
	}

	void disableRuneModPlugin()
	{
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

	boolean hasLostRuneModWindow()
	{
		if (sharedmem_rm.RuneModHandle != null && !sharedmem_rm.isRuneModHandleValid())
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	int noTimeOutsSinceLastUeCom = 0;

	void communicateWithUnreal(String funcLocation)
	{

		if (alreadyCommunicatedUnreal)
		{
			log_Timed_Heavy("Already communicated. cancelled communicateWithUnreal::" + funcLocation);
			return;
		}

		if (config.disableUeCom())
		{
			return;
		}

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

		alreadyCommunicatedUnreal = true;

		if (config.increaseTimerResolution())
		{
			sharedmem_rm.SetTimeBeginPeriod();
		}

		log_Timed_Heavy("communicateWithUnreal::" + funcLocation);


		//if (client.getGameCycle() % 4 == 0)
		//{
			//SwingUtilities.invokeLater(() -> {
			MaintainRuneModAttachment();
			//});
		//}

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
		while (true)
		{
			int timeOut = 0;

			boolean useLockStep = false; //lockstep. prevents runelite from going to next frame until unreal has acknowledged the current one.
			if (config.lockStep())
			{
				useLockStep = true;
			}
			if (ticksSinceLoadScene < 10)
			{
				useLockStep = true;
			}
			if (!unrealIsReady)
			{
				useLockStep = true;
			}

			if (useLockStep == true)
			{
				timeOut = 20000;
				//System.out.println("using locksetp");
			}

			int val = sharedmem_rm.myKernel32.WaitForSingleObject(sharedmem_rm.EventUeDataReady, timeOut);

			//if wait timed out
			if (val == 258)
			{
				noTimeOutsSinceLastUeCom++;
				log.debug("wait timeOut, noTimeOuts: " + noTimeOutsSinceLastUeCom);

				if (useLockStep)
				{
					if (hasLostRuneModWindow())
					{
						log.debug("lockstep timeout, and lost runemod window");
						disableRuneModPlugin();  //we do this so as to keep the client useable even if rm crashes.
						return;
					}
					else
					{
						continue; //continue waiting for runemod. the wait/loop could be infinite, aslong as the runemod window exists.
					}
				}
				else
				{
					if (noTimeOutsSinceLastUeCom > 40)
					{
						if (hasLostRuneModWindow())
						{
							log.debug("nonlockstep timeout, and lost runemod window");
							disableRuneModPlugin(); //we do this so as not to lockup the client when rm crashes.
						}
					}
					return;
				}
			}
			else
			{
				noTimeOutsSinceLastUeCom = 0;
				log_Timed_Heavy("1 awaitUeDataIsReady = " + val);
				break;
			}
		}


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

	void setGpuFlags(int flags)
	{
		if (curGpuFlags != flags)
		{
			client.setGpuFlags(flags);
			client.getCanvas().setIgnoreRepaint(true);
			client.resizeCanvas(); //resize canvas to force rebuild for working alpha channel
			curGpuFlags = flags;
			log.debug("GPU Flags have been changed to " + flags);
		}
	}

	//HashMap<Long, DynamicObject> animatedDynamicObjects = new HashMap<Long, DynamicObject>(); //testing

	Set<Tile> tilesWithAnimateGameObjects = new HashSet<>();
	Set<TileObject> AnimatedTileObjects = new HashSet<>();

	int[] npcHeights = new int[65535];
	int[] playerHeights = new int[65535];

/*	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ)
	{
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
	}*/

	@SneakyThrows
	@Override
	public void draw(int overlayColor)
	{
		if(config.nullifyDrawCallbacks()) {return;}

		if (overlayColor_LastFrame != overlayColor)
		{
			overlayColor_LastFrame = overlayColor;
			//overlayColourChanged();
		}

		log_Timed_Heavy("draw");
		UpdateSharedMemoryUiPixels();
		communicateWithUnreal("Draw");
	}

	@SneakyThrows
	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		if(config.nullifyDrawCallbacks()) {return;}

		client.getTopLevelWorldView().getScene().setDrawDistance(90);

		if(curGpuFlags == 17) {//code for zbuff gpu mode
			for(Player obj: client.getTopLevelWorldView().players()) {
				visibleActors.add(obj);
				playerHeights[obj.getId()] = 0;
			}
			for(NPC obj: client.getTopLevelWorldView().npcs()) {
				visibleActors.add(obj);
			}
			for(Projectile obj: client.getProjectiles()) {
				visibleActors.add(obj);
			}
			for(GraphicsObject obj: client.getGraphicsObjects()) {
				visibleActors.add(obj);
			}
		}

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
		if (!alreadyCommunicatedUnreal)
		{
			communicateWithUnreal("drawScene");
			visibleActors.clear();
			tilesWithAnimateGameObjects.clear();
			AnimatedTileObjects.clear();
		}
	}

	@SneakyThrows
	@Override
	public void postDrawScene()
	{
		if(config.nullifyDrawCallbacks()) {return;}

		log_Timed_Heavy("postDrawScene");
	}
	//if npc in curfram has overrides, and it didnt in last frame, overridesChanged.
	//if npcsWithOverrides in last frame is missing from npcsWithOverrides in currentFrame, overridesChanged.
	//if npc With Overrides is present in last and current frame, check if the overrides from each frame are equal. if they are not, , overridesChanged.

/*	@Override
	public void animate(Texture texture, int diff)
	{
	}*/


	/**
	 * This event is run from the maploader thread prior to the map load completing.
	 * Most client operations can't be done from this thread safely.
	 * You probably don't want to use this event.
	 */
/*	@Subscribe
	public void onPreMapLoad(PreMapLoad preMapLoad)
	{
		System.out.println("preMapLoad. baseX:" +preMapLoad.getScene().getBaseX());
	}*/

	void DespawnServerSpawnedObjs() {
		for(GameObjectSpawned event: serverSpawnedGameObjects) {
/*			if(event.getGameObject()!=null) {
				System.out.println("despawning serverSpawnedObj: "+event.getGameObject().getId());
			}*/
			GameObjectDespawned despawnEvent = new GameObjectDespawned();
			despawnEvent.setTile(event.getTile());
			despawnEvent.setGameObject(event.getGameObject());
			onGameObjectDespawned(despawnEvent);
		}
		serverSpawnedGameObjects.clear();
	}

	Set<WorldView> worldViews = new HashSet<>();
	@Override
	public void loadScene(WorldView worldView, Scene scene)
	{
		clientThread.invoke(() -> //invoke on client thread because might be dangerous doing stuff on maploader thread.
		{
			log.debug("loadScene. worldView id:" + worldView.getId() + " baseX:" + worldView.getBaseX() + "baseY:" + worldView.getBaseY());
			worldViews.add(worldView);
		});
	}

	void sendSceneBaseInfo(int baseX, int baseY, Scene scene) { //sends base coordinate and chunk spawn packets
		//if the scene is instanced we have to destroy all old terrains. we do this because the ones around the scene edges will have some blank tiles which were not included in the instance-map when those terrains were last generated.
		ticksSinceLoadScene = 0;
		log.debug("sendSceneBaseInfo");

		if(isInstanced) {
			log.debug("despawning all terrains because is instanced area");
			WorldPoint[] activeChunksArr = activeChunks.toArray(WorldPoint[]::new);
			for(WorldPoint chunkBase : activeChunksArr) {
				send_DespawnChunk_Packet(chunkBase);
			}
		}

		DespawnServerSpawnedObjs();
		//perhaps we need to do this for wall objects too?
		//despawn any gameobjects spawned by server. This fixes the scenario where a player leaves an area with player lit fires, and then comes back to it to find fire still there when they should be despawned.


		if(config.nullifyDrawCallbacks()) {return;}
		sendBaseCoordinatePacket(baseX, baseY, scene); //sends basecoordinate and instance map

		//spawn chunks in scene
		for (int x = 0 - 1; x < (Constants.SCENE_SIZE / Constants.CHUNK_SIZE) + 1; ++x)
		{
			for (int y = 0 - 1; y < (Constants.SCENE_SIZE / Constants.CHUNK_SIZE) + 1; ++y)
			{
				int chunkBaseX = (x * Constants.CHUNK_SIZE) + baseX;
				int chunkBaseY = (y * Constants.CHUNK_SIZE) + baseY;
				WorldPoint chunkBase = new WorldPoint(chunkBaseX, chunkBaseY, 0);

				if (chunkBase.getX() % 16 != 0 || chunkBase.getY() % 16 != 0)
				{
					continue;
				}

				send_SpawnChunk_Packet(chunkBase);
			}
		}
	}

	volatile boolean needSendBaseInfo = false;
	volatile Scene baseInfoScene = null;
	@Override
	public void loadScene(Scene scene)
	{
		if(divergentStuff.clientType == ClientType.RUNELITE) { //alora uses onLoadRegion instead, because it is single threaded as has no async map load.
			needSendBaseInfo = true;
			baseInfoScene = scene;
			log.debug("loadScene");
		}
	}

	private static final int GROUND_MIN_Y = 350; // how far below the ground models extend
	@Override
	public boolean tileInFrustum(Scene scene, float pitchSin, float pitchCos, float yawSin, float yawCos, int cameraX, int cameraY, int cameraZ, int plane, int msx, int msy)
	{
		if(config.disableFrustrumTileCulling()) { return true; }
		int[][][] tileHeights = scene.getTileHeights();
		int x = ((msx - SCENE_OFFSET) << Perspective.LOCAL_COORD_BITS) + 64 - cameraX;
		int z = ((msy - SCENE_OFFSET) << Perspective.LOCAL_COORD_BITS) + 64 - cameraZ;
		int y = Math.max(
			Math.max(tileHeights[plane][msx][msy], tileHeights[plane][msx][msy + 1]),
			Math.max(tileHeights[plane][msx + 1][msy], tileHeights[plane][msx + 1][msy + 1])
		) + GROUND_MIN_Y - cameraY;

		int radius = 96; // ~ 64 * sqrt(2)

		int zoom = client.get3dZoom();
		int Rasterizer3D_clipMidX2 = client.getRasterizer3D_clipMidX2();
		int Rasterizer3D_clipNegativeMidX = client.getRasterizer3D_clipNegativeMidX();
		int Rasterizer3D_clipNegativeMidY = client.getRasterizer3D_clipNegativeMidY();

		float var11 = yawCos * z - yawSin * x;
		float var12 = pitchSin * y + pitchCos * var11;
		float var13 = pitchCos * radius;
		float depth = var12 + var13;
		if (depth > 50)
		{
			float rx = z * yawSin + yawCos * x;
			float var16 = (rx - radius) * zoom;
			float var17 = (rx + radius) * zoom;
			// left && right
			if (var16 < Rasterizer3D_clipMidX2 * depth && var17 > Rasterizer3D_clipNegativeMidX * depth)
			{
				float ry = pitchCos * y - var11 * pitchSin;
				float ybottom = pitchSin * radius;
				float var20 = (ry + ybottom) * zoom;
				// top
				if (var20 > Rasterizer3D_clipNegativeMidY * depth)
				{
					// we don't test the bottom so we don't have to find the height of all the models on the tile
					return true;
				}
			}
		}
		return false;
	}

	public static Point toExtendedSceneCoordinate(int sceneX, int sceneY)
	{
		return new Point(sceneX + SCENE_OFFSET, sceneY + SCENE_OFFSET);
	}

	public static Point toNonExtendedSceneCoordinate(int extendedSceneX, int extendedSceneY)
	{
		return new Point(extendedSceneX - SCENE_OFFSET, extendedSceneY - SCENE_OFFSET);
	}

	@Override
	public void swapScene(Scene scene)
	{
		if(config.nullifyDrawCallbacks()) {return;}
		log.debug("SwapScene");
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

	void enableLoginFire(boolean enable) {
		if(clientType == ClientType.RUNELITE) { //(Alora doesnt use login fire)
			RuneModPlugin.runeModPlugin.client.setShouldRenderLoginScreenFire(enable);
		}
	}

	void setDefaults()
	{
		log.debug("setting defaults");

		needSendBaseInfo = false;
		baseInfoScene = null;

		isRuneModSettingsOpen_PrevFrame = false;

		powPrevFrame = -1;
		removeRoofsPrevFrame = false;

		canvasAncestor = null;

		storedMaxFps = -1;

		lastBaseOffsetX = -1;
		lastBaseOffsetY = -1;

		FashionScape_EquipmentIds_PrevFrame = null;
		FashionScape_Colors_PrevFrame = null;

		playerLocation_prevTick = new WorldPoint(0,0,0);

		window = (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas());

		activeChunks.clear();

		taggedTileObjects.clear();

		varbits = new int[30000];
		varps = new int[10000];

		client.setLoginScreen(null);
		enableLoginFire(true);

		CacheReader.cacheFullyLoaded= false;

		runeModAwaitingRsCacheHashes = false;

		initedDisallowedDynamicSpawns = false;

		startedWhileLoggedIn = false;

		clientPlane_prevFrame = -1;

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
		//client.setUnlockedFpsTarget(50);

		lastView3DX = 0;
		lastView3dY = 0;
		lastView3dSizeX = 0;
		lastView3dSizeY = 0;

		curGpuFlags = -1;
	}

	void setDrawCallbacks(DrawCallbacks drawCallbacks)
	{
		client.setDrawCallbacks(drawCallbacks);

		if (drawCallbacks == null)
		{
			log.debug("Changed DrawCallbacks To Null");
		}
		else
		{
			log.debug("Changed DrawCallbacks");
		}
	}

	@SneakyThrows
	void startUp_Custom()
	{
		runeModPlugin = this;

		setDefaults();

		CheckUePreReqs();

		if (client.getGameState().ordinal() > GameState.LOGIN_SCREEN_AUTHENTICATOR.ordinal())
		{
			startedWhileLoggedIn = true;

			disableRuneModPlugin();
			SwingUtilities.invokeLater(() ->
			{
				int response = JOptionPane.showConfirmDialog(null,
					"To enable RuneMod, you must first logout",
					"RuneMod Error",
					JOptionPane.DEFAULT_OPTION);
			});

			return;
		}else {
			startedWhileLoggedIn = false;
		}

		SwingUtilities.invokeLater(() ->
		{
			SwingUtilities.invokeLater(() ->
			{
				muteLoginScreenMusic(!unrealIsReady);
			});
		});

		canvasAncestor = client.getCanvas().getParent().getParent(); //is "clientPannel" class

		myCacheReader = new CacheReader(getCachePath());

		registerMouseListener();

		sharedmem_rm = new SharedMemoryManager(this);
		sharedmem_rm.createSharedMemory("sharedmem_rm", 50000000); //50 mb
		sharedmem_rm.CreateNamedEvents();
		sharedmem_rm.write_Rl_hwnd_ToSharedMem();

		client.getCanvas().getParent().getParent().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				//log.debug("window resized");
			}
			@Override
			public void componentMoved(ComponentEvent e) {
				//log.debug("window mover");
			}
		});

		runeMod_loadingScreen = new RuneMod_LoadingScreen(window, this);
		//window.setIgnoreRepaint(true); //doing this could save a bit of performance however it causes visual issues in runelite.


		runeModLauncher = new RuneMod_Launcher(config.UseAltRuneModLocation() ? config.AltRuneModLocation() : "", config.StartRuneModOnStart());

		isShutDown = false;

		RuneModPlugin.toggleRuneModLoadingScreen(true);

		configManager.setConfiguration("stretchedmode", "keepAspectRatio", true); //We need keepAspectRatio enabled, because runemod does not support nonuniform canvas scaling.

		log.debug("runelitDir: " + RUNELITE_DIR);

		{
			sharedmem_rm.startNewRsData();
			sharedmem_rm.transferBackBufferToSharedMem();
			sharedmem_rm.passRsDataToUnreal();
			sharedmem_rm.myKernel32.SetEvent(sharedmem_rm.EventRlDataReady); //signal rl  data is ready. either ue or rl has to signal it is ready for the back and forth communication to begin
		}

		//int GpuFlags = DrawCallbacks.GPU | (computeMode == ComputeMode.NONE ? 0 : DrawCallbacks.HILLSKEW);
		client.setExpandedMapLoading(2);

		setMaxFps(config.MaxFps());

		setGpuFlags(0);
		setDrawCallbacks(this);
	}

	int storedMaxFps = -1;
	public void setMaxFps(int maxFps) {
		if (maxFps < 50) { maxFps = 50; }
		if(storedMaxFps!=maxFps) {
			if(maxFps > 50) {
				client.setUnlockedFps(true);
				client.setUnlockedFpsTarget(maxFps);
			}else {
				client.setUnlockedFps(false);
			}
			storedMaxFps = maxFps;
		}
	}

	public String getBetaKeys()
	{
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://files.runemod.net/beta_keys_hashed.txt")).build();
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

/*	public boolean checkBetaKey()
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
	}*/

	void CheckUePreReqs() {
		if(!PrerequisiteChecker.isVCRedistInstalled()) {
			disableRuneModPlugin();
			SwingUtilities.invokeLater(() ->
			{
				PrerequisiteChecker.createMissingPreReqsPopup();
			});
		}
	}



	@SneakyThrows
	private ArrayList<String> getWindowsGpuNames() throws IOException
	{
		ArrayList<String> gpusNames = new ArrayList<String>();
		String psPath = System.getenv("WINDIR") + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";

		ProcessBuilder pb = new ProcessBuilder(
			psPath, "-Command",
			"Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name"
		);

		pb.redirectErrorStream(true); // Merge stderr into stdout
		Process process = pb.start();

		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					//System.out.println("GPU: " + line);
					gpusNames.add(line);
				}
			}
		}
		return gpusNames;
		//process.waitFor();
/*		try
		{
			ProcessBuilder pb = new ProcessBuilder("wmic", "path", "win32_VideoController", "get", "name");
			Process process = pb.start();
			java.util.Scanner sc = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
			String output = sc.hasNext() ? sc.next() : "";
			sc.close();
			return output;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}*/
	}

	boolean hasDedicatedGpu() {
		try
		{
			for(String GPU_name : getWindowsGpuNames()) {
				log.debug("Has Gpu: "+GPU_name);
				String gpuName_lowCase = GPU_name.toLowerCase();
				if(gpuName_lowCase.contains("amd") || gpuName_lowCase.contains("nvidia")) {
					return true;
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		log.debug("Has non dedicated Gpu");
		return false;
	}

	@SneakyThrows
	@Override
	protected void startUp() throws IOException
	{
		setupLoggingLevel();

		deleteLogsIfLarge(7500); //delete logs if > 7.5mb

		log.debug("\r\n\r\nRuneMod plugin start______________________________________________________________________________");

		if(OSType.getOSType() != OSType.Windows) {
			disableRuneModPlugin();
			SwingUtilities.invokeLater(() ->
			{
				int response = JOptionPane.showConfirmDialog(null,
					"RuneMod only works on Windows OS",
					"RuneMod Error",
					JOptionPane.DEFAULT_OPTION);
			});
		}

		if(!hasDedicatedGpu()) {
			disableRuneModPlugin();
			SwingUtilities.invokeLater(() ->
			{
				int response = JOptionPane.showConfirmDialog(null,
					"No dedicated GPU was detected. RuneMod requires a GPU to work.",
					"RuneMod Error",
					JOptionPane.DEFAULT_OPTION);
			});
		}

		if(SharedMemoryManager.RuneModWinExists_ByName()) {
			disableRuneModPlugin();
			SwingUtilities.invokeLater(() ->
			{
				int response = JOptionPane.showConfirmDialog(null,
					"Only one RuneMod client can be used at a time",
					"RuneMod Error",
					JOptionPane.DEFAULT_OPTION);
			});
		}

		divergentStuff = new DivergentStuff();
		eventBus.register(divergentStuff);

		client_static = client;
		config_static = config;

		//initializeExecutor();

		//checkBetaKey();
		ticksSincePluginLoad = -1;
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("RuneMod is stopping");
		isShutDown = true;

		eventBus.unregister(divergentStuff);

		clientThread.invoke(() -> {
			unRegisterMouseListener();

			if(runeModLauncher != null) {
				toggleRuneModLoadingScreen(false);
			}

			if (runeModLauncher != null)
			{
				if (runeModLauncher.runemodApp != null)
				{
					runeModLauncher.runemodApp.destroy();
				}
			}

			setDefaults();
		});
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		log.debug("RuneLite is exiting");
		if (runeModLauncher != null)
		{
			if (runeModLauncher.runemodApp != null)
			{
				log.debug("Destroying runemod win");
				runeModLauncher.runemodApp.destroy();
			}
		}
	}

	private void sendModifierPacket(RsCache0 cacheType, long elementId, String fieldName, DataType dataType, int data)
	{
		Buffer actorSpawnPacket = new Buffer(new byte[100]);
		actorSpawnPacket.writeByte(cacheType.ordinal());
		actorSpawnPacket.writeLong(elementId);

		actorSpawnPacket.writeStringCp1252NullTerminated(fieldName);
		actorSpawnPacket.writeByte(dataType.ordinal());
		actorSpawnPacket.writeInt(data);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "Modifier");
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
		myCacheReader.sendSkeletalFrameSets();
		//myCacheReader.sendFrames_Test();

		Buffer mainBuffer = new Buffer(new byte[10]);
		mainBuffer.writeInt(789728); //put this random number in packet content because im not sure it is ok to send empty packets.
		RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "RsCacheDataProvided");
		log.debug("RsCacheData has been provided To Unreal");
	}

	void setupLoggingLevel() {
		Level level = config.DebugLogging() ? Level.DEBUG : Level.INFO;
		((Logger) LoggerFactory.getLogger(RuneModPlugin.class)).setLevel(level);
		((Logger) LoggerFactory.getLogger(SharedMemoryManager.class)).setLevel(level);
		((Logger) LoggerFactory.getLogger(CacheReader.class)).setLevel(level);
		((Logger) LoggerFactory.getLogger(RuneMod_Launcher.class)).setLevel(level);
		((Logger) LoggerFactory.getLogger(DivergentStuff.class)).setLevel(level);
		((Logger) LoggerFactory.getLogger(PrerequisiteChecker.class)).setLevel(level);
	}

	@SneakyThrows
	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		clientThread.invokeAtTickEnd(() ->
		{
			if (event.getGroup().equalsIgnoreCase("RuneMod"))
			{
				log.debug("RuneModConfig " + event.getKey() + " Changed to " + event.getNewValue());


				if(event.getKey().equalsIgnoreCase("DebugSwitch")) {
					setupTransientMod();
				}

				if (event.getKey().equalsIgnoreCase("RuneModVisibility"))
				{
					if (config.RuneModVisibility())
					{
						setDrawCallbacks(this);
					}
					else
					{
						setDrawCallbacks(null);
					}
				}

				if (event.getKey().equalsIgnoreCase("useTwoRenderers"))
				{
					if (config.useTwoRenderers())
					{
						setDrawCallbacks(null);
					}
					else
					{
						setDrawCallbacks(this);
					}
				}

				if (event.getKey().equalsIgnoreCase("DebugLogging"))
				{
					setupLoggingLevel();
				}

				if (event.getKey().equalsIgnoreCase("MaxFps"))
				{
					setMaxFps(config.MaxFps());
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

	private void argbIntToColorChannels(int col)
	{
		int a = (col >> 24) & 0xFF;
		int r = (col >> 16) & 0xFF;
		int g = (col >> 8) & 0xFF;
		int b = col & 0xFF;
	}

	private void forEachTile(Scene scene, Consumer<Tile> consumer)
	{
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

	private void forEachExtendedTile(Consumer<Tile> consumer)
	{
		final Scene scene = client.getScene();
		final Tile[][][] tiles = scene.getExtendedTiles();

		for (int z = 0; z < Constants.MAX_Z; ++z)
		{
			for (int x = 0; x < Constants.EXTENDED_SCENE_SIZE; ++x)
			{
				for (int y = 0; y < Constants.EXTENDED_SCENE_SIZE; ++y)
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

	void simulateTilObjectSpawns(Tile tile) {
			if(tile == null) {return;}
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
					//if (object.getSceneMinLocation().equals(tile.getSceneLocation()))
					//{
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
					//}
				}
			}

/*			ItemLayer itemLayer = tile.getItemLayer();
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
			}*/
	}

	void getTileObjectTagsOnTile(Tile tile, Set<Long> setToAddTo) {
		if(tile == null) {return;}
		WallObject wallObject = tile.getWallObject();
		if (wallObject != null)
		{
			setToAddTo.add(getTag_Unique(wallObject));
		}

		DecorativeObject decorativeObject = tile.getDecorativeObject();
		if (decorativeObject != null)
		{
			setToAddTo.add(getTag_Unique(decorativeObject));
		}

		GroundObject groundObject = tile.getGroundObject();
		if (groundObject != null)
		{
			setToAddTo.add(getTag_Unique(groundObject));
		}

		for (GameObject object : tile.getGameObjects())
		{
			if (object != null)
			{
				//if (object.getSceneMinLocation().equals(tile.getSceneLocation()))
				//{
				if (object instanceof TileObject)
				{
					if (object.getRenderable() != null)
					{
						if (object.getRenderable() instanceof DynamicObject || object.getRenderable() instanceof Model || object.getRenderable() instanceof ModelData)
						{
							setToAddTo.add(getTag_Unique(object));
							//final GameObjectSpawned objectSpawned = new GameObjectSpawned();
							//objectSpawned.setTile(tile);
							//objectSpawned.setGameObject(object);
							//onGameObjectSpawned(objectSpawned);
						}
						else
						{
/*									if(object.getRenderable() instanceof Actor) {
										log.debug("unhandled renderableClass: Actor");
									}*/
						}
					}
				}
				//}
			}
		}
	}

	void simulateTilObjectDespawns(Tile tile) {
		if(tile == null) {return;}
		WallObject wallObject = tile.getWallObject();
		if (wallObject != null)
		{
			final WallObjectDespawned objectSpawned = new WallObjectDespawned();
			objectSpawned.setTile(tile);
			objectSpawned.setWallObject(wallObject);
			onWallObjectDespawned(objectSpawned);
		}

		DecorativeObject decorativeObject = tile.getDecorativeObject();
		if (decorativeObject != null)
		{
			final DecorativeObjectDespawned objectSpawned = new DecorativeObjectDespawned();
			objectSpawned.setTile(tile);
			objectSpawned.setDecorativeObject(decorativeObject);
			onDecorativeObjectDespawned(objectSpawned);
		}

		GroundObject groundObject = tile.getGroundObject();
		if (groundObject != null)
		{
			final GroundObjectDespawned objectSpawned = new GroundObjectDespawned();
			objectSpawned.setTile(tile);
			objectSpawned.setGroundObject(groundObject);
			onGroundObjectDespawned(objectSpawned);
		}

		for (GameObject object : tile.getGameObjects())
		{
			if (object != null)
			{
				//if (object.getSceneMinLocation().equals(tile.getSceneLocation()))
				//{
				if (object instanceof TileObject)
				{
					if (object.getRenderable() != null)
					{
						if (object.getRenderable() instanceof DynamicObject || object.getRenderable() instanceof Model || object.getRenderable() instanceof ModelData)
						{
							final GameObjectDespawned objectSpawned = new GameObjectDespawned();
							objectSpawned.setTile(tile);
							objectSpawned.setGameObject(object);
							onGameObjectDespawned(objectSpawned);
						}
						else
						{
/*									if(object.getRenderable() instanceof Actor) {
										log.debug("unhandled renderableClass: Actor");
									}*/
						}
					}
				}
				//}
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

				final ItemDespawned itemSpawned = new ItemDespawned(tile, item);
				onItemDespawned(itemSpawned);
			}
		}
	}

	public void simulateGameEvents()
	{
		System.out.printf("simulating gameEvents");
		if (client.getGameState() != GameState.LOGGED_IN) //if not logged in, retrigger gameStateChange. Appsettings are reloaded when gamestate changes to loginscreen, this means we can (and do) call reload scene in urneal engine to force rl to reload the appsettings file.
		{
			final GameStateChanged gameStateChanged = new GameStateChanged();
			gameStateChanged.setGameState(client.getGameState());
			onGameStateChanged(gameStateChanged);
			return;
		}

		//reset prevFrame variables
		clientPlane_prevFrame = -1;
		powPrevFrame = -1;
		removeRoofsPrevFrame = false;
		animSmoothingPrevFrame = false;
		playerLocation_prevTick = new WorldPoint(0,0,0);
		worldVisibilityCalcedTick = -1;
		exteriorVisibility_Prev = -1;
		npcsWithOverrides_LastFrame.clear();;

		reSendVarBits();

		activeChunks.clear();
		if(clientType == ClientType.ALORA) { //in Alora, setting gamestate to loading does not trigger loadRegion event.
			Scene scene = client.getTopLevelWorldView().getScene();
			sendSceneBaseInfo(scene.getBaseX(), scene.getBaseY(), scene);
		}
		client.setGameState(GameState.LOADING);

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

/*		log.debug("simulating game-events");

		if (client.getGameState() != GameState.LOGGED_IN) //if not logged in, just resend game state
		{
			final GameStateChanged gameStateChanged = new GameStateChanged();
			gameStateChanged.setGameState(client.getGameState());
			onGameStateChanged(gameStateChanged);
			return;
		}

		sendBaseCoordinatePacket();

		sendPlaneChanged();

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
			simulateTilObjectSpawns(tile);
		});*/
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

/*	@Subscribe
	private void onAnimationChanged(AnimationChanged event) {

	}*/

	void initDisAllowedDynamicSpawns() {
		if(!initedDisallowedDynamicSpawns) {
			for (int i = 0; i < 70000; i++) {
				ObjectComposition objDef = client.getObjectDefinition(i);
				if(objDef!=null) {

					//only certain objects should be considered
					boolean considerDisAllowingDynamicSpawn = false;
					for(String name : disAllowedDynamicSpawns_Names) {
						if(objDef.getName().toLowerCase().contains(name)) {
							considerDisAllowingDynamicSpawn = true;
							break;
						}
					}

					if(!considerDisAllowingDynamicSpawn) {
						continue;
					}

					for (String action : objDef.getActions())
					{
						if(action!=null)
						{
							if (action.equals("Squeeze-through") || action.equals("Climb-through ") || action.equals("Climb-over") || action.equals("Jump-over") || action.equals("Hop-over") || action.equals("Enter") || action.equals("Climb") || action.equals("Step-into"))
							{
								log.debug("disallowed dynamic spawn on obj: " + objDef.getName());
								disAllowedDynamicSpawns.add(i);
								break;
							}
						}
					}
				}
			}
			initedDisallowedDynamicSpawns = true;
		}
	}

	boolean isTileUnderRoof(int sceneX, int sceneY, int plane)
	{
		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();

		Tile floorTile = tiles[plane][sceneX][sceneY];

		if(floorTile == null) {
			return false;
		}

		if(floorTile.getPlane() >=3 ) {return false;} //there are no tiles above plane index 3

		Point floorTileSceneLoc  = floorTile.getSceneLocation();

		int floorTileSettings = client.getTopLevelWorldView().getTileSettings()[floorTile.getPlane()][floorTileSceneLoc.getX()][floorTileSceneLoc.getY()];

		if((floorTileSettings & 4) != 0) //if valid tile and tile settings indicate it has a roof overhead.
		{
			for (int plane_i = floorTile.getPlane() + 1; plane_i < 4; plane_i++)
			{
				Tile tile = tiles[plane_i][floorTileSceneLoc.getX()][floorTileSceneLoc.getY()];
				if(tile!=null)
				{
					//boolean tileHasSignificantHeight = (tile->height-floorTile->height) > 60;
					boolean isTileVisible = tile.getSceneTileModel() != null || tile.getSceneTilePaint()!=null;
					boolean tileHasWallObject = tile.getWallObject() != null;
					boolean tileHasDecorativeObject = tile.getDecorativeObject() != null;
					boolean tileHasGroundObject = tile.getGroundObject() != null;
					boolean tileHasGameObject = false;
					for(int i = 0; i < tile.getGameObjects().length; i++) {
						if(tile.getGameObjects()[i]!=null) {
							tileHasGameObject = true;
							break;
						}
					}

					//UE_LOG(LogTemp, Display, TEXT("checking tile. isTileVisible: %i isTileVisible %i tileHasObject: %i"), tileHasSignificantHeight, isTileVisible, tileHasObject);
					//boolean isRoofTile = tileHasSignificantHeight&&(isTileVisible || tileHasObject);
					boolean isRoofTile = isTileVisible || tileHasWallObject || tileHasDecorativeObject || tileHasGroundObject || tileHasGameObject;

					if(isRoofTile) {return true;}
				}
			}
		}
		return false;
	}


	WorldPoint playerLocation_prevTick = new WorldPoint(0,0,0);
	int worldVisibilityCalcedTick = -1;
	double exteriorVisibility_Prev = -1;

	void calcWorldVisibility() { //calculate how much of the world visible to the player is outdoors/indoors. used for muffling sound.
		if(client.getTopLevelWorldView() == null) {return;}
		if(client.getTopLevelWorldView().getScene().getTiles() == null) { return; } //prevents an error that can happen on first tick of login

		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int x_ = (lp.getX()) / 128;
		int y_ = (lp.getY()) / 128;
		WorldPoint playerLocation = WorldPoint.fromScene(client, x_, y_, client.getPlane());

		//WorldPoint playerLocation = WorldPoint.fromLocal(client, client.getLocalPlayer().getLocalLocation());
		boolean playerHasMoved = !playerLocation.equals(playerLocation_prevTick);

		if(playerHasMoved) {
			//System.out.println("playerHasMoved");
			playerLocation_prevTick = playerLocation;
		}

		if(!playerHasMoved && worldVisibilityCalcedTick==client.getTickCount()) { //we only calc visibility if the player has moved tile or the server has ticked.
			return;
		}

		worldVisibilityCalcedTick = client.getTickCount();

		//boolean[] visibilities = new boolean[25];
		int noTilesSampled = 0;
		int noTilesWithLos = 0;
		int noInteriorTiles = 0;
		int noExteriorTiles = 0;

		WorldArea area = client.getLocalPlayer().getWorldArea();

		for(int x = -2; x <= 2; x++) {
			for(int y = -2; y <= 2; y++) {
				if(Math.abs(x) == 2 && Math.abs(y) == 2) {continue;} //prevents sampling corner tiles. makes sample shape more circe-like.

				noTilesSampled++;
				WorldPoint targetLocation = new WorldPoint(playerLocation.getX()+x, playerLocation.getY()+y, playerLocation.getPlane());
				int SceneX = targetLocation.getX()-client.getTopLevelWorldView().getBaseX();
				int SceneY = targetLocation.getY()-client.getTopLevelWorldView().getBaseY();
				boolean isTileUnderRoof = isTileUnderRoof(SceneX, SceneY, playerLocation.getPlane());
				// Running the line of sight algorithm 100 times per frame doesn't
				// seem to use much CPU time, however rendering 100 tiles does
				if ((x == 0 && y== 0) || area.hasLineOfSightTo(client.getTopLevelWorldView(), targetLocation))
				{
					if(isTileUnderRoof) {
						noInteriorTiles++;
					} else {
						noExteriorTiles++;
					}
					//visibilities[(x+2)+((y+2)*5)] = true;
					noTilesWithLos++;
				} else{
					//visibilities[(x+2)+((y+2)*5)] = false;
				}
			}
		}

/*		System.out.println("noTilesWithLos: " + noTilesWithLos);
		System.out.println("noExteriorTiles: " + noExteriorTiles);
		System.out.println("noInteriorTiles: " + noInteriorTiles);
		System.out.println("noTilesSampled: " + noTilesSampled);*/

		double exteriorVisibility = (double)noExteriorTiles/(double)noTilesWithLos;
		exteriorVisibility*=100;
		exteriorVisibility*=1.6; //biases towards exterior sound. exterior sounds are ambient, they bounce. this bias emulates that.
		if(exteriorVisibility > 100) { //clamp
			exteriorVisibility = 100;
		}

		if(exteriorVisibility_Prev != exteriorVisibility) { //we dont send the visibility packet if the visibility hasnt changed
			exteriorVisibility_Prev = exteriorVisibility;
		}else {
			return;
		}

		double interiorVisibility = (double)noInteriorTiles/(double)noTilesWithLos;
		interiorVisibility*=100;

		//System.out.println("exteriorVisibility: " + exteriorVisibility + "%");
		//System.out.println("interiorVisibility: " + interiorVisibility + "%");

		Buffer packet = new Buffer(new byte[10]);
		packet.writeByte((byte)exteriorVisibility);
		packet.writeByte((byte)interiorVisibility);

		int visibilityBlendTime = 600; //over how many ms take to blend into new visibility state. max is 600 because thats the length of a tick.

		if(!playerHasMoved) { //if player has not moved, means evironment has changed. EG a door has been openned. so we want a small blend time.
			visibilityBlendTime = 100;
		}
		if(playerLocation.distanceTo(playerLocation_prevTick) > 6) { //if player has teleported
			visibilityBlendTime = 0;
		}
		packet.writeShort((short)visibilityBlendTime);

		//log.debug("visibilityBlendTime: " + visibilityBlendTime);

		sharedmem_rm.backBuffer.writePacket(packet, "WorldVisibility");
	}

	@SneakyThrows
	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		lastGameState = curGamestate;
		curGamestate = event.getGameState();
		log.debug("gameStateChanged to: ");

		if(client.getGameState() != GameState.LOGGED_IN) {
			loggedInForNoServerTicks = 0;
		}

		if (curGamestate == GameState.LOGIN_SCREEN)
		{
			DespawnServerSpawnedObjs();

			SwingUtilities.invokeLater(() ->
			{
				SwingUtilities.invokeLater(() ->
				{
					muteLoginScreenMusic(!unrealIsReady); //unmutes login screen music when unreal is ready.
				});
			});

			appSettings = loadAppSettings(); //load appSettings file

			if (appSettings != null)
			{ //makes login screen background transparent if animeLoginScreen appsetting is true
				if (appSettings.animateLoginScreen)
				{
					SpritePixels spritePixels = client.createSpritePixels(new int[0], 0, 0);
					client.setLoginScreen(spritePixels);
					enableLoginFire(false);
				}
				else
				{
					client.setLoginScreen(null);
					enableLoginFire(true);
				}
			}
			else
			{
				log.debug("appsettings is null");
			}

			log.debug("Login SCREEN...");
		}
		else if (curGamestate == GameState.LOGGING_IN)
		{
			initDisAllowedDynamicSpawns();
			log.debug("logging in...");
		}
		else if (curGamestate == GameState.LOGGED_IN)
		{
			log.debug("logged in...");
		}
		else if (curGamestate == GameState.HOPPING)
		{
			DespawnServerSpawnedObjs();
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

		if(unrealIsReady)
		{
			Buffer packet = new Buffer(new byte[10]);
			packet.writeByte(event.getGameState().ordinal());
			sharedmem_rm.backBuffer.writePacket(packet, "GameStateChanged");
		}

	}

	@Subscribe
	private void onWallObjectDespawned(WallObjectDespawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{

		if (!config.spawnGameObjects())
		{
			return;
		}

		if(loggedInForNoServerTicks > 1 && disAllowedDynamicSpawns.contains(event.getWallObject().getId())) {return;}

			Tile tile = event.getTile();;

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getWallObject());
			actorSpawnPacket.writeByte(5); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		//});
	}

	Set<WallObject> wallObjects = new HashSet<WallObject>();

	@Subscribe
	private void onWallObjectSpawned(WallObjectSpawned event)
	{
		if(needSendBaseInfo) {
			sendSceneBaseInfo(baseInfoScene.getBaseX(), baseInfoScene.getBaseY(), baseInfoScene);
			needSendBaseInfo = false;
			baseInfoScene = null;
		}

		if (ticksSincePluginLoad <= 1) { return; }
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			if (!config.spawnGameObjects())
			{
				return;
			}

			if(loggedInForNoServerTicks > 1 && disAllowedDynamicSpawns.contains(event.getWallObject().getId())) {return;}

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

/*			int tileX = (event.getWallObject().getX()) / 128;
			int tileY = (event.getWallObject().getY()) / 128;*/
			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();


		int height = event.getWallObject().getZ() * -1;

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
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
			taggedTileObjects.add(tag);
		//});
	}

	@Subscribe
	private void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			Tile tile;
			tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getDecorativeObject());

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			taggedTileObjects.remove(tag);
		//});
	}

	@Subscribe
	private void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		if(needSendBaseInfo) {
			sendSceneBaseInfo(baseInfoScene.getBaseX(), baseInfoScene.getBaseY(), baseInfoScene);
			needSendBaseInfo = false;
			baseInfoScene = null;
		}

		if (ticksSincePluginLoad <= 1) { return; }
		if (!config.spawnGameObjects())
		{
			return;
		}
		//clientThread.invokeAtTickEnd(() ->
		//{
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

/*			int tileX = (event.getDecorativeObject().getX()) / 128;
			int tileY = (event.getDecorativeObject().getY()) / 128;*/
			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();

			int height = event.getDecorativeObject().getZ() * -1;

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tileObjectModelType);
			actorSpawnPacket.writeByte(var4);
			actorSpawnPacket.writeShort(objectOrientationA);
			actorSpawnPacket.writeShort(objectOrientationB);
			actorSpawnPacket.writeShort(objectDefinitionId);
			actorSpawnPacket.writeByte(plane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
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
			taggedTileObjects.add(tag);
		//});
	}

	int[] varbits;
	int[] varps;

	@Subscribe
	private void onVarbitChanged(VarbitChanged event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		//in VarbitChanged func in unreal, we read: varType. varId. varValue. custom0.
		Buffer buffer = new Buffer(new byte[12]);

		if (event.getVarbitId() != -1)
		{
			//log.debug("varbit "+ event.getVarbitId()+"cahnged to "+event.getValue());
			buffer.writeInt(event.getVarbitId());
			buffer.writeInt(event.getValue());
			varbits[event.getVarbitId()] = event.getValue();
			sharedmem_rm.backBuffer.writePacket(buffer, "Varbit");
		}
		else
		{
			//log.debug("varP "+ event.getVarbitId()+"cahnged to "+event.getValue());
			buffer.writeInt(event.getVarpId());
			buffer.writeInt(event.getValue());
			varps[event.getVarpId()] = event.getValue();
			sharedmem_rm.backBuffer.writePacket(buffer, "Varp");
		}
	}

	void reSendVarBits() {
		for(int i = 0; i < varbits.length; i++) {
			if(varbits[i]!=0) {
				VarbitChanged event = new VarbitChanged();
				event.setVarbitId(i);
				event.setValue(varbits[i]);
				onVarbitChanged(event);
			}
		}
		for(int i = 0; i < varps.length; i++) {
			if(varps[i]!=0)
			{
				VarbitChanged event = new VarbitChanged();
				event.setVarbitId(-1);
				event.setVarpId(i);
				event.setValue(varps[i]);
				onVarbitChanged(event);
			}
		}
	}

/*	private Map<Long, DynamicObject> getAnimatedGameObjects()
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
	}*/

	// Instance-level ExecutorService for reuse
/*	private ExecutorService executor;

	// Initialize the ExecutorService once (e.g., in your constructor or initialization code)
	public void initializeExecutor()
	{
		if (executor == null) { // Check if it's already initialized
			int threadCount = Runtime.getRuntime().availableProcessors();
			executor = Executors.newFixedThreadPool(threadCount);
		}
	}*/

	// Shut down the executor service (call this when you're done using the executor)
/*	public void shutdownExecutor()
	{
		if (executor != null) {
			executor.shutdown(); // Shutdown the executor service
			executor = null;
		}
	}*/

	@SneakyThrows
	private Map<Long, DynamicObject> getAnimatedGameObjects() throws InterruptedException
	{
		Map<Long, DynamicObject> allGameObjects = new HashMap<>();
		if(curGpuFlags != 17) {
			for(Tile tile : tilesWithAnimateGameObjects) {
				findAnimatedGameObjectsOnTile(allGameObjects, tile);
			}
			return allGameObjects;
		}else {
			for (TileObject tileObj : AnimatedTileObjects) {
				Renderable r = null;
				if(tileObj instanceof GameObject) {
					r = ((GameObject)tileObj).getRenderable();
				}
				if(tileObj instanceof GroundObject) {
					r = ((GroundObject)tileObj).getRenderable();
				}
				if(tileObj instanceof WallObject) {
					r = ((WallObject)tileObj).getRenderable1();
				}
				if(tileObj instanceof DecorativeObject) {
					r = ((DecorativeObject)tileObj).getRenderable();
				}
				if(r instanceof DynamicObject){
					DynamicObject dynOb = (DynamicObject)r;
					if(dynOb.getAnimation()!=null) {
						allGameObjects.put(getTag_Unique(tileObj), (DynamicObject)r);
					}
				}
			}
		}
		return allGameObjects;
	}

	private void findAnimatedGameObjectsOnTile(Map<Long, DynamicObject> allGameObjects, Tile tile)
	{
		if(tile == null) {return;}
		// Extract gameObjects, groundObjects, etc. (same logic as before)
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject obj : gameObjects)
			{
				if (obj != null)
				{
					Renderable r = obj.getRenderable();
					if (r instanceof DynamicObject)
					{
						DynamicObject d = (DynamicObject) r;
						if (d.getAnimation() != null)
						{
							allGameObjects.put(getTag_Unique(obj), d);
						}
					}
				}
			}
		}

		GroundObject ground = tile.getGroundObject();
		if (ground != null)
		{
			Renderable r = ground.getRenderable();
			if (r instanceof DynamicObject)
			{
				DynamicObject d = (DynamicObject) r;
				if (d.getAnimation() != null)
				{
					allGameObjects.put(getTag_Unique(ground), d);
				}
			}
		}

		DecorativeObject deco = tile.getDecorativeObject();
		if (deco != null)
		{
			Renderable r = deco.getRenderable();
			if (r instanceof DynamicObject)
			{
				DynamicObject d = (DynamicObject) r;
				if (d.getAnimation() != null)
				{
					allGameObjects.put(getTag_Unique(deco), d);
				}
			}
			r = deco.getRenderable2();
			if (r instanceof DynamicObject)
			{
				DynamicObject d = (DynamicObject) r;
				if (d.getAnimation() != null)
				{
					allGameObjects.put(getTag_Unique(deco), d);
				}
			}
		}


		WallObject wall = tile.getWallObject();
		if (wall != null)
		{
			Renderable r = wall.getRenderable1();
			if (r instanceof DynamicObject)
			{
				DynamicObject d = (DynamicObject) r;
				if (d.getAnimation() != null)
				{
					allGameObjects.put(getTag_Unique(wall), d);
				}
			}

			r = wall.getRenderable2();
			if (r instanceof DynamicObject)
			{
				DynamicObject d = (DynamicObject) r;
				if (d.getAnimation() != null)
				{
					allGameObjects.put(getTag_Unique(wall), d);
				}
			}
		}
	}

	boolean eventIsSimulation = false;

	ArrayList<GameObjectSpawned> serverSpawnedGameObjects = new ArrayList<>();
	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		RuneLiteObject runeLiteObject;

		if(needSendBaseInfo) {
			sendSceneBaseInfo(baseInfoScene.getBaseX(), baseInfoScene.getBaseY(), baseInfoScene);
			needSendBaseInfo = false;
			baseInfoScene = null;
		}

		if (ticksSincePluginLoad <= 1) { return; }
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			if (!config.spawnGameObjects())
			{
				return;
			}

			if(loggedInForNoServerTicks > 1 && disAllowedDynamicSpawns.contains(event.getGameObject().getId())) {return;}


			//client.getObjectDefinition(event.getGameObject().getConfig()).getName();

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
			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();

/*			int settings = client.getTopLevelWorldView().getTileSettings()[tileX][tileY][plane];
			int CalcedMinPlane = -1;
		if ((settings & 8) != 0) {
			CalcedMinPlane = 0;
		} else {
			FIntVector coordinatesPlane1 = FIntVector(coordinates.X, coordinates.Y, 1);
			int64 tilePlane1_id = UUser::MakeAssetId(0, UUser::CoordinatesToContentId(coordinatesPlane1));

			UTile_RSClass* tilePlane1 = nullptr;
			if(tilePlane1_id != id) //prevents tile loading self
			{
				tilePlane1 = UMyBPFunctions::getTile(tilePlane1_id, nullptr, false);
			} else
			{
				tilePlane1 = this;
			}

			int tilePlane1_settings = 0;
			if (tilePlane1)
			{
				tilePlane1_settings = tilePlane1->settings & 2;
			}
			CalcedMinPlane = plane > 0 && (tilePlane1_settings) != 0 ? coordinates.Z - 1 : coordinates.Z;
		}*/


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
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			int tileMinPlane = tile.getPlane();

			actorSpawnPacket.writeByte(tileMinPlane);
			actorSpawnPacket.writeShort(height);
			actorSpawnPacket.writeLong(tag);
			actorSpawnPacket.writeShort(cycleStart);
			actorSpawnPacket.writeShort(frame);
			LocalPoint min_localPoint = new LocalPoint((tileX * 128)+64, (tileY*128)+64);
			int offsetX = event.getGameObject().getX() - min_localPoint.getX(); //explanation: centreX-MinX = offsetX;
			int offsetY = event.getGameObject().getY() - min_localPoint.getY(); //explanation: centreY-MinY = offsetY;
			actorSpawnPacket.writeShort(offsetX);
			actorSpawnPacket.writeShort(offsetY);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
			taggedTileObjects.add(tag);
			if(!eventIsSimulation && client.getGameState().ordinal()>=GameState.LOGGED_IN.ordinal()) {
				serverSpawnedGameObjects.add(event);
			}
		//});
	}

	byte[][][] minPLanes = new byte[4][104][104];
	void calcMinPLanes() {
		for (int plane = 0; plane < Constants.MAX_Z; plane++)
		{
			for (int sceneX = 1; sceneX < Constants.SCENE_SIZE - 1; ++sceneX)
			{
				for (int sceneY = 1; sceneY < Constants.SCENE_SIZE - 1; ++sceneY)
				{
					int minPlane = -1;
					if ((client.getTileSettings()[plane][sceneY][sceneX] & 8) != 0)
					{
						minPlane = 0;
					}
					else if (plane > 0 && (client.getTileSettings()[1][sceneY][sceneX] & 2) != 0)
					{
						minPlane = plane - 1;
					}
					else
					{
						minPlane = plane;
					}

					minPLanes[plane][sceneX][sceneY] = (byte)minPlane;
					//var5.setTileMinPlane(plane, sceneY, sceneX, minPlane);
				}
			}
		}
	}

	@Subscribe
	private void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGroundObject());

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			taggedTileObjects.remove(tag);
		//});
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }

		if(loggedInForNoServerTicks > 1 && disAllowedDynamicSpawns.contains(event.getGameObject().getId())) {return;}
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGameObject());

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			taggedTileObjects.remove(tag);
		//});
	}

	long Unique = 1000;
	long getTag_Unique(TileObject tileObject)
	{
		//if(true) {1000return ThreadLocalRandom.current().nextLong(Long.MIN_VALUE, Long.MAX_VALUE);}
		//Our tag add the tileObj type to make it more unique. it also uses world coordinates so it does not change when the basecoordinate changes.
		long tag = tileObject.getHash();

		//int plane = (int)(tag >> 62 & 3);
		int worldView = (int)(tag >> 50 & 4095);
		//int id = (int)(tag >> 18 & 0xffffffff);
		int wall = (int)(tag >> 17 & 1);
		//int type = (int)(tag >> 14 & 7);
		//int sceney = (int)(tag >> 7 & 127);
		//int scenex = (int)(tag >> 0 & 127);

		int instConfig = 0;
		int tileObjType = 0;
		if (tileObject instanceof GroundObject)
		{
			tileObjType = 0;
			instConfig = ((GroundObject) tileObject).getConfig();
		}
		if (tileObject instanceof GameObject)
		{
			tileObjType = 1;
			instConfig = ((GameObject) tileObject).getConfig();
		}
		if (tileObject instanceof DecorativeObject)
		{
			tileObjType = 2;
			instConfig = ((DecorativeObject) tileObject).getConfig();
		}
		if (tileObject instanceof WallObject)
		{
			tileObjType = 3;
			instConfig = ((WallObject) tileObject).getConfig();
		}


		int worldX = tileObject.getWorldLocation().getX();
		int worldY = tileObject.getWorldLocation().getY();

		int isWallB = 0; //is always 0 for spawn events, because wall b doesnt have its own spawn event. in UE we modify the last be so isWallB is true, when we spawn a wallB


		Unique++;

		int unused = 0;
		worldView = 0;

		long newTag = 0;
		newTag |= ((long)(tileObject.getPlane() & 0b11)) << 62;             // bits 62–63
		newTag |= ((long)(worldView & 0b111)) << 59;        // bits 59–61
		newTag |= ((long)(tileObject.getId() & 0x1FFFF)) << 42;             // bits 42–58
		newTag |= ((long)(worldY & 0x7FFF)) << 26;          // bits 26–40
		newTag |= ((long)(worldX & 0x7FFF)) << 11;          // bits 11–25
		newTag |= ((long)(instConfig & 0xFF)) << 3;         // bits 3–10
		newTag |= ((long)(tileObjType & 0b11)) << 1;        // bits 1–2
		newTag |= ((long)(isWallB & 0b1));                  // bit 0

		return newTag;
	}

	@Subscribe
	private void onGroundObjectSpawned(GroundObjectSpawned event) //GroundObject is aka a FloorDecoration
	{
		if(needSendBaseInfo) {
			sendSceneBaseInfo(baseInfoScene.getBaseX(), baseInfoScene.getBaseY(), baseInfoScene);
			needSendBaseInfo = false;
			baseInfoScene = null;
		}

		if (ticksSincePluginLoad <= 1) { return; }
		if (!config.spawnGameObjects())
		{
			return;
		}
		//clientThread.invokeAtTickEnd(() ->
		//{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[100]);
			int tileObjectModelType = getObjModelTypeFromFlags(event.getGroundObject().getConfig());
			//int var4 = (event.getGroundObject().getConfig()) >> 6 & 3;
			int var4 = (event.getGroundObject().getConfig() >> 6) & 3;
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
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);

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
			taggedTileObjects.add(tag);
		//});
	}

	@Subscribe
	private void onItemSpawned(ItemSpawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		if (!config.spawnItems())
		{
			return;
		}
		//clientThread.invokeAtTickEnd(() ->
		//{
			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = event.getTile().getPlane();
/*			if (event.getTile().getBridge() != null)
			{
				tilePlane++;
			}*/

			int tileX = event.getTile().getSceneLocation().getX();
			int tileY = event.getTile().getSceneLocation().getY();
			int height = Perspective.getTileHeight(client, event.getTile().getLocalLocation(), client.getTopLevelWorldView().getPlane()) * -1;
			height += event.getTile().getItemLayer().getHeight();
			int itemDefinitionId = event.getItem().getId();
			int itemQuantity = event.getItem().getQuantity();
			actorSpawnPacket.writeByte(3); //write tileItem data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeShort(height);
			actorSpawnPacket.writeShort(itemDefinitionId);
			actorSpawnPacket.writeShort(itemQuantity);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		//});
	}

	@Subscribe
	private void onItemDespawned(ItemDespawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		//clientThread.invokeAtTickEnd(() ->
		//{
			Buffer actorSpawnPacket = new Buffer(new byte[100]);

			int tilePlane = event.getTile().getPlane();
/*			if (event.getTile().getBridge() != null)
			{
				tilePlane++;
			}*/

			int tileX = event.getTile().getSceneLocation().getX();
			int tileY = event.getTile().getSceneLocation().getY();
			int itemDefinitionId = event.getItem().getId();
			actorSpawnPacket.writeByte(3); //write tileItem data type
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeShort(itemDefinitionId);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		//});
	}

	@SneakyThrows
	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }

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

		if(event.getNpc().getModelOverrides()!=null) {
			NpcOverrides npcOverrides = event.getNpc().getModelOverrides();
			actorSpawnPacket.writeInt_Array(npcOverrides.getModelIds());
			actorSpawnPacket.writeShort_Array(npcOverrides.getTextureToReplaceWith());
			actorSpawnPacket.writeShort_Array(npcOverrides.getColorToReplaceWith());
			actorSpawnPacket.writeBoolean(npcOverrides.useLocalPlayer());
			System.out.println("npc overrides exist on npc at index: "+instanceId);
			npcsWithOverrides_LastFrame.put(event.getNpc(), new NpcOverrides_Copy(npcOverrides));
		}else {
			if(npcsWithOverrides_LastFrame.containsKey(event.getNpc())) {
				npcsWithOverrides_LastFrame.remove(event.getNpc());
			}
		}
		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
	}

	@Subscribe
	private void onNpcChanged(NpcChanged event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
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
		if (ticksSincePluginLoad <= 1) { return; }
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
		PlayerSpawned playerSpawnedEvent = new PlayerSpawned(event.getPlayer());
		onPlayerSpawned(playerSpawnedEvent);
/*		if (ticksSincePluginLoad <= 1) { return; }
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

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");*/
	}


	@Subscribe
	private void onPlayerSpawned(PlayerSpawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
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
		if(InstanceId == -1) {return;} //added for Alora
		if(player.getPlayerComposition() == null) {return;}//added for Alora

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

		//log.debug("player spawn. id: " + InstanceId);
		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");

		if(isLocalPlayer == 1) { //for fashionScapePlugin
			FashionScape_EquipmentIds_PrevFrame = player.getPlayerComposition().getEquipmentIds().clone();
			FashionScape_Colors_PrevFrame = player.getPlayerComposition().getColors().clone();
		}
	}

	@Subscribe
	private void onPlayerDespawned(PlayerDespawned event)
	{
		if (ticksSincePluginLoad <= 1) { return; }
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		int instanceId = event.getPlayer().getId();
		actorSpawnPacket.writeByte(2); //write player data type
		actorSpawnPacket.writeShort(instanceId);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	private void sendPlaneChanged() //send planeChanged if needed.
	{
		int curPlane = client.getLocalPlayer().getWorldLocation().getPlane(); /*client.getTopLevelWorldView().getPlane()*/
		if (curPlane != clientPlane_prevFrame)
		{
			Buffer buffer = new Buffer(new byte[4]);
			buffer.writeByte(curPlane);
			sharedmem_rm.backBuffer.writePacket(buffer, "PlaneChanged");

			clientPlane_prevFrame = curPlane;
		}
	}

/*	public byte[] SpotAnimationModel_get(int Id)
	{
		IndexDataBase SpotAnimModelArchive = client.getIndex(7);
		byte[] bytes = SpotAnimModelArchive.loadData(Id, 0); //loadData(ArchiveId, FileId). For modeldata, file id is always 0.
		return bytes;
	}*/

	float getDpiScalingFactor()
	{
		final GraphicsConfiguration graphicsConfiguration = clientUI.getGraphicsConfiguration();
		final AffineTransform t = graphicsConfiguration.getDefaultTransform();
		return (float) t.getScaleX();
	}

	private void UpdateUiPosOffsets()
	{ //sets the pos offsets, using swing thread, to avoid crash.
		float dpiScalingFactor = getDpiScalingFactor();

		//calcBaseOffset();

		baseOffsetX = Math.round((float)client.getCanvas().getLocation().x*dpiScalingFactor);
		baseOffsetY = Math.round((float)client.getCanvas().getLocation().y*dpiScalingFactor);

		rsUiPosX = baseOffsetX;
		rsUiPosY = baseOffsetY;

		float ratioX = 1;
		float ratioY = 1;

		if (client.isStretchedEnabled())
		{
			ratioX = (float) client.getStretchedDimensions().width / (float) client.getBufferProvider().getWidth();
			ratioY = (float) client.getStretchedDimensions().height / (float) client.getBufferProvider().getHeight();
		}

		boolean onLoginScreen = client.getGameState() == GameState.STARTING || client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN || client.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR;
		//3d viewport size
		if (onLoginScreen)
		{ //when we first arrive on login screen, viewport is 0 width because it is uninitialized. In such cases, I am using the canvas dimensions instead.
			View3dSizeX = Math.round(client.getCanvas().getParent().getWidth() * dpiScalingFactor);
			View3dSizeY = Math.round(client.getCanvas().getParent().getHeight() * dpiScalingFactor);
			View3dOffsetX = 0;
			View3dOffsetY = 0;
		}else {
			View3dSizeX = client.getViewportWidth();
			View3dSizeX *= dpiScalingFactor;
			View3dSizeX *= ratioX;

			View3dSizeY = client.getViewportHeight();
			View3dSizeY *= dpiScalingFactor;
			View3dSizeY *= ratioY;

			//top left position of 3d viewport in rl window.
			View3dOffsetX = client.getViewportXOffset();
			View3dOffsetX *= dpiScalingFactor;
			View3dOffsetX *= ratioX;
			View3dOffsetX += baseOffsetX; //add the relative location compared to parent, to the 3d viewOffset.

			View3dOffsetY = client.getViewportYOffset();
			View3dOffsetY *= dpiScalingFactor;
			View3dOffsetY *= ratioY;
			View3dOffsetY += baseOffsetY; //add the relative location compared to parent, to the 3d viewOffset.
		}

		canvas2DSizeX = client.getCanvasWidth();
		canvas2DSizeX *= dpiScalingFactor;
		canvas2DSizeX *= ratioX;
		canvas2DSizeY = client.getCanvasHeight();
		canvas2DSizeY *= dpiScalingFactor;
		canvas2DSizeY *= ratioY;
	}

	public int View3dSizeX = 200;
	public int View3dSizeY = 200;
	public int View3dOffsetX = 0;
	public int View3dOffsetY = 0;

	private void UpdateSharedMemoryUiPixels()
	{
		if(!config.enableUiPixelsUpdate()) {return;}
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

		sharedmem_rm.myKernel32.ResetEvent(sharedmem_rm.EventViewportPixelsReady);

		//SwingUtilities.invokeLater(() -> {
			UpdateUiPosOffsets();
		//});

		sharedMemPixelsUpdatedTick = client.getGameCycle();
		final BufferProvider bufferProvider = client.getBufferProvider();

		int bufferWidth = bufferProvider.getWidth();
		int bufferHeight = bufferProvider.getHeight();

		//pixelBuffser dimension
		sharedmem_rm.setInt(30000000, bufferWidth);
		sharedmem_rm.setInt(30000005, bufferHeight);

		//CanvasPose. not used currently afik
		sharedmem_rm.setInt(30000010, rsUiPosX);
		sharedmem_rm.setInt(30000015, rsUiPosY);

		//View3dSize
		sharedmem_rm.setInt(30000020, Math.round(View3dSizeX));
		sharedmem_rm.setInt(30000025, Math.round(View3dSizeY));
		//View3dOffset
		sharedmem_rm.setInt(30000030, Math.round(View3dOffsetX));
		sharedmem_rm.setInt(30000035, Math.round(View3dOffsetY));

		//Size of ui. Can differ from buffer size because of stretchmode.
		sharedmem_rm.setInt(30000040, Math.round(canvas2DSizeX));
		sharedmem_rm.setInt(30000045, Math.round(canvas2DSizeY));

		sharedmem_rm.setInt(30000065, overlayColor_LastFrame);

		sharedmem_rm.SharedMemoryData.write(30000080, bufferProvider.getPixels(), 0, bufferProvider.getHeight() * bufferProvider.getWidth());

		sharedmem_rm.myKernel32.SetEvent(sharedmem_rm.EventViewportPixelsReady);
		log_Timed_Heavy("_UpdateSharedMemoryUiPixels_1");
	}

	private void sendInstancedAreaState(Scene scene)
	{
		int ByteArray1DSize = (4 * 13 * 13) * 4;
		Buffer packet = new Buffer(new byte[ByteArray1DSize + 10]);

		boolean isInstancedArea = scene.isInstance();
		packet.writeBoolean(isInstancedArea);

		if (isInstancedArea)
		{
			log.debug("sending instance template chunks");

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

	private void sendBaseCoordinatePacket(int baseX_, int baseY_, Scene scene) //changed for alora. we now only get instanceArea state from scene. baseX and baseY are explicitly provided.
	{ //send Base Coordinate if needed
		//clientThread.invoke(() -> {
		sendInstancedAreaState(scene);

		isInstanced = scene.isInstance();
		baseX = baseX_;
		baseY = baseY_;

		log.debug("sending baseCoordinate "+baseX);
		log.debug("sending baseCoordinate "+baseY);

		Buffer packet = new Buffer(new byte[20]);
		packet.writeShort(baseX);
		packet.writeShort(baseY);
		packet.writeByte(client.getTopLevelWorldView().getPlane());
		sharedmem_rm.backBuffer.writePacket(packet, "BaseCoordinate");
		//});
	}

/*	private void sendBaseCoordinatePacket()
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
	}*/

	int[] FashionScape_EquipmentIds_PrevFrame = null;
	int[] FashionScape_Colors_PrevFrame = null;
	void checkForFashionScapeChanges() {
		 Player localPlayer = client.getLocalPlayer();
		 if (localPlayer!=null && FashionScape_EquipmentIds_PrevFrame !=null){
		 	PlayerComposition curComp = localPlayer.getPlayerComposition();
		 	if(curComp!=null) {
				if(!Arrays.equals(FashionScape_Colors_PrevFrame, curComp.getColors()) || !Arrays.equals(FashionScape_EquipmentIds_PrevFrame, curComp.getEquipmentIds())) {
					log.debug("runemod detected player EquipmentIds altered by fashionScape Plugin");
					PlayerChanged playerChangedEvent = new PlayerChanged(localPlayer);
					onPlayerChanged(playerChangedEvent); //FashionScape_EquipmentIds_PrevFrame will be set/stored by the code in here
				}
			} else {
			}
		 }else {
			 FashionScape_EquipmentIds_PrevFrame = null;
			 FashionScape_Colors_PrevFrame = null;
		 }
	}

	double powPrevFrame = -1;
	boolean removeRoofsPrevFrame = false;
	boolean animSmoothingPrevFrame = false;

	float lerp(float a, float b, float f)
	{
		return a * (1.0f - f) + (b * f);
	}
	void SendRsOptionsPacket() {
		double pow = client.getTextureProvider().getBrightness(); //1.0 at low. 0.5 at high
		boolean removeRoofs = client.getVarbitValue(12378) > 0;
		boolean animSmoothing = client.getAnimationInterpolationFilter() != null;
		if(powPrevFrame != pow || removeRoofsPrevFrame != removeRoofs || animSmoothingPrevFrame!= animSmoothing) { //if options have changed
			Buffer rsOptionsPacket = new Buffer(new byte[20]);

			float lerpVal = ((float)pow-0.5f)*2.0f;
			log.debug("BrightnessLerpVal "+lerpVal);
			double UeGamma = lerp(1.0f, 0.7f, lerpVal);
			rsOptionsPacket.writeByte((int)(UeGamma*100.0));
			rsOptionsPacket.writeBoolean(removeRoofs);
			rsOptionsPacket.writeBoolean(animSmoothing);
			rsOptionsPacket.writeBoolean(false);
			rsOptionsPacket.writeBoolean(false);
			rsOptionsPacket.writeBoolean(false);
			rsOptionsPacket.writeBoolean(false);

			powPrevFrame = pow;
			removeRoofsPrevFrame = removeRoofs;
			animSmoothingPrevFrame = animSmoothing;

			sharedmem_rm.backBuffer.writePacket(rsOptionsPacket, "RsOptions");
		}
	}

	int playerCount = 0; // playerCountThisFrame
	@SneakyThrows
	private void WritePerFramePacket()
	{
		if(!config.enablePerFramePacket()) {return;}

		if (client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN || curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{ //dont send perframe packet while on login screen because doing so would interfere with the animated login screen's camera.
			return;
		}

		checkFor_NpcModelOverride_Changes(); //iterates npcs to see if any of their overrides have changed. if overrides have changed, they will be re-spawned with the correct overrides.

		if(config.useTwoRenderers()) { //makes actor height work when using two renderers. not quite so accurate but works enough for showcase
			for(NPC npc : client.getNpcs()) {
				//LocalPoint offsetCentre = new LocalPoint(npc.getLocalLocation().getX()+config.actorOffsetDebug(), npc.getLocalLocation().getY()+config.actorOffsetDebug());
				npcHeights[npc.getId()] = Perspective.getTileHeight(client, npc.getLocalLocation(), client.getLocalPlayer().getWorldLocation().getPlane());
			}
			for(Player player : client.getPlayers()) {
				//LocalPoint offsetCentre = new LocalPoint(player.getLocalLocation().getX()+config.actorOffsetDebug(), player.getLocalLocation().getY()+config.actorOffsetDebug());
				playerHeights[player.getId()] = Perspective.getTileHeight(client, player.getLocalLocation(), client.getLocalPlayer().getWorldLocation().getPlane());
			}
		}

		sendPlaneChanged();

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
				//LocalPoint offsetCentre = new LocalPoint(npcX+(npc.getComposition().getSize()*config.NpcOffsetX()), npcY+(npc.getComposition().getSize()*config.NpcOffsetY()));
				int npcHeight = npcHeights[npcInstanceId]*-1; /*Perspective.getTileHeight(client, offsetCentre, client.getLocalPlayer().getWorldLocation().getPlane()) * -1;*/
				int npcOrientation = npc.getCurrentOrientation();

				int animationId_Action = (config.spawnAnimations() ? getAnimation_Unmasked(npc) : -1);
				int animationId_Pose = (config.spawnAnimations() ? npc.getPoseAnimation() : -1);

				int animationFrameIdx_Action = npc.getAnimationFrame();
				int animationFrameIdx_Pose = npc.getPoseAnimationFrame();

				//boolean enableActionSeq = player.getAnimation() != -1 && player.getSequenceDelay() == 0;
				boolean disableMovementSeq = npc.getPoseAnimation() == -1 || (npc.getPoseAnimation() == npc.getIdlePoseAnimation() && animationId_Action != -1);
				if(disableMovementSeq) {
					animationId_Pose = -1;
					animationFrameIdx_Pose = -1;
				}

				boolean shouldDraw = visibleActors.contains(npc) && hooks.draw(npc, false);

				if (!shouldDraw)
				{
					animationFrameIdx_Action = -2; //-2 causes entity to be hidden in rm
					animationFrameIdx_Pose = -2;
				}

				perFramePacket.writeInt(npcInstanceId);
				perFramePacket.writeShort(npcX);
				perFramePacket.writeShort(npcY);
				perFramePacket.writeShort(npcHeight);
				perFramePacket.writeShort(npcOrientation);

				perFramePacket.writeInt(animationId_Action);
				perFramePacket.writeShort(animationFrameIdx_Action);

				perFramePacket.writeInt(animationId_Pose);
				perFramePacket.writeShort(animationFrameIdx_Pose);

				int numActorSpotAnims = 0;
				if (!config.spawnNpcGFX() || !shouldDraw)
				{
					perFramePacket.writeByte(numActorSpotAnims);
				}
				else
				{
					for (ActorSpotAnim spotAnim : npc.getSpotAnims())
					{
						boolean spotAnimInactive = spotAnim.getFrame() == 0 && spotAnim.getCycle() == 0; //bodge way of preventing telling iof a spotanim is inactive
						if(!spotAnimInactive) {
							numActorSpotAnims++;
						}
					}
					perFramePacket.writeByte(numActorSpotAnims);

					if (numActorSpotAnims > 0)
					{
						for (ActorSpotAnim spotAnim : npc.getSpotAnims())
						{
							boolean spotAnimInactive = spotAnim.getFrame() == 0 && spotAnim.getCycle() == 0;
							if(!spotAnimInactive)
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
		}

		IndexedObjectSet<? extends Player> players = client.getTopLevelWorldView().players();
/*		ArrayList<Player> players = new ArrayList<>();
		for(WorldView wv : worldViews) {
			if(!wv.isTopLevel()) {
				for (Player player : wv.players()) {
					players.add(player);
				}
			}
		}*/

		playerCount = 0;
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
				int playerHeight = playerHeights[playerInstanceId]*-1; /*Perspective.getTileHeight(client, player.getLocalLocation(), client.getLocalPlayer().getWorldLocation().getPlane()) * -1*/;
				int playerOrientation = player.getCurrentOrientation();

				int animationId_Action = (config.spawnAnimations() ? player.getAnimation() : -1);
				int animationId_Pose = (config.spawnAnimations() ? player.getPoseAnimation() : -1);


				//boolean enableActionSeq = player.getAnimation() != -1 && player.getSequenceDelay() == 0;
				boolean disableMovementSeq = player.getPoseAnimation() == -1 || /*player.isUnanimated || */player.getPoseAnimation() == player.getIdlePoseAnimation() && animationId_Action != -1;
				if(disableMovementSeq) {animationId_Pose = -1;}


/*				SequenceDefinition var1 = super.sequence != -1 && super.sequenceDelay == 0 ? class353.SequenceDefinition_get(super.sequence) : null;
				SequenceDefinition var2 = super.movementSequence == -1 || this.isUnanimated || super.movementSequence == super.idleSequence && var1 != null ? null : class353.SequenceDefinition_get(super.movementSequence);*/

				int animationFrameIdx_Action = player.getAnimationFrame();
				int animationFrameIdx_Pose = player.getPoseAnimationFrame();

				boolean shouldDraw = visibleActors.contains(player) && hooks.draw(player, false);

/*				if(!player.getWorldView().isTopLevel()) {
					if(player.getName().equalsIgnoreCase("noodleeater")) {
						playerInstanceId = 20;
						shouldDraw = true;
						int sizeDifX = 104-player.getWorldView().getSizeX();
						int sizeDifY = 104-player.getWorldView().getSizeY();
						//playerX = 6208;
						//playerY = -1555;
						//int offsetX = player.getWorldLocation().getX()*128;
						//int offsetY = player.getWorldLocation().getY()*128;
						//int DifX = client.getTopLevelWorldView().getBaseX()-player.getWorldView().getBaseX();
						//int DifY = client.getTopLevelWorldView().getBaseY()-player.getWorldView().getBaseY();

						int wvBaseX = player.getWorldView().getBaseX();
						int wvBaseY = player.getWorldView().getBaseY();
						System.out.println("playerLocalLocation:"+player.getLocalLocation());
						LocalPoint localFromWorld = LocalPoint.fromWorld(player.getWorldView(), player.getWorldLocation());
						System.out.println("localFromWorld:"+localFromWorld);

						System.out.println("wvBaseX:"+wvBaseX + " wvBaseY:"+wvBaseY);
						System.out.println("wvSizeX: "+player.getWorldView().getSizeX()+" wvSizeY"+player.getWorldView().getSizeY());
						playerX+=sizeDifX*128;
						playerY+=sizeDifY*128;
						playerHeight = 0;
					}
				}*/

				if (!shouldDraw)
				{
					animationFrameIdx_Action = -2; //-2 causes entity to be hidden in rm
					animationFrameIdx_Pose = -2;
				}

				perFramePacket.writeShort(playerInstanceId);
				perFramePacket.writeShort(playerX);
				perFramePacket.writeShort(playerY);
				perFramePacket.writeShort(playerHeight);
				perFramePacket.writeShort(playerOrientation);

				perFramePacket.writeInt(animationId_Action);
				perFramePacket.writeShort(animationFrameIdx_Action);
				perFramePacket.writeInt(animationId_Pose);
				perFramePacket.writeShort(animationFrameIdx_Pose);


				int numActorSpotAnims = 0;
				if (!config.spawnPlayerGFX() || !shouldDraw)
				{
					perFramePacket.writeByte(numActorSpotAnims);
				}
				else
				{
					for (ActorSpotAnim spotAnim : player.getSpotAnims())
					{
						boolean spotAnimInactive = spotAnim.getFrame() == 0 && spotAnim.getCycle() == 0; //bodge way of preventing telling iof a spotanim is inactive
						if(!spotAnimInactive) {
							numActorSpotAnims++;
						}
					}
					perFramePacket.writeByte(numActorSpotAnims);

					if (numActorSpotAnims > 0)
					{
						for (ActorSpotAnim spotAnim : player.getSpotAnims())
						{
							boolean spotAnimInactive = spotAnim.getFrame() == 0 && spotAnim.getCycle() == 0; //bodge way of preventing telling iof a spotanim is inactive
							if(!spotAnimInactive)
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
				}else {
					if(graphicsObject.getModel() == null) {
						shouldDraw = false;
					}
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
			for (Projectile projectile : client.getProjectiles())
			{
				noProjectiles++;
				hashedEntitys_ThisFrame.add(projectile.hashCode());
			}
		}

		if (config.spawnProjectiles() && noProjectiles > 0)
		{
			perFramePacket.writeShort(noProjectiles);

			for (Projectile projectile : client.getProjectiles())
			{
				int sceneId = projectile.hashCode();
				perFramePacket.writeInt(sceneId);
				short localX = (short) projectile.getX();
				perFramePacket.writeShort(localX);
				short localY = (short) projectile.getY();
				perFramePacket.writeShort(localY);
				short Z = (short) ((projectile.getZ() * -1)); //not sure if getStartHeight is correct/helping things.
				perFramePacket.writeShort(Z);

				short localX_target = 0;
				short localY_target = 0;

				LocalPoint targetPoint = projectile.getTarget();
				if(targetPoint!=null) {
					localX_target = (short) targetPoint.getX();
					localY_target = (short) targetPoint.getY();
				}

				/*				WorldPoint targetPoint = projectile.getTargetPoint();
				if(targetPoint!=null) {
					localX_target = (short) (64+((projectile.getTargetPoint().getX()-baseX)*128));
					localY_target = (short) (64+((projectile.getTargetPoint().getY()-baseY)*128));
				}*/

				perFramePacket.writeShort(localX_target);
				perFramePacket.writeShort(localY_target);
				short Z_target = (short) (projectile.getEndHeight());
				Z_target += (Perspective.getTileHeight(client, new LocalPoint(localX_target, localY_target), client.getLocalPlayer().getWorldLocation().getPlane()/*client.getTopLevelWorldView().getPlane()*/) * -1);
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
				//short animimationFrameCycle = (short) -1;
				perFramePacket.writeShort(projectile.getOrientation()); //Yaw
				//short remainingCycles = (short) projectile.getRemainingCycles();
				perFramePacket.writeShort(0);
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
					//log.debug("hashedEntityDespawned. Entity " + lastFrameHashedEntity);
					hashedEntityDespawned(lastFrameHashedEntity);
					//hashedEntityDespawned(lastFrameHashedEntity); //not sure why I had this line duplicated?
				}
			}
		}
		hashedEntitys_LastFrame = hashedEntitys_ThisFrame;

		sharedmem_rm.backBuffer.writePacket(perFramePacket, "PerFramePacket");

		checkFor_ActorColourOverride_Changes();

		checkForFashionScapeChanges();

		SendRsOptionsPacket();
	}

	void hashedEntityDespawned(int SceneId)
	{
		Buffer actorSpawnPacket = new Buffer(new byte[20]);

		actorSpawnPacket.writeByte(6); //write hashedEntity data type
		actorSpawnPacket.writeInt(SceneId);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

/*	boolean RmNeedsWindowUpdate()
	{

		return false;
	}*/

	public void deleteLogsIfLarge(int MaxKb)
	{
		String logsFolderString = System.getProperty("user.home") + "\\.runemod\\logs\\";
		String RM_application_log_Txt = logsFolderString+"RM_application_log.txt";
		String RM_launcher_log_Txt = logsFolderString+"RM_launcher_log.txt";

		Path RM_application_log_Txt_Path = Paths.get(RM_application_log_Txt);
		Path RM_launcher_log_Txt_Path = Paths.get(RM_launcher_log_Txt);

		if(exists(RM_application_log_Txt_Path)) {
			try {
				long size = Files.size(RM_application_log_Txt_Path); // Fast: metadata-only

				boolean isTooLarge = size > (1000*MaxKb);
				if(isTooLarge) {
					deleteIfExists(RM_application_log_Txt_Path);
					deleteIfExists(RM_launcher_log_Txt_Path);
					log.debug("deleted runemod logs because they are large");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public ApplicationSettings loadAppSettings()
	{
		String jsonFileLocation = System.getProperty("user.home") + "\\.runemod\\AppSettings.json";

		//Gson gson = new GsonBuilder().create();
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

	//not currently used, is part of wip code to handle npcOverrides.
	class NpcOverrides_Copy
	{
		int[] modelIds = null;

		short[] colorToReplaceWith = null;

		short[] textureToReplaceWith = null;

		boolean useLocalPlayer = false;

		public NpcOverrides_Copy(NpcOverrides overrides)
		{
			if(overrides.getModelIds()!=null) {
				this.modelIds = overrides.getModelIds().clone();
			}

			if(overrides.getColorToReplaceWith()!=null) {
				this.colorToReplaceWith = overrides.getColorToReplaceWith().clone();
			}

			if(overrides.getTextureToReplaceWith()!=null){
				this.textureToReplaceWith = overrides.getTextureToReplaceWith().clone();
			}

			this.useLocalPlayer = overrides.useLocalPlayer();
		}

		public boolean isIdenticalTo(NpcOverrides_Copy other)
		{
			boolean isEqual = true;

			if (!Arrays.equals(other.modelIds, this.modelIds))
			{
				isEqual = false;
			}
			if (!Arrays.equals(other.colorToReplaceWith, this.colorToReplaceWith))
			{
				isEqual = false;
			}
			if (!Arrays.equals(other.textureToReplaceWith, this.textureToReplaceWith))
			{
				isEqual = false;
			}
			if (!(other.useLocalPlayer == this.useLocalPlayer))
			{
				isEqual = false;
			}
			return isEqual;
		}
	}

	Set<Actor> actorsWithColorOverrides_LastFrame = new HashSet<>();
	private void checkFor_ActorColourOverride_Changes() {
		for (Actor actor : client.getPlayers()) {
			if(actor == null) {
				continue;
			}
			if(visibleActors.contains(actor)) {
				Model model = actor.getModel();
				if(model == null) {continue;}
				if(model.getOverrideAmount() != 0) { //if override is active
					if(!actorsWithColorOverrides_LastFrame.contains(actor)) { //if override has changed to become active
						actorOverrideChanged(actor, (byte) 0);
						actorsWithColorOverrides_LastFrame.add(actor);
						log.debug("col override has become active");
					}
				} else { //if override is inactive
					if(actorsWithColorOverrides_LastFrame.contains(actor)) { //if override has changed to become inactive
						actorOverrideChanged(actor, (byte) 0);
						actorsWithColorOverrides_LastFrame.remove(actor);
						log.debug("col override has become inactive");
					}
				}
			}
		}

		for (Actor actor : client.getNpcs()) {
			if(actor == null) {
				continue;
			}
			if(visibleActors.contains(actor))
			{
				Model model = actor.getModel();
				if (model == null)
				{
					continue;
				}
				if (model.getOverrideAmount() != 0)
				{ //if override is active
					if (!actorsWithColorOverrides_LastFrame.contains(actor))
					{ //if override has changed to become active
						actorOverrideChanged(actor, (byte) 0);
						actorsWithColorOverrides_LastFrame.add(actor);
						log.debug("col override has become active");
					}
				}
				else
				{ //if override is inactive
					if (actorsWithColorOverrides_LastFrame.contains(actor))
					{ //if override has changed to become inactive
						actorOverrideChanged(actor, (byte) 0);
						actorsWithColorOverrides_LastFrame.remove(actor);
						log.debug("col override has become inactive");
					}
				}
			}
		}
	}


	private void actorOverrideChanged(Actor actor, byte overridesType) //overridesType 0 = colourOverrides //overrides 1 = NPCOverrides
	{
		int actorId = 0;
		byte actorType = 0;
		if(actor instanceof Player) {
			actorId = ((Player) actor).getId();
			actorType = 1;
		}
		if(actor instanceof NPC) {
			actorId = ((NPC) actor).getIndex();
			actorType = 2;
		}

		if(actorType == 0) {
			log.debug("ColourOverridesChanged unhandled actor type has ColourOverride");
			return;
		}

		Buffer packet = new Buffer(new byte[200]);
		packet.writeByte(overridesType);
		packet.writeByte(actorType);
		packet.writeShort(actorId);

		if(overridesType == 0) {
			Model model = actor.getModel();
			packet.writeByte(model.getOverrideAmount());
			packet.writeByte(model.getOverrideHue());
			packet.writeByte(model.getOverrideLuminance());
			packet.writeByte(model.getOverrideSaturation());
			log.debug("ColourOverridesChanged on actor id "+ actorId + " hue:"+model.getOverrideHue() + " sat:"+model.getOverrideSaturation() + " Lum:"+model.getOverrideLuminance() + " amount:"+model.getOverrideAmount());
		}

		if(overridesType == 1) {
			NPC npc = (NPC)actor;
			NpcOverrides npcOverrides = npc.getModelOverrides();
			boolean hasNpcOverrides = npcOverrides!=null;
			packet.writeBoolean(hasNpcOverrides);

			if (hasNpcOverrides)
			{
				packet.writeInt_Array(npcOverrides.getModelIds());
				packet.writeShort_Array(npcOverrides.getTextureToReplaceWith());
				packet.writeShort_Array(npcOverrides.getColorToReplaceWith());
				packet.writeBoolean(npcOverrides.useLocalPlayer());
			}

			log.debug("NPCModelOverridesChanged on actor id "+ actorId);
		}

		sharedmem_rm.backBuffer.writePacket(packet, "ActorOverridesChanged");
	}

	HashMap<NPC, NpcOverrides_Copy> npcsWithOverrides_LastFrame = new HashMap<NPC, NpcOverrides_Copy>();
	private void checkFor_NpcModelOverride_Changes() {
		for (NPC actor : client.getNpcs()) {
			if(actor == null) {
				continue;
			}
			if(visibleActors.contains(actor))
			{
				if (actor.getModelOverrides()!=null) //if NPCOverride exist on npc
				{
					if (!npcsWithOverrides_LastFrame.containsKey(actor)) //if override has only just become existant
					{
						//respawn the npc so new overrides can be activated
						NpcSpawned event = new NpcSpawned(actor);
						onNpcSpawned(event);
						log.debug("NpcModelOverride  has become active");
					}else {//if override already exists, check if it's contents have changed
						boolean OverrideValsHaveChanged = !npcsWithOverrides_LastFrame.get(actor).isIdenticalTo(new NpcOverrides_Copy(actor.getModelOverrides()));
						if(OverrideValsHaveChanged){
							//respawn the npc so override alterations can take effect
							NpcSpawned event = new NpcSpawned(actor);
							onNpcSpawned(event);
							log.debug("NpcOverideVals have changed");
						}
					}
				}
				else
				{
					if (npcsWithOverrides_LastFrame.containsKey(actor))
					{
						// override just became inactive
						//respawn the npc so that overrides can be deactivated
						NpcSpawned event = new NpcSpawned(actor);
						onNpcSpawned(event);
						log.debug("NpcModelOverride has become inactive");
					}
				}
			}
		}
	}

	void sendNpcOverridesIfNeeded(NPC actor) {
		if(actor == null) {
			return;
		}
		if(visibleActors.contains(actor))
		{
			if (actor.getModelOverrides()!=null) //if NPCOverride exist on npc
			{
				if (!npcsWithOverrides_LastFrame.containsKey(actor)) //if override has only just become existant
				{
					actorOverrideChanged(actor, (byte) 1);
					npcsWithOverrides_LastFrame.put(actor, new NpcOverrides_Copy(actor.getModelOverrides()));
					log.debug("NpcModelOverride  has become active");
				}else {//if override already exists, check if it's contents have changed
					boolean OverrideValsHaveChanged = !npcsWithOverrides_LastFrame.get(actor).isIdenticalTo(new NpcOverrides_Copy(actor.getModelOverrides()));
					if(OverrideValsHaveChanged){
						actorOverrideChanged(actor, (byte) 1);
					}
				}
			}
			else
			{ //if override is inactive
				if (npcsWithOverrides_LastFrame.containsKey(actor))
				{ //if override has changed to become inactive
					actorOverrideChanged(actor, (byte) 1);
					npcsWithOverrides_LastFrame.remove(actor);
					log.debug("NpcModelOverride has become inactive");
				}
			}
		}
	}

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		if(config.nullifyDrawCallbacks()) {
			return;
		}
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

		if(renderable instanceof DynamicObject) {
/*			long worldView = (long)hash >> 52 & 4095;
			long id = hash >> 20 & 0xffffffff;
			long wall = hash >> 19 & 1;
			long type = hash >> 16 & 7;*/
			long plane = (long)(hash >> 14 & 3);
			long sceneY = (long)(hash >> 7 & 127);
			long sceneX = (long)(hash >> 0 & 127);

			if(plane < 4 && sceneY < 104 && sceneX < 104) {

				Tile tile = client.getTopLevelWorldView().getScene().getTiles()[(int)plane][(int)sceneX][(int)sceneY];
				tilesWithAnimateGameObjects.add(tile);
				if(plane > 0) {
					tilesWithAnimateGameObjects.add(client.getTopLevelWorldView().getScene().getTiles()[(int)plane-1][(int)sceneX][(int)sceneY]); //required where tile uses linkedbellow stuff
				}
			}
		} else {
			if (renderable instanceof Player)
			{
				visibleActors.add(renderable);
				playerHeights[((Player)renderable).getId()] = y;
			}
			else
			{
				if (renderable instanceof NPC)
				{
					visibleActors.add(renderable);
					npcHeights[((NPC)renderable).getIndex()] = y;
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
}


