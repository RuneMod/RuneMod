package com.runemod;

import com.runemod.cache.ConfigType;
import com.runemod.cache.IndexType;
import com.runemod.cache.definitions.MapDefinition;
import com.runemod.cache.definitions.loaders.MapLoader;
import com.runemod.cache.fs.*;
import com.runemod.cache.index.FileData;
import com.runemod.cache.models.JagexColor;
import com.runemod.cache.region.Region;
import com.runemod.cache.util.KeyProvider;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.runelite.client.RuneLite.RUNELITE_DIR;


public class CacheReader {

    public Store store;

    private KeyProvider keyProvider;

    public CacheReader() {
        try {
            store = new Store(new File(RUNELITE_DIR + "\\jagexcache\\oldschool\\LIVE"));
            store.load();
            System.out.println("cache contains" + store.getIndexes().size() + "indexs");
        } catch (IOException e) {
            System.out.println("issue loading cache from disk");
            e.printStackTrace();
        }
    }

/*    public static void main(String[] args) throws IOException
    {
        CacheReader cacheExporter = new CacheReader("C:\\Users\\soma.wheelhouse\\.runelite\\jagexcache\\oldschool\\LIVE");
        cacheExporter.SpotAnimationDefinition_get(800);
    }*/

    @SneakyThrows
    public int GetFileCount(IndexType IndexId, int ArchiveId) {
        Index index = store.getIndex(IndexId);
        Archive archive = index.getArchive(ArchiveId);
        return index.toIndexData().getArchives()[ArchiveId].getFiles().length;
    }

    @SneakyThrows
    public int GetArchiveCount(IndexType IndexId) {
        Index index = store.getIndex(IndexId);
        return index.toIndexData().getArchives().length;
    }

    public int[] provideRsCacheHashes() {
            List<Index> indexes = store.getIndexes();
            int[] hashes = new int[indexes.size()];
            for(Index index : indexes) {
                hashes[index.getId()] = index.getCrc();
                System.out.println("index "+index.getId() + " crc32 hash is "+index.getCrc());
            }

            Buffer mainBuffer = new Buffer(new byte[(hashes.length*4)+12]);
            mainBuffer.writeInt_Array(hashes, hashes.length);
            RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "RsCacheHashesProvided");

