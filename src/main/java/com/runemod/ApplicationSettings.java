package com.runemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ApplicationSettings {
    public String lastUsedUser = "No Name";
    public boolean modMode = false;
    public int[] rsCacheHashes;
    public boolean loaded = false;
    public float volume = 0.5f;
    public boolean highPerformanceMode = false;
    public int resolutionScaling = 100;
    public boolean animateLoginScreen = true;

    // Getters and Setters
}