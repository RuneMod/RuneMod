package com.runemod;

import com.runemod.cache.Cache;
import com.runemod.cache.IndexType;
import com.runemod.cache.definitions.MapDefinition;
import com.runemod.cache.definitions.loaders.MapLoader;
import com.runemod.cache.fs.Archive;
import com.runemod.cache.fs.Index;
import com.runemod.cache.fs.Storage;
import com.runemod.cache.fs.Store;
import com.runemod.cache.region.Region;

import java.io.IOException;
import java.util.HashSet;

public class cacheTesting {

    static Store store;
    static Storage storage;

    public static void main(String[] args) throws IOException
    {
        try {
            store = Cache.loadStore("C:\\Users\\soma.wheelhouse\\.runelite\\jagexcache\\oldschool\\LIVE");
            storage = store.getStorage();
        } catch (IOException e) {
            //e.printStackTrace();
            return;
        }
/*        Store store = Cache.loadStore("C:\\Users\\soma.wheelhouse\\.runelite\\jagexcache\\oldschool\\LIVE");
        System.out.println( store.getIndexes().size());
        int height = getTileHeightAtCoordinate(3233, 3218,0);
        System.out.println("height: "+height);*/
        for (int i = 0; i < 32768; i++) {
            printRegionStats(i);
        }
        //printRegionStats(12850);
       // Store store = loadStore(cache);
    }



    public static void printRegionStats(int regionId) {
            int regionX = regionId >> 8;;
            int regionY = regionId & 0xFF;

            HashSet<Integer> underlayIds = new HashSet<>();

            Index index = store.getIndex(IndexType.MAPS);
            Archive map = index.findArchiveByName("m" + regionX + "_" + regionY);

            if(map==null) {return;}

            byte[] bytes;

            try {
                bytes = map.decompress(storage.loadArchive(map));
            } catch (IOException e) {
                //e.printStackTrace();
                return;
            }

            MapLoader mapLoader = new MapLoader();
            MapDefinition mapDef = mapLoader.load(regionX, regionY, bytes);
            Region region = new Region(regionId);
            region.loadTerrain(mapDef);

            if (bytes!=null) {
                for (int z = 0; z < 4; z++)
                {
                    for (int x = 0; x < 64; x++)
                    {
                        for (int y = 0; y < 64; y++)
                        {
                            MapDefinition.Tile tile = mapDef.getTiles()[z][x][y];
                            underlayIds.add((int)tile.overlayId);
                        }
                    }
                }
                System.out.println(""+underlayIds.size());
                //System.out.println("Sent TerrainData: " + id + " bytes len: "+ bytes.length);
            }
    }



    public static int getTileHeightAtCoordinate(int tileX_WorldPos, int tileY_WorldPos, int plane) {
        Store store = null;
        try {
            store = Cache.loadStore("C:\\Users\\soma.wheelhouse\\.runelite\\jagexcache\\oldschool\\LIVE");
        } catch (IOException e) {
            e.printStackTrace();
        }
        int regionX = tileX_WorldPos/64;
        int regionY = tileY_WorldPos/64;
        int regionId = regionX << 8 | regionY;
        System.out.println("regionId: "+regionId);

        Index index = store.getIndex(IndexType.MAPS);
        Storage storage = store.getStorage();
        Archive map = index.findArchiveByName("m" + regionX + "_" + regionY);

        byte[] bytes = new byte[0];

        try {
            bytes = map.decompress(storage.loadArchive(map));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MapLoader mapLoader = new MapLoader();
        MapDefinition mapDef = mapLoader.load(regionX, regionY, bytes);
        Region region = new Region(regionId);
        region.loadTerrain(mapDef);

        int x_tilePosInRegion = tileX_WorldPos-(regionX*64);
        System.out.println("x_tilePosInRegion: "+x_tilePosInRegion);
        int y_tilePosInRegion = tileY_WorldPos-(regionY*64);
        System.out.println("y_tilePosInRegion: "+y_tilePosInRegion);

        int height = region.getTileHeight(plane,x_tilePosInRegion,y_tilePosInRegion);
        return height;
    }
}