            System.out.println("RsCacheHashes have been provided To Unreal");
            return hashes;
    }

    @SneakyThrows
    public byte[] GetCacheFileBytes(IndexType IndexId, int ArchiveId, int FileId) {
        Index index = store.getIndex(IndexId);
        Archive archive = index.getArchive(ArchiveId);

        if (archive == null) {
            System.out.println("Failed To Find Archive Id "+ArchiveId + " index Id:"+IndexId + " FileId: "+FileId);
            return null;
        }

        Storage storage = store.getStorage();
        byte[] archiveData = storage.loadArchive(archive);
        ArchiveFiles archiveFiles = archive.getFiles(archiveData);
        FSFile actualFile = archiveFiles.findFile(FileId);
        if (actualFile != null) {
            byte[] fileBytes = actualFile.getContents();
            return fileBytes;
        } else {
            return null;
        }
    }

    @SneakyThrows
    public ArchiveFiles GetArchiveFiles(IndexType IndexId, int ArchiveId) {
        Index index = store.getIndex(IndexId);
        Archive archive = index.getArchive(ArchiveId);
        Storage storage = store.getStorage();
        byte[] archiveData = storage.loadArchive(archive);
        ArchiveFiles archiveFiles = archive.getFiles(archiveData);
        return archiveFiles;
    }

    public SpotAnimationDefinition SpotAnimationDefinition_get(int id) {
        byte[] bytes = GetCacheFileBytes(IndexType.CONFIGS, ConfigType.SPOTANIM.getId(), id);

        //decode(buffer)
        Buffer var1 = new Buffer(bytes);

        SpotAnimationDefinition spotAnimDef = new SpotAnimationDefinition();
        spotAnimDef.decode(var1);

        return spotAnimDef;
    }

    @SneakyThrows
    public List<FSFile> getCacheFiles(IndexType IndexId, int ArchiveId)
    {
        Index index = store.getIndex(IndexId);
        Archive archive = index.getArchive(ArchiveId);

        Storage storage = store.getStorage();
        byte[] archiveData = storage.loadArchive(archive);
        ArchiveFiles archiveFiles = archive.getFiles(archiveData);
        return archiveFiles.getFiles();
    }

    @SneakyThrows
    public void sendFrames() {
        Storage storage = store.getStorage();
        Index frameIndex = store.getIndex(IndexType.ANIMATIONS);
        int counter = 0;
        for (Archive archive : frameIndex.getArchives())
        {
            byte[] archiveData = storage.loadArchive(archive);

            ArchiveFiles archiveFiles = archive.getFiles(archiveData);
            for (FSFile archiveFile : archiveFiles.getFiles()) {
                byte[] bytes = archiveFile.getContents();
                if (bytes != null && bytes.length > 0) {
                    counter++;
                    int frameId_Packed = (archive.getArchiveId() << 16 | archiveFile.getFileId());
                    Buffer mainBuffer = new Buffer(new byte[0]);
                    mainBuffer = new Buffer(new byte[bytes.length+12]);

                    mainBuffer.writeLong(frameId_Packed);
                    mainBuffer.writeByte_Array(bytes, bytes.length);
                    RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "AnimationFrame");
                }
            }
        }
        System.out.println("Sent "+ counter +" Frames");
    }

    @SneakyThrows
    public void sendSkeletons() {
        int archiveCount = GetArchiveCount(IndexType.SKELETONS);
        int counter = 0;
        for (int i = 0; i < archiveCount; i++) {
            byte[] bytes = GetCacheFileBytes(IndexType.SKELETONS, i,0);
            if (bytes != null && bytes.length > 0) {
                counter++;
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(i);
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "Skeleton");
            }
        }
        System.out.println("Sent "+ counter +" Skeletons");
    }

    public void sendObjectDefinitions() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.OBJECT.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            if (bytes != null && bytes.length > 0) {
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(files.get(i).getFileId());
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "ObjectDefinition");
                counter++;
            }
        }
        System.out.println("Sent "+ counter +" ObjectDefinitions");
    }

    public void sendKitDefinitions() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.IDENTKIT.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            if (bytes != null && bytes.length > 0) {
                counter++;
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(files.get(i).getFileId());
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "KitDefinition");
            }
        }
        System.out.println("Sent "+ counter +" SpotSequenceDefinitions");
    }

    public void sendSequenceDefinitions() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.SEQUENCE.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            if (bytes != null && bytes.length > 0) {
                counter++;
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(files.get(i).getFileId());
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "SequenceDefinition");
            }
        }
        System.out.println("Sent "+ counter +" SpotSequenceDefinitions");
    }

    public void sendSpotAnimations() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.SPOTANIM.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            if (bytes != null && bytes.length > 0) {
                counter++;
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(files.get(i).getFileId());
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "SpotAnimDefinition");
            }
        }
        System.out.println("Sent "+ counter +" SpotAnimations");
    }

    public void sendNpcDefinitions() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.NPC.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
            if (bytes != null && bytes.length > 0) {
                counter++;
                mainBuffer.writeLong(files.get(i).getFileId());
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "NpcDefinition");
            }
        }
        System.out.println("Sent "+ counter +" NpcDefinitions");
    }

    public void sendItemDefinitions() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.ITEM.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
            if (bytes != null && bytes.length > 0) {
                mainBuffer.writeLong(files.get(i).getFileId());
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "ItemDefinition");
                counter++;
            }
        }
        System.out.println("Sent "+ counter +" ItemDefinitions");
    }

    public void sendOverlayDefinitions() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.OVERLAY.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            if (bytes != null && bytes.length > 0) {
                counter++;
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(files.get(i).getFileId() +1);
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "OverlayDefinition");
            }
        }
        System.out.println("Sent "+ counter +" OverlayDefinitions");
    }

    public void sendUnderlayDefinitions() {
        List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.UNDERLAY.getId());
        int counter = 0;
        for (int i = 0; i < files.size(); i++) {
            byte[] bytes = files.get(i).getContents();
            if (bytes != null && bytes.length > 0) {
                counter++;
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(files.get(i).getFileId() +1);
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "UnderlayDefinition");
            }
        }
        System.out.println("Sent "+ counter +" UnderlayDefinitions");
    }

    public void sendModels() {
        int archiveCount = GetArchiveCount(IndexType.MODELS);
        int counter = 0;
        for (int i = 0; i < archiveCount; i++) {
            byte[] bytes = GetCacheFileBytes(IndexType.MODELS, i,0);
            if (bytes != null && bytes.length > 0) {
                counter++;
                Buffer mainBuffer = new Buffer(new byte[bytes.length+12]);
                mainBuffer.writeLong(i);
                mainBuffer.writeByte_Array(bytes, bytes.length);
                RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "ModelData");
            }
        }
        System.out.println("Sent "+ counter +" Models");
    }

