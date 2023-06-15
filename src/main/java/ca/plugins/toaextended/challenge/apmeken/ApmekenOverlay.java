/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, rdutta <https://github.com/rdutta>
 * Copyright (c) 2022, LlemonDuck
 * Copyright (c) 2022, TheStonedTurtle
 * Copyright (c) 2019, Ron Young <https://github.com/raiyni>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.plugins.toaextended.challenge.apmeken;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Perspective;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class ApmekenOverlay extends Overlay implements PluginLifecycleComponent
{

	private static final int ANIMATION_ID_VOLATILE_BABOON = 9756;
	private static final int AREA_SIZE_3X3 = 3;

	private final OverlayManager overlayManager;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final Client client;
	private final ToaExtendedConfig config;
	private final Apmeken apmeken;

	@Inject
	public ApmekenOverlay(final OverlayManager overlayManager, final ModelOutlineRenderer modelOutlineRenderer, final Client client, final ToaExtendedConfig config, final Apmeken apmeken)
	{
		this.overlayManager = overlayManager;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.client = client;
		this.config = config;
		this.apmeken = apmeken;

		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return apmeken.isEnabled(config, raidState);
	}

	@Override
	public void startUp()
	{
		overlayManager.add(this);
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(this);
	}

	@Override
	public Dimension render(final Graphics2D graphics2D)
	{
		if (config.apmekenBaboonOutline())
		{
			renderBaboonOutline();
		}

		if (config.apmekenVolatileBaboonTiles())
		{
			renderVolatileBaboonTiles(graphics2D);
		}

		if (config.apmekenRoofVentOutline())
		{
			renderRoofVentOutline();
		}

		return null;
	}

	private void renderRoofVentOutline()
	{
		final List<TileObject> tileObjects;

		switch (apmeken.getSense())
		{
			case VENTS:
				tileObjects = apmeken.getVents();
				break;
			case ROOF:
				tileObjects = apmeken.getRoofs();
				break;
			case NONE:
			default:
				return;
		}

		final GameObject statue = apmeken.getStatue();

		if (statue != null)
		{
			modelOutlineRenderer.drawOutline(statue, 2, Color.MAGENTA, 4);
		}

		if (tileObjects.isEmpty())
		{
			return;
		}

		for (final TileObject tileObject : tileObjects)
		{
			modelOutlineRenderer.drawOutline(tileObject, 2, Color.MAGENTA, 4);
		}
	}

	private void renderBaboonOutline()
	{
		final List<NPC> combatBaboons = apmeken.getBaboons();

		if (combatBaboons.isEmpty())
		{
			return;
		}

		for (final NPC npc : combatBaboons)
		{
			final Color color;

			switch (npc.getId())
			{
				case NpcID.BABOON_BRAWLER:
				case NpcID.BABOON_BRAWLER_11712:
					color = Color.RED;
					break;
				case NpcID.BABOON_MAGE:
				case NpcID.BABOON_MAGE_11714:
					color = Color.BLUE;
					break;
				case NpcID.BABOON_THROWER:
				case NpcID.BABOON_THROWER_11713:
					color = Color.GREEN;
					break;
				case NpcID.BABOON_SHAMAN:
					color = Color.CYAN;
					break;
				case NpcID.CURSED_BABOON:
					color = Color.MAGENTA;
					break;
				case NpcID.VOLATILE_BABOON:
				case NpcID.BABOON_THRALL:
				default:
					continue;
			}

			modelOutlineRenderer.drawOutline(npc, 1, color, 0);
		}
	}

	private void renderVolatileBaboonTiles(final Graphics2D graphics2D)
	{
		final List<NPC> volatileBaboons = apmeken.getVolatileBaboons();

		if (volatileBaboons.isEmpty())
		{
			return;
		}

		for (final NPC npc : volatileBaboons)
		{
			final Color color = npc.getAnimation() == ANIMATION_ID_VOLATILE_BABOON ? Color.RED : Color.ORANGE;

			final LocalPoint localPoint = npc.getLocalLocation();

			if (localPoint == null)
			{
				continue;
			}

			final Polygon polygon = Perspective.getCanvasTileAreaPoly(client, localPoint, AREA_SIZE_3X3);

			if (polygon == null)
			{
				continue;
			}

			ToaUtils.drawOutlineAndFill(graphics2D, color,
				new Color(color.getRed(), color.getGreen(), color.getBlue(), 20),
				1, polygon);
		}
	}

}
