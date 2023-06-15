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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.GroundObject;
import net.runelite.api.TileObject;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ScabarasMatchingPuzzle implements PluginLifecycleComponent
{

	private static final Set<Integer> UP_TILE_IDS = ImmutableSet.of(
		45365, 45366, 45367, 45368, 45369, 45370, 45371, 45372, 45373
	);

	private static final Set<Integer> DOWN_TILE_IDS = ImmutableSet.of(
		45356, 45357, 45358, 45359, 45360, 45361, 45362, 45363, 45364
	);

	static final Map<Integer, Integer> upToDown = ImmutableMap.<Integer, Integer>builder()
		.put(45365, 45356)
		.put(45366, 45357)
		.put(45367, 45358)
		.put(45368, 45359)
		.put(45369, 45360)
		.put(45370, 45361)
		.put(45371, 45362)
		.put(45372, 45363)
		.put(45373, 45364)
		.build();

	private final EventBus eventBus;

	@Getter(AccessLevel.PACKAGE)
	private final Set<Integer> upTiles = new HashSet<>(5);
	@Getter(AccessLevel.PACKAGE)
	private final Map<TileObject, Integer> downTiles = new HashMap<>(5);

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.SCABARAS && config.scabarasMatchingPuzzle();
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
		upTiles.clear();
		downTiles.clear();
	}

	@Subscribe
	public void onGroundObjectSpawned(final GroundObjectSpawned event)
	{
		final GroundObject groundObject = event.getGroundObject();

		final int id = groundObject.getId();

		if (UP_TILE_IDS.contains(id))
		{
			if (upTiles.contains(id))
			{
				upTiles.remove(id);
			}
			else
			{
				upTiles.add(id);
			}
		}
		else if (DOWN_TILE_IDS.contains(id))
		{
			downTiles.put(groundObject, id);
		}
	}

	@Subscribe
	public void onGroundObjectDespawned(final GroundObjectDespawned event)
	{
		final GroundObject groundObject = event.getGroundObject();

		final int id = groundObject.getId();

		if (UP_TILE_IDS.contains(id))
		{
			upTiles.remove(id);
		}
		else if (DOWN_TILE_IDS.contains(id))
		{
			downTiles.remove(groundObject);
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event)
	{
		final String message = event.getMessage();

		if (message.startsWith("Your party failed to complete the challenge") ||
			message.startsWith("Challenge complete"))
		{
			upTiles.clear();
			downTiles.clear();
		}
	}
}
