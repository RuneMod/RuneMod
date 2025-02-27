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
	@ConfigItem(
		keyName = "BetaTesterKey",
		name = "Enter Your Beta Key.",
		description = "RuneMod is not yet ready for public use. It is currently only useable by those with beta keys.",
		position = 0
	)

	default String BetaTesterKey()
	{
		return "";
	}

	@ConfigSection(
		name = "Settings",
		description = "Settings",
		position = 1,
		closedByDefault = true
	)
	String settings = "settings";

	@ConfigItem(
		keyName = "MaxFps",
		name = "MaxFps",
		description = "Note, increasing the fps beyond 50 may produce a less responsive experience",
		position = 1,
		section = settings
	)

	default int MaxFps()
	{
		return 50;
	}

	@ConfigSection(
		name = "Developer",
		description = "Developer",
		position = 3,
		closedByDefault = true
	)
	String Developer = "Developer";

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
	default boolean DebugLogging() { return true; }

	@ConfigItem(
		keyName = "Heavy",
		name = "Heavy Logging",
		description = "Enables heavy logging, can impact performance.",
		position = 6,
		section = Developer
	)
	default boolean HeavyLogging() { return false; }

	@ConfigItem(
		keyName = "startRuneModOnStart",
		name = "Auto start RuneMod.exe",
		description = "should generally be enabled",
		position = 7,
		section = Developer
	)
	default boolean StartRuneModOnStart() { return true; }

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

	@ConfigSection(
		name = "Developer_Debug",
		description = "Developer_Debug",
		position = 10,
		closedByDefault = true
	)
	String Developer_Debug = "Developer_Debug";

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
}
