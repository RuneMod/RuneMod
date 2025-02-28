package com.runemod;

import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class keyGen
{
	static String[] keys = {
		"DMagvAS2mAPXQMKnY3qmUCtthZO0R8Si",
		"A1B2C3D4E5F6G708I9J0K1L2M3x4O5P6Q",}; //keys to hash. note, keys must be more than 30 characters long.

	@SneakyThrows
	public static String hashKey(String key)
	{
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
		String encoded = Base64.getEncoder().encodeToString(hash);
		return encoded;
	}

	public static void createKeyHashes(String[] keys)
	{
		for (String key : keys)
		{
			System.out.println(hashKey(key));
		}
	}

	public static void main(String[] args)
	{
		System.out.println("hashed keys: ");
		createKeyHashes(keys);
	}
}
