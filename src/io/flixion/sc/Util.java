package io.flixion.sc;

import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;

public class Util {
	private static Random random = new Random();
	
	public static UUID genUUID(String s) {
		return UUID.nameUUIDFromBytes(s.toLowerCase().getBytes());
	}
	
	public static String addColor(String m) {
		return ChatColor.translateAlternateColorCodes('&', m);
	}
	
	public static String sendPluginMessage(String m) {
		return ChatColor.translateAlternateColorCodes('&', "[SpawnerChunks] " + m);
	}
	
	public static String stripColor(String m) {
		return ChatColor.stripColor(m);
	}
	
	public static int generateRandomInt(int upperBound, int lowerBound) {
		return random.nextInt(upperBound - lowerBound + 1) + lowerBound;
	}
	
	public static boolean generateRandomBoolean() {
		return random.nextBoolean();
	}
}
