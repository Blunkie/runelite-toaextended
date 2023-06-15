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
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Animation;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.Renderable;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AkkhaMemoryBlast implements PluginLifecycleComponent
{

	private static final int GAME_OBJECT_ID_FIRE_SYMBOL = 45868;
	private static final int GAME_OBJECT_ID_LIGHTNING_SYMBOL = 45869;
	private static final int GAME_OBJECT_ID_STAR_SYMBOL = 45870;
	private static final int GAME_OBJECT_ID_DEATH_SYMBOL = 45871;
	private static final int ANIMATION_ID_GLOW = 9759;

	private static final Set<Integer> GRAPHICS_OBJECT_IDS_ELEMENTS = ImmutableSet.of(2256, 2257, 2258, 2259);

	private static final String MESSAGE_GLOW = "sections of the room start to glow...";
	private static final String MESSAGE_START = "challenge started: akkha.";
	private static final String MESSAGE_FAILED = "your party failed";

	private final EventBus eventBus;
	private final Client client;
	private final Akkha akkha;

	private final Map<Integer, WorldPoint> idToWorldPoint = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final LinkedList<WorldPoint> worldPoints = new LinkedList<>();

	private int lastTick;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return akkha.isEnabled(config, raidState);
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
		idToWorldPoint.clear();
		worldPoints.clear();
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final String message = Text.standardize(event.getMessage());

		if (message.equals(MESSAGE_GLOW) || message.equals(MESSAGE_START) || message.endsWith(MESSAGE_FAILED))
		{
			reset();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();

		final Renderable renderable = gameObject.getRenderable();

		if (!(renderable instanceof DynamicObject))
		{
			return;
		}

		final Animation animation = ((DynamicObject) renderable).getAnimation();

		if (animation == null || animation.getId() != ANIMATION_ID_GLOW)
		{
			return;
		}

		final int id = gameObject.getId();

		if (id != GAME_OBJECT_ID_DEATH_SYMBOL &&
			id != GAME_OBJECT_ID_FIRE_SYMBOL &&
			id != GAME_OBJECT_ID_LIGHTNING_SYMBOL &&
			id != GAME_OBJECT_ID_STAR_SYMBOL)
		{
			return;
		}

		WorldPoint worldPoint = idToWorldPoint.get(id);

		if (worldPoint == null)
		{
			int dx = 0;
			int dy = 0;

			switch (id)
			{
				case GAME_OBJECT_ID_DEATH_SYMBOL:
					dx = dy = -1;
					break;
				case GAME_OBJECT_ID_FIRE_SYMBOL:
					dx = 1;
					dy = -1;
					break;
				case GAME_OBJECT_ID_LIGHTNING_SYMBOL:
					dx = -1;
					dy = 1;
					break;
				case GAME_OBJECT_ID_STAR_SYMBOL:
					dx = dy = 1;
					break;
			}

			worldPoint = gameObject.getWorldLocation().dx(dx).dy(dy);
			idToWorldPoint.put(id, worldPoint);
		}

		worldPoints.addLast(worldPoint);
	}

	@Subscribe
	public void onGraphicsObjectCreated(final GraphicsObjectCreated event)
	{
		if (worldPoints.isEmpty())
		{
			return;
		}
		if (!GRAPHICS_OBJECT_IDS_ELEMENTS.contains(event.getGraphicsObject().getId()))
		{
			return;
		}
		final int tick = client.getTickCount();
		if (tick == lastTick)
		{
			return;
		}
		lastTick = tick;
		worldPoints.removeFirst();
	}

}
