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
package ca.plugins.toaextended.challenge.het;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class HetOverlay extends Overlay implements PluginLifecycleComponent
{

	private final OverlayManager overlayManager;
	private final Client client;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private final Het het;
	private final ToaExtendedConfig config;

	@Inject
	public HetOverlay(final OverlayManager overlayManager, final Client client,
					  final ModelOutlineRenderer modelOutlineRenderer, final ToaExtendedConfig config,
					  final Het het)
	{
		this.overlayManager = overlayManager;
		this.client = client;
		this.modelOutlineRenderer = modelOutlineRenderer;
		this.config = config;
		this.het = het;
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return het.isEnabled(config, raidState);
	}

	@Override
	public void startUp()
	{
		overlayManager.add(this);
	}

	@Override
	public void shutDown()
	{
		overlayManager.removeIf(o -> o instanceof HetOverlay);
	}

	@Override
	public Dimension render(final Graphics2D graphics)
	{
		if (config.hetCasterStatueBeamTimer())
		{
			final GameObject casterStatue = het.getCasterStatue();

			if (casterStatue == null)
			{
				return null;
			}

			final Point canvasPoint = Perspective.localToCanvas(client, casterStatue.getLocalLocation(), client.getPlane());

			if (canvasPoint == null)
			{
				return null;
			}

			final double progress = het.getProgress();
			final Color c = progress == 0 ? Color.green : Color.cyan;
			if (progress < 0)
			{
				return null;
			}

			final ProgressPieComponent pie = new ProgressPieComponent();
			pie.setPosition(canvasPoint);
			pie.setProgress(1 - progress);
			pie.setBorderColor(c);
			pie.setFill(c);
			pie.render(graphics);
		}

		if (config.hetMirrorOutline())
		{
			final List<GameObject> mirrors = het.getMirrors();

			if (!mirrors.isEmpty())
			{
				for (final GameObject mirror : mirrors)
				{
					modelOutlineRenderer.drawOutline(mirror, 1,
						Color.MAGENTA, 0);
				}
			}
		}

		return null;
	}

}
