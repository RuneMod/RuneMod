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

public class ApplicationSettings
{
	String lastUsedUser = "No Name";
	boolean modMode = false;
	int[] rsCacheHashes; //read when requesting rsCache. saved when recieved all the RsCache.
	boolean loaded = false;
	float volume = 0.5f;
	boolean highPerformanceMode = false;
	int resolutionOverride = 0;
	boolean animateLoginScreen = true;
	boolean autoRecord = false;
	int antiAliasing = 1;
	boolean showFps = false;
	float contrast = 1.0f;
	float fogAmount = 1.0f;
	//FColor FogTint = FColor(255,255,255,255);
	//EWeatherTag weatherOverride = EWeatherTag::None;
	float grunge = 0.2f;
	float colorShift = 0.2f;
	long customWeather = 0;
	float chanceOfRain = 1.0f;
	float chanceOfSun = 1.0f;
	float chanceOfCloud = 1.0f;
	float chanceOfSnow = 0.0f;
	int drawDistance = 46; //how many tile into the distance we can load
	int maxFps = 50;
}