package com.runemod;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("RuneMod")
public interface RuneModConfig extends Config
{
	@ConfigItem(
			keyName = "RuneModVisibility",
			name = "RuneModVisibility",
			description = "Toggles visibility of runemod",
			position = 6
	)

	default boolean RuneModVisibility()
	{
		return true;
	}

	@ConfigSection(
			name = "Developer Stuff",
			description = "Developer Stuff",
			position = 7,
			closedByDefault = false
	)
	String developerStuff = "DeveloperStuff";

	@ConfigItem(
			keyName = "startRuneModOnStart",
			name = "startRuneModOnStart",
			description = "startRuneModOnStart",
			position = 6,
			section = developerStuff
	)
	default boolean StartRuneModOnStart()
	{
		return true;
	}

	@ConfigItem(
			position = 9,
			keyName = "AltRuneModLocation",
			name = "AltRuneModLocation",
			description = "",
			section = developerStuff
	)

	default String AltRuneModLocation()
	{
		return "";
	}

	@ConfigSection(
			name = "DebugStuff",
			description = "Debug Stuff",
			position = 10,
			closedByDefault = false
	)
	String debugStuff = "DebugStuff";

	@ConfigItem(
			keyName = "attachRmWindowToRL",
			name = "attachRmWindowToRL",
			description = "attachRmWindowToRL",
			position = 12,
			section = debugStuff
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
			section = debugStuff
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
			section = debugStuff
	)
	default boolean spawnPlayerGFX()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnStaticGFX",
			name = "spawnStaticGFX",
			description = "spawnStaticGFX",
			position = 20,
			section = debugStuff
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
			section = debugStuff
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
			section = debugStuff
	)
	default boolean spawnNPCs()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnTerrain",
			name = "spawnTerrain",
			description = "spawnTerrain",
			position = 50,
			section = debugStuff
	)
	default boolean spawnTerrain()
	{
		return true;
	}

	@ConfigItem(
			keyName = "spawnGameObjects",
			name = "spawnGameObjects",
			description = "spawnGameObjects",
			position = 60,
			section = debugStuff
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
			section = debugStuff
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
			section = debugStuff
	)
	default boolean spawnItems()
	{
		return true;
	}
}
