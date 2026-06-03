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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;
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
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.InputStream;

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

	private String[] disAllowedDynamicSpawns_Names = {"obstacle pipe", "rail", "stile", "forest", "fence", "rocks", "shortcut", "low wall", "sparkling pool", "Crumbling wall"/*, "Glowing symbol"*/};
	private Set<Integer> disAllowedDynamicDeSpawns = new HashSet<>(); //objdefs in here are not allowed to spawn/despawn except during loading. We have these in order to prevent things like stiles becoming invisible due to being incorporated into the player model. Its bodge, but its the best we can do as we cant tell whether a objdef has been put in a player model, in rl api.
	boolean initedDisallowedDynamicSpawns = false;

	public Set<Long> taggedTileObjects = new HashSet<>();

	public int overlayColor_LastFrame = 0;
	public ApplicationSettings appSettings = new ApplicationSettings();
	//int sharedMemPixelsUpdatedTick = -1; //used to prevent updating sharedMemory twice in the same tick.

	int ticksSincePluginLoad = 0;

	boolean startedWhileLoggedIn;
	boolean runeModAwaitingRsCacheHashes = false;
	boolean alreadyCommunicatedUnreal = false; //whether we have communicated with unreal this frame.
	int curGpuFlags = -1; //there is no client.setGpuFlags, so I use this to keep track of them myself.
	//int GpuFlagsEnableNo = 17;//DrawCallbacks.GPU | DrawCallbacks.ZBUF
	//int GpuFlagsEnableNo = 3;
	int GpuFlagsEnableNo = DrawCallbacks.GPU|DrawCallbacks.ZBUF;//1;
	Set<Renderable> visibleActors = new HashSet<Renderable>();
	Set<WorldPoint> activeSubRegions = new HashSet<>();//key is the subregion's corner tileCoord.
	Set<PopulatedTile> populatedObjsTiles = new HashSet<>();

	class PopulatedTile
	{
		int x;
		int y;
		int plane;
		//int wv; havent implemented yet. isEnteringMainScene could be disabled for world views since they tend not to exist in extended scene.

		boolean wasInMainScene = false;

		long wallObj = 0;
		//long[] gameObjs = new long[5];//can implement these later
		boolean hasTileObjects = false;

		PopulatedTile(Tile tile) {
			x = tile.getWorldLocation().getX();
			y = tile.getWorldLocation().getY();
			plane = tile.getWorldLocation().getPlane();

			wallObj = getTag_Unique(tile.getWallObject(), tile);

			if(wallObj!=0) {hasTileObjects = true;}

			int sceneX = x-baseX;
			int sceneY = x-baseY;
			wasInMainScene = (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104);
		}

		PopulatedTile(int x_, int y_, int plane_) { //should only be used for comparison purposes, should not store a PopulatedTile created via this method.
			x = x_;
			y = y_;
			plane = plane_;
		}

/*
		void isEnteringMainScene() { //would need to call this periodically, to ensure stale object left over from extended scene's state are removed

			if(!hasTileObjects) { return; } //optimization thing. If no tile objects, then check is not needed.

			boolean isEnteringMainScene = false;
			if(!wasInMainScene) {
				int sceneX = x-baseX;
				int sceneY = y-baseY;
				boolean isInMainScene = (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104);
				if(isInMainScene) {
					isEnteringMainScene = true;
					wasInMainScene = true;
				}

				if(isEnteringMainScene) {
					Tile curTile = client.getScene().getTiles()[plane][sceneX][sceneY];

					if((curTile.getWallObject() == null && wallObj!=0) || getTag_Unique(curTile.getWallObject(), curTile) != wallObj) {
						System.out.println("despawning stale object left over from extended scene "+wallObj);
						despawnTaggedTileObj(wallObj);
						wallObj = 0;
					}
				}
			}
		}
*/

		@Override
		public boolean equals(Object o)
		{
			if (o == this) return true;
			if (!(o instanceof PopulatedTile)) return false;

			PopulatedTile other = (PopulatedTile) o;

			return this.x == other.x
				&& this.y == other.y
				&& this.plane == other.plane;
		}

		@Override
		public int hashCode()
		{
			return (plane << 28) | (x << 14) | y;
		}

		@Override
		public String toString()
		{
			return "x:"+x+" y:"+y+" plane:"+plane;
		}
	}

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




	//idea: no world views in unreal. unreal doesnt have concept of world views, it keeps the existing player/npc arrays. We do transforms in runelite and feed transformed positions to unreal.
	boolean[] availableIds = new boolean[2048];
	ArrayList<ArrayList<ArrayList<Integer>>> customizedActorIds =new ArrayList<>(5); //actType, wv, id //these values create a global id, which is used to lookup our own dynamic id

	//HashMap<Actor, Integer> customizedActorIds = new HashMap<>(); //could be more optimized, couyld be a multidimensional array, where first dimension is actorType, second is worldview, and third is id.

	int getOrAddCustomizedId(Actor actor) {
		//int actorType;
		int actorIndex = -1;

		if (actor instanceof Player) {
			//actorType = 0;
			actorIndex = ((Player) actor).getId();
		} else { // NPC
			//actorType = 1;
			actorIndex = ((NPC) actor).getIndex();
		}

		//if(true){ //for testing
			return actorIndex;
		//}

/*
		int actorWorldView = actor.getWorldView().getId();
		if(actorWorldView == -1) {
			actorWorldView = 4095;
		}

		ArrayList<Integer> list = customizedActorIds.get(actorType).get(actorWorldView);

		// Ensure list is large enough
		int curArrSize = list.size();
		int desiredSize = actorIndex + 100; // padding

		if (actorIndex >= curArrSize) {
			for (int i = curArrSize; i < desiredSize; i++) {
				list.add(-1);
			}
		}

		int id_customized = list.get(actorIndex);

		if (id_customized == -1) { //if actor has no customId, give it one
			id_customized = getFreshId();
			list.set(actorIndex, id_customized);
		}

		return id_customized;*/
	}

	void customizedIdActorDestroyed(Actor actor) {
		int actorType = -1;
		int actorWorldView = actor.getWorldView().getId();
		int actorIndex = -1;

		if(actor instanceof Player) {
			actorType = 0;
			actorIndex = ((Player) actor).getId();
		}
		if(actor instanceof NPC) {
			actorType = 1;
			actorIndex = ((NPC) actor).getId();
		}

		int id_Customized = customizedActorIds.get(actorType).get(actorWorldView).get(actorIndex);
		releaseId(id_Customized);
	}

	int getFreshId() {
		for(int i = 0; i < availableIds.length; i++) {
			if(availableIds[i] == false) {
				availableIds[i] = true;
				System.out.println("freshIdProvided: "+i);
				return i;
			}
		}
		System.out.println("ran out of freshIds");
		return -1;
	}

	void releaseId(int id) {
		availableIds[id] = false;
	}





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

	public regionTxtParser regionTxtParser;

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
			log.debug("[" + (System.currentTimeMillis() - 1767376710035L) + "]	" + message);
		}
	}

	long timeLastInteracted = Long.MAX_VALUE;

	MouseListener mouseListener = new MouseAdapter()
	{
		@Override
		public void mousePressed(MouseEvent mouseEvent)
		{
			setMaxFps(appSettings.maxFps);
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
			setMaxFps(appSettings.maxFps);
			timeLastInteracted = System.currentTimeMillis();
		}
	};

	KeyListener keyListener = new KeyAdapter()
	{
		@Override
		public void keyPressed(KeyEvent KeyEvent)
		{
			setMaxFps(appSettings.maxFps);
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
		int musicVolume = 255;
		if(mute) {
			musicVolume = 0;
		}
		client.setMusicVolume(musicVolume);
	}

	boolean discovered_GetActionAnimIfValid = false;
	private Method GetActionAnimIfValid_Meth = null;
	int GetActionAnimIfValid_GarbageVal = 1;



	public Field pitchField;
	public int pitch_GarbageVal = 1;

	@SneakyThrows
	public void discoverField_ProjectilePitch(Projectile projectile)
	{
		pitch_GarbageVal = -266919833;
		Class<?> clazz = projectile.getClass();

		try
		{
			pitchField = clazz.getDeclaredField("aw");
			pitchField.setAccessible(true);
		}
		catch (NoSuchFieldException e)
		{
			pitchField = null;
			log.warn("Field 'at' not found in class {}", clazz.getName());
		}

/*		try
		{
			Class<?> clazz = projectile.getClass();
			ClassNode projectileClass = getClassNode(clazz);

			for (MethodNode method : projectileClass.methods)
			{
				for (AbstractInsnNode insn : method.instructions)
				{
					if (!(insn instanceof MethodInsnNode))
						continue;

					MethodInsnNode call = (MethodInsnNode) insn;

					// Match Math.atan2(double,double)
					if (!call.owner.equals("java/lang/Math") ||
						!call.name.equals("atan2") ||
						!call.desc.equals("(DD)D"))
					{
						continue;
					}

					// -----------------------------------------
					// STEP 1: Ensure this is pitch (uses GETFIELD)
					// -----------------------------------------
					boolean usesField = false;

					AbstractInsnNode back = insn.getPrevious();
					int backSteps = 0;

					while (back != null && backSteps++ < 10)
					{
						if (back instanceof FieldInsnNode)
						{
							FieldInsnNode f = (FieldInsnNode) back;

							if (f.getOpcode() == Opcodes.GETFIELD &&
								f.owner.equals(projectileClass.name))
							{
								usesField = true;
								break;
							}
						}
						back = back.getPrevious();
					}

					if (!usesField)
						continue;

					// -----------------------------------------
					// STEP 2: Walk forward and validate pattern
					// -----------------------------------------
					AbstractInsnNode cursor = insn.getNext();

					boolean seenIAND = false;
					boolean seenIADD = false; // yaw has +1024
					boolean seenIMUL = false;

					int steps = 0;

					while (cursor != null && steps++ < 20)
					{
						int op = cursor.getOpcode();

						if (op == Opcodes.IADD)
							seenIADD = true;

						if (op == Opcodes.IAND)
							seenIAND = true;

						if (op == Opcodes.IMUL)
							seenIMUL = true;

						if (cursor instanceof FieldInsnNode)
						{
							FieldInsnNode f = (FieldInsnNode) cursor;

							if (f.getOpcode() == Opcodes.PUTFIELD &&
								f.owner.equals(projectileClass.name) &&
								f.desc.equals("I"))
							{
								// -----------------------------------------
								// FINAL FILTERS (THIS IS THE FIX)
								// -----------------------------------------
								if (!seenIAND) continue;   // must mask
								if (seenIADD) continue;    // yaw has +1024
								if (!seenIMUL) continue;   // new multiplier stage

								String fieldName = f.name;

								pitchField = clazz.getDeclaredField(fieldName);
								pitchField.setAccessible(true);

								int stored = pitchField.getInt(projectile);

								Integer multiplier = discoverMultiplier(projectileClass, fieldName);
								pitch_GarbageVal = multiplier != null ? multiplier : 1;

								int pitch = stored * pitch_GarbageVal;

								System.out.println(
									"Found projectile pitch field: " + fieldName +
										" raw=" + stored +
										" pitch=" + pitch +
										" multiplier=" + pitch_GarbageVal
								);

								return pitch;
							}
						}

						cursor = cursor.getNext();
					}
				}
			}

			System.out.println("Error - Projectile pitch field not found");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return -1;*/
	}

	@SneakyThrows
	public static ClassNode getClassNode(Class<?> clazz)
	{
		String classFile = clazz.getName().replace('.', '/') + ".class";

		InputStream in = clazz.getClassLoader().getResourceAsStream(classFile);

		if (in == null)
		{
			throw new RuntimeException("Could not load class bytes for " + clazz.getName());
		}

		ClassReader reader = new ClassReader(in);

		ClassNode node = new ClassNode();
		reader.accept(node, ClassReader.SKIP_DEBUG);

		return node;
	}

	public static Integer discoverMultiplier(ClassNode clazz, String fieldName)
	{
		for (MethodNode method : clazz.methods)
		{
			for (AbstractInsnNode insn = method.instructions.getFirst();
				 insn != null;
				 insn = insn.getNext())
			{
				if (!(insn instanceof FieldInsnNode))
					continue;

				FieldInsnNode field = (FieldInsnNode) insn;

				// ✅ Look for where the field is WRITTEN
				if (field.getOpcode() != Opcodes.PUTFIELD ||
					!field.name.equals(fieldName))
				{
					continue;
				}

				// -----------------------------------------
				// Walk BACKWARDS to find IMUL constant
				// -----------------------------------------
				AbstractInsnNode cursor = insn.getPrevious();

				int steps = 0;

				while (cursor != null && steps++ < 10)
				{
					if (cursor.getOpcode() == Opcodes.IMUL)
					{
						AbstractInsnNode prev = cursor.getPrevious();

						// Look for constant before IMUL
						while (prev != null)
						{
							if (prev instanceof LdcInsnNode)
							{
								Object cst = ((LdcInsnNode) prev).cst;
								if (cst instanceof Integer)
								{
									return (Integer) cst;
								}
							}

							if (prev instanceof IntInsnNode)
							{
								return ((IntInsnNode) prev).operand;
							}

							if (prev instanceof InsnNode)
							{
								int op = prev.getOpcode();
								if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5)
								{
									return op - Opcodes.ICONST_0;
								}
							}

							prev = prev.getPrevious();
						}
					}

					cursor = cursor.getPrevious();
				}
			}
		}

		return null;
	}

	@SneakyThrows
	public int getProjectilePitch(Projectile p)
	{
		if(pitchField == null) {
			discoverField_ProjectilePitch(p);
		}

		if(pitchField != null)
		{
			return (pitchField.getInt(p) * pitch_GarbageVal);
		}else {
			return 0;
		}
	}

	public static Integer findGarbageParam(Class<?> clazz, String methodName) throws Exception
	{
		String resource = clazz.getName().replace('.', '/') + ".class";
		InputStream in = clazz.getClassLoader().getResourceAsStream(resource);

		ClassReader reader = new ClassReader(in);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);

		for (MethodNode m : node.methods)
		{
			if (!m.name.equals(methodName))
				continue;

			AbstractInsnNode insn = m.instructions.getFirst();

			while (insn != null)
			{
				if (insn.getOpcode() == Opcodes.ILOAD)
				{
					AbstractInsnNode ldcNode = insn.getNext();

					if (ldcNode instanceof LdcInsnNode)
					{
						LdcInsnNode ldc = (LdcInsnNode) ldcNode;

						if (ldc.cst instanceof Integer)
						{
							AbstractInsnNode cmp = ldcNode.getNext();

							if (cmp != null &&
								(cmp.getOpcode() == Opcodes.IF_ICMPNE ||
									cmp.getOpcode() == Opcodes.IF_ICMPEQ))
							{
								AbstractInsnNode next = cmp.getNext();

								if (next != null && next.getOpcode() == Opcodes.NEW)
								{
									AbstractInsnNode afterNew = next.getNext();

									if (afterNew != null && afterNew.getOpcode() == Opcodes.DUP)
									{
										AbstractInsnNode throwNode = afterNew.getNext();

										while (throwNode != null)
										{
											if (throwNode.getOpcode() == Opcodes.ATHROW)
											{
												return (Integer) ldc.cst;
											}
											throwNode = throwNode.getNext();
										}
									}
								}
							}
						}
					}
				}

				insn = insn.getNext();
			}
		}

		return null;
	}

	Method getMethodByName(Class<?> clazz, String name) {
		while (clazz != null) {
			Method[] methods = clazz.getDeclaredMethods();
			for (Method method : methods) {
				// Test any other things about it beyond the name...
				if (method.getName().equals(name)) {
					return method;
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public void discoverField_ActionAnimValid()
	{
		//if(true) {return;}
		if(discovered_GetActionAnimIfValid) {return;}
		Player actor = client.getLocalPlayer();
		int minFieldCount = 0;
		if (actor == null)
		{
			System.out.println("Actor is null, cant discover field");
			return;
		}

		GetActionAnimIfValid_GarbageVal = 0/*-2102997845*/;
		Class<?> clazz = actor.getClass().getSuperclass();
		GetActionAnimIfValid_Meth = getMethodByName(clazz, "lt");
		GetActionAnimIfValid_Meth.setAccessible(true); // allows access to private fields
		discovered_GetActionAnimIfValid = true;

		if(GetActionAnimIfValid_Meth==null) {
			System.out.println("Failed to discover GetActionAnimIfValid_Meth");
		}
/*
		Class<?> actorClass = actor.getClass().getSuperclass();
		// We will gather declared methods at this level and test them.
		Method[] declared = actorClass.getDeclaredMethods();
		List<Method> candidates = new ArrayList<>();
		for (Method m : declared)
		{
			// Filter: the sequence does not define itself as public zero parameters and non-void return and return types who have Object as class (AnimationSequence class is toplevel so "Object" is it's superclass)
			if (!Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1 && m.getReturnType() != void.class && m.getReturnType().getSuperclass()==Object.class) //added m.getReturnType().getSuperclass()==Object.class but not 100% sure about it
			{
				System.out.println("adding candidate who returns object of type: "+m.getReturnType().getName()+" funcName: "+m.getName());
				candidates.add(m);
			}
		}

		if (!candidates.isEmpty())
		{
			System.out.printf("Testing %d no-arg non-void methods on class %s%n", candidates.size(), actorClass.getName());
		}

		// Test each candidate method individually
		for (Method m : candidates)
		{
			try
			{
				m.setAccessible(true);

				GetActionAnimIfValid_GarbageVal = findGarbageParam(actorClass, m.getName());
				System.out.println("testing method + "+m.getName() + " with garbage val: "+ GetActionAnimIfValid_GarbageVal);
				// see if func return null when anim is set to null
				Object before = null;
				try
				{
					before = m.invoke(actor, GetActionAnimIfValid_GarbageVal);
				}
				catch (InvocationTargetException ite)
				{
					// If the method throws, skip it
					System.out.printf("Method %s threw before invocation: %s%n", m.getName(), ite.getCause());
					continue;
				}

				// Set animation to a non-null anim
				actor.setAnimation(442);

				Object after = null;
				try
				{
					after = m.invoke(actor, GetActionAnimIfValid_GarbageVal);
				}
				catch (InvocationTargetException ite)
				{
					System.out.printf("Method %s threw after invocation: %s%n", m.getName(), ite.getCause());
					// Optionally reset animation (actor.setAnimation(0)), then continue
					try { actor.setAnimation(-1); } catch (Throwable t) {}
					continue;
				}

				// condition for picking a candidate: before setting valid anim = Null and after setting valid anim = Not Null
				if (before == null && after != null)
				{
					Class<?> retClass = after.getClass();

					boolean superclassIsObject = retClass.getSuperclass() == Object.class;
					//int declaredFieldCount = retClass.getDeclaredFields().length;

					System.out.printf("Candidate method %s matched null->non-null. Return type runtime: %s, superclass=%s%n",
						m.getName(), retClass.getName(), retClass.getSuperclass() == null ? "null" : retClass.getSuperclass().getName());

					if (superclassIsObject)
					{
						System.out.printf("discovered and Selected method %s from class %s as probable match. ReturnType: %s%n", m.getName(), actorClass.getName(), m.getReturnType().getName());
						// Optionally reset animation to 0 (clean up)
						try { actor.setAnimation(-1); } catch (Throwable t) {}
						GetActionAnimIfValid_Meth = m;
						//return;
					}
					else
					{
						System.out.printf("Method %s passed null->non-null but failed extra heuristics (topLevel=%b).%n",
							m.getName(), superclassIsObject, minFieldCount);
					}
				}

				// Reset animation after test to avoid leaving state changed. Tune as needed.
				try { actor.setAnimation(-1); } catch (Throwable t) {}

			}
			catch (IllegalAccessException iae)
			{
				System.out.printf("IllegalAccess for method %s: %s%n", m.getName(), iae.getMessage());
			}
			catch (Exception ex)
			{
				System.out.printf("Unexpected exception testing method %s: %s%n", m.getName(), ex);
			}
		}

		if(GetActionAnimIfValid_Meth == null) {
			System.out.println("unable to discover GetActionAnimIfValid_Meth");
		}

		return;*/
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

	int SUBREGION_SIZE = 8;

	void send_DespawnSubRegion_Packet(WorldPoint chunkBase) { //generally just used to despawn extended chunks.
		//System.out.println("despawning chunk at:  " + chunkBase);

		if (!activeSubRegions.contains(chunkBase))
		{
			return;
		}
		activeSubRegions.remove(chunkBase);

		//unreal clears objects when it despawns subregions. we reflect that here
		for(int z = 0; z < 4; z++ ) {
			for(int x = 0; x < CHUNK_SIZE; x++ ) {
				for(int y = 0; y < CHUNK_SIZE; y++ ) {
					//WorldPoint worldLocation = new WorldPoint(chunkBase.getX()+x, chunkBase.getY()+y, z);
					populatedObjsTiles.remove(new PopulatedTile(chunkBase.getX()+x, chunkBase.getY()+y, z));
				}
			}
		}

		Buffer actorSpawnPacket = new Buffer(new byte[20]);
		actorSpawnPacket.writeByte(7); //write chunk data type

		actorSpawnPacket.writeShort(-1); //write worldView. -1 for now.

		actorSpawnPacket.writeShort(chunkBase.getX());
		actorSpawnPacket.writeShort(chunkBase.getY());
		actorSpawnPacket.writeByte(0);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		System.out.println("De-spawning subregion "+chunkBase);
	}

	boolean send_SpawnSubRegion_Packet(WorldPoint chunkBase, boolean skipIfInActiveCHunks)
	{
		if(chunkBase.getX()%SUBREGION_SIZE!=0 || chunkBase.getY()%SUBREGION_SIZE!=0) {return false;} //we only care about subregion, which are every SUBREGION_SIZE tiles

		if(skipIfInActiveCHunks) { //when skipIfInActiveCHunks is false, the chunk spawn packet is sent regardless if it is already sent/in ActiveCHunks. For the sake of having terrain in rmrecs, we set this to false for chunks inside the main scene.
			//spawn subregion's gameObjects if they are on a chunk in the extended scene
/*			clientThread.invokeAtTickEnd(() -> {
				for (int iX = 0; iX < 2 ; iX++) {
					for (int iY = 0; iY < 2 ; iY++)
					{
						WorldPoint chunk = new WorldPoint(chunkBase.getX()+(iX*8), chunkBase.getY()+(iY*8), 0);

						int sceneX = chunk.getX() - client.getBaseX();
						int sceneY = chunk.getY() - client.getBaseY();

						boolean isInMainScene = (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104);

						if(!isInMainScene) {//if chunk is in extended scene, simulate spawn events on it.
							if(!populatedObjsChunks.contains(chunk)) {
								simulateSpawnEventsForChunk(chunk);
								populatedObjsChunks.add(chunk);
							}
						}else
						{
							populatedObjsChunks.add(chunk);
						}
					}
				}
			});*/
			if (activeSubRegions.contains(chunkBase))
			{
				return false;
			}
		}


		//System.out.println("spawning chunk at:  " + chunkBase);
		activeSubRegions.add(chunkBase);
		Buffer actorSpawnPacket = new Buffer(new byte[20]);
		actorSpawnPacket.writeByte(7); //write chunk data type

		actorSpawnPacket.writeShort(-1); //write worldView. -1 for now.

		actorSpawnPacket.writeShort(chunkBase.getX());
		actorSpawnPacket.writeShort(chunkBase.getY());
		actorSpawnPacket.writeByte(0);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorSpawn");
		System.out.println("spawning SubRegion at "+chunkBase);

		return true;
	}

	public boolean isActionAnimValid(Actor actorInstance) //checks if actior has a valid action anim using internal function that includes use of the sequenceDelay==0 check
	{
		if (GetActionAnimIfValid_Meth == null) //if the method field wasnt found, use to basic check.
		{
			log_Timed_Heavy("GetActionAnimIfValid_Meth is null");
			return actorInstance.getAnimation()!=-1;
		}

		if(actorInstance == null) {
			return false;
		}

		try
		{
			boolean valid = GetActionAnimIfValid_Meth.invoke(null, actorInstance,  (byte)4)!=null; //null is first param because is static func
/*			if(actorInstance.getName().contains("dorvis")) {
				if(valid == false) {
					System.out.println("vard action anim valid = false");
				}
			}*/
			return valid;
		}
		catch (InvocationTargetException | IllegalAccessException e)
		{
			System.err.printf("Invocation failed for %s: %s%n", actorInstance, e);
			return false;
		}
	}


	void despawnTaggedTileObj(long tag) {
		Buffer actorSpawnPacket = new Buffer(new byte[20]);

		actorSpawnPacket.writeByte(4); //write tileObject data type
		actorSpawnPacket.writeShort(-1);//worldBview, unused
		actorSpawnPacket.writeByte(-1); //unused tilez
		actorSpawnPacket.writeShort(-1); //unused tilex
		actorSpawnPacket.writeShort(-1);//unused tiley
		actorSpawnPacket.writeLong(tag);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
/*		Tile tile = client.getSelectedSceneTile();
		if(tile!=null) {
			Point scenePoint = tile.getSceneLocation();
			//Tile tile_defaultState = defaultTileState[tile.getPlane()][scenePint.getX()][scenePint.getY()]; //client.getScene().getExtendedTiles()[tile.getPlane()][scenePint.getX()+SCENE_OFFSET][scenePint.getY()+SCENE_OFFSET];

			long wallObjHash = 0;
			if(tile.getWallObject()!=null) {
				wallObjHash = getTag_Unique(tile.getWallObject());
			}
			defaultTileState defaultTile = tile_DefaultState[tile.getPlane()][scenePoint.getX()][scenePoint.getY()];
			if(defaultTile!=null) {
				long defaultStateWallObjHash = defaultTile.wallObj_Default;
				System.out.println("wallObjHash: "+wallObjHash + " defaultStateWallObjHash: "+defaultStateWallObjHash);
			}
		}*/
/*		WorldView mainWorldView = client.getTopLevelWorldView();
		for(WorldEntity we : mainWorldView.worldEntities()) {
			WorldView boatWorldView = we.getWorldView();
			for(Player player : boatWorldView.players()) {
				if(player.getName().equalsIgnoreCase("noodleeater")) {
					System.out.println("nood is in wv idx "+boatWorldView.getId() );
					System.out.println("noEntities in nood's' world: "+boatWorldView.worldEntities().stream().count());

					LocalPoint playerLocalLocation = player.getLocalLocation();
					System.out.println("nood loc: "+playerLocalLocation);

					LocalPoint local_MainWv = we.transformToMainWorld(playerLocalLocation);
					System.out.println("nood loc_MainWv: "+local_MainWv);

					mainWorldView.getMainWorldProjection()
					//LocalPoint locInWv = wv.
					//System.out.println();
				}
			}
		}*/

/*		if(ticksSinceLoadScene > 100 && client.getTickCount()%10 == 0) {
			WorldPoint playerTile = getPlayerLocationInWorld();
			int softBaseX = ((playerTile.getX()/8)*8)-52;
			int softBaseY = ((playerTile.getY()/8)*8)-52;
			sendBaseCoordinatePacket(softBaseX, softBaseY, client.getScene());
			resendGameStateChanged();
		}*/

/*		String typed = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
		if(typed.equalsIgnoreCase("send")) {
			client.setVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT,"");
			spawn_All_SubRegionModels();
		}*/
/*		String typed = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
		if(typed.equalsIgnoreCase("doWork")) {
			client.setVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT,"");
			for(PopulatedTile popTile : populatedObjsTiles) {
				popTile.isEnteringMainScene();
			}
		}*/



		discoverField_ActionAnimValid();


		if(config.reduceFpsWhenIdle()) {
			if(storedMaxFps != 50 && client.getTickCount()%6 == 0) {
				if(System.currentTimeMillis() - timeLastInteracted > 5000) { //reduce fps over time if window appears to be inactive/unused. Prevents overwoking pc when rs is idle.
					log.debug("using lower fps lock because afk");
					setMaxFps((int)((float)client.getFPS()*0.8));
				}
			}
		}

		loggedInForNoServerTicks++;
		//spawn stuff on extended tiles. does not yet handle despawn
	}

	WorldPoint getPlayerLocationInWorld() { //get local playerLocation, accounting for conversion needed if player is in a boat
		WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();

		WorldView wv = client.getLocalPlayer().getWorldView();
		if(!wv.isTopLevel()) {
			WorldEntity we = client.getTopLevelWorldView().worldEntities().byIndex(wv.getId());
			LocalPoint localPos = we.transformToMainWorld(client.getLocalPlayer().getLocalLocation());
			playerLoc = WorldPoint.fromLocal(client, localPos);
		}
		return playerLoc;
	}

	public static double getWorldPointDistance(WorldPoint a, WorldPoint b)
	{
		if (a == null || b == null)
		{
			throw new IllegalArgumentException("WorldPoints cannot be null");
		}

		// Different planes = effectively unreachable
/*		if (a.getPlane() != b.getPlane())
		{
			return Double.MAX_VALUE;
		}*/

		int dx = a.getX() - b.getX();
		int dy = a.getY() - b.getY();

		return Math.sqrt((dx * dx) + (dy * dy));
	}

	int maxChunkDist = -1;
	int maxPopulateDIst = -1;

	void processExtendedChunkSpawnTask(int maxChunksToSpawn)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		//ArrayList<WorldPoint> spawnedChunks = new ArrayList();
		//WorldEntity worldEntity
		WorldPoint playerLoc = getPlayerLocationInWorld();

		int noChunksSpawned = 0;

		for (int dx = -maxChunkDist; dx <= maxChunkDist; dx++)
		{
			for (int dy = -maxChunkDist; dy <= maxChunkDist; dy++)
			{
				int playerChunkX = playerLoc.getX() / CHUNK_SIZE;
				int playerChunkY = playerLoc.getY() / CHUNK_SIZE;

				int chunkX = playerChunkX + dx;
				int chunkY = playerChunkY + dy;

				WorldPoint chunkBase = new WorldPoint(chunkX * CHUNK_SIZE, chunkY * CHUNK_SIZE, 0);

				WorldPoint chunkCentre = new WorldPoint((chunkX * CHUNK_SIZE)+4, (chunkY * CHUNK_SIZE)+4, 0);
				double distance = getWorldPointDistance(chunkCentre, playerLoc);
				boolean isInRange = distance < appSettings.drawDistance;
				if(!isInRange) {
					continue;
				}

				if(isInstanced) {
					int sceneX = chunkBase.getX() - client.getBaseX();
					int sceneY = chunkBase.getY() - client.getBaseY();

					boolean isInMainScene = (sceneX >= 0 && sceneX <= 104-8 && sceneY >= 0 && sceneY <= 104-8);
					if(!isInMainScene) {continue;} //disallows extended subregion loading when in instanced areas.
				}

				boolean spawnedChunk = send_SpawnSubRegion_Packet(chunkBase, true);
				if(spawnedChunk) {
					noChunksSpawned++;
					if(noChunksSpawned >= maxChunksToSpawn) {
						return;
					}
				}
/*				if (spawnedSubRegion)
				{
					noSubregionsSpawned++;
					if(noSubregionsSpawned == 10) {
						return;
					}
					//return;
				}*/
			}
		}
		return;
		//activeExtendedChunks.addAll(spawnedChunks);
	}

	void processExtendedChunkDespawnTask(int maxChunksToDespawn) //find chunks that need to be despawned and despawns them.
	{
		if (activeSubRegions.isEmpty() || maxChunksToDespawn <1)
		{
			return;
		}

		if(client.getLocalPlayer() == null) {
			return;
		}

		WorldPoint playerLoc = getPlayerLocationInWorld();

		int noChunksDeSpawned = 0;
		WorldPoint[] activeSubRegionsArr = activeSubRegions.toArray(WorldPoint[]::new);
		for (WorldPoint chunkBase : activeSubRegionsArr)
		{
			//if(chunkBase.getX()%SUBREGION_SIZE!=0 || chunkBase.getY()%SUBREGION_SIZE!=0) {continue;}

			int sceneX = chunkBase.getX() - baseX;
			int sceneY = chunkBase.getY() - baseY;
			boolean isInMainScene = (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104);
			if(isInstanced && isInMainScene) { //when in instanced areas, we only destroy chunks that have left the scene. destroying a chunk in the scene is problematic, because if it needs to be loaded again, we would have to implement a system to re-copy the template to the tiles in the chunk's area.
				continue;
			}

			//int playerChunkX = playerLoc.getX() / CHUNK_SIZE;
			//int playerChunkY = playerLoc.getY() / CHUNK_SIZE;

			int chunkX = chunkBase.getX() / CHUNK_SIZE;
			int chunkY = chunkBase.getY() / CHUNK_SIZE;

			WorldPoint chunkCentre = new WorldPoint((chunkX * CHUNK_SIZE)+4, (chunkY * CHUNK_SIZE)+4, 0);
			double distance = getWorldPointDistance(chunkCentre, playerLoc);
			boolean isInRange = distance < appSettings.drawDistance;

			if (!isInRange/* && !isInMainScene*/)
			{
				send_DespawnSubRegion_Packet(chunkBase);
				noChunksDeSpawned++;
				if(noChunksDeSpawned >= maxChunksToDespawn) {
					return;
				}
/*				if(noSubregionsDeSpawned == 40) {
					return;
				}*/
			}
		}

		//activeChunks.removeAll(DespawnedChunks);
	}

	int ticksSinceLoadScene = 0;

	boolean isCacheFullyLoaded = false;

	void RmUiPacket_SettingsWindow() {
		Buffer packet = new Buffer(new byte[8]);

		packet.writeByte(1); //UiType: SettingsWindow
		packet.writeBoolean(isRuneModSettingsOpen_PrevFrame); //isUiOpen

		sharedmem_rm.backBuffer.writePacket(packet, "RmUi");
	}

	boolean isRuneModSettingsOpen_PrevFrame = false;
	Canvas canvas_prevFrame;

	long gameCycle_50fps;//usefull when using gamecycle with modulo as a way to time things. gameCycle would be inaccurate because it can increase at more than 50fps if user has changed fps value.

	boolean changedDrawDist = false;


	private static final int MAX_DrawDist = 68;

	private static final int[][] SPIRAL = generateSpiral(MAX_DrawDist);

	private static int[][] generateSpiral(int radius)
	{
		int size = (radius * 2 + 1) * (radius * 2 + 1);

		int[][] result = new int[size][2];

		int index = 0;

		int x = 0;
		int y = 0;

		result[index][0] = 0;
		result[index][1] = 0;
		index++;

		int stepSize = 1;

		while (index < size)
		{
			// Right
			for (int i = 0; i < stepSize && index < size; i++)
			{
				x++;

				if (Math.max(Math.abs(x), Math.abs(y)) <= radius)
				{
					result[index][0] = x;
					result[index][1] = y;
					index++;
				}
			}

			// Down
			for (int i = 0; i < stepSize && index < size; i++)
			{
				y++;

				if (Math.max(Math.abs(x), Math.abs(y)) <= radius)
				{
					result[index][0] = x;
					result[index][1] = y;
					index++;
				}
			}

			stepSize++;

			// Left
			for (int i = 0; i < stepSize && index < size; i++)
			{
				x--;

				if (Math.max(Math.abs(x), Math.abs(y)) <= radius)
				{
					result[index][0] = x;
					result[index][1] = y;
					index++;
				}
			}

			// Up
			for (int i = 0; i < stepSize && index < size; i++)
			{
				y--;

				if (Math.max(Math.abs(x), Math.abs(y)) <= radius)
				{
					result[index][0] = x;
					result[index][1] = y;
					index++;
				}
			}

			stepSize++;
		}

		return result;
	}

	boolean playerHasTeleported = false;
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

		if(client.getLocalPlayer() != null) {
			ticksSinceLoadScene++;
			if(ticksSinceLoadScene < 12) {
				System.out.println("ticksSinceLoadScene = "+ticksSinceLoadScene);
			}
		}

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
				//log.debug("WindowIsMinimized, sleeping...");
			}

			if(client.getGameCycle() % 3 != 0) //runemods recycle bin system only works on frames that have time to spare. We ponly limit framerate every other frame because wee don't want to constantly prevent the recycle bin from working while in the sleep state.
			{
				Thread.sleep(60);
			}
		}


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

	void processSpawnSimulations() {
		if (curGamestate == GameState.LOGGED_IN && client.getLocalPlayer() != null/* && ticksSinceLoadScene > 0*/)
		{
			WorldPoint playerLocation = getPlayerLocationInWorld();
			playerHasTeleported = playerLocation.distanceTo(playerLocation_prevTick) > 7;

			if(playerHasTeleported) {
				log.debug("player has teleported");
			}


			int maxChunksToSpawn = 2;
			int maxTileSpawnsPerFrame =  16;

			int maxChunkDeSpawnsPerFrame = 0;

			if(client.getGameCycle()%4 == 0) {
				maxChunkDeSpawnsPerFrame = 1;
			}

			if(changedDrawDist || playerHasTeleported) { //if the draw dist setting is being changed allow loading to be instance and hitch once.
				maxChunkDeSpawnsPerFrame = 999999;
				maxTileSpawnsPerFrame = 999999;
				maxChunksToSpawn =  999999;
				if(changedDrawDist) {
					changedDrawDist = false;
				}
			}

			processExtendedChunkSpawnTask(maxChunksToSpawn);
			processExtendedChunkDespawnTask(maxChunkDeSpawnsPerFrame);

			//spawn tile objects
			eventIsSimulation = true;
			if(client.getScene().getExtendedTiles()[0][1].length == Constants.EXTENDED_SCENE_SIZE) {
				//boolean breakLoop = false;
				Tile[][][] extendedTiles = client.getScene().getExtendedTiles();
				Tile[][][] Tiles = client.getScene().getTiles();

/*				System.out.println("player is null: " +client.getLocalPlayer() != null);
				System.out.println("extendedSceneValid: "+(client.getScene().getExtendedTiles()[0][1].length == Constants.EXTENDED_SCENE_SIZE));
				WorldPoint playerLoc = getPlayerLocationInWorld();
				System.out.println("PlayerLoc: "+playerLoc);*/

				int playerSceneX = playerLocation.getX() - client.getBaseX();
				int playerSceneY = playerLocation.getY() - client.getBaseY();

				int noTileSpawns = 0;
				int noTileDespawns = 0;
				for (int i = 0; i < SPIRAL.length; i++)
				{
					for (int z = 0; z < 4; z++)
					{
						int sceneX = playerSceneX + SPIRAL[i][0];
						int sceneY = playerSceneY + SPIRAL[i][1];

						int sceneX_Extended = sceneX+SCENE_OFFSET;
						int sceneY_Extended = sceneY+SCENE_OFFSET;
						if(sceneX_Extended >= Constants.EXTENDED_SCENE_SIZE-1|| sceneY_Extended >= Constants.EXTENDED_SCENE_SIZE-1) { //skip invalid range tiles. extended tiles at max index aldo seem problematic/dont have objects, so I skip those
							continue;
						}
						if(sceneX_Extended <=0 || sceneY_Extended <=0) {  //skip invalid range tiles. extended tiles at zero seem problematic/dont have objects, so I skip those
							continue;
						}

						if(noTileSpawns > maxTileSpawnsPerFrame) {break;}//hacky way to spread out spawns over multiple frames when loading spawning on the fly.

						boolean isInMainScene = (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104);

						Tile tile;
						if(isInMainScene) {
							tile = Tiles[z][sceneX][sceneY];
						}else {
							//if(true) {continue;}
							tile = extendedTiles[z][sceneX_Extended][sceneY_Extended];
						}

						if(tile == null) {continue;}

						WorldPoint worldLocation = tile.getWorldLocation();

						WorldPoint subRegionBase = new WorldPoint((worldLocation.getX()/8)*8, (worldLocation.getY()/8)*8, 0);
						if(!activeSubRegions.contains(subRegionBase)) {
							continue;
						}

						double distance = getWorldPointDistance(worldLocation, playerLocation);
						if(distance < maxPopulateDIst) {
							PopulatedTile populatedTile = new PopulatedTile(tile);
							if(!populatedObjsTiles.contains(populatedTile)) {
								simulateTilObjectSpawns(tile);
								noTileSpawns++;
								populatedObjsTiles.add(populatedTile);
							}
						}
					}
				}
			}
			eventIsSimulation = false;
		}
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
		}else {
			log.debug("BackBuffer Is Overflowed, not sending perframe packet");
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

	int prevVal = -1;
	@SneakyThrows
	@Override
	public void draw(int overlayColor)
	{
		if(client.getTopLevelWorldView() == null) {
			return;
		}

		//if(client.getTopLevelWorldView()!=null) { //update all the worldviews
		for(WorldView wv : client.getTopLevelWorldView().worldViews()) {
				boolean shouldDraw = hooks.draw(wv.getScene(), false);
				if(shouldDraw == false) {
					worldViewUpdate(wv, 2); //2 means send visibility state of worldView
				}/*else { //is done in preSceneDraw
					if(wv.getMainWorldProjection()!=null) {
						worldViewUpdate(wv, 1); //1 means update existing worldview
					}
				}*/
		}
		//}

		if(config.nullifyDrawCallbacks()) {return;}

		if (overlayColor_LastFrame != overlayColor)
		{
			overlayColor_LastFrame = overlayColor;
			//overlayColourChanged();
		}

		log_Timed_Heavy("draw");
		int val = sharedmem_rm.myKernel32.WaitForSingleObject(sharedmem_rm.EventUeDataReady, 0);
		if(val == 0 || prevVal == -1) {
			prevVal = val;
			UpdateSharedMemoryUiPixels();
		}

		if (!alreadyCommunicatedUnreal)
		{
			client.getTopLevelWorldView().getScene().setDrawDistance(appSettings.drawDistance+8);

			if(curGpuFlags == 17) {//bodge code for zbuff gpu mode. makes all projectiles visible aswell as all graphicsobjects
				for(Projectile obj: client.getProjectiles()) {
					visibleActors.add(obj);
				}
				for(WorldView wv : client.getTopLevelWorldView().worldViews()) {
					for(GraphicsObject obj: wv.getGraphicsObjects()) {
						visibleActors.add(obj);
					}
				}
			}

			communicateWithUnreal("draw");

			visibleActors.clear();
			tilesWithAnimateGameObjects.clear();
			AnimatedTileObjects.clear();
		}
		//communicateWithUnreal("Draw");
	}

	private class intVec3
	{
		int x;
		int y;
		int z;

		intVec3(int x_, int y_, int z_) {
			x = x_;
			y = y_;
			z = z_;
		}
	}

	@Override
	public void preSceneDraw(
		Scene scene,
		float cameraX, float cameraY, float cameraZ, float cameraPitch, float cameraYaw,
		int minLevel, int level, int maxLevel, Set<Integer> hideRoofIds) {

		int wvId = scene.getWorldViewId();
		worldViewUpdate(client.getWorldView(wvId), 1); //1 means update existing worldview
	}

	@SneakyThrows
	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		if(config.nullifyDrawCallbacks()) {return;}

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
	}

	@SneakyThrows
	@Override
	public void postDrawScene()
	{
		if(config.nullifyDrawCallbacks()) {return;}
		log_Timed_Heavy("postDrawScene");
	}

	@Override
	public void postSceneDraw(Scene scene)
	{
		if(scene.getWorldViewId() == WorldView.TOPLEVEL) { //it seems this is the correct location to simulate spawns. doing it earlie can result in default state/wrong obj rotations
			calcWorldVisibility();
			processSpawnSimulations();

			if (client.getLocalPlayer() != null && client.getGameState() == GameState.LOGGED_IN)
			{
				WorldPoint playerLocation = getPlayerLocationInWorld();
				playerLocation_prevTick = playerLocation;
				playerHasTeleported = false;
			}
		}
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

	void DespawnServerSpawnedObjs(boolean outsideSceneOnly) { //outsideSceneOnly. set to true if you only want objs outside scene to be despawned. serverspawned tile objects may not recieve a despawn event, thus the spawn will persist until the chunk is despawned by unreal. to fix this, we always despawn server=spawned objects as soon as they are outside the main scene.
		for(int i = 0; i < serverSpawnedGameObjects.size(); i++) {
			GameObjectSpawned event = serverSpawnedGameObjects.get(i);
			GameObjectDespawned despawnEvent = new GameObjectDespawned();
			if(outsideSceneOnly) {
				long tag = serverSpawnedGameObjects_Tags.get(i);
				int worldY = (int)((tag >> 26) & 0x7FFFL);
				int worldX = (int)((tag >> 11) & 0x7FFFL);
				int sceneX = worldX-client.getBaseX();
				int sceneY = worldY-client.getBaseY();
				boolean isInMainScene = (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104);
				if(isInMainScene) {
					//System.out.println("not in scene so not despawning");
					continue;
				}
			}

			despawnEvent.setTile(event.getTile());
			despawnEvent.setGameObject(event.getGameObject());

			onGameObjectDespawned(despawnEvent, serverSpawnedGameObjects_Tags.get(i));

			System.out.println("despawning serverSpawnedObj: "+event.getGameObject().getId());

/*			if(event.getGameObject().getId() == 26185) {
				System.out.println("serverSpawnClear for fire with tag: "+serverSpawnedGameObjects_Tags.get(i));
			}*/
		}
		serverSpawnedGameObjects.clear();
		serverSpawnedGameObjects_Tags.clear();
		//System.out.println("ClearedServerSpawnedGameObjects");
	}

	@Override
	public void despawnWorldView(WorldView worldView)
	{
		log.debug("despawnWorldView wv:"+worldView.getId());
		worldViewUpdate(worldView, 3); //3 means despawn worldview
	}

	@Override
	public void loadScene(WorldView worldView, Scene scene)
	{
			/*clientThread.invoke(() ->
			{
				//log.debug("loadScene. worldView id:" + worldView.getId() + " baseX:" + worldView.getBaseX() + "baseY:" + worldView.getBaseY());
			});*/
	}

	void worldViewUpdate(WorldView wv, int updateType)
	{
		if(wv.isTopLevel()) {return;}//toplevel does have/need a worldView actor


		if(updateType == 0) {
			//System.out.println("worldViewSpawned");
		}

		if(updateType == 1) {
			//log.debug("updatingWorldViewTransform for wv "+wv.getId() + " baseX:"+wv.getBaseX()+ " baseY:"+wv.getBaseX() + " SceneX:"+wv.getScene().getBaseX() + " SceneY:"+wv.getScene().getBaseY());
		}

		if(updateType == 2) {
			//log.debug("updatingWorldView visibility for wv "+wv.getId());
		}

		Buffer packet = new Buffer(new byte[200]);

		packet.writeByte(updateType); //0 = spawn. 1 = update transform. 2 = de-spawn

		packet.writeShort(wv.getId());

		//wv.getScene().ove

		if(updateType == 0) {
			packet.writeShort(wv.getSizeX());
			packet.writeShort(wv.getSizeY());
			packet.writeShort(wv.getBaseX());
			packet.writeShort(wv.getBaseY());
		}

		if(updateType == 1) {
			Projection projection =  wv.getMainWorldProjection();
			float[] location;

			location = projection.project((wv.getSizeX()*128)/2,0,(wv.getSizeY()*128)/2);//seem origin is supposed to be at wv centre, ortherwise rotation/bobbing motion is out of whack

			packet.writeInt((int)(location[0]*1000.0)); //x
			packet.writeInt((int)(location[1]*1000.0)); //y
			packet.writeInt((int)(location[2]*1000.0)); //Z

			FloatProjection matrix = (FloatProjection) wv.getMainWorldProjection();
			float[] matrixValues = matrix.getProjection();
			for(int i = 0; i < 16; i++) {
				packet.writeInt((int)(matrixValues[i]*1000.0));
			}

			packet.writeByte(wv.getScene().getOverrideHue());
			packet.writeByte(wv.getScene().getOverrideSaturation());
			packet.writeByte(wv.getScene().getOverrideLuminance());
			packet.writeByte(wv.getScene().getOverrideAmount());
		}

		if(updateType == 2) {
			packet.writeBoolean(hooks.draw(wv.getScene(), false));
			packet.writeByte(0);//unused
			packet.writeByte(0);//unused
		}

		sharedmem_rm.backBuffer.writePacket(packet, "WorldViewUpdate");//WorldViewSpawned code not yet implemented
	}

	void despawnAllSubRegions(){ //despawns all chunks the plugin is aware of/sent spawn packets for
		WorldPoint[] activeSubRegionssArr = activeSubRegions.toArray(WorldPoint[]::new);
		for(WorldPoint chunkBase : activeSubRegionssArr) {
			send_DespawnSubRegion_Packet(chunkBase);
		}
	}

	void sendSceneBaseInfo(int baseX, int baseY, Scene scene) { //sends base coordinate and chunk spawn packets
		//if the scene is instanced we have to destroy all old terrains. we do this because the ones around the scene edges will have some blank tiles which were not included in the instance-map when those terrains were last generated.
		ticksSinceLoadScene = 0;
		log.debug("sendSceneBaseInfo");

		if(isInstanced) {
			//log.debug("despawning all terrains because is instanced area");
			//despawnAllSubRegions();
		}

		//DespawnServerSpawnedObjs();
		//perhaps we need to do this for wall objects too?
		//despawn any gameobjects spawned by server. This fixes the scenario where a player leaves an area with player lit fires, and then comes back to it to find fire still there when they should be despawned.


		if(config.nullifyDrawCallbacks()) {return;}
		sendBaseCoordinatePacket(baseX, baseY, scene); //sends basecoordinate and instance map
		sendHiddenChunksPacket(scene);

		//spawn chunks in scene
/*		for (int x = 0*//* - 1*//*; x < (Constants.SCENE_SIZE / Constants.CHUNK_SIZE)*//* + 1*//*; ++x)
		{
			for (int y = 0*//* - 1*//*; y < (Constants.SCENE_SIZE / Constants.CHUNK_SIZE)*//* + 1*//*; ++y)
			{
				int chunkBaseX = (x * Constants.CHUNK_SIZE) + baseX;
				int chunkBaseY = (y * Constants.CHUNK_SIZE) + baseY;
				WorldPoint chunkBase = new WorldPoint(chunkBaseX, chunkBaseY, 0);

*//*				if (chunkBase.getX() % SUBREGION_SIZE != 0 || chunkBase.getY() % SUBREGION_SIZE != 0)
				{
					continue;
				}*//*

				send_SpawnSubRegion_Packet(chunkBase, false);
			}
		}*/
	}

	volatile boolean needSendBaseInfo = false;
	volatile Scene baseInfoScene = null;
	@Override
	public void loadScene(Scene scene)
	{
		log.debug("loadScene. wv: "+scene.getWorldViewId());
		if(divergentStuff.clientType == ClientType.RUNELITE) { //alora uses onLoadRegion instead, because it is single threaded as has no async map load.
			if(scene.getWorldViewId() == WorldView.TOPLEVEL) {
				needSendBaseInfo = true;
				baseInfoScene = scene;
			}
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
		log.debug("SwapScene. wv:"+scene.getWorldViewId());
	}

/*	void overlayColourChanged()
	{
		Buffer packet = new Buffer(new byte[8]);

		packet.writeInt(overlayColor_LastFrame);

		//extra blank data, for future use
		packet.writeInt(0);

		sharedmem_rm.backBuffer.writePacket(packet, "OverlayColorChanged");

		log.debug("overlayColourChanged");
	}*/

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

		regionTxtParser = null;

		storedMaxFps = -1;

		prevVal = -1;

		lastBaseOffsetX = -1;
		lastBaseOffsetY = -1;

		FashionScape_EquipmentIds_PrevFrame = null;
		FashionScape_Colors_PrevFrame = null;

		npcsWithOverrides_LastFrame = new HashMap<NPC, NpcOverrides_Copy>();

		playerLocation_prevTick = new WorldPoint(0,0,0);

		window = (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas());

		activeSubRegions.clear();
		populatedObjsTiles.clear();

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

		discovered_GetActionAnimIfValid = false;
		GetActionAnimIfValid_Meth = null;

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

		for(int i=0; i < 5; i++) {
			customizedActorIds.add(new ArrayList());
			for(int i1=0; i1 < 4096; i1++)
			{
				customizedActorIds.get(i).add((new ArrayList()));
			}
		}

		int mainWvIdx = 4095;
		for(int i=0; i < 2048; i++) { //preallocate players in main world
			customizedActorIds.get(0).get(mainWvIdx).add(-1);
		}
		for(int i=0; i < 80000; i++) {//preallocate npcs in main world
			customizedActorIds.get(0).get(mainWvIdx).add(-1);
		}
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

	void initDrawDistance() {
		maxChunkDist = (int)Math.ceil((float) appSettings.drawDistance /8.0f);
		maxPopulateDIst = appSettings.drawDistance - 6;
	}

	@SneakyThrows
	void startUp_Custom()
	{
		runeModPlugin = this;

		setDefaults();

		CheckUePreReqs();

		initDrawDistance();

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

		muteLoginScreenMusic(true);
		SwingUtilities.invokeLater(() ->
		{
			muteLoginScreenMusic(true);
			SwingUtilities.invokeLater(() ->
			{
				muteLoginScreenMusic(true);
			});
		});

		regionTxtParser = new regionTxtParser();

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
		client.setExpandedMapLoading(6);

		setMaxFps(appSettings.maxFps);

		setGpuFlags(0);
		setDrawCallbacks(this);
	}

	int storedMaxFps = -1;
	public void setMaxFps(int maxFps) {
		if (maxFps < 50) { maxFps = 50; }
		if(maxFps > 90) {maxFps = 90;}
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

	boolean isKnownBadGpu(String n) { //list of unsupported gpus thant cant run dx12 shadermodel
		return n.matches(".*\\br(7|9)\\b.*")
			|| n.contains("m2")
			|| n.contains("m3")
			|| n.contains("m360")
			|| n.contains("hd 7")
			|| n.contains("hd 6")
			|| n.contains("hd 5");
	}

	boolean hasDedicatedGpu() {
		try
		{
			for(String GPU_name : getWindowsGpuNames()) {
				log.debug("Has Gpu: "+GPU_name);
				String gpuName_lowCase = GPU_name.toLowerCase();

				if (isKnownBadGpu(gpuName_lowCase)) {
					continue;
				}

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

	void spawn_A_SubRegionModels(WorldPoint chunkBase) {
		SwingUtilities.invokeLater(() -> {
			Tile[][][] tiles = client.getScene().getExtendedTiles();
			FastModel fastModel = new FastModel();
			int chunkBaseX = chunkBase.getX();
			int chunkBaseY = chunkBase.getY();

			int chunkLocalX = (chunkBaseX-baseX)*128;
			int chunkLocalY = (chunkBaseY-baseY)*128;

			for (int z = 0; z < 4; z++)
			{
				for (int x = 0; x < SUBREGION_SIZE; x++)
				{
					for (int y = 0; y < SUBREGION_SIZE; y++)
					{
						//WorldPoint tileWorldPoint = new WorldPoint(chunkBaseX+x, chunkBaseY+y, z);

						// Convert world coordinates to extended scene coordinates

						int sceneX_extended = ((chunkBaseX+x) - baseX) + SCENE_OFFSET;
						int sceneY_extended = ((chunkBaseY+y) - baseY) + SCENE_OFFSET;

						int arrXLen = tiles[0].length;
						int arrYLen = tiles[0][0].length;
						if (sceneX_extended < 0 || sceneX_extended >= arrXLen || sceneY_extended < 0 || sceneY_extended >= arrYLen)
						{
							continue;
						}

						Tile tile = tiles[z][sceneX_extended][sceneY_extended];

						if(tile==null) {
							continue;
						}

		/*				if(tile.getSceneTilePaint()!=null || tile.getSceneTileModel()!=null) {
							intVec3[] tilePoints = new intVec3[6];
							intVec3 pointA = intVec3(0,tile.)
							fastModel.AddPoints()
						}*/

						{
							WallObject object = tile.getWallObject();
							if (object != null)
							{
								if (object.getRenderable1() != null)
								{
									fastModel.AddRenderable(object.getRenderable1(), object.getX() - 64 - chunkLocalX, object.getY() - 64 - chunkLocalY, object.getZ(), 0);
								}
								if (object.getRenderable2() != null)
								{
									fastModel.AddRenderable(object.getRenderable2(), object.getX() - 64 - chunkLocalX, object.getY() - 64 - chunkLocalY, object.getZ(), 0);
								}
							}
						}

						{
							GameObject[] gameObjects = tile.getGameObjects();
							for (GameObject object : gameObjects)
							{
								if(object!= null && object.getRenderable()!=null) {
									fastModel.AddRenderable(object.getRenderable(), object.getX() - 64 - chunkLocalX, object.getY() - 64 - chunkLocalY, object.getZ(), 0);
								}
							}
						}
					}
				}
			}

			fastModel.BuildMerged();

			send_SpawnModel_Packet(fastModel, 0,chunkBase.getX(), chunkBase.getY(),0);
		});
	}

	class FastModelPart {
		float[] verticesX;
		float[] verticesY;
		float[] verticesZ;
		int[] faceIndices1;
		int[] faceIndices2;
		int[] faceIndices3;

		int x;
		int y;
		int Height;
		int Orientation;

		FastModelPart(Model model, int x_, int y_, int Height_, int Orientation_) {
			verticesX = model.getVerticesX();
			verticesY = model.getVerticesY();
			verticesZ = model.getVerticesZ();
			faceIndices1 = model.getFaceIndices1();
			faceIndices2 = model.getFaceIndices2();
			faceIndices3 = model.getFaceIndices3();

			x = x_;
			y = y_;
			Height = Height_;
			Orientation = Orientation_;
		}

		FastModelPart(intVec3[] points, int x_, int y_, int Height_, int Orientation_)
		{
			int noVerts = points.length;
			verticesX = new float[noVerts];
			verticesY = new float[noVerts];
			verticesZ = new float[noVerts];

			for(int vertIdx = 0; vertIdx < noVerts; vertIdx++) {
				verticesX[vertIdx] = points[vertIdx].x;
				verticesY[vertIdx] = points[vertIdx].y;
				verticesZ[vertIdx] = points[vertIdx].z;
			}

			x = x_; //localX
			y = y_; //local
			Height = Height_;
			Orientation = Orientation_;
		}
	}

	class FastModel{ //a type of model with a simple structure. every triangle is separate, and correctly ordered. As such, the indices are simply 1,2,3,4,5,6,7,8 etc....
		ArrayList<FastModelPart> models = new ArrayList<>();

		short[] VerticesX;
		short[] VerticesY;
		short[] VerticesZ;

		int VerticesCount;
		int FaceCount;

		void AddPoints(intVec3[] points, int x, int y, int heightPos, int orient) {
			FastModelPart fastModelPart = new FastModelPart(points, x, y, heightPos, orient);
			models.add(fastModelPart);
		}

		void AddRenderable(Renderable renderable, int x, int y, int heightPos, int orient) {
			if (renderable instanceof Model)
			{
				Model model = (Model) renderable;
				if(model!=null)
				{
					FastModelPart fastModelPart = new FastModelPart(model, x, y, heightPos, orient);
					models.add(fastModelPart);
				}
			}
			else if (renderable instanceof DynamicObject)
			{
				Model model = ((DynamicObject) renderable).getModelZbuf();
				if(model!=null) {
					FastModelPart fastModelPart = new FastModelPart(model, x, y, heightPos, orient);
					models.add(fastModelPart);
				}
			}
		}

		void BuildMerged() {
			FaceCount = 0;
			int noModels = 0;
			for(FastModelPart model : models) {
				if(model==null) {continue;}
				FaceCount+=model.faceIndices1.length;
				noModels++;
			}
			System.out.println("making FastModel with "+FaceCount+" faces, made from "+noModels+ " models");
			VerticesCount=FaceCount*3;

			VerticesX = new short[VerticesCount];
			VerticesY = new short[VerticesCount];
			VerticesZ = new short[VerticesCount];

			int vertIndex = -1;

			int i = -1;
			for(FastModelPart model : models) {
				i++;
				if(model==null) {continue;}
				float[] verticesX_ = model.verticesX;
				float[] verticesY_ = model.verticesY;
				float[] verticesZ_ = model.verticesZ;


				//models_y and  models_Height should perhaps be swapped
				int offsetX = model.x;
				int offsetY = model.Height;
				int offsetZ = model.y;

				if(model.faceIndices1 != null) {
					for (int face_i = 0; face_i < model.faceIndices1.length; face_i++) { //for each face
						//index of each point on face
						int Indice1 = model.faceIndices1[face_i];
						int Indice2 = model.faceIndices2[face_i];
						int Indice3 = model.faceIndices3[face_i];

						vertIndex++;
						VerticesX[vertIndex] = (short)(verticesX_[Indice1]+offsetX);
						VerticesY[vertIndex] = (short)(verticesY_[Indice1]+offsetY);
						VerticesZ[vertIndex] = (short)(verticesZ_[Indice1]+offsetZ);

						vertIndex++;
						VerticesX[vertIndex] = (short)(verticesX_[Indice2]+offsetX);
						VerticesY[vertIndex] = (short)(verticesY_[Indice2]+offsetY);
						VerticesZ[vertIndex] = (short)(verticesZ_[Indice2]+offsetZ);

						vertIndex++;
						VerticesX[vertIndex] = (short)(verticesX_[Indice3]+offsetX);
						VerticesY[vertIndex] = (short)(verticesY_[Indice3]+offsetY);
						VerticesZ[vertIndex] = (short)(verticesZ_[Indice3]+offsetZ);
					}
				}else { //if no indices defined, assume each verts are already correctly ordered
					for (int vertIndexOnPart = 0; vertIndexOnPart < model.verticesX.length; vertIndexOnPart++)
					{
						vertIndex++;
						VerticesX[vertIndex] = (short)(verticesX_[vertIndexOnPart]+offsetX);
						VerticesY[vertIndex] = (short)(verticesY_[vertIndexOnPart]+offsetY);
						VerticesZ[vertIndex] = (short)(verticesZ_[vertIndexOnPart]+offsetZ);
					}
				}
			}
		}
	}

	void send_SpawnModel_Packet(FastModel MergedModel, int id, int x, int y, int z) {
		Buffer actorSpawnPacket = new Buffer(new byte[(MergedModel.VerticesCount*3*2)+10000]);
		actorSpawnPacket.writeLong(id);
		actorSpawnPacket.writeInt(x);
		actorSpawnPacket.writeInt(y);
		actorSpawnPacket.writeInt(z);
		actorSpawnPacket.writeShort(0);//rotation
		actorSpawnPacket.writeShort(128);//size
		actorSpawnPacket.writeInt(0);//options

		actorSpawnPacket.writeShort_Array(MergedModel.VerticesX);
		actorSpawnPacket.writeShort_Array(MergedModel.VerticesY);
		actorSpawnPacket.writeShort_Array(MergedModel.VerticesZ);

		System.out.println("spawning model with " + MergedModel.VerticesX.length+" verts");

		clientThread.invoke(() -> {
			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "SpawnModel");
		});
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

/*		if(!hasDedicatedGpu()) {
			disableRuneModPlugin();
			SwingUtilities.invokeLater(() ->
			{
				int response = JOptionPane.showConfirmDialog(null,
					"No dedicated GPU was detected. RuneMod requires a GPU to work.",
					"RuneMod Error",
					JOptionPane.DEFAULT_OPTION);
			});
		}*/

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

/*				if (event.getKey().equalsIgnoreCase("MaxFps"))
				{
					setMaxFps(config.MaxFps());
				}*/

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

/*				if (event.getKey().equalsIgnoreCase("drawDistance"))
				{
					initDrawDistance();
					changedDrawDist = true;
				}*/
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
		if(tile.getBridge()!=null) {
			simulateTilObjectSpawns(tile.getBridge());
		}
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
							//Point min = object.getSceneMinLocation(), max = object.getSceneMaxLocation();

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

		if(tile.getGroundItems()!=null) {
			for(TileItem item : tile.getGroundItems()) {
				if (item != null)
				{
					final ItemSpawned objectSpawned = new ItemSpawned(tile, item);
					onItemSpawned(objectSpawned);
				}
			}
		}
	}

/*
	void simulateTilObjectDeSpawns(Tile tile) {
		if(tile == null) {return;}
		if(tile.getBridge()!=null) {
			simulateTilObjectDeSpawns(tile.getBridge());
		}
		WallObject wallObject = tile.getWallObject();
		if (wallObject != null)
		{
			final WallObjectDespawned objectDespawned = new WallObjectDespawned();
			objectDespawned.setTile(tile);
			objectDespawned.setWallObject(wallObject);
			onWallObjectDespawned(objectDespawned);
		}

		DecorativeObject decorativeObject = tile.getDecorativeObject();
		if (decorativeObject != null)
		{
			final DecorativeObjectDespawned objectDespawned = new DecorativeObjectDespawned();
			objectDespawned.setTile(tile);
			objectDespawned.setDecorativeObject(decorativeObject);
			onDecorativeObjectDespawned(objectDespawned);
		}

		GroundObject groundObject = tile.getGroundObject();
		if (groundObject != null)
		{
			final GroundObjectDespawned objectDespawned = new GroundObjectDespawned();
			objectDespawned.setTile(tile);
			objectDespawned.setGroundObject(groundObject);
			onGroundObjectDespawned(objectDespawned);
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
							//Point min = object.getSceneMinLocation(), max = object.getSceneMaxLocation();

							final GameObjectDespawned objectDespawned = new GameObjectDespawned();
							objectDespawned.setTile(tile);
							objectDespawned.setGameObject(object);
							onGameObjectDespawned(objectDespawned);
						}
						else
						{
*/
/*									if(object.getRenderable() instanceof Actor) {
										log.debug("unhandled renderableClass: Actor");
									}*//*

						}
					}
				}
				//}
			}
		}
	}
*/

/*	void simulateSomeTilObjectSpawns(Tile tile) {
			if(tile == null) {return;}
			WallObject wallObject = tile.getWallObject();
			if (wallObject != null)
			{
				final WallObjectSpawned objectSpawned = new WallObjectSpawned();
				objectSpawned.setTile(tile);
				objectSpawned.setWallObject(wallObject);
				onWallObjectSpawned(objectSpawned);
			}

*//*			DecorativeObject decorativeObject = tile.getDecorativeObject();
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
			}*//*

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
*//*									if(object.getRenderable() instanceof Actor) {
										log.debug("unhandled renderableClass: Actor");
									}*//*
							}
						}
					}
					//}
				}
			}
	}*/

	void getTileObjectTagsOnTile(Tile tile, Set<Long> setToAddTo) {
		if(tile == null) {return;}
		WallObject wallObject = tile.getWallObject();
		if (wallObject != null)
		{
			setToAddTo.add(getTag_Unique(wallObject, tile));
		}

		DecorativeObject decorativeObject = tile.getDecorativeObject();
		if (decorativeObject != null)
		{
			setToAddTo.add(getTag_Unique(decorativeObject, tile));
		}

		GroundObject groundObject = tile.getGroundObject();
		if (groundObject != null)
		{
			setToAddTo.add(getTag_Unique(groundObject, tile));
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
							setToAddTo.add(getTag_Unique(object, tile));
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

/*
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
*/
/*									if(object.getRenderable() instanceof Actor) {
										log.debug("unhandled renderableClass: Actor");
									}*//*

						}
					}
				}
				//}
			}
		}

*/
/*		ItemLayer itemLayer = tile.getItemLayer();
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
		}*//*

	}
*/

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

		despawnAllSubRegions();
		DespawnServerSpawnedObjs(false); //not sure if this is correct.

		powPrevFrame = -1;
		removeRoofsPrevFrame = false;
		animSmoothingPrevFrame = false;
		playerLocation_prevTick = new WorldPoint(0,0,0);
		worldVisibilityCalcedTick = -1;
		exteriorVisibility_Prev = -1;
		npcsWithOverrides_LastFrame.clear();
		populatedObjsTiles.clear();
		changedDrawDist = true;

		reSendVarBits();

		activeSubRegions.clear();
		//if(clientType == ClientType.ALORA) { //in Alora, setting gamestate to loading does not trigger loadRegion event.
			//Scene scene = client.getTopLevelWorldView().getScene();
			//sendSceneBaseInfo(scene.getBaseX(), scene.getBaseY(), scene);
		//}
		client.setGameState(GameState.LOADING);

		ArrayList<WorldView> worldViews = new ArrayList<>();
		worldViews.add(client.getTopLevelWorldView());
		for (WorldView wv : client.getTopLevelWorldView().worldViews()) {
			worldViews.add(wv);
		}

		for(WorldView worldView : worldViews) {
			if(!worldView.isTopLevel()) {
				WorldViewLoaded event = new WorldViewLoaded(worldView);
				onWorldViewLoaded(event);  //seems we dont automatically get spawn events fro world views when we change gametsate to loading so we simulate them

				Scene scene = worldView.getScene();

				for (int plane = 0; plane < 4; plane++) //seems we dont automatically get tileObject spawn events in boats when we set game state to loading, so we have to simulate them ourself
				{
					for (int sceneX = 0; sceneX < worldView.getSizeX(); sceneX++)
					{
						for (int sceneY = 0; sceneY < worldView.getSizeY(); sceneY++)
						{
							Tile tile = scene.getTiles()[plane][sceneX][sceneY];
							simulateTilObjectSpawns(tile);
						}
					}
				}
			}

			for (NPC npc : worldView.npcs())
			{
				if (npc != null)
				{
					final NpcSpawned npcSpawned = new NpcSpawned(npc);
					onNpcSpawned(npcSpawned);
				}
			}

			for (Player player : worldView.players())
			{
				if (player != null)
				{
					final PlayerSpawned playerSpawned = new PlayerSpawned(player);
					onPlayerSpawned(playerSpawned);
				}
			}
		}
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
						if(objDef.getName().toLowerCase().contains(name.toLowerCase())) {
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
							if (action.equals("Escape") || action.equals("Touch") || action.equals("Squeeze-through") || action.equals("Climb-through ") || action.equals("Climb-over") || action.equals("Jump-over") || action.equals("Hop-over") || action.equals("Enter") || action.equals("Climb") || action.equals("Step-into"))
							{
								//log.debug("disallowed dynamic spawn on obj: " + objDef.getName());
								disAllowedDynamicDeSpawns.add(i);
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
		if(client.getLocalPlayer()==null) {return;}
		if(client.getTopLevelWorldView() == null) {return;}
		if(client.getTopLevelWorldView().getScene().getTiles() == null) { return; } //prevents an error that can happen on first tick of login

		if(!client.getLocalPlayer().getWorldView().isTopLevel()) { //todo skipping muffling for now on non main worldviews
			return;
		}

		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int sceneX = (lp.getX()) / 128;
		int sceneY = (lp.getY()) / 128;
		WorldPoint playerLocation = getPlayerLocationInWorld();

		boolean isInMainScene = (sceneX >= 0 && sceneX < 104 && sceneY >= 0 && sceneY < 104); //prevents weird crash when logging into sailing
		if(!isInMainScene) {
			return;
		}

		//WorldPoint playerLocation = WorldPoint.fromLocal(client, client.getLocalPlayer().getLocalLocation());
		boolean playerHasMoved = !playerLocation.equals(playerLocation_prevTick);

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
		if(playerHasTeleported) { //if player has teleported
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
			log.debug("Login SCREEN...");

			clientPlane_prevFrame = -1; //prevents issue where login screen anims change clientplane, and so when we login, out plane is wrong.

			playerLocation_prevTick = new WorldPoint(0,0,0);

			despawnAllSubRegions();

			DespawnServerSpawnedObjs(false);

			SwingUtilities.invokeLater(() ->
			{
				SwingUtilities.invokeLater(() ->
				{
					muteLoginScreenMusic(!unrealIsReady); //unmutes login screen music when unreal is ready.
				});
			});

			loadAppSettings(); //load appSettings file

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

			client.resizeCanvas();
		}
		else if (curGamestate == GameState.LOGGING_IN)
		{
			log.debug("logging in...");
			initDisAllowedDynamicSpawns();
		}
		else if (curGamestate == GameState.LOGGED_IN)
		{
			log.debug("logged in...");

			DespawnServerSpawnedObjs(true);
/*			if(lastGameState == GameState.HOPPING) {
				DespawnServerSpawnedObjs(); //should really add a if was hopping
			}*/
		}
		else if (curGamestate == GameState.HOPPING)
		{
			log.debug("hopping...");
			//DespawnServerSpawnedObjs(false);
			//despawnAllSubRegions();
			playerLocation_prevTick = new WorldPoint(0,0,0);
			populatedObjsTiles.clear(); //unreal clears scene when Gamestate == hopping, so we reflect that be clearing populated tiles.
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
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{

		if (!config.spawnGameObjects())
		{
			return;
		}

		if(loggedInForNoServerTicks > 1 && disAllowedDynamicDeSpawns.contains(event.getWallObject().getId())) {return;}

			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[20]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getWallObject(), tile);
			actorSpawnPacket.writeByte(5); //write tileObject data type
			actorSpawnPacket.writeShort(event.getWallObject().getWorldView().getId());
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		//});
	}

	Set<WallObject> wallObjects = new HashSet<WallObject>();

	@Subscribe
	private void onWorldViewLoaded(WorldViewLoaded event)
	{
		log.debug("onWorldViewLoaded wv:"+event.getWorldView().getId());
		worldViewUpdate(event.getWorldView(), 0); //0 means spawn worldview
	}

	@Subscribe
	private void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		log.debug("onWorldViewUnLoaded wv:"+event.getWorldView().getId());
		worldViewUpdate(event.getWorldView(), 3); //3 means despawn worldview
	}

	GameState disAllowSpawnOnThisGamestate = GameState.LOADING;
	@Subscribe
	private void onWallObjectSpawned(WallObjectSpawned event)
	{
		if(needSendBaseInfo) {
			sendSceneBaseInfo(baseInfoScene.getBaseX(), baseInfoScene.getBaseY(), baseInfoScene);
			needSendBaseInfo = false;
			baseInfoScene = null;
		}

		if(event.getWallObject() == null) {return;}
		WorldView worldView = event.getWallObject().getWorldView();
		if(curGamestate == disAllowSpawnOnThisGamestate && worldView.isTopLevel()) { //toplevel world view uses simulated spawn event that happen when they come into range
			return;
		}

		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			if (!config.spawnGameObjects())
			{
				return;
			}

			//if(loggedInForNoServerTicks > 1 && disAllowedDynamicSpawns.contains(event.getWallObject().getId())) {return;}

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
			actorSpawnPacket.writeShort(worldView.getId());
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
			long tag = getTag_Unique(event.getWallObject(), tile);
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
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			Tile tile;
			tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[20]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getDecorativeObject(), tile);

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeShort(event.getDecorativeObject().getWorldView().getId());
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

		if(event.getDecorativeObject() == null) {return;}
		WorldView worldView = event.getDecorativeObject().getWorldView();
		if(curGamestate == disAllowSpawnOnThisGamestate && worldView.isTopLevel()) { //toplevel world view uses simulated spawn event that happen when they come into range
			return;
		}

		if (!config.spawnGameObjects())
		{
			return;
		}

		if(!eventIsSimulation && worldView.isTopLevel()) { //if this is a non simulated spawn, discard the spawn if the tile is not yet populated/out of range.
			WorldPoint tileLocation = event.getTile().getWorldLocation();
			boolean tileIsPopulated = populatedObjsTiles.contains(new PopulatedTile(tileLocation.getX(), tileLocation.getY(), tileLocation.getPlane()));
			if(!tileIsPopulated) {return;} //disregard spawns for tiles that are not populated. when the tile comes into range, we produce spawn events for it's tileItems.
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
			actorSpawnPacket.writeShort(worldView.getId());
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
			long tag = getTag_Unique(event.getDecorativeObject(), tile);
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

		for(Tile tile : tilesWithAnimateGameObjects) {
			findAnimatedGameObjectsOnTile(allGameObjects, tile);
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
							allGameObjects.put(getTag_Unique(obj, tile), d);
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
					allGameObjects.put(getTag_Unique(ground, tile), d);
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
					allGameObjects.put(getTag_Unique(deco, tile), d);
				}
			}
			r = deco.getRenderable2();
			if (r instanceof DynamicObject)
			{
				DynamicObject d = (DynamicObject) r;
				if (d.getAnimation() != null)
				{
					allGameObjects.put(getTag_Unique(deco, tile), d);
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
					allGameObjects.put(getTag_Unique(wall, tile), d);
				}
			}

			r = wall.getRenderable2();
			if (r instanceof DynamicObject)
			{
				DynamicObject d = (DynamicObject) r;
				if (d.getAnimation() != null)
				{
					allGameObjects.put(getTag_Unique(wall, tile), d);
				}
			}
		}
	}

	boolean eventIsSimulation = false;

	ArrayList<GameObjectSpawned> serverSpawnedGameObjects = new ArrayList<>();
	ArrayList<Long> serverSpawnedGameObjects_Tags = new ArrayList<>(); //the original tags are stored here. after moving to a new area, old gameObject refs become messed up, so we have to rely on pre-calculated tags, made before the scene was reloaded/rebased.

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		if(needSendBaseInfo) {
			sendSceneBaseInfo(baseInfoScene.getBaseX(), baseInfoScene.getBaseY(), baseInfoScene);
			needSendBaseInfo = false;
			baseInfoScene = null;
		}

		if(event.getGameObject() == null) {return;}
		WorldView worldView = event.getGameObject().getWorldView();
		if(curGamestate == disAllowSpawnOnThisGamestate && worldView.isTopLevel()) { //toplevel world view uses our own simulated spawn events that happen when they come into range
			return;
		}

		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
		if (!config.spawnGameObjects())
		{
			return;
		}

		if(!eventIsSimulation && worldView.isTopLevel()) { //if this is a non simulated spawn, discard the spawn if the tile is not yet populated/out of range.
			WorldPoint tileLocation = event.getTile().getWorldLocation();
			boolean tileIsPopulated = populatedObjsTiles.contains(new PopulatedTile(tileLocation.getX(), tileLocation.getY(), tileLocation.getPlane()));
			if(!tileIsPopulated) {return;} //disregard spawns for tiles that are not populated. when the tile comes into range, we produce spawn events for it's tileItems.
		}

			//if(loggedInForNoServerTicks > 1 && disAllowedDynamicSpawns.contains(event.getGameObject().getId())) {return;}


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


			int height = event.getGameObject().getZ() * -1;

			long tag = getTag_Unique(event.getGameObject(), tile);
			int cycleStart = 0;
			int frame = 0;

			int worldViewId = worldView.getId();

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeShort(worldViewId);
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
			if(!eventIsSimulation && client.getGameState().ordinal()>=GameState.LOGGED_IN.ordinal()/* && loggedInForNoServerTicks > 1*/) {
				if(worldView.isTopLevel()) { //disabled the server spawned gameObjects restriction on boats, since boats are pretty much always spawned while gamestaste is >= GameState.LOGGED_IN
					serverSpawnedGameObjects.add(event);
					serverSpawnedGameObjects_Tags.add(tag);
/*					if(event.getGameObject().getId() == 26185) {
						System.out.println("serverSpawn Add for fire with tag: "+tag);
					}*/
				}
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
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[20]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGroundObject(), tile);

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeShort(event.getGroundObject().getWorldView().getId());
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
		if(loggedInForNoServerTicks > 1 && disAllowedDynamicDeSpawns.contains(event.getGameObject().getId())) {return;}

		//System.out.println("gameObjectDespawned");
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
			Tile tile = event.getTile();

			Buffer actorSpawnPacket = new Buffer(new byte[20]);

			int tilePlane = tile.getRenderLevel();

			int tileX = tile.getSceneLocation().getX();
			int tileY = tile.getSceneLocation().getY();
			long tag = getTag_Unique(event.getGameObject(), tile);

			int worldView = event.getGameObject().getWorldView().getId();

			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeShort(worldView);
			actorSpawnPacket.writeByte(tilePlane);
			actorSpawnPacket.writeShort(tileX);
			actorSpawnPacket.writeShort(tileY);
			actorSpawnPacket.writeLong(tag);

			sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
			taggedTileObjects.remove(tag);
			//System.out.println("gameObjDespawned");
		//});
	}

	private void onGameObjectDespawned(GameObjectDespawned event, long tag)
	{
		if(loggedInForNoServerTicks > 1 && disAllowedDynamicDeSpawns.contains(event.getGameObject().getId())) {return;}
		//clientThread.invokeAtTickEnd(() -> //invoking later because baslocation likely hasnt been sent to unreal yet
		//{
		Tile tile = event.getTile();

		Buffer actorSpawnPacket = new Buffer(new byte[20]);

		int tilePlane = tile.getRenderLevel();

		int tileX = tile.getSceneLocation().getX();
		int tileY = tile.getSceneLocation().getY();
		//long tag = getTag_Unique(event.getGameObject());

		int worldView = event.getGameObject().getWorldView().getId();

		actorSpawnPacket.writeByte(4); //write tileObject data type
		actorSpawnPacket.writeShort(worldView);
		actorSpawnPacket.writeByte(tilePlane);
		actorSpawnPacket.writeShort(tileX);
		actorSpawnPacket.writeShort(tileY);
		actorSpawnPacket.writeLong(tag);

		sharedmem_rm.backBuffer.writePacket(actorSpawnPacket, "ActorDeSpawn");
		taggedTileObjects.remove(tag);
		//System.out.println("gameObjDespawned_ExplicitTag");
		//});
	}

	long Unique = 1000;
	long getTag_Unique(TileObject tileObject, Tile tile)
	{
		if(tileObject == null) {return 0;}
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

		int unused = 0;
		worldView = 0;

		long newTag = 0;
		newTag |= ((long)(tile.getPlane() & 0b11)) << 62;             // bits 62–63 //tileObj getPlane method doesnt work right with bridge tiles, so we use get plane method on tiles.
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

		if(event.getGroundObject() == null) {return;}
		WorldView worldView = event.getGroundObject().getWorldView();
		if(curGamestate == disAllowSpawnOnThisGamestate && worldView.isTopLevel()) { //toplevel world view uses simulated spawn event that happen when they come into range
			return;
		}

		if (!config.spawnGameObjects())
		{
			return;
		}

		if(!eventIsSimulation && worldView.isTopLevel()) { //if this is a non simulated spawn, discard the spawn if the tile is not yet populated/out of range.
			WorldPoint tileLocation = event.getTile().getWorldLocation();
			boolean tileIsPopulated = populatedObjsTiles.contains(new PopulatedTile(tileLocation.getX(), tileLocation.getY(), tileLocation.getPlane()));
			if(!tileIsPopulated) {return;} //disregard spawns for tiles that are not populated. when the tile comes into range, we produce spawn events for it's tileItems.
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
			long tag = getTag_Unique(event.getGroundObject(), tile);

			int cycleStart = 0;
			int frame = 0;
			actorSpawnPacket.writeByte(4); //write tileObject data type
			actorSpawnPacket.writeShort(worldView.getId());
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
		//System.out.println("itemSpawned");
		if(needSendBaseInfo) {
			sendSceneBaseInfo(baseInfoScene.getBaseX(), baseInfoScene.getBaseY(), baseInfoScene);
			needSendBaseInfo = false;
			baseInfoScene = null;
		}

		if (!config.spawnItems())
		{
			return;
		}

		int worldViewId = event.getTile().getLocalLocation().getWorldView();
		WorldView worldView = client.getWorldView(worldViewId);

		if(!eventIsSimulation && worldView.isTopLevel()) { //if this is a non simulated spawn, discard the spawn if the tile is not yet populated/out of range.
			WorldPoint tileLocation = event.getTile().getWorldLocation();
			boolean tileIsPopulated = populatedObjsTiles.contains(new PopulatedTile(tileLocation.getX(), tileLocation.getY(), tileLocation.getPlane()));
			if(!tileIsPopulated) {return;} //disregard spawns for tiles that are not populated. when the tile comes into range, we produce spawn events for it's tileItems.
		}

		//clientThread.invokeAtTickEnd(() ->
		//{
			Buffer actorSpawnPacket = new Buffer(new byte[50]);

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
			actorSpawnPacket.writeShort(event.getTile().getLocalLocation().getWorldView());
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
			actorSpawnPacket.writeShort(event.getTile().getLocalLocation().getWorldView());
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
		if (event.getNpc() == null)
		{
			return;
		}

/*		boolean shouldDraw = hooks.draw(event.getNpc(), false);
		if (!shouldDraw)
		{
			return;
		}*/

		if (!config.spawnNPCs())
		{
			return;
		}

		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		int instanceId = getOrAddCustomizedId(event.getNpc());
		int definitionId = event.getNpc().getId();
		actorSpawnPacket.writeByte(1); //write npc data type
		actorSpawnPacket.writeShort(event.getNpc().getWorldView().getId());
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
		int instanceId = getOrAddCustomizedId(event.getNpc());
		actorSpawnPacket.writeByte(1); //write npc data type
		actorSpawnPacket.writeShort(event.getNpc().getWorldView().getId());
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
		actorSpawnPacket.writeShort(event.getPlayer().getWorldView().getId());

		int InstanceId = getOrAddCustomizedId(event.getPlayer());
		if(InstanceId == -1) {return;} //added for Alora
		if(player.getPlayerComposition() == null) {return;}//added for Alora

		actorSpawnPacket.writeShort(InstanceId);

		byte isLocalPlayer = (client.getLocalPlayer()!=null && client.getLocalPlayer().getId() == player.getId()) ? (byte) 1 : (byte) 0;
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
		Buffer actorSpawnPacket = new Buffer(new byte[100]);

		int instanceId = getOrAddCustomizedId(event.getPlayer());
		actorSpawnPacket.writeByte(2); //write player data type
		actorSpawnPacket.writeShort(event.getPlayer().getWorldView().getId());
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
/*		if (sharedMemPixelsUpdatedTick == client.getGameCycle())
		{
			return;
		}
		sharedMemPixelsUpdatedTick = client.getGameCycle();*/

		if (!client.getCanvas().isShowing())
		{
			return;
		}

		sharedmem_rm.myKernel32.ResetEvent(sharedmem_rm.EventViewportPixelsReady);

		//SwingUtilities.invokeLater(() -> {
			UpdateUiPosOffsets();
		//});

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
		//DespawnServerSpawnedObjs();
		//});
	}

	//send hidden chunks/chunks in unrelated areas
	void sendHiddenChunksPacket(Scene scene) {
		ArrayList<Integer> hiddenChunks = regionTxtParser.getHiddenChunks(scene);
		Buffer packet = new Buffer(new byte[2000]);
		packet.writeShort(hiddenChunks.size());
		for(int i = 0; i < hiddenChunks.size(); i++) {
			int packed = hiddenChunks.get(i);
			int cx = packed >> 16;
			int cy = packed & 0xFFFF;
			int sceneX = (cx*8)-(baseX);
			int sceneY = (cy*8)-(baseY);
			packet.writeShort(sceneX);
			packet.writeShort(sceneY);
		}
		sharedmem_rm.backBuffer.writePacket(packet, "HiddenChunks");
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
	int drawDistance_PrevFrame = -1;

	float lerp(float a, float b, float f)
	{
		return a * (1.0f - f) + (b * f);
	}
	void SendRsOptionsPacket(boolean forceSend) {
		double pow = client.getTextureProvider().getBrightness(); //1.0 at low. 0.5 at high
		boolean removeRoofs = client.getVarbitValue(12378) > 0;
		boolean animSmoothing = client.getAnimationInterpolationFilter() != null;
		if(powPrevFrame != pow || removeRoofsPrevFrame != removeRoofs || animSmoothingPrevFrame != animSmoothing || drawDistance_PrevFrame!=appSettings.drawDistance || forceSend) { //if options have changed
			Buffer rsOptionsPacket = new Buffer(new byte[20]);

			float lerpVal = ((float)pow-0.5f)*2.0f;
			log.debug("BrightnessLerpVal "+lerpVal);
			double UeGamma = lerp(1.0f, 0.8f, lerpVal);
			rsOptionsPacket.writeByte((int)(UeGamma*100.0));
			rsOptionsPacket.writeBoolean(removeRoofs);
			rsOptionsPacket.writeBoolean(animSmoothing);
			rsOptionsPacket.writeByte(appSettings.drawDistance);
			rsOptionsPacket.writeBoolean(false);
			rsOptionsPacket.writeBoolean(false);
			rsOptionsPacket.writeBoolean(false);

			powPrevFrame = pow;
			removeRoofsPrevFrame = removeRoofs;
			animSmoothingPrevFrame = animSmoothing;
			drawDistance_PrevFrame = appSettings.drawDistance;

			sharedmem_rm.backBuffer.writePacket(rsOptionsPacket, "RsOptions");
		}
	}

	boolean isPoseAnimValid(Actor actor) {
		boolean actionAnimIsValid = isActionAnimValid(actor);
		// Check if the current pose animation sequence is valid
		boolean ImvalidPoseAnim = actor.getPoseAnimation() == -1 /*|| !this.poseAnimationSequence.getSequenceDefinition().iValid()*/;

		// Check if the current pose animation is the same as the idle animation
		boolean isPoseSameAsIdle = (actor.getPoseAnimation() == actor.getIdlePoseAnimation());

		// If any invalid conditions are met, return null
		if (ImvalidPoseAnim || (isPoseSameAsIdle && actionAnimIsValid/*ActionAnim != null*/)) {
			return false;
		}

		// Otherwise, return the valid pose animation sequence
		return true;
	}

	@SneakyThrows
	private void WritePerFramePacket()
	{
		if(!config.enablePerFramePacket()) {return;}

		if (client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.LOGGING_IN || curGamestate == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{ //dont send perframe packet while on login screen because doing so would interfere with the animated login screen's camera.
			return;
		}

		if(client.getLocalPlayer() == null) {return;}

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

		checkFor_NpcModelOverride_Changes(); //iterates npcs to see if any of their overrides have changed. if overrides have changed, they will be re-spawned with the correct overrides.

		sendPlaneChanged();

		ArrayList<WorldView> worldViews = new ArrayList<>();
		worldViews.add(client.getTopLevelWorldView());
		for(WorldView wv : client.getTopLevelWorldView().worldViews()) {
			worldViews.add(wv);
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

		ArrayList<NPC> npcs = new ArrayList<>();
		for(WorldView wv : worldViews) {
			for (NPC actor : wv.npcs()) {
				if(actor!=null)
				{
					npcs.add(actor);
				}
			}
		}

		int npcCount = npcs.size();

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

				int npcInstanceId = getOrAddCustomizedId(npc); //npc.getIndex();
				LocalPoint LocalLocation = npc.getLocalLocation();
				int npcX = LocalLocation.getX();
				int npcY = LocalLocation.getY();
				//LocalPoint offsetCentre = new LocalPoint(npcX+(npc.getComposition().getSize()*config.NpcOffsetX()), npcY+(npc.getComposition().getSize()*config.NpcOffsetY()));
				int npcHeight = npcHeights[npc.getIndex()]*-1; /*Perspective.getTileHeight(client, offsetCentre, client.getLocalPlayer().getWorldLocation().getPlane()) * -1;*/
				int npcOrientation = npc.getCurrentOrientation();

				int animationId_Action = (config.spawnAnimations() ? getAnimation_Unmasked(npc) : -1);
				int animationId_Pose = (config.spawnAnimations() ? npc.getPoseAnimation() : -1);

				int animationFrameIdx_Action = npc.getAnimationFrame();

				int animationFrameIdx_Pose = npc.getPoseAnimationFrame();

				//boolean enableActionSeq = player.getAnimation() != -1 && player.getSequenceDelay() == 0;
				boolean isPoseValid = isPoseAnimValid(npc);
				//boolean disableMovementSeq = npc.getPoseAnimation() == -1 || (npc.getPoseAnimation() == npc.getIdlePoseAnimation() && animationId_Action != -1);
				if(!isPoseValid) {
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

		//IndexedObjectSet<? extends Player> players;

		ArrayList<Player> players = new ArrayList<>();

		for(WorldView wv : worldViews) {
			for (Player actor : wv.players()) {
				if (actor != null)
				{
					players.add(actor);
				}
			}
		}

		int playerCount = players.size();
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
				int playerInstanceId = getOrAddCustomizedId(player);

				LocalPoint LocalLocation = player.getLocalLocation();

				int playerX = LocalLocation.getX();
				int playerY = LocalLocation.getY();
				int playerHeight = playerHeights[player.getId()]*-1;
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
			LocalPlayerIndex = getOrAddCustomizedId(client.getLocalPlayer());
		}
		perFramePacket.writeShort(LocalPlayerIndex);


		int noGraphicsObjects = 0;
		if (config.spawnStaticGFX())
		{
			for(WorldView wv : worldViews)
			{
				for (GraphicsObject graphicsObject : wv.getGraphicsObjects())
				{
					noGraphicsObjects++;
					if (!hashedEntitys_LastFrame.contains(graphicsObject.hashCode()))
					{
						//log.debug("graphicsObjSpawn. id: " + graphicsObject.getId() + " localPoint:"+graphicsObject.getLocation());
					}
					hashedEntitys_ThisFrame.add(graphicsObject.hashCode());
				}
			}
		}

		perFramePacket.writeShort(noGraphicsObjects);
		if (noGraphicsObjects > 0)
		{
			for(WorldView wv : worldViews)
			{
				for (GraphicsObject graphicsObject : wv.getGraphicsObjects())
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
					perFramePacket.writeShort(wv.getId());
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
				perFramePacket.writeShort(getProjectilePitch(projectile));
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
			//System.out.println("Animating " + NoTileObjects + " gameObjects");
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
			int NoTileObjects = 0;
			perFramePacket.writeInt(NoTileObjects);
		}

		//send info about every specialObject every frame
		int NoSpecialObjects = SpecialObjects_CurFrame.size();
		perFramePacket.writeShort(NoSpecialObjects);
		for (Map.Entry<Integer, SpecialObj> entry : SpecialObjects_CurFrame.entrySet()) {
			hashedEntitys_ThisFrame.add(entry.getKey());

			SpecialObj specialObj = entry.getValue();
			byte objectType = specialObj.ObjectType; //futureproofer. we can alter code path depending on type
			int sceneId = specialObj.gameObject.hashCode();
			short localX = (short) specialObj.gameObject.getX();
			short localY = (short) specialObj.gameObject.getY();
			long spotAnimId = specialObj.Id_Custom;
			short animimationFrameIdx = (short) -1;
			//if (!shouldDraw) { animimationFrameIdx = -2; }
			int Z = specialObj.gameObject.getZ();
			int Orientation = specialObj.gameObject.getOrientation();


			perFramePacket.writeByte(objectType);
			perFramePacket.writeInt(sceneId);
			perFramePacket.writeShort(specialObj.gameObject.getWorldView().getId());
			perFramePacket.writeShort(localX);
			perFramePacket.writeShort(localY);
			perFramePacket.writeLong(spotAnimId);
			perFramePacket.writeShort(animimationFrameIdx);
			perFramePacket.writeShort(Z);
			perFramePacket.writeShort(Orientation);
		}
		SpecialObjects_CurFrame.clear(); //we are done with specialObjects so clear them so its fresh fro next frame


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

		sharedmem_rm.backBuffer.writePacket(perFramePacket, "PerFramePacket");

		hashedEntitys_LastFrame = hashedEntitys_ThisFrame;

		checkFor_ActorColourOverride_Changes();

		checkForFashionScapeChanges();

		SendRsOptionsPacket(false);
	}

	void hashedEntityDespawned(int SceneId)
	{
		Buffer actorSpawnPacket = new Buffer(new byte[20]);

		actorSpawnPacket.writeByte(6); //write hashedEntity data type
		actorSpawnPacket.writeShort(-1);//using -1 worldview for now
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

	public void loadAppSettings()
	{
		String jsonFileLocation = System.getProperty("user.home") + "\\.runemod\\AppSettings.json";

		//Gson gson = new GsonBuilder().create();
		try (BufferedReader reader = new BufferedReader(new FileReader(jsonFileLocation)))
		{
			appSettings = gson.fromJson(reader, ApplicationSettings.class);
		}
		catch (IOException e)
		{
			System.out.println("error in loadAppSettings");
			e.printStackTrace();
			return;
		}
	}

	public void appSettingChanged(String setting) {
		loadAppSettings();

		if(setting.equalsIgnoreCase("maxFps")) {
			setMaxFps(appSettings.maxFps);
			//System.out.println("maxFpsSetting Changed");
		}

		if(setting.equalsIgnoreCase("drawDistance")) {
			initDrawDistance();
			changedDrawDist = true;
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
			actorId = getOrAddCustomizedId(actor);//((Player) actor).getId();
			actorType = 1;
		}
		if(actor instanceof NPC) {
			actorId = getOrAddCustomizedId(actor);//((NPC) actor).getIndex();
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
	public void drawDynamic(Projection worldProjection, Scene scene, TileObject tileObject, Renderable r, Model m, int orient, int x, int y, int z)
	{
		//boolean nullRenderable = r == null;
		long tag = tileObject.getHash();
		int entityType = (int) ((tag >>> 16) & 7);
		//System.out.println("encountered tileObject id"+tileObject.getId()+ "entityType: "+entityType/*"r is null? "+(r==null) +" m is null?"+(m == null)*/);
		if(tileObject!=null && entityType == 2) {
			long plane = (long)(tag >> 14 & 3);
			long sceneY = (long)(tag >> 7 & 127);
			long sceneX = (long)(tag >> 0 & 127);


			int maxPlane = scene.getTiles().length;
			int maxX = scene.getTiles()[0].length;
			int maxY = scene.getTiles()[0][0].length;

			if(plane < maxPlane && sceneY < maxY && sceneX < maxX && plane >= 0 && sceneY >= 0 && sceneX >= 0) {
				Tile tile = scene.getTiles()[(int)plane][(int)sceneX][(int)sceneY];
				tilesWithAnimateGameObjects.add(tile);

				if(plane > 0) {
					tilesWithAnimateGameObjects.add(scene.getTiles()[(int)plane-1][(int)sceneX][(int)sceneY]); //required where tile uses linkedbellow stuff
				}
			}
		}
	}

	class SpecialObj {
		GameObject gameObject;
		long Id_Custom; //Custom data. Idea is this points to a custom definition we have setup in runemod
		byte ObjectType;
	}

	HashMap<Integer, SpecialObj> SpecialObjects_CurFrame = new HashMap<>(); //key is java object hash, value is SpecialObject data

	@Override
	public void drawTemp(Projection worldProjection, Scene scene, GameObject gameObject, Model m, int orient, int x, int y, int z)
	{
		if(gameObject!=null) {
			long tag = gameObject.getHash();
			// (entityType & 7) << 16
			int entityType = (int)((tag >>> 16) & 7); //0 is player. 1 is npc

			if (entityType == 0)
			{
				// index stored in bits [20..51] (32 bits)
				int index = (int)((tag >> 20) & 0xffffffff);

				// worldView stored in bits [52..63] (12 bits)
				int worldView = (int)((tag >> 52) & 4095);
				if(worldView == 4095) {
					worldView = -1;
				}

				visibleActors.add(client.getWorldView(worldView).players().byIndex(index));

				playerHeights[index] = y;
			}
			else
			{
				if (entityType == 1)
				{
					// index stored in bits [20..51] (32 bits)
					int index = (int)((tag >> 20) & 0xffffffff);

					// worldView stored in bits [52..63] (12 bits)
					int worldView = (int)((tag >> 52) & 4095);
					if(worldView == 4095) {
						worldView = -1;
					}

					visibleActors.add(client.getWorldView(worldView).npcs().byIndex(index));
					npcHeights[index] = y;
				}
				else
				{
					if(entityType == 5) {
/*						// index stored in bits [20..51] (32 bits)
						int index = (int)((tag >> 20) & 0xffffffff);

						// worldView stored in bits [52..63] (12 bits)
						int worldView = (int)((tag >> 52) & 4095);
						if(worldView == 4095) {
							worldView = -1;
						}

						int plane = (int)(tag >> 14 & 3);
						int sceney = (int)(tag >> 7 & 127);
						int scenex = (int)(tag >> 0 & 127);*/

						if(m!=null && m.getVerticesCount() == 42) { //if seems to be heading arrow
							SpecialObj specialObj = new SpecialObj();

							specialObj.Id_Custom = 25769807520L; //default white arrow id for unhandled cases.

							if(m.getFaceColors1()[60] == 10161) {
								specialObj.Id_Custom = 25769807521L;//gold arrow spotAnimId
							}
							if(m.getFaceColors1()[60] == 92) {
								specialObj.Id_Custom = 25769807520L;//white arrow spotAnimId
							}

							specialObj.gameObject = gameObject;
							specialObj.ObjectType = 14;//14 means spotAnimDef in Runemod

							SpecialObjects_CurFrame.put(gameObject.hashCode(), specialObj);
						}
					}
				}
			}

/*				if(entityType == 2) {
					System.out.println("encountered tileObject (type 2) id: "+gameObject.getId());
				}

				if(entityType == 3) {
					System.out.println("encountered tileItem (type 3) id: "+gameObject.getId());
				}

				if(entityType == 4) {
					System.out.println("encountered worldEntity (type 4) id: "+gameObject.getId());
				}
				if(entityType == 5) {
					System.out.println("encountered tileObject (type 5). id: "+gameObject.getId());
				}

				if(entityType > 5) {
					System.out.println("encountered unknownentitytype (type 5). id: "+gameObject.getId());
				}*/
		}
	}

	//legacy draw for alora. not called/used in modern runelite
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


