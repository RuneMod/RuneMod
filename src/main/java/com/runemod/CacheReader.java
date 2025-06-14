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

import com.runemod.cache.ConfigType;
import com.runemod.cache.IndexType;
import com.runemod.cache.definitions.MapDefinition;
import com.runemod.cache.definitions.loaders.MapLoader;
import com.runemod.cache.fs.Archive;
import com.runemod.cache.fs.ArchiveFiles;
import com.runemod.cache.fs.FSFile;
import com.runemod.cache.fs.Index;
import com.runemod.cache.fs.Storage;
import com.runemod.cache.fs.Store;
import com.runemod.cache.index.FileData;
import com.runemod.cache.region.Region;
import java.util.HashSet;
import java.util.Set;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@Slf4j
public class CacheReader
{
	public Store store;
	public String myCachePath;

	public CacheReader(String cachePath)
	{
		cacheFullyLoaded = false;
		myCachePath = cachePath;
		initCacheStore();
	}

	void initCacheStore() {
		try
		{
			store = new Store(new File(myCachePath));
			//log.debug("cachePath: " + cachePath);
			store.load();
			//log.debug("cache contains" + store.getIndexes().size() + "indexs");
		}
		catch (IOException e)
		{
			store = null;
			log.debug("issue loading cache from disk");
			e.printStackTrace();
		}
	}


	private final Set<String> loadedArchives = new HashSet<>();
	public static boolean cacheFullyLoaded = false;

	public boolean checkIfCacheFullyLoaded()
	{
			try
			{
				if (cacheFullyLoaded == true) {
					return true;
				}

				initCacheStore(); //reInitCacheStore cos store has changed.

				if (store.getIndexes() == null || store.getIndexes().size() < 23) {
					log.debug("Cache store not ready yet");
					return false;
				}

				boolean hasAnyArchives = false;
				for (Index index : store.getIndexes()) {
					if (index.getArchives().size() > 0) {
						hasAnyArchives = true;
						break;
					}
				}

				if (!hasAnyArchives) {
					log.debug("Cache indexes are present but contain no archives. Likely still initializing.");
					return false;
				}

				int total = 0;
				int missing = 0;

				for (Index index : store.getIndexes())
				{
					for (Archive archive : index.getArchives())
					{
						total++;

						String key = index.getId() + ":" + archive.getArchiveId();

						if (loadedArchives.contains(key))
							continue;
						try
						{
							byte[] data = store.getStorage().load(index.getId(), archive.getArchiveId());
							if (data != null && data.length > 0)
							{
								try
								{
									ArchiveFiles archiveFiles = archive.getFiles(data);
									if(archiveFiles.getFiles().size() > 0) {
										loadedArchives.add(key);
									} else {
										missing++;
										cacheFullyLoaded = false;
										return false;
									}
								}
								catch (IOException io)
								{
									loadedArchives.add(key);
								}
							}
							else
							{
								missing++;
								cacheFullyLoaded = false;
								return false;
							}
						}
						catch (IOException e)
						{
							missing++;
							cacheFullyLoaded = false;
							return false;
						}
					}
				}

				if (loadedArchives.size() == total && missing == 0)
				{
					log.debug("there are "+store.getIndexes().size() +"indexes");
					cacheFullyLoaded = true;
					return true;
				}
				else
				{
					cacheFullyLoaded = false;
					return false;
				}
			}
			catch (Exception e)
			{
				log.debug("Error during cache scan", e);
			}

			return false;
	}


/*    public static void main(String[] args) throws IOException
    {
        CacheReader cacheExporter = new CacheReader("C:\\Users\\soma.wheelhouse\\.runelite\\jagexcache\\oldschool\\LIVE");
        cacheExporter.SpotAnimationDefinition_get(800);
    }*/

	private static byte[] trimmedBufferBytes(Buffer buffer)
	{
		return Arrays.copyOfRange(buffer.array, 0, buffer.offset);
	}

	static int convertTileCoordinatesToContentId(int z, int x, int y)
	{
		int regionId = ((x >> 6) << 8) | (y >> 6);
		int tileIdRelativeToRegion = (z << 12) | ((x & 0x3F) << 6) | (y & 0x3F);
		return (regionId * 16384) + tileIdRelativeToRegion;
	}

	@SneakyThrows
	public int GetFileCount(IndexType IndexId, int ArchiveId)
	{
		Index index = store.getIndex(IndexId);
		Archive archive = index.getArchive(ArchiveId);
		return index.toIndexData().getArchives()[ArchiveId].getFiles().length;
	}

	@SneakyThrows
	public int GetArchiveCount(IndexType IndexId)
	{
		Index index = store.getIndex(IndexId);
		return index.toIndexData().getArchives().length;
	}

