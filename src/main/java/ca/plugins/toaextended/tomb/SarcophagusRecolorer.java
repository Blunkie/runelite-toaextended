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
package ca.plugins.toaextended.tomb;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.event.Sarcophagus;
import ca.plugins.toaextended.module.PluginLifecycleComponent;
import ca.plugins.toaextended.util.RaidRoom;
import ca.plugins.toaextended.util.RaidState;
import ca.plugins.toaextended.util.ToaUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.MessageNode;
import net.runelite.api.Model;
import net.runelite.api.Renderable;
import net.runelite.api.WallObject;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SarcophagusRecolorer implements PluginLifecycleComponent
{

	private static final Pattern LOOT_PATTERN = Pattern.compile("^(?<prefix>.+ found something special: )(?<loot>.+)$");

	private static final int[] VARBIT_MULTILOC_IDS_CHEST = new int[]{
		14356, 14357, 14358, 14359, 14360, 14370, 14371, 14372
	};

	private static final int VARBIT_VALUE_CHEST_KEY = 2;
	private static final int VARBIT_ID_SARCOPHAGUS = 14373;
	private static final int WALL_OBJECT_ID_SARCOPHAGUS = 46221;
	private static final int ANIMATION_ID_BALL_OF_LIGHT = 9523;

	private final EventBus eventBus;
	private final Client client;
	private final ClientThread clientThread;
	private final Hooks hooks;
	private final ToaExtendedConfig config;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final Collection<WallObject> wallObjects = new ArrayList<>();
	@Nullable
	private int[] defaultFaceColors1;
	private boolean sarcophagusIsPurple;
	private boolean purpleIsMine = true;

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.TOMB;
	}

	@Override
	public void startUp()
	{
		clientThread.invokeLater(() ->
		{
			parseVarbits();
			recolor(wallObjects);
			eventBus.post(new Sarcophagus(sarcophagusIsPurple && purpleIsMine));
		});

		eventBus.register(this);
		hooks.registerRenderableDrawListener(drawListener);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		hooks.unregisterRenderableDrawListener(drawListener);
		wallObjects.clear();
		defaultFaceColors1 = null;
		sarcophagusIsPurple = false;
		purpleIsMine = true;
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals(ToaExtendedConfig.CONFIG_GROUP) || wallObjects.isEmpty())
		{
			return;
		}

		final String key = event.getKey();

		if (key.startsWith(ToaExtendedConfig.SARCOPHAGUS_PREFIX))
		{
			clientThread.invokeLater(() -> recolor(wallObjects));
		}
	}

	@Subscribe
	public void onWallObjectSpawned(final WallObjectSpawned event)
	{
		final WallObject wallObject = event.getWallObject();

		if (wallObject.getId() != WALL_OBJECT_ID_SARCOPHAGUS)
		{
			return;
		}

		if (defaultFaceColors1 == null)
		{
			initDefaultFaceColors1(wallObject);
		}

		recolor(wallObject);
		wallObjects.add(wallObject);
	}

	@Subscribe
	public void onWallObjectDespawned(final WallObjectDespawned event)
	{
		final WallObject wallObject = event.getWallObject();

		if (wallObject.getId() == WALL_OBJECT_ID_SARCOPHAGUS)
		{
			wallObjects.remove(wallObject);
		}
	}

	@Subscribe
	public void onChatMessage(final ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final Matcher matcher = LOOT_PATTERN.matcher(event.getMessage());

		if (!matcher.matches())
		{
			return;
		}

		final String loot = matcher.group("loot");

		if (loot == null || loot.isEmpty())
		{
			return;
		}

		if (config.sarcophagusHideLoot() && purpleIsMine)
		{
			final MessageNode messageNode = event.getMessageNode();

			final ChatLineBuffer chatLineBuffer = client.getChatLineMap().get(messageNode.getType().getType());

			if (chatLineBuffer != null)
			{
				chatLineBuffer.removeMessageNode(messageNode);
			}
		}

		if (config.sarcophagusRecolorLoot())
		{
			final Color color = getLootColor(Text.standardize(loot));
			clientThread.invokeLater(() -> recolor(wallObjects, color));
		}
	}

	private void parseVarbits()
	{
		sarcophagusIsPurple = client.getVarbitValue(VARBIT_ID_SARCOPHAGUS) % 2 != 0;
		purpleIsMine = true;

		for (final int varbitId : VARBIT_MULTILOC_IDS_CHEST)
		{
			if (client.getVarbitValue(varbitId) == VARBIT_VALUE_CHEST_KEY)
			{
				purpleIsMine = false;
				break;
			}
		}
	}

	private void initDefaultFaceColors1(final WallObject wallObject)
	{
		final Model model = wallObject.getRenderable1().getModel();

		if (model == null)
		{
			return;
		}

		defaultFaceColors1 = model.getFaceColors1().clone();
	}

	private void recolor(final Iterable<WallObject> wallObjects)
	{
		for (final WallObject wallObject : wallObjects)
		{
			recolor(wallObject);
		}
	}

	private void recolor(final Iterable<WallObject> wallObjects, final Color color)
	{
		for (final WallObject wallObject : wallObjects)
		{
			final Model model = wallObject.getRenderable1().getModel();

			if (model == null)
			{
				continue;
			}

			recolor(model.getFaceColors1(), color);
		}
	}

	private void recolor(final WallObject wallObject)
	{
		final Model model = wallObject.getRenderable1().getModel();

		if (model == null)
		{
			return;
		}

		final int[] faceColors1 = model.getFaceColors1();

		final Color color;

		if (sarcophagusIsPurple)
		{
			if (purpleIsMine)
			{
				if (!config.sarcophagusRecolorMyPurple())
				{
					resetFaceColors1(faceColors1);
					return;
				}

				color = config.sarcophagusMyPurpleRecolor();
			}
			else
			{
				if (!config.sarcophagusRecolorOtherPurple())
				{
					resetFaceColors1(faceColors1);
					return;
				}

				color = config.sarcophagusOtherPurpleRecolor();
			}
		}
		else
		{
			if (!config.sarcophagusRecolorWhite())
			{
				resetFaceColors1(faceColors1);
				return;
			}

			color = config.sarcophagusWhiteRecolor();
		}

		recolor(faceColors1, color);
	}

	private void recolor(final int[] faceColors1, final Color color)
	{
		Arrays.fill(faceColors1, ToaUtils.colorToRs2hsb(color));
	}

	private void resetFaceColors1(final int[] faceColors1)
	{
		if (defaultFaceColors1 == null)
		{
			log.error("defaultFaceColors1 was not initialized. Failed to reset faceColors1.");
			return;
		}

		System.arraycopy(defaultFaceColors1, 0, faceColors1, 0, faceColors1.length);
	}

	private boolean shouldDraw(final Renderable renderable, final boolean drawingUI)
	{
		if (!purpleIsMine || !config.sarcophagusHideLoot() || !(renderable instanceof DynamicObject))
		{
			return true;
		}

		final Animation animation = ((DynamicObject) renderable).getAnimation();

		return animation == null || animation.getId() != ANIMATION_ID_BALL_OF_LIGHT;
	}

	private Color getLootColor(final String loot)
	{
		switch (loot)
		{
			case "lightbearer":
				return config.sarcophagusLightbearerColor();
			case "osmumten's fang":
				return config.sarcophagusOsmumtensFangColor();
			case "elidinis' ward":
				return config.sarcophagusElidinisWardColor();
			case "masori mask":
				return config.sarcophagusMasoriMaskColor();
			case "masori body":
				return config.sarcophagusMasoriBodyColor();
			case "masori chaps":
				return config.sarcophagusMasoriChapsColor();
			case "tumeken's shadow (uncharged)":
				return config.sarcophagusTumekensShadowColor();
			default:
				log.error("Failed to get color for unsupported loot: {}", loot);
				return Color.BLACK;
		}
	}
}
