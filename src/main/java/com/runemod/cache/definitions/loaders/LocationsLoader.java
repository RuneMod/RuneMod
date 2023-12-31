/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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
package com.runemod.cache.definitions.loaders;

import com.runemod.cache.definitions.LocationsDefinition;
import com.runemod.cache.io.InputStream;
import com.runemod.cache.region.Location;
import com.runemod.cache.region.Position;

public class LocationsLoader
{
	public LocationsDefinition load(int regionX, int regionY, byte[] b)
	{
		LocationsDefinition loc = new LocationsDefinition();
		loc.setRegionX(regionX);
		loc.setRegionY(regionY);
		loadLocations(loc, b);
		return loc;
	}

	private void loadLocations(LocationsDefinition loc, byte[] b)
	{
		InputStream buf = new InputStream(b);

		int id = -1;
		int idOffset;

		while ((idOffset = buf.readUnsignedIntSmartShortCompat()) != 0)
		{
			id += idOffset;

			int position = 0;
			int positionOffset;

			while ((positionOffset = buf.readUnsignedShortSmart()) != 0)
			{
				position += positionOffset - 1;

				int localY = position & 0x3F;
				int localX = position >> 6 & 0x3F;
				int height = position >> 12 & 0x3;

				int attributes = buf.readUnsignedByte();
				int type = attributes >> 2;
				int orientation = attributes & 0x3;

				loc.getLocations().add(new Location(id, type, orientation, new Position(localX, localY, height)));
			}
		}
	}
}
