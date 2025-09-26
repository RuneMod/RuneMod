package com.runemod;

import net.runelite.api.Constants;
import net.runelite.client.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldType;

import static com.runemod.RuneModPlugin.myCacheReader;
import static com.runemod.RuneModPlugin.runeModPlugin;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

/**
 * A class for holding functions that differ between different clients. Apart from this file, the rest of the runemod files should be the same across all github branches.
 */

@Slf4j
public class DivergentStuff
{

	static final String PreReqsFileUrl = "https://files.runemod.net/application/UEPrereqSetup_x64.exe"; //This file will probably never need to be updated.

	static String getRuneModFiles_Url() {
		switch (runeModPlugin.config.version()) {
			case Stable:
				return "https://files.runemod.net/application/"; //these files will likely be updated less than once per week.
			case Latest:
				return "https://files.runemod.net/application/dev/"; //these files will be updated frequently. E.G more than once per week.
			default:
				return "nullUrl";
		}
	}

	static final RuneModPlugin.ClientType clientType = RuneModPlugin.ClientType.RUNELITE; //osrs's version would be RuneModPlugin.ClientType.RUNELITE

	public static final int SCENE_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

	public static String getCachePath() {
		String cachePath = RUNELITE_DIR + "\\jagexcache\\oldschool\\LIVE";

		if (runeModPlugin.client.getWorldType().contains(WorldType.BETA_WORLD))
		{ //incomplete. would need a system to detec when we have changed to a beta world and donwloaded beta cache
			log.debug("isBetaWorld");
			cachePath = RUNELITE_DIR + "\\jagexcache\\oldschool-beta\\LIVE";
		}

		return cachePath;
	}

	public static boolean isPastModeChooser() { //osrs's version would simply return true always
		return true;
	}

	public static void setupTransientMod() {
		//empty. for now, no transient modding is needed in vanilla osrs.
	}

	public static boolean isCacheFullyLoaded() {
		return myCacheReader.checkIfCacheFullyLoaded(); //for osrs
		//return RuneModPlugin.client_static.isCacheDownloaded(); //for alora
	}

	//Non-shared functions go bellow this line_____________________________________________________________________________________________
}