/*    public void sendTileData() {
        int archiveCount = GetArchiveCount(IndexType.MODELS);
        for (int i = 0; i < archiveCount; i++) {
            byte[] bytes = GetCacheFileBytes(IndexType.MODELS, i,0);
            if (bytes != null) {
                bytes = RuneModPlugin.insertIDToByteArray(bytes, i);
                //RuneModPlugin.myRunnableSender.sendBytes(bytes,"ModelData");
                System.out.println("Sent Model: " + i);
            }
        }
    }*/

    long MakeAssetId_ue4(long userId_in, long contentId_in) {
        long AssetId = ((long) userId_in << 32) | contentId_in;
        return AssetId;
    }

    long CoordinatesToContentId(int x, int y, int z)
    {
        int shift = 32;
        int p2 = x;
        p2 = p2 << (shift -= 15);
        int p3 = y;
        p3 = p3 << (shift -= 15);
        int p4 = z;
        p4 = p4 << (shift -= 2);

        long ContentId = p2 | p3 | p4;

        return ContentId;
    }

    private static byte[] trimmedBufferBytes(Buffer buffer) {
        return Arrays.copyOfRange(buffer.array, 0, buffer.offset);
    }

    static int convertTileCoordinatesToContentId(int z, int x, int y) {
        int regionId = ((x >> 6) << 8) | (y >> 6);
        int tileIdRelativeToRegion = (z << 12) | ((x & 0x3F) << 6) | (y & 0x3F);
        return (regionId * 16384) + tileIdRelativeToRegion;
    }



    public void sendTiles() {
        System.out.println("sending tiles...");
        for (int regionID = 0; regionID < Short.MAX_VALUE; regionID++) {
            //System.out.println("sending region"+regionID);
            sendTilesInRegion(regionID);
        }
        System.out.println("sent tiles");
    }

    @SneakyThrows
    public void isCacheComplete() {
        for (Index index : store.getIndexes()) {
            for(Archive archive : index.getArchives()) {
                store.getStorage().loadArchive(archive);
                byte[] archiveData = store.getStorage().loadArchive(archive);
                ArchiveFiles archiveFiles = archive.getFiles(archiveData);

                //create list of downloaded files
                List<Integer> DownloadedFileIdList = new ArrayList<>();
                for (FSFile fsFile : archiveFiles.getFiles()) {
                    DownloadedFileIdList.add(fsFile.getFileId());
                }

                //check if any files are missing from the downloaded file-list
                for (FileData fileData : archive.getFileData()) {
                    if (!DownloadedFileIdList.contains(fileData.getId())) {
                        System.out.println("file missing. id: "+fileData.getId() + " index: "+index.getId());
                    }
                }
            }
        }
    }

    @SneakyThrows
    public void sendTilesInRegion(int regionId) {
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

                bytes = map.decompress(storage.loadArchive(map));

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
                                long tileId = convertTileCoordinatesToContentId(z, tileX, tileY);
                                //long tileId = MakeAssetId_ue4(0, CoordinatesToContentId(tileX, tileY, z));
/*                                if (tileId >= 2147483647) {
                                    System.out.println("warning, id is more than 32bits. is:" + tileId);
                                }*/

                                MapDefinition.Tile tile = mapDef.getTiles()[z][x][y];

                                if(tile.height == nullTile.height && tile.overlayId == nullTile.overlayId && tile.underlayId == nullTile.underlayId && tile.overlayRotation == nullTile.overlayRotation && tile.settings == nullTile.settings  && tile.overlayPath == nullTile.overlayPath && tile.attrOpcode == nullTile.attrOpcode) { //dont send tile if it is null;.
                                    continue;
                                }

                                mainBuffer.writeLong(tileId); //write cacheElment id

                                Buffer elementBuffer = new Buffer(new byte[20]);//create cacheElementBytes;
                                elementBuffer.writeShort((region.getTileHeight(z,x,y)/8)*-1);
                                elementBuffer.writeShort(tile.overlayId);
                                elementBuffer.writeByte(tile.overlayRotation);
                                elementBuffer.writeByte(tile.overlayPath);
                                elementBuffer.writeByte(tile.underlayId);
                                elementBuffer.writeByte(tile.settings);

                                mainBuffer.writeByte_Array(elementBuffer.array, elementBuffer.offset);
/*                                while (true)
                                {
                                    int attribute = MapDataBuffer.readUnsignedShort();
                                    if (attribute == 0)
                                    {
                                        buffer.writeByte(1); //write height opcode
                                        buffer.writeByte((region.getTileHeight(z,x,y)/8)*-1); //write height.
                                        break;
                                    }
                                    if (attribute == 1)
                                    {
                                        buffer.writeByte(attribute);
                                        buffer.writeByte((region.getTileHeight(z,x,y)/8)*-1);
                                        //int height = MapDataBuffer.readUnsignedByte();
                                        //System.out.println("height: "+height);
                                        //tile.height = height;
                                        break;
                                    }
                                    else if (attribute <= 49)
                                    {
                                        buffer.writeByte(attribute);
                                        //tile.attrOpcode = attribute;
                                        //int overlayId = (short)MapDataBuffer.readShort();
                                        //System.out.println("oberlayId: "+overlayId);
                                        buffer.writeByte(tile.overlayId);
                                        //tile.overlayPath = (byte) ((attribute - 2) / 4);
                                        //tile.overlayRotation = (byte) (attribute - 2 & 3);
                                    }
                                    else if (attribute <= 81)
                                    {
                                        buffer.writeByte(attribute);
                                        //tile.settings = (byte) (attribute - 49);
                                    }
                                    else
                                    {
                                        buffer.writeByte(attribute);
                                        //tile.underlayId = (short) (attribute - 81);
                                    }
                                }*/
                                //if(isNulltile==false) { //if tile is non null tile
                                    //System.out.println("sending tile at: X:"+tileX+" Y:"+tileY+ "Z:"+z);
                                    //byte[] tileBytes = RuneModPlugin.insertIDToByteArray(buffer.array, (int)tileId);

                                   //int delay = 15000;
                                    //long start = System.nanoTime();
                                    //while(start + delay >= System.nanoTime());
                                //}
                            }
                        }
                    }
                    //RuneModPlugin.myRunnableSender.sendBytes(trimmedBufferBytes(mainBuffer),"RegionTiles");
                    RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "RegionTiles");
                    //System.out.println("Sent TerrainData: " + id + " bytes len: "+ bytes.length);
