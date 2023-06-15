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
package ca.plugins.toaextended.boss.akkha;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Renderable;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;
import net.runelite.client.util.Text;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AkkhaFinalStand implements PluginLifecycleComponent
{

	private static final String MESSAGE_CHALLENGE = "challenge";
	private static final String MESSAGE_FAILED = "your party failed";

	private final EventBus eventBus;
	private final Hooks hooks;
	private final ToaExtendedConfig config;
	private final NpcOverlayService npcOverlayService;
	private final Akkha akkha;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final Function<NPC, HighlightedNpc> npcHighlighter = this::npcHighlight;

	@Getter(AccessLevel.PACKAGE)
	private final List<Actor> unstableOrbs = new ArrayList<>();

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return akkha.isEnabled(config, raidState);
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		npcOverlayService.registerHighlighter(npcHighlighter);
		hooks.registerRenderableDrawListener(drawListener);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		npcOverlayService.unregisterHighlighter(npcHighlighter);
		hooks.unregisterRenderableDrawListener(drawListener);
		reset();
	}

	private void reset()
	{
		unstableOrbs.clear();
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals(ToaExtendedConfig.CONFIG_GROUP))
		{
			return;
		}

		final String key = event.getKey();

		if (key.equals("miscDangerOutlineColor") || key.equals("miscDangerFillColor") ||
			key.equals("akkhaUnstableOrbTiles"))
		{
			npcOverlayService.rebuild();
		}
	}

	@Subscribe
	public void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		if (id == NpcID.UNSTABLE_ORB)
		{
			unstableOrbs.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(final NpcDespawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		if (id == NpcID.UNSTABLE_ORB)
		{
			unstableOrbs.remove(npc);
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final String standardized = Text.standardize(event.getMessage());

		if (standardized.startsWith(MESSAGE_CHALLENGE) || standardized.startsWith(MESSAGE_FAILED))
		{
			reset();
		}
	}

	private boolean shouldDraw(final Renderable renderable, final boolean drawingUI)
	{
		if (renderable instanceof NPC)
		{
			final NPC npc = (NPC) renderable;
			final int id = npc.getId();

			if (id == NpcID.UNSTABLE_ORB)
			{
				return !config.akkhaHideUnstableOrbs();
			}
		}

		return true;
	}

	private HighlightedNpc npcHighlight(final NPC npc)
	{
		final int id = npc.getId();

		if (id == NpcID.UNSTABLE_ORB)
		{
			final ToaExtendedConfig.Tile tile = config.akkhaUnstableOrbTiles();

			if (tile == ToaExtendedConfig.Tile.OFF)
			{
				return null;
			}

			final HighlightedNpc.HighlightedNpcBuilder builder = HighlightedNpc.builder()
				.npc(npc)
				.borderWidth(1)
				.highlightColor(config.dangerOutlineColor())
				.fillColor(config.dangerFillColor());

			if (tile == ToaExtendedConfig.Tile.TILE)
			{
				builder.tile(true);
			}
			else
			{
				builder.trueTile(true);
			}

			return builder.build();
		}

		return null;
	}

}