	/*public void printRevs()
	{
		List<Index> indexes = store.getIndexes();
		int[] hashes = new int[indexes.size()];
		for (Index index : indexes)
		{
			hashes[index.getId()] = index.getCrc();
			//log.debug("index "+index.getId() + " crc32 hash is "+index.getCrc());
			log.debug("index " + index.getId() + " Rev is " + index.getRevision());

*//*            for(Archive archive : index.getArchives()) {
                log.debug("archive "+archive.getArchiveId() + " Rev is "+archive.getRevision());
            }*//*
		}
	}*/

	public static final int cacheExporterVersion = 1; //I increment this this to force a reexport of rscache. useful for forcing update when the cache exporter is altered.

	public int[] provideRsCacheHashes()
	{
		log.debug("provideRsCacheHashes()");
		List<Index> indexes = store.getIndexes();
		int[] hashes = new int[indexes.size()+1];
		for (Index index : indexes)
		{
			//hashes[index.getId()] = index.getCrc();
			//log.debug("index "+index.getId() + " crc32 hash is "+index.getCrc());
			hashes[index.getId()] = index.getRevision(); //moved to rev system instead of crc.
			log.debug("index " + index.getId() + " rev is " + index.getRevision());
		}

		hashes[hashes.length-1] = cacheExporterVersion;

		Buffer mainBuffer = new Buffer(new byte[(hashes.length * 4) + 12]);
		mainBuffer.writeInt_Array(hashes, hashes.length);
		RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "RsCacheHashesProvided");