/*                    int delay = 1000000;
                    long start = System.nanoTime();
                    while(start + delay >= System.nanoTime());*/
                } else {
                    System.out.println("region data is null");
                }
            }
            catch (IOException ex)
            {
                System.out.println("Can't decrypt region " + regionId);
            }
    }

    public int getTileHeightAtCoordinate(int tileX_WorldPos, int tileY_WorldPos, int plane) {
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

    public MapDefinition.Tile getTileAtCoordinate(int tileX_WorldPos, int tileY_WorldPos, int plane) {
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
        //Region region = new Region(regionId);
        //region.loadTerrain(mapDef);

        int x_tilePosInRegion = tileX_WorldPos-(regionX*64);
        int y_tilePosInRegion = tileY_WorldPos-(regionY*64);

        MapDefinition.Tile tile = mapDef.getTiles()[plane][x_tilePosInRegion][y_tilePosInRegion];
        return tile;
    }

    public void sendColourPallette() {
        Buffer buffer = new Buffer(new byte[(65536*4)+1]);
        int[] pallette = JagexColor.createPalette(1);
        for (int i = 0; i < 65536; i++) {
            buffer.writeInt(pallette[i]);
        }
        //RuneModPlugin.myRunnableSender.sendBytes(buffer.array,"ColourPalette");
    }
}

