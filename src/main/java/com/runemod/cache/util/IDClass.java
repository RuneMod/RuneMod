/*
 * Copyright (c) 2018 Abex
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
package com.runemod.cache.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class IDClass extends PrintWriter
{
	private final Namer namer = new Namer();

	private IDClass(File file) throws FileNotFoundException
	{
		super(file);
	}

	public static IDClass create(File directory, String name) throws IOException
	{
		IDClass c = new IDClass(new File(directory, name + ".java"));
		c.println("/* This file is automatically generated. Do not edit. */");
		c.println("package net.runelite.api;");
		c.println();
		c.print("public final class ");
		c.println(name);
		c.println("{");
		return c;
	}

	public void add(String name, int id)
	{
		String javaName = namer.name(name, id);
		if (javaName == null)
		{
			return;
		}

		println("	public static final int " + javaName + " = " + id + ";");
	}

	@Override
	public void println()
	{
		// Java 9+ only reads line.separator on startup, so we have to override it here
		write('\n');
	}

	@Override
	public void close()
	{
		println("\t/* This file is automatically generated. Do not edit. */");
		println("}");
		super.close();
	}
}
