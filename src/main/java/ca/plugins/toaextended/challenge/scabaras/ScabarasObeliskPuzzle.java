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
package ca.plugins.toaextended.challenge.scabaras;

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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ScabarasObeliskPuzzle implements PluginLifecycleComponent
{

	private static final int OBELISK_ID_INACTIVE = 11698;
	private static final int OBELISK_ID_ACTIVE = 11699;
	private static final int GRAPHICS_OBJECT_ID_FALLING_ROCKS = 317;
	private static final int GAME_OBJECT_ID_ENTRANCE = 45135;

	private static final int SCENE_X = 46;
	private static final int SCENE_Y = 51;

	private final EventBus eventBus;
	private final ToaExtendedConfig config;
	@Getter(AccessLevel.PACKAGE)
	private final List<LocalPoint> obeliskOrder = new ArrayList<>(6);
	@Getter(AccessLevel.PACKAGE)
	private final List<GraphicsObject> fallingRocks = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private int activeObelisks = 0;
	private GameObject northEntrance;
	private GameObject southEntrance;

	@Getter(AccessLevel.PACKAGE)
	private TileObject entranceTile;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.SCABARAS && config.scabarasObeliskPuzzle();
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		activeObelisks = 0;
		obeliskOrder.clear();
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onNpcChanged(final NpcChanged event)
	{
		if (event.getNpc().getId() == OBELISK_ID_ACTIVE)
		{
			final LocalPoint obeliskTile = event.getNpc().getLocalLocation();

			if (!obeliskOrder.contains(obeliskTile))
			{
				obeliskOrder.add(obeliskTile);
			}

			activeObelisks++;
		}
		else if (event.getNpc().getId() == OBELISK_ID_INACTIVE)
		{
			activeObelisks = 0;
		}
	}

	@Subscribe
	public void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();

		if (npc.getId() == OBELISK_ID_INACTIVE)
		{
			final LocalPoint localPoint = npc.getLocalLocation();

			entranceTile = localPoint.getSceneY() >= SCENE_Y ?
				localPoint.getSceneX() >= SCENE_X ? northEntrance : southEntrance :
				localPoint.getSceneX() >= SCENE_X ? southEntrance : northEntrance;
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event)
	{
		if (event.getMessage().startsWith("Your party failed to complete the challenge"))
		{
			activeObelisks = 0;
			obeliskOrder.clear();
		}
	}

	@Subscribe
	public void onGameTick(final GameTick event)
	{
		if (!fallingRocks.isEmpty())
		{
			fallingRocks.removeIf(GraphicsObject::finished);
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(final GraphicsObjectCreated event)
	{
		if (!config.scabarasObeliskFallingRocksHighlight())
		{
			return;
		}

		final GraphicsObject graphicsObject = event.getGraphicsObject();

		if (graphicsObject.getId() != GRAPHICS_OBJECT_ID_FALLING_ROCKS)
		{
			return;
		}

		fallingRocks.add(graphicsObject);
	}

	@Subscribe
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();

		if (gameObject.getId() == GAME_OBJECT_ID_ENTRANCE)
		{
			if (gameObject.getLocalLocation().getSceneY() >= SCENE_Y)
			{
				northEntrance = gameObject;
			}
			else
			{
				southEntrance = gameObject;
			}
		}
	}

}
