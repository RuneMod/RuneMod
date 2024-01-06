package com.runemod;

import com.runemod.cache.Cache;
import com.runemod.cache.IndexType;
import com.runemod.cache.definitions.MapDefinition;
import com.runemod.cache.definitions.loaders.MapLoader;
import com.runemod.cache.fs.*;
import com.runemod.cache.index.FileData;
import com.runemod.cache.region.Region;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

public class cacheTesting {

    static Store store;
    static Storage storage;

    public static void main(String[] args)
    {
        try {
            store = new Store(new File(RUNELITE_DIR + "\\jagexcache\\oldschool\\LIVE"));
            store.load();
            System.out.println("cache contains" + store.getIndexes().size() + "indexs");
        } catch (IOException e) {
            System.out.println("issue loading cache from disk");
            e.printStackTrace();
        }

        isCacheComplete();
/*        Store store = Cache.loadStore("C:\\Users\\soma.wheelhouse\\.runelite\\jagexcache\\oldschool\\LIVE");
        System.out.println( store.getIndexes().size());
        int height = getTileHeightAtCoordinate(3233, 3218,0);
        System.out.println("height: "+height);*/
/*        for (int i = 0; i < 32768; i++) {
            printRegionStats(i);
        }*/
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

    public static boolean isCacheComplete() {
        for (Index index : store.getIndexes()) {
            for(Archive archive : index.getArchives()) {
                if(archive == null) { System.out.println("null archive"); continue;}
                Storage storage = store.getStorage();
                if(storage==null) { System.out.println("null storage"); continue;}
                byte[] archiveData = new byte[0];
                try {
                    archiveData = storage.loadArchive(archive);
                } catch (IOException e) {
                    System.out.println("failed to load archive "+archive.getArchiveId() + " in index: " + index.getId());
                }

                if(archiveData == null) {
                    System.out.println("null archive data");
                    System.out.println("rsCache Is Not Complete");
                    return false;
                }
            }
        }
        System.out.println("rsCache Is Complete");
        return true;
    }

    public static void sendTilesInRegion(int regionId) {
        MapDefinition.Tile nullTile = new MapDefinition.Tile();
        try
        {

            int regionX = regionId >> 8;;
            int regionY = regionId & 0xFF;


            Index index = store.getIndex(IndexType.MAPS);
            Storage storage = store.getStorage();
            Archive map = index.findArchiveByName("m" + regionX + "_" + regionY);

            if(map==null) {return;}

            byte[] bytes = null;
            byte[] bytes_compressed = null;

            try {
                bytes_compressed = storage.loadArchive(map);
            } catch (IOException e) {
                System.out.println("failed to load archive "+map.getArchiveId() + " in index: " + index.getId());
            }

            bytes = map.decompress(bytes_compressed);

            if(bytes==null) {return;}

/*                try {
                    bytes = map.decompress(storage.loadArchive(map));
                } catch (IOException e) {
                    //e.printStackTrace();
                    return;
                }*/

            MapLoader mapLoader = new MapLoader();
            MapDefinition mapDef = mapLoader.load(regionX, regionY, bytes);
            Region region = new Region(regionId);
            region.loadTerrain(mapDef);

            int swTileX = regionX*64;
            int swTileY = regionY*64;
            if (bytes!=null) {
                //mapdatasLoaded++;
                //bytes = RuneModPlugin.insertIDToByteArray(bytes, i); //i needs to be turned into x and y coords. can figure/test that out in chrome console and devtool location info.
                //Buffer MapDataBuffer = new Buffer(bytes);

                Buffer mainBuffer = new Buffer(new byte[64*64*4*(32)]); //always sending 6bytes+4 for id. inefficient can optimize somewhat easily by trimming unneeded bytes.

                for (int z = 0; z < 4; z++)
                {
                    for (int x = 0; x < 64; x++)
                    {
                        for (int y = 0; y < 64; y++)
                        {
                            int tileX = swTileX+x;
                            int tileY = swTileY+y;
                            long tileId = CacheReader.convertTileCoordinatesToContentId(z, tileX, tileY);
                            //long tileId = MakeAssetId_ue4(0, CoordinatesToContentId(tileX, tileY, z));
/*                                if (tileId >= 2147483647) {
                                    System.out.println("warning, id is more than 32bits. is:" + tileId);
                                }*/

                            MapDefinition.Tile tile = mapDef.getTiles()[z][x][y];

                            if(tile.height == nullTile.height && tile.overlayId == nullTile.overlayId && tile.underlayId == nullTile.underlayId && tile.overlayRotation == nullTile.overlayRotation && tile.settings == nullTile.settings  && tile.overlayPath == nullTile.overlayPath && tile.attrOpcode == nullTile.attrOpcode) { //dont send tile if it is null;.
                                continue;
                            }

                        }
                    }
                }
            } else {
                System.out.println("region data is null");
            }
        }
        catch (IOException ex)
        {
            System.out.println("Can't decrypt region " + regionId);
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

