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
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.VarClientInt;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public abstract class PrayerWidgetOverlay extends Overlay
{

	private static final Dimension DIMENSION = new Dimension(10, 5);
	private static final int TICK_PIXEL_SIZE = 60;

	private static final int PRAYER_TAB_ID = 5;
	private static final int WIDGET_ID_PRAYER_GROUP = 541;
	private static final int WIDGET_ID_PRAYER_PROTECT_MAGIC = 21;
	private static final int WIDGET_ID_PRAYER_PROTECT_MISSILES = 22;
	private static final int WIDGET_ID_PRAYER_PROTECT_MELEE = 23;

	protected final Client client;
	protected final ToaExtendedConfig config;

	private Queue<AttackProjectile> projectileQueue;

	protected PrayerWidgetOverlay(final Client client, final ToaExtendedConfig config)
	{
		this.client = client;
		this.config = config;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	protected abstract @Nullable Queue<AttackProjectile> getAttackProjectileQueue();

	protected abstract @Nullable Prayer getNextPrayer();

	protected abstract long getLastTickTime();

	protected abstract boolean isPrayerWidgetEnabled();

	@Override
	public Dimension render(final Graphics2D graphics2D)
	{
		if (!isPrayerWidgetEnabled() || isPrayerTabClosed(client))
		{
			return null;
		}

		projectileQueue = getAttackProjectileQueue();

		final PrayerMode prayerMode = config.prayerMode();

		if (prayerMode == PrayerMode.ALL || prayerMode == PrayerMode.WIDGET)
		{
			renderPrayerWidget(graphics2D);
		}

		if (config.prayerDescendingBoxes())
		{
			renderDescendingBoxes(graphics2D);
		}

		return null;
	}

	private void renderPrayerWidget(final Graphics2D graphics2D)
	{
		final Prayer prayer = getNextPrayer();

		if (prayer == null)
		{
			return;
		}

		final Widget widget = getPrayerWidget(client, prayer);

		if (widget == null)
		{
			return;
		}

		final Rectangle rectangle = widget.getBounds();

		Color color = getPrayerColor(prayer);

		OverlayUtil.renderPolygon(graphics2D, ToaUtils.rectangleToPolygon(rectangle), color);

		if (projectileQueue == null || projectileQueue.isEmpty())
		{
			return;
		}

		final int ticks = projectileQueue.peek().getTicks();

		final String text = String.valueOf(ticks);

		color = ticks <= 1 ? Color.RED : Color.WHITE;

		final int x = (int) (rectangle.getX() + rectangle.getWidth() / 2) - 3;
		final int y = (int) (rectangle.getY() + rectangle.getHeight() / 2) + 6;

		ToaUtils.renderTextLocation(graphics2D,
			new Point(x, y), text, color, 16, Font.BOLD, true);
	}

	private void renderDescendingBoxes(final Graphics2D graphics2D)
	{
		if (projectileQueue == null || projectileQueue.isEmpty())
		{
			return;
		}

		final Map<Integer, AttackProjectile> tickPriorityMap = getTickPriorityMap(projectileQueue);

		for (final AttackProjectile projectile : projectileQueue)
		{
			final Widget widget = getPrayerWidget(client, projectile.getPrayer());

			if (widget == null)
			{
				continue;
			}

			final int tick = projectile.getTicks();

			final Color color = tick == 1 ? config.prayerBoxWarnColor() : config.prayerBoxColor();

			int baseX = (int) widget.getBounds().getX();
			baseX += widget.getBounds().getWidth() / 2;
			baseX -= DIMENSION.width / 2;

			int baseY = (int) widget.getBounds().getY() - tick * TICK_PIXEL_SIZE - DIMENSION.height;
			baseY += TICK_PIXEL_SIZE - ((getLastTickTime() + 600 - System.currentTimeMillis()) / 600.0 * TICK_PIXEL_SIZE);

			final Rectangle rectangle = new Rectangle(DIMENSION);

			rectangle.translate(baseX, baseY);

			if (projectile.getPrayer().equals(tickPriorityMap.get(projectile.getTicks()).getPrayer()))
			{
				ToaUtils.renderFilledPolygon(graphics2D, rectangle, color);
			}
			else if (config.prayerNonPriorityBoxes())
			{
				ToaUtils.renderOutlinePolygon(graphics2D, rectangle, color);
			}
		}
	}

	private static Map<Integer, AttackProjectile> getTickPriorityMap(final Iterable<AttackProjectile> queue)
	{
		final Map<Integer, AttackProjectile> map = new HashMap<>();

		queue.forEach(projectile ->
		{
			if (!map.containsKey(projectile.getTicks()))
			{
				map.put(projectile.getTicks(), projectile);
			}

			if (projectile.getPriority() < map.get(projectile.getTicks()).getPriority())
			{
				map.put(projectile.getTicks(), projectile);
			}
		});

		return map;
	}

	private static Color getPrayerColor(final Prayer prayer)
	{
		switch (prayer)
		{
			case PROTECT_FROM_MAGIC:
				return Color.BLUE;
			case PROTECT_FROM_MISSILES:
				return Color.GREEN;
			case PROTECT_FROM_MELEE:
				return Color.RED;
			default:
				return null;
		}
	}

	private static Widget getPrayerWidget(final Client client, final Prayer prayer)
	{
		final int widgetId;

		switch (prayer)
		{

			case PROTECT_FROM_MAGIC:
				widgetId = WIDGET_ID_PRAYER_PROTECT_MAGIC;
				break;
			case PROTECT_FROM_MISSILES:
				widgetId = WIDGET_ID_PRAYER_PROTECT_MISSILES;
				break;
			case PROTECT_FROM_MELEE:
				widgetId = WIDGET_ID_PRAYER_PROTECT_MELEE;
				break;
			default:
				return null;
		}

		return client.getWidget(WIDGET_ID_PRAYER_GROUP, widgetId);
	}

	private static boolean isPrayerTabClosed(final Client client)
	{
		return client.getVarcIntValue(VarClientInt.INVENTORY_TAB) != PRAYER_TAB_ID;
	}

}
