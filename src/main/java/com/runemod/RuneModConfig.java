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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("RuneMod")
public interface RuneModConfig extends Config
{
	@ConfigSection(
		name = "Settings",
		description = "Settings",
		position = 1,
		closedByDefault = false
	)
	String settings = "settings";
	@ConfigSection(
		name = "Developer",
		description = "Developer",
		position = 3,
		closedByDefault = true
	)
	String Developer = "Developer";
	@ConfigSection(
		name = "Developer_Debug",
		description = "Developer_Debug",
		position = 10,
		closedByDefault = true
	)
	String Developer_Debug = "Developer_Debug";

/*	@ConfigItem(
		keyName = "BetaTesterKey",
		name = "Enter Your Beta Key.",
		description = "RuneMod is not yet ready for public use. It is currently only useable by those with beta keys.",
		position = 0
	)

	default String BetaTesterKey()
	{
		return "";
	}*/

	@ConfigItem(
		keyName = "MaxFps",
		name = "MaxFps",
		description = "Note, increasing the fps beyond 50 may produce a less responsive experience",
		position = 0,
		section = settings
	)

	default int MaxFps()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "lockStep",
		name = "LockStep",
		description = "should generally be disabled. When enabled, input lag may occur",
		position = 1,
		section = Developer
	)

	default boolean lockStep()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ChunkStreamingDistance",
		name = "ChunkStreamingDistance",
		description = "allows runemod to stream in objects outside of the main scene area, which can reduces loading times while moving through the world",
		position = 1,
		section = Developer
	)
	default int StreamChunkDistance()
	{
		return 7; //6 would mean a 13X13 chunk square around player (6 is like radius. 6X2 = 12. +1 because its radius from centre tile. But this is all thrown off a bit because we actually use subregions, not chunks.
	}

	@ConfigItem(
		keyName = "RuneModVisibility",
		name = "RuneModVisibility",
		description = "Toggles visibility of runemod",
		position = 1,
		section = Developer
	)
	default boolean RuneModVisibility()
	{
		return true;
	}

	@ConfigItem(
		keyName = "OrbitCamera",
		name = "OrbitCamera",
		description = "OrbitCamera",
		position = 2,
		section = Developer
	)
	default boolean OrbitCamera()
	{
		return false;
	}

	@ConfigItem(
		keyName = "DebugLogging",
		name = "Debug Logging",
		description = "enables debug logging for this plugin",
		position = 5,
		section = Developer
	)
	default boolean DebugLogging()
	{
		return true;
	}

	@ConfigItem(
		keyName = "Heavy",
		name = "Heavy Logging",
		description = "Enables heavy logging, can impact performance.",
		position = 6,
		section = Developer
	)
	default boolean HeavyLogging()
	{
		return false;
	}

	@ConfigItem(
		keyName = "startRuneModOnStart",
		name = "Auto start RuneMod.exe",
		description = "should generally be enabled",
		position = 7,
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

	@ConfigItem(
		keyName = "useTwoRenderers",
		name = "Use Two Renderers",
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
		name = "Attach Rm Window To RL",
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
		name = "SpawnAnimations",
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
		name = "SpawnPlayerGFX",
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
		name = "SpawnNpcGFX",
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
		name = "SpawnStaticGFX",
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
		name = "SpawnProjectiles",
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
		name = "SpawnNPCs",
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
		name = "SpawnGameObjects",
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
		name = "SpawnPlayers",
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
		name = "SpawnItems",
		description = "spawnItems",
		position = 90,
		section = Developer_Debug
	)
	default boolean spawnItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableUiPixelsUpdate",
		name = "enableUiPixelsUpdate",
		description = "enableUiPixelsUpdate",
		position = 91,
		section = Developer_Debug
	)
	default boolean enableUiPixelsUpdate()
	{
		return true;
	}

	@ConfigItem(
		keyName = "disableUeCom",
		name = "disableUeCom",
		description = "disableUeCom",
		position = 92,
		section = Developer_Debug
	)
	default boolean disableUeCom()
	{
		return false;
	}

	@ConfigItem(
		keyName = "disableFrustrumTileCulling",
		name = "disableFrustrumTileCulling",
		description = "disableFrustrumTileCulling",
		position = 92,
		section = Developer_Debug
	)
	default boolean disableFrustrumTileCulling()
	{
		return false;
	}

	@ConfigItem(
		keyName = "nullifyDrawCallbacks",
		name = "nullifyDrawCallbacks",
		description = "nullifyDrawCallbacks",
		position = 92,
		section = Developer_Debug
	)
	default boolean nullifyDrawCallbacks()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enablePerFramePacket",
		name = "enablePerFramePacket",
		description = "enablePerFramePacket",
		position = 92,
		section = Developer_Debug
	)
	default boolean enablePerFramePacket()
	{
		return true;
	}

	@ConfigItem(
		keyName = "increaseTimerResolution",
		name = "increaseTimerResolution",
		description = "increaseTimerResolution",
		position = 93,
		section = Developer_Debug
	)
	default boolean increaseTimerResolution()
	{
		return true;
	}
}
