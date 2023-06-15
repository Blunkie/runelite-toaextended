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
package ca.plugins.toaextended.boss;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.ToaExtendedConfig.PrayerMode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import lombok.NonNull;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public abstract class PrayerInfoboxOverlay extends Overlay
{

	private static final Dimension DIMENSION = new Dimension(30, 30);
	private static final Color COLOR_OFF = new Color(150, 0, 0, 150);

	protected final Client client;
	protected final ToaExtendedConfig config;

	private final SpriteManager spriteManager;

	private final PanelComponent panelComponent;
	private final InfoBoxComponent infoBoxComponent;

	private BufferedImage magicSprite;
	private BufferedImage rangeSprite;
	private BufferedImage meleeSprite;

	protected PrayerInfoboxOverlay(final Client client, final ToaExtendedConfig config, final SpriteManager spriteManager)
	{
		this.client = client;
		this.config = config;
		this.spriteManager = spriteManager;

		infoBoxComponent = new InfoBoxComponent();
		infoBoxComponent.setColor(Color.WHITE);
		infoBoxComponent.setPreferredSize(DIMENSION);

		panelComponent = new PanelComponent();
		panelComponent.getChildren().add(infoBoxComponent);
		panelComponent.setPreferredSize(DIMENSION);
		panelComponent.setBorder(new Rectangle(0, 0, 0, 0));

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.BOTTOM_RIGHT);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	protected abstract @Nullable Prayer getNextPrayer();

	protected abstract boolean isPrayerInfoboxEnabled();

	@Override
	public Dimension render(final Graphics2D graphics)
	{
		if (!isPrayerInfoboxEnabled())
		{
			return null;
		}

		final PrayerMode prayerMode = config.prayerMode();

		if (prayerMode == PrayerMode.ALL || prayerMode == PrayerMode.INFO_BOX)
		{
			final Prayer prayer = getNextPrayer();

			if (prayer == null)
			{
				return null;
			}

			infoBoxComponent.setImage(getPrayerSprite(prayer));

			infoBoxComponent.setBackgroundColor(client.isPrayerActive(prayer) ?
				ComponentConstants.STANDARD_BACKGROUND_COLOR : COLOR_OFF);

			return panelComponent.render(graphics);
		}

		return null;
	}

	private BufferedImage getPrayerSprite(@NonNull final Prayer prayer)
	{
		switch (prayer)
		{
			case PROTECT_FROM_MAGIC:
				if (magicSprite == null)
				{
					magicSprite = scaleSprite(spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MAGIC, 0));
				}

				return magicSprite;
			case PROTECT_FROM_MISSILES:
				if (rangeSprite == null)
				{
					rangeSprite = scaleSprite(spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MISSILES, 0));
				}

				return rangeSprite;
			case PROTECT_FROM_MELEE:
				if (meleeSprite == null)
				{
					meleeSprite = scaleSprite(spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MELEE, 0));
				}

				return meleeSprite;
			default:
				throw new IllegalArgumentException("Unsupported prayer: " + prayer);
		}
	}

	private static BufferedImage scaleSprite(final BufferedImage bufferedImage)
	{
		if (bufferedImage == null)
		{
			return null;
		}

		final double width = bufferedImage.getWidth(null);
		final double height = bufferedImage.getHeight(null);

		final double scalex = (DIMENSION.width - 5) / width;
		final double scaley = (DIMENSION.height - 5) / height;

		final double scale = Math.min(scalex, scaley);

		final int newWidth = (int) (width * scale);
		final int newHeight = (int) (height * scale);

		final BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

		final Graphics g = scaledImage.createGraphics();
		g.drawImage(bufferedImage, 0, 0, newWidth, newHeight, null);
		g.dispose();

		return scaledImage;
	}

}