		log.debug("RsCacheHashes/Revs have been provided To Unreal");
		return hashes;
	}


	public byte[] GetCacheFileBytes(IndexType IndexId, int ArchiveId, int FileId)
	{
		Index index = store.getIndex(IndexId);
		if(index == null) {
			log.debug("Failed To Find Index " + ArchiveId + " index Id:" + IndexId);
			return null;
		}
		Archive archive = index.getArchive(ArchiveId);

		if (archive == null)
		{
			log.debug("Failed To Find Archive Id " + ArchiveId + " index Id:" + IndexId + " FileId: " + FileId);
			return null;
		}

		Storage storage = store.getStorage();
		byte[] archiveData = new byte[0];
		try
		{
			archiveData = storage.loadArchive(archive);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
		if(archiveData == null) {return null;}
		ArchiveFiles archiveFiles = null;
		try
		{
			archiveFiles = archive.getFiles(archiveData);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}

		if(archiveFiles == null) {return null;}
		FSFile actualFile = archiveFiles.findFile(FileId);
		if (actualFile != null)
		{
			byte[] fileBytes = actualFile.getContents();
			return fileBytes;
		}
		else
		{
			return null;
		}
	}

	@SneakyThrows
	public ArchiveFiles GetArchiveFiles(IndexType IndexId, int ArchiveId)
	{
		Index index = store.getIndex(IndexId);
		Archive archive = index.getArchive(ArchiveId);
		Storage storage = store.getStorage();
		byte[] archiveData = storage.loadArchive(archive);
		ArchiveFiles archiveFiles = archive.getFiles(archiveData);
		return archiveFiles;
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
	public void sendFrames()
	{
		Storage storage = store.getStorage();
		Index frameIndex = store.getIndex(IndexType.ANIMATIONS);
		int counter = 0;
		for (Archive archive : frameIndex.getArchives())
		{
			byte[] archiveData = storage.loadArchive(archive);

			ArchiveFiles archiveFiles = archive.getFiles(archiveData);
			for (FSFile archiveFile : archiveFiles.getFiles())
			{
				byte[] bytes = archiveFile.getContents();
				if (bytes != null && bytes.length > 0)
				{
					counter++;
					int frameId_Packed = (archive.getArchiveId() << 16 | archiveFile.getFileId());
					Buffer mainBuffer = new Buffer(new byte[0]);
					mainBuffer = new Buffer(new byte[bytes.length + 12]);

					mainBuffer.writeLong(frameId_Packed);
					mainBuffer.writeByte_Array(bytes, bytes.length);
					RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "AnimationFrame");
				}
			}
		}
		log.debug("Sent " + counter + " Frames");
	}

	@SneakyThrows
	public void sendSkeletalFrameSets()
	{
		Storage storage = store.getStorage();
		Index frameIndex = store.getIndex(IndexType.SKELANIMFRAMES);
		int counter = 0;
		for (Archive archive : frameIndex.getArchives())
		{
			byte[] archiveData = storage.loadArchive(archive);

			ArchiveFiles archiveFiles = archive.getFiles(archiveData);
			for (FSFile archiveFile : archiveFiles.getFiles())
			{
				byte[] bytes = archiveFile.getContents();
				if (bytes != null && bytes.length > 0)
				{
					counter++;
					int frameId_Packed = (archive.getArchiveId() << 16 | archiveFile.getFileId());
					Buffer mainBuffer = new Buffer(new byte[0]);
					mainBuffer = new Buffer(new byte[bytes.length + 12]);

					mainBuffer.writeLong(frameId_Packed);
					mainBuffer.writeByte_Array(bytes, bytes.length);
					RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "SkeletalFrameSet");
				}
			}
		}
		log.debug("Sent " + counter + " SkeletalFrameSets");
	}

	@SneakyThrows
	public void sendSkeletons()
	{
		int counter = 0;
		for (int i : getArchiveIds(IndexType.SKELETONS))
		{
			byte[] bytes = GetCacheFileBytes(IndexType.SKELETONS, i, 0);
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(i);
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "Skeleton");
			} else {
				log.debug("SkeletonId: "+i+" not found");
			}
		}
		log.debug("Sent " + counter + " Skeletons");
	}

	public void sendObjectDefinitions()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.OBJECT.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			if (bytes != null && bytes.length > 0)
			{
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(files.get(i).getFileId());
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "ObjectDefinition");
				counter++;
			}
		}
		log.debug("Sent " + counter + " ObjectDefinitions");
	}

	public void sendKitDefinitions()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.IDENTKIT.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(files.get(i).getFileId());
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "KitDefinition");
			}
		}
		log.debug("Sent " + counter + " KitDefinitions");
	}

	public void sendSequenceDefinitions()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.SEQUENCE.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(files.get(i).getFileId());
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "SequenceDefinition");
			}
		}
		log.debug("Sent " + counter + " SpotSequenceDefinitions");
	}

	public void sendSpotAnimations()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.SPOTANIM.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(files.get(i).getFileId());
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "SpotAnimDefinition");
			}
		}
		log.debug("Sent " + counter + " SpotAnimations");
	}

	public void sendNpcDefinitions()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.NPC.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				mainBuffer.writeLong(files.get(i).getFileId());
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "NpcDefinition");
			}
		}
		log.debug("Sent " + counter + " NpcDefinitions");
	}

	public void sendItemDefinitions()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.ITEM.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
			if (bytes != null && bytes.length > 0)
			{
				mainBuffer.writeLong(files.get(i).getFileId());
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "ItemDefinition");
				counter++;
			}
		}
		log.debug("Sent " + counter + " ItemDefinitions");
	}

	public void sendOverlayDefinitions()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.OVERLAY.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(files.get(i).getFileId() + 1);
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "OverlayDefinition");
			}
		}
		log.debug("Sent " + counter + " OverlayDefinitions");
	}

	public void sendUnderlayDefinitions()
	{
		List<FSFile> files = getCacheFiles(IndexType.CONFIGS, ConfigType.UNDERLAY.getId());
		int counter = 0;
		for (int i = 0; i < files.size(); i++)
		{
			byte[] bytes = files.get(i).getContents();
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(files.get(i).getFileId() + 1);
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "UnderlayDefinition");
			}
		}
		log.debug("Sent " + counter + " UnderlayDefinitions");
	}

	public ArrayList<Integer> getArchiveIds(IndexType indexType) {
		ArrayList<Integer> ids = new ArrayList<>();
		Index index = store.getIndex(indexType);
		if(index!=null) {
			for (Archive archive : index.getArchives())
			{
				if(archive!= null) {
					ids.add(archive.getArchiveId());
				}
			}
		}

		return ids;
	}

	public void sendModels()
	{
		//int archiveCount = GetArchiveCount(IndexType.MODELS);
		//Index index = store.getIndex(IndexType.MODELS);

		int counter = 0;
		for (int i : getArchiveIds(IndexType.MODELS))
		{
			byte[] bytes = GetCacheFileBytes(IndexType.MODELS, i, 0);
			if (bytes != null && bytes.length > 0)
			{
				counter++;
				Buffer mainBuffer = new Buffer(new byte[bytes.length + 12]);
				mainBuffer.writeLong(i);
				mainBuffer.writeByte_Array(bytes, bytes.length);
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "ModelData");
			} else {
				log.debug("ModelId: "+i+" not found");
			}
		}
		log.debug("Sent " + counter + " Models");
	}

	long MakeAssetId_ue4(long userId_in, long contentId_in)
	{
		long AssetId = (userId_in << 32) | contentId_in;
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

	public void sendTiles()
	{
		log.debug("sending tiles...");
		for (int regionID = 0; regionID < Short.MAX_VALUE; regionID++)
		{
			//log.debug("sending region"+regionID);
			sendTilesInRegion(regionID);
		}
		log.debug("sent tiles");
	}

	@SneakyThrows
	public void isCacheComplete()
	{
		for (Index index : store.getIndexes())
		{
			for (Archive archive : index.getArchives())
			{
				store.getStorage().loadArchive(archive);
				byte[] archiveData = store.getStorage().loadArchive(archive);
				ArchiveFiles archiveFiles = archive.getFiles(archiveData);

				//create list of downloaded files
				List<Integer> DownloadedFileIdList = new ArrayList<>();
				for (FSFile fsFile : archiveFiles.getFiles())
				{
					DownloadedFileIdList.add(fsFile.getFileId());
				}

				//check if any files are missing from the downloaded file-list
				for (FileData fileData : archive.getFileData())
				{
					if (!DownloadedFileIdList.contains(fileData.getId()))
					{
						log.debug("file missing. id: " + fileData.getId() + " index: " + index.getId());
					}
				}
			}
		}
	}

	@SneakyThrows
	public void sendTilesInRegion(int regionId)
	{
		MapDefinition.Tile nullTile = new MapDefinition.Tile();
		try
		{

			int regionX = regionId >> 8;
			int regionY = regionId & 0xFF;


			Index index = store.getIndex(IndexType.MAPS);
			Storage storage = store.getStorage();
			Archive map = index.findArchiveByName("m" + regionX + "_" + regionY);

			if (map == null)
			{
				return;
			}

			byte[] bytes = null;

			bytes = map.decompress(storage.loadArchive(map));

			if (bytes == null)
			{
				return;
			}

			MapLoader mapLoader = new MapLoader();
			MapDefinition mapDef = mapLoader.load(regionX, regionY, bytes);
			//Region region = new Region(regionId);
			//region.loadTerrain(mapDef);

			int swTileX = regionX * 64;
			int swTileY = regionY * 64;
			if (bytes != null)
			{
				Buffer mainBuffer = new Buffer(new byte[64 * 64 * 4 * (32)]); //always sending 6bytes+4 for id. inefficient can optimize somewhat easily by trimming unneeded bytes.

				for (int z = 0; z < 4; z++)
				{
					for (int x = 0; x < 64; x++)
					{
						for (int y = 0; y < 64; y++)
						{
							int tileX = swTileX + x;
							int tileY = swTileY + y;
							long tileId = convertTileCoordinatesToContentId(z, tileX, tileY);

							MapDefinition.Tile tile = mapDef.getTiles()[z][x][y];

							if (tile.height == nullTile.height && tile.overlayId == nullTile.overlayId && tile.underlayId == nullTile.underlayId && tile.overlayRotation == nullTile.overlayRotation && tile.settings == nullTile.settings && tile.overlayPath == nullTile.overlayPath && tile.attrOpcode == nullTile.attrOpcode)
							{ //dont send tile if it is null;.
								continue;
							}

							mainBuffer.writeLong(tileId); //write cacheElment id

							Buffer elementBuffer = new Buffer(new byte[20]);//create cacheElementBytes;
							//elementBuffer.writeShort((region.getTileHeight(z, x, y) / 8) * -1);
							if(tile.height != nullTile.height) {
								elementBuffer.writeShort(tile.height);
							}else{
								elementBuffer.writeShort(-1);
							}
							elementBuffer.writeShort(tile.overlayId&0x7FFF);
							elementBuffer.writeByte(tile.overlayRotation);
							elementBuffer.writeByte(tile.overlayPath);
							elementBuffer.writeByte(tile.underlayId&0x7FFF);
							elementBuffer.writeByte(tile.settings);

							mainBuffer.writeByte_Array(elementBuffer.array, elementBuffer.offset);
						}
					}
				}
				RuneModPlugin.sharedmem_rm.backBuffer.writePacket(mainBuffer, "RegionTiles");
			}
			else
			{
				log.debug("region data is null");
			}
		}
		catch (IOException ex)
		{
			log.debug("Can't decrypt region " + regionId);
		}
	}

	public MapDefinition.Tile getTileAtCoordinate(int tileX_WorldPos, int tileY_WorldPos, int plane)
	{
		int regionX = tileX_WorldPos / 64;
		int regionY = tileY_WorldPos / 64;
		int regionId = regionX << 8 | regionY;
		log.debug("regionId: " + regionId);

		Index index = store.getIndex(IndexType.MAPS);
		Storage storage = store.getStorage();
		Archive map = index.findArchiveByName("m" + regionX + "_" + regionY);

		byte[] bytes = new byte[0];

		try
		{
			bytes = map.decompress(storage.loadArchive(map));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		MapLoader mapLoader = new MapLoader();
		MapDefinition mapDef = mapLoader.load(regionX, regionY, bytes);
		//Region region = new Region(regionId);
		//region.loadTerrain(mapDef);

		int x_tilePosInRegion = tileX_WorldPos - (regionX * 64);
		int y_tilePosInRegion = tileY_WorldPos - (regionY * 64);

		MapDefinition.Tile tile = mapDef.getTiles()[plane][x_tilePosInRegion][y_tilePosInRegion];
		return tile;
	}
}

