package com.runemod;/*
 * Copyright (c) 2025, Adam <Adam@sigterm.info>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.Constants;
import net.runelite.api.Scene;

@Singleton
class regionTxtParser
{
	public regionTxtParser() {

		try (var in = RuneModPlugin.class.getResourceAsStream("/regions.txt"))
		{
			buildChunkGroups(in);
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	public static class ChunkGroup { //groups of chunks that should be visible when we are located inside the group
		HashSet<Integer> chunks = new HashSet<>(); //perhaps should be A set, for fast contains() calls.
	}

	public static final ArrayList<ChunkGroup> chunkGroups = new ArrayList<>();
	public static final HashMap<Integer, Integer> chunkToGroupIndex = new HashMap<>();

	public static void buildChunkGroups(InputStream in) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));


		Pattern pattern = Pattern.compile("^[ \\t]*(?<expr>" +
			"//.*$|" +
			"n|" +
			"m[ \\t]*(?<mrx>[0-9]+)[ \\t]+(?<mry>[0-9]+)|" +
			"r[ \\t]*(?<rx>[0-9]+)[ \\t]+(?<ry>[0-9]+)|" +
			"R[ \\t]*(?<rx1>[0-9]+)[ \\t]+(?<ry1>[0-9]+)[ \\t]+(?<rx2>[0-9]+)[ \\t]+(?<ry2>[0-9]+)|" +
			"c[ \\t]*(?<cx>[0-9-]+)[ \\t]+(?<cy>[0-9-]+)|" +
			"C[ \\t]*(?<cx1>[0-9-]+)[ \\t]+(?<cy1>[0-9-]+)[ \\t]+(?<cx2>[0-9-]+)[ \\t]+(?<cy2>[0-9-]+)|" +
			")[ \\t]*");

		Matcher matcher = pattern.matcher("");

		int currentGroupIdx = -1;

		int rx1 = 0, ry1 = 0,
			rx2 = 0, ry2 = 0;

		for (String line; (line = br.readLine()) != null; )
		{
			matcher.reset(line);
			int end = 0;

			while (end < line.length())
			{
				matcher.region(end, line.length());
				if (!matcher.find())
					break;

				end = matcher.end();

				String expr = matcher.group("expr");
				if (expr == null || expr.startsWith("//"))
					continue;

				char ch = expr.charAt(0);

				switch (ch)
				{
					case 'n': //create a new chunkGroup. meaning a group of chunks, that -when we are inside them- are the only chunks visible. any chunks not included in the group become invisible.
					{
						chunkGroups.add(new ChunkGroup());
						currentGroupIdx = chunkGroups.size()-1;
						break;
					}

					case 'm':
					{
						rx1 = rx2 = Integer.parseInt(matcher.group("mrx"));
						ry1 = ry2 = Integer.parseInt(matcher.group("mry"));
						break;
					}

					case 'r':
					case 'R':
					{
						if (ch == 'r')
						{
							rx1 = rx2 = Integer.parseInt(matcher.group("rx"));
							ry1 = ry2 = Integer.parseInt(matcher.group("ry"));
						}
						else
						{
							rx1 = Integer.parseInt(matcher.group("rx1"));
							ry1 = Integer.parseInt(matcher.group("ry1"));
							rx2 = Integer.parseInt(matcher.group("rx2"));
							ry2 = Integer.parseInt(matcher.group("ry2"));
						}


						for (int regionX = rx1; regionX <= rx2; regionX++)
						{
							for (int regionY = ry1; regionY <= ry2; regionY++)
							{
								int chunkStartX = regionX * 8;
								int chunkStartY = regionY * 8;

								for (int dx = 0; dx < 8; dx++)
								{
									for (int dy = 0; dy < 8; dy++)
									{
										AddChunkToGroup(chunkStartX + dx,
											chunkStartY + dy,
											currentGroupIdx);
									}
								}
							}
						}
						break;
					}

					case 'c':
					case 'C':
					{
						int regionChunkX = rx1 * 8;
						int regionChunkY = ry1 * 8;

						int cx1, cy1, cx2, cy2;

						if (ch == 'c')
						{
							cx1 = cx2 = Integer.parseInt(matcher.group("cx"));
							cy1 = cy2 = Integer.parseInt(matcher.group("cy"));
						}
						else
						{
							cx1 = Integer.parseInt(matcher.group("cx1"));
							cy1 = Integer.parseInt(matcher.group("cy1"));
							cx2 = Integer.parseInt(matcher.group("cx2"));
							cy2 = Integer.parseInt(matcher.group("cy2"));
						}

						for (int dx = cx1; dx <= cx2; dx++)
						{
							for (int dy = cy1; dy <= cy2; dy++)
							{
								AddChunkToGroup(
									regionChunkX + dx,
									regionChunkY + dy,
									currentGroupIdx
								);
							}
						}

						break;
					}
				}
			}
		}
		//System.out.println("Parsed regions.txt. Made "+chunkGroups.size() + " groups and "+chunkToGroupIndex.size() + " pointers to groups");
		return;
	}

	static int makeChunkId(int cx, int cy) //packs chunk x y to int. its like chunkId but packed/ more efficient.
	{
		return (cx << 16) | (cy & 0xFFFF);
	}

	static void AddChunkToGroup(int cx, int cy, int groupIdx) {
		int packed = makeChunkId(cx, cy);

		chunkGroups.get(groupIdx).chunks.add(makeChunkId(cx, cy));

		chunkToGroupIndex.put(packed, groupIdx);
	}

	ArrayList<Integer> getHiddenChunks(Scene scene)
	{
		ArrayList<Integer> hiddenChunks = new ArrayList<>();
		if (scene.isInstance())
		{
			return hiddenChunks;
		}

		int baseX = scene.getBaseX() / 8;
		int baseY = scene.getBaseY() / 8;
		int centerX = baseX + 6;
		int centerY = baseY + 6;

		int centerChunkId = makeChunkId(centerX, centerY);

		boolean AreWeInsideAChunkGroup = chunkToGroupIndex.containsKey(centerChunkId);

		if(!AreWeInsideAChunkGroup) {
			return hiddenChunks;
		}

		ChunkGroup chunkGroup = chunkGroups.get(chunkToGroupIndex.get(centerChunkId));

		int noHiddenChunks = 0;
		int noVisibleChunks = 0;

		int r = Constants.SCENE_SIZE / 16;
		for (int offx = -r; offx <= r; ++offx)
		{
			for (int offy = -r; offy <= r; ++offy)
			{
				int cx = centerX + offx;
				int cy = centerY + offy;
				int packedChunk = makeChunkId(cx, cy);
				if(!chunkGroup.chunks.contains(packedChunk)){
					hiddenChunks.add(packedChunk);
					//System.out.println("encountered hidden chunk at"+ cx*8+ " "+cy*8);
					noHiddenChunks++;
				}else {
					noVisibleChunks++;
				}
			}
		}
		//System.out.println("found "+noHiddenChunks + " hidden Chunks and "+ noVisibleChunks + " visible Chunks");
		return hiddenChunks;
	}
};