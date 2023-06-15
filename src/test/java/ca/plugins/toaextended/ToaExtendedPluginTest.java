package ca.plugins.toaextended;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ToaExtendedPluginTest
{
	public static void main(final String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ToaExtendedPlugin.class);
		RuneLite.main(args);
	}
}