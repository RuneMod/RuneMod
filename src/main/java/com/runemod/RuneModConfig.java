package com.runemod;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("RuneMod")
public interface RuneModConfig extends Config
{
	@ConfigItem(
			keyName = "BetaTesterKey",
			name = "Enter Your Beta Key.",
			description = "RuneMod is not yet ready for public use. It is currently only useable by those with beta keys.",
			position = 2
	)

	default String BetaTesterKey()
	{
		return "";
	}

	@ConfigSection(
			name = "Settings",
			description = "Settings",
			position = 3,
			closedByDefault = true
	)
	String settings = "settings";

	@ConfigItem(
			keyName = "MaxFps",
			name = "MaxFps",
			description = "Note, increasing the fps beyond 50 may produce a less responsive experience",
			position = 4,
			section = settings
	)

	default int MaxFps()
	{
		return 50;
	}

	@ConfigSection(
			name = "Developer",
			description = "Developer",
			position = 7,
			closedByDefault = true
	)
	String Developer = "Developer";

	@ConfigItem(
			keyName = "RuneModVisibility",
			name = "RuneModVisibility",
			description = "Toggles visibility of runemod",
			position = 5,
			section = Developer
	)

	default boolean RuneModVisibility()
	{
		return true;
	}

	@ConfigItem(
			keyName = "startRuneModOnStart",
			name = "startRuneModOnStart",
			description = "startRuneModOnStart",
			position = 6,
			section = Developer
	)
	default boolean StartRuneModOnStart()
	{
		return true;
	}

	@ConfigItem(
			keyName = "UseAltRuneModLocation",
			name = "UseAltRuneModLocation",
			description = "UseAltRuneModLocation",
			position = 8,
			section = Developer
	)
	default boolean UseAltRuneModLocation()
	{
		return false;
	}

	@ConfigItem(
			keyName = "OrbitCamera",
			name = "OrbitCamera",
			description = "OrbitCamera",
			position = 8,
			section = Developer
	)
	default boolean OrbitCamera()
	{
		return false;
	}

	@ConfigItem(
			position = 9,
			keyName = "AltRuneModLocation",
			name = "AltRuneModLocation",
			description = "",
			section = Developer
	)

	default String AltRuneModLocation()
	{
		return "";
	}

	@ConfigSection(
			name = "Developer_Debug",
			description = "Developer_Debug",
			position = 10,
			closedByDefault = true
	)
	String Developer_Debug = "Developer_Debug";

	@ConfigItem(
			keyName = "useTwoRenderers",
			name = "useTwoRenderers",
			description = "useful for debugging or comparing between runemod and vanilla. uncheck attach attachRmWindowToRL and move the rl window, to separate the two render views.",
			position = 11,
			section = Developer_Debug
	)
	default boolean useTwoRenderers()
	{
		return false;
	}

	@ConfigItem(
			keyName = "attachRmWindowToRL",
			name = "attachRmWindowToRL",
			description = "attachRmWindowToRL",
			position = 12,
			section = Developer_Debug
	)
	default boolean attachRmWindowToRL()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnAnimations",
			name = "spawnAnimations",
			description = "spawnAnimations",
			position = 13,
			section = Developer_Debug
	)

	default boolean spawnAnimations()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnPlayerGFX",
			name = "spawnPlayerGFX",
			description = "spawnPlayerGFX",
			position = 14,
			section = Developer_Debug
	)
	default boolean spawnPlayerGFX()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnNpcGFX",
			name = "spawnNpcGFX",
			description = "spawnNpcGFX",
			position = 15,
			section = Developer_Debug
	)
	default boolean spawnNpcGFX()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnStaticGFX",
			name = "spawnStaticGFX",
			description = "spawnStaticGFX",
			position = 20,
			section = Developer_Debug
	)
	default boolean spawnStaticGFX()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnProjectiles",
			name = "spawnProjectiles",
			description = "spawnProjectiles",
			position = 30,
			section = Developer_Debug
	)
	default boolean spawnProjectiles()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnNPCs",
			name = "spawnNPCs",
			description = "spawnNPCs",
			position = 40,
			section = Developer_Debug
	)
	default boolean spawnNPCs()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnGameObjects",
			name = "spawnGameObjects",
			description = "spawnGameObjects",
			position = 60,
			section = Developer_Debug
	)
	default boolean spawnGameObjects()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnPlayers",
			name = "spawnPlayers",
			description = "spawnPlayers",
			position = 70,
			section = Developer_Debug
	)
	default boolean spawnPlayers()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnItems",
			name = "spawnItems",
			description = "spawnItems",
			position = 90,
			section = Developer_Debug
	)
	default boolean spawnItems()
	{
		return true;
	}
}
