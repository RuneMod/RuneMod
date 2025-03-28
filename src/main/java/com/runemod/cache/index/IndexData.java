/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
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
package com.runemod.cache.index;

import lombok.Getter;
import lombok.Setter;
import com.runemod.cache.io.InputStream;
import com.runemod.cache.io.OutputStream;

@Setter
@Getter
public class IndexData
{
	private static final int NAMED = 1;
	private static final int SIZED = 4;

	private int protocol;
	private int revision;
	private boolean named;
	private boolean sized;
	private ArchiveData[] archives;

	public void load(byte[] data)
	{
		InputStream stream = new InputStream(data);
		protocol = stream.readUnsignedByte();
		if (protocol < 5 || protocol > 7)
		{
			throw new IllegalArgumentException("Unsupported protocol");
		}

		if (protocol >= 6)
		{
			this.revision = stream.readInt();
		}

		int flags = stream.readUnsignedByte();
		named = (flags & NAMED) != 0;
		sized = (flags & SIZED) != 0;

		if ((flags & ~(NAMED | SIZED)) != 0)
		{
			throw new IllegalArgumentException("Unknown flags: " + flags);
		}

		int validArchivesCount = protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort();
		int lastArchiveId = 0;

		archives = new ArchiveData[validArchivesCount];

		for (int index = 0; index < validArchivesCount; ++index)
		{
			int archive = lastArchiveId += protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort();

			ArchiveData ad = new ArchiveData();
			ad.id = archive;
			archives[index] = ad;
		}

		if (named)
		{
			for (int index = 0; index < validArchivesCount; ++index)
			{
				int nameHash = stream.readInt();
				ArchiveData ad = archives[index];
				ad.nameHash = nameHash;
			}
		}

		for (int index = 0; index < validArchivesCount; ++index)
		{
			int crc = stream.readInt();

			ArchiveData ad = archives[index];
			ad.crc = crc;
		}

		if (sized)
		{
			for (int i = 0; i < validArchivesCount; i++)
			{
				ArchiveData ad = archives[i];
				ad.compressedSize = stream.readInt();
				ad.decompressedSize = stream.readInt();
			}
		}

		for (int index = 0; index < validArchivesCount; ++index)
		{
			int revision = stream.readInt();

			ArchiveData ad = archives[index];
			ad.revision = revision;
		}

		int[] numberOfFiles = new int[validArchivesCount];
		for (int index = 0; index < validArchivesCount; ++index)
		{
			int num = protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort();
			numberOfFiles[index] = num;
		}

		for (int index = 0; index < validArchivesCount; ++index)
		{
			ArchiveData ad = archives[index];
			int num = numberOfFiles[index];

			ad.files = new FileData[num];

			int last = 0;
			for (int i = 0; i < num; ++i)
			{
				int fileId = last += protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort();

				FileData fd = ad.files[i] = new FileData();
				fd.id = fileId;
			}
		}

		if (named)
		{
			for (int index = 0; index < validArchivesCount; ++index)
			{
				ArchiveData ad = archives[index];
				int num = numberOfFiles[index];

				for (int i = 0; i < num; ++i)
				{
					FileData fd = ad.files[i];
					int name = stream.readInt();
					fd.nameHash = name;
				}
			}
		}
	}

	public byte[] writeIndexData()
	{
		OutputStream stream = new OutputStream();
		stream.writeByte(protocol);
		if (protocol >= 6)
		{
			stream.writeInt(this.revision);
		}

		stream.writeByte((named ? NAMED : 0) | (sized ? SIZED : 0));
		if (protocol >= 7)
		{
			stream.writeBigSmart(this.archives.length);
		}
		else
		{
			stream.writeShort(this.archives.length);
		}

		for (int i = 0; i < this.archives.length; ++i)
		{
			ArchiveData a = this.archives[i];
			int archive = a.getId();

			if (i != 0)
			{
				ArchiveData prev = this.archives[i - 1];
				assert a.id > prev.id : "archive ids out of order";
				archive -= prev.getId();
			}

			if (protocol >= 7)
			{
				stream.writeBigSmart(archive);
			}
			else
			{
				stream.writeShort(archive);
			}
		}

		if (named)
		{
			for (int i = 0; i < this.archives.length; ++i)
			{
				ArchiveData a = this.archives[i];
				stream.writeInt(a.getNameHash());
			}
		}

		for (int i = 0; i < this.archives.length; ++i)
		{
			ArchiveData a = this.archives[i];
			stream.writeInt(a.getCrc());
		}

		if (sized)
		{
			for (int i = 0; i < this.archives.length; ++i)
			{
				ArchiveData a = this.archives[i];
				stream.writeInt(a.getCompressedSize());
				stream.writeInt(a.getDecompressedSize());
			}
		}

		for (int i = 0; i < this.archives.length; ++i)
		{
			ArchiveData a = this.archives[i];
			stream.writeInt(a.getRevision());
		}

		for (int i = 0; i < this.archives.length; ++i)
		{
			ArchiveData a = this.archives[i];

			int len = a.getFiles().length;

			if (protocol >= 7)
			{
				stream.writeBigSmart(len);
			}
			else
			{
				stream.writeShort(len);
			}
		}

		for (int i = 0; i < this.archives.length; ++i)
		{
			ArchiveData a = this.archives[i];

			for (int j = 0; j < a.getFiles().length; ++j)
			{
				FileData file = a.getFiles()[j];
				int offset = file.getId();

				if (j != 0)
				{
					FileData prev = a.getFiles()[j - 1];
					assert file.id > prev.id : "file ids out of order";
					offset -= prev.getId();
				}

				if (protocol >= 7)
				{
					stream.writeBigSmart(offset);
				}
				else
				{
					stream.writeShort(offset);
				}
			}
		}

		if (named)
		{
			for (int i = 0; i < this.archives.length; ++i)
			{
				ArchiveData a = this.archives[i];

				for (int j = 0; j < a.getFiles().length; ++j)
				{
					FileData file = a.getFiles()[j];
					stream.writeInt(file.getNameHash());
				}
			}
		}

		return stream.flip();
	}
}
