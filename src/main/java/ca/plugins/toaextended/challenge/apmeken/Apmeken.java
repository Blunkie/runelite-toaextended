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
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.NullObjectID;
import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Apmeken implements PluginLifecycleComponent
{

	private static final String MESSAGE_SENSE_ROOF_SUPPORTS = "you sense an issue with the roof supports.";
	private static final String MESSAGE_SENSE_FUMES = "you sense some strange fumes coming from holes in the floor.";
	private static final String MESSAGE_FIX_ROOF_SUPPORTS = "you repair the damaged roof support.";
	private static final String MESSAGE_FIX_FUMES = "you neutralise the fumes coming from the hole.";
	private static final String MESSAGE_FAIL_ROOF_SUPPORTS = "damaged roof supports cause some debris to fall on you!";
	private static final String MESSAGE_FAIL_FUMES = "the fumes filling the room suddenly ignite!";

	private final EventBus eventBus;
	private final ToaExtendedConfig config;

	@Getter(AccessLevel.PACKAGE)
	private final List<NPC> volatileBaboons = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<NPC> baboons = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<TileObject> roofs = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private final List<TileObject> vents = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private GameObject statue;

	@Getter(AccessLevel.PACKAGE)
	private ApmekenSense sense = ApmekenSense.NONE;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.APMEKEN;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		reset();
	}

	private void reset()
	{
		volatileBaboons.clear();
		baboons.clear();
		sense = ApmekenSense.NONE;
		roofs.clear();
		vents.clear();
		statue = null;
	}

	@Subscribe
	public void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		switch (id)
		{
			case NpcID.VOLATILE_BABOON:
				volatileBaboons.add(npc);
			case NpcID.BABOON_BRAWLER:
			case NpcID.BABOON_BRAWLER_11712:
			case NpcID.BABOON_MAGE:
			case NpcID.BABOON_MAGE_11714:
			case NpcID.BABOON_THROWER:
			case NpcID.BABOON_THROWER_11713:
			case NpcID.BABOON_SHAMAN:
			case NpcID.CURSED_BABOON:
			case NpcID.BABOON_THRALL:
				baboons.add(npc);
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(final NpcDespawned event)
	{
		removeNpc(event.getNpc());
	}

	@Subscribe
	public void onActorDeath(final ActorDeath event)
	{
		final Actor actor = event.getActor();

		if (!(actor instanceof NPC))
		{
			return;
		}

		removeNpc((NPC) actor);
	}

	@Subscribe
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();

		final int id = gameObject.getId();

		if (id == ObjectID.ROOF_SUPPORT || id == ObjectID.ROOF_SUPPORT_45495)
		{
			roofs.add(gameObject);
		}
		else if (id == ObjectID.STATUE_45496)
		{
			statue = gameObject;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(final GameObjectDespawned event)
	{
		final GameObject gameObject = event.getGameObject();

		final int id = gameObject.getId();

		if (id == ObjectID.ROOF_SUPPORT || id == ObjectID.ROOF_SUPPORT_45495)
		{
			roofs.remove(gameObject);
		}
		else if (id == ObjectID.STATUE_45496)
		{
			statue = null;
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(final GroundObjectSpawned event)
	{
		final GroundObject groundObject = event.getGroundObject();

		if (groundObject.getId() == NullObjectID.NULL_45499)
		{
			vents.add(groundObject);
		}
	}

	@Subscribe
	public void onGroundObjectDespawned(final GroundObjectDespawned event)
	{
		final GroundObject groundObject = event.getGroundObject();

		if (groundObject.getId() == NullObjectID.NULL_45499)
		{
			vents.remove(groundObject);
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final String message = Text.standardize(event.getMessage());

		if (message.startsWith("challenge complete") || message.startsWith("you have died"))
		{
			sense = ApmekenSense.NONE;
			return;
		}

		switch (message)
		{
			case MESSAGE_FIX_ROOF_SUPPORTS:
			case MESSAGE_FIX_FUMES:
			case MESSAGE_FAIL_ROOF_SUPPORTS:
			case MESSAGE_FAIL_FUMES:
				sense = ApmekenSense.NONE;
				break;
			case MESSAGE_SENSE_FUMES:
				sense = ApmekenSense.VENTS;
				break;
			case MESSAGE_SENSE_ROOF_SUPPORTS:
				sense = ApmekenSense.ROOF;
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(final MenuEntryAdded event)
	{
		if (!config.apmekenRepairMenuEntry() || sense == ApmekenSense.ROOF)
		{
			return;
		}

		final MenuEntry menuEntry = event.getMenuEntry();

		if (!menuEntry.getOption().equals("Repair"))
		{
			return;
		}

		menuEntry.setDeprioritized(true);
	}

	private void removeNpc(final NPC npc)
	{
		final int id = npc.getId();

		switch (id)
		{
			case NpcID.VOLATILE_BABOON:
				volatileBaboons.remove(npc);
			case NpcID.BABOON_BRAWLER:
			case NpcID.BABOON_BRAWLER_11712:
			case NpcID.BABOON_MAGE:
			case NpcID.BABOON_MAGE_11714:
			case NpcID.BABOON_THROWER:
			case NpcID.BABOON_THROWER_11713:
			case NpcID.BABOON_SHAMAN:
			case NpcID.CURSED_BABOON:
			case NpcID.BABOON_THRALL:
				baboons.remove(npc);
				break;
			default:
				break;
		}
	}

	enum ApmekenSense
	{
		NONE,
		VENTS,
		ROOF
	}

}
