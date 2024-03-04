package com.keyremappingplus;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class KeyRemappingPlusPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(KeyRemappingPlusPlugin.class);
		RuneLite.main(args);
	}
}