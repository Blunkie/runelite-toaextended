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
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.Renderable;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcChanged;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Het implements PluginLifecycleComponent
{

	private static final Set<Integer> PICKAXE_IDS = ImmutableSet.of(
		ItemID.BRONZE_PICKAXE,
		ItemID.IRON_PICKAXE,
		ItemID.STEEL_PICKAXE,
		ItemID.BLACK_PICKAXE,
		ItemID.MITHRIL_PICKAXE,
		ItemID.ADAMANT_PICKAXE,
		ItemID.RUNE_PICKAXE,
		ItemID.DRAGON_PICKAXE,
		ItemID.DRAGON_PICKAXE_12797,
		ItemID.DRAGON_PICKAXE_OR,
		ItemID.DRAGON_PICKAXE_OR_25376,
		ItemID.INFERNAL_PICKAXE,
		ItemID.INFERNAL_PICKAXE_OR,
		ItemID.INFERNAL_PICKAXE_UNCHARGED,
		ItemID.INFERNAL_PICKAXE_UNCHARGED_25369,
		ItemID.CRYSTAL_PICKAXE,
		ItemID.CRYSTAL_PICKAXE_23863,
		ItemID.CRYSTAL_PICKAXE_INACTIVE,
		ItemID._3RD_AGE_PICKAXE
	);

	private static final String CHALLENGE_START_MESSAGE = "Challenge started: Path of Het.";
	private static final String CHALLENGE_COMPLETE_MESSAGE = "Challenge complete: Path of Het.";

	private static final int BEAM_FIRE_RATE_TICKS = 9;
	private static final int GRAPHICS_OBJECT_ID_ORB_DESPAWN = 379;

	// at least one of these three is guaranteed to happen
	// there are others for each corner direction
	private static final Set<Integer> BEAM_GRAPHICS_OBJECT_IDS = ImmutableSet.of(
		2114, // horizontal
		2064, // vertical
		2120 // crash (into an object)
	);

	private final Function<NPC, HighlightedNpc> npcHighlighter = this::npcHighlight;
	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	private final EventBus eventBus;
	private final Client client;
	private final Hooks hooks;

	private final ToaExtendedConfig config;

	private final NpcOverlayService npcOverlayService;

	@Getter(AccessLevel.PACKAGE)
	private final List<GameObject> mirrors = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private GameObject casterStatue;

	private int nextFireTick = -1;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.HET;
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

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals(ToaExtendedConfig.CONFIG_GROUP))
		{
			return;
		}

		final String key = event.getKey();

		if (key.equals("miscDangerOutlineColor") || key.equals("miscDangerFillColor"))
		{
			npcOverlayService.rebuild();
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().equals(CHALLENGE_START_MESSAGE))
		{
			this.nextFireTick = client.getTickCount() + BEAM_FIRE_RATE_TICKS + 1;
		}
		else if (event.getMessage().equals(CHALLENGE_COMPLETE_MESSAGE))
		{
			this.nextFireTick = -1;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();

		final int id = gameObject.getId();

		if (id == ObjectID.CASTER_STATUE)
		{
			casterStatue = event.getGameObject();
		}
		else if (id == ObjectID.MIRROR_45455)
		{
			mirrors.add(gameObject);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(final GameObjectDespawned event)
	{
		final GameObject gameObject = event.getGameObject();

		final int id = gameObject.getId();

		if (id == ObjectID.CASTER_STATUE)
		{
			casterStatue = null;
		}
		else if (id == ObjectID.MIRROR_45455)
		{
			mirrors.remove(gameObject);
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(final GraphicsObjectCreated event)
	{
		if (BEAM_GRAPHICS_OBJECT_IDS.contains(event.getGraphicsObject().getId()))
		{
			this.nextFireTick = client.getTickCount() + BEAM_FIRE_RATE_TICKS;
		}
	}

	@Subscribe
	public void onNpcChanged(final NpcChanged event)
	{
		if (event.getOld().getId() == NpcID.HETS_SEAL_WEAKENED && event.getNpc().getId() == NpcID.HETS_SEAL_PROTECTED)
		{
			this.nextFireTick = client.getTickCount() + BEAM_FIRE_RATE_TICKS + 1;
		}
		else if (event.getOld().getId() == NpcID.HETS_SEAL_PROTECTED && event.getNpc().getId() == NpcID.HETS_SEAL_WEAKENED)
		{
			this.nextFireTick = -1;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(final MenuEntryAdded event)
	{
		final ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		final ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);

		if (inv == null || equip == null)
		{
			return;
		}

		final boolean hasPickaxe = containsAny(inv) || containsAny(equip);

		if (!hasPickaxe)
		{
			return;
		}

		if (config.hetDepositPickaxe())
		{
			final boolean statueSwap = isTakePickaxe(event.getMenuEntry());
			final boolean exitSwap = isExitRoom(event.getMenuEntry());

			if (statueSwap || exitSwap)
			{
				event.getMenuEntry().setDeprioritized(true);
			}
		}
	}

	private HighlightedNpc npcHighlight(final NPC npc)
	{
		if (npc.getId() == NpcID.ORB_OF_DARKNESS)
		{
			return HighlightedNpc.builder()
				.npc(npc)
				.tile(true)
				.borderWidth(1)
				.highlightColor(config.dangerOutlineColor())
				.fillColor(config.dangerFillColor())
				.render(n -> config.hetOrbOfDarknessTiles())
				.build();
		}

		return null;
	}

	private boolean shouldDraw(final Renderable renderable, final boolean drawingUI)
	{
		if (!config.hetHideOrbsOfDarkness())
		{
			return true;
		}

		if (renderable instanceof NPC)
		{
			return ((NPC) renderable).getId() != NpcID.ORB_OF_DARKNESS;
		}
		else if (renderable instanceof GraphicsObject)
		{
			return ((GraphicsObject) renderable).getId() != GRAPHICS_OBJECT_ID_ORB_DESPAWN;
		}

		return true;
	}

	private void reset()
	{
		casterStatue = null;
		mirrors.clear();
		nextFireTick = -1;
	}

	double getProgress()
	{
		return (double) (this.nextFireTick - client.getTickCount()) / BEAM_FIRE_RATE_TICKS;
	}

	private static boolean isTakePickaxe(final MenuEntry menuEntry)
	{
		return menuEntry.getOption().equals("Take-pickaxe") && menuEntry.getTarget().contains("Statue");
	}

	private static boolean isExitRoom(final MenuEntry menuEntry)
	{
		return menuEntry.getOption().contains("Enter") && menuEntry.getTarget().contains("Entry");
	}

	private static boolean containsAny(final ItemContainer itemContainer)
	{
		for (final Item item : itemContainer.getItems())
		{
			if (PICKAXE_IDS.contains(item.getId()))
			{
				return true;
			}
		}

		return false;
	}

}
