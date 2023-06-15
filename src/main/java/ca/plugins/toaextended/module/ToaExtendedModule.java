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
package ca.plugins.toaextended.module;

import ca.plugins.toaextended.ToaExtendedConfig;
import ca.plugins.toaextended.boss.akkha.Akkha;
import ca.plugins.toaextended.boss.akkha.AkkhaFinalStand;
import ca.plugins.toaextended.boss.akkha.AkkhaMemoryBlast;
import ca.plugins.toaextended.boss.akkha.AkkhaPrayerInfoboxOverlay;
import ca.plugins.toaextended.boss.akkha.AkkhaPrayerWidgetOverlay;
import ca.plugins.toaextended.boss.akkha.AkkhaSceneOverlay;
import ca.plugins.toaextended.boss.baba.Baba;
import ca.plugins.toaextended.boss.baba.BabaSceneOverlay;
import ca.plugins.toaextended.boss.kephri.Kephri;
import ca.plugins.toaextended.boss.kephri.KephriSceneOverlay;
import ca.plugins.toaextended.boss.warden.phase2.WardenP2;
import ca.plugins.toaextended.boss.warden.phase2.WardenP2PrayerInfoboxOverlay;
import ca.plugins.toaextended.boss.warden.phase2.WardenP2PrayerWidgetOverlay;
import ca.plugins.toaextended.boss.warden.phase2.WardenP2SceneOverlay;
import ca.plugins.toaextended.boss.warden.phase3.WardenP3;
import ca.plugins.toaextended.boss.warden.phase3.WardenP3PrayerInfoboxOverlay;
import ca.plugins.toaextended.boss.warden.phase3.WardenP3PrayerWidgetOverlay;
import ca.plugins.toaextended.boss.warden.phase3.WardenP3SceneOverlay;
import ca.plugins.toaextended.boss.zebak.Zebak;
import ca.plugins.toaextended.boss.zebak.ZebakPrayerInfoboxOverlay;
import ca.plugins.toaextended.boss.zebak.ZebakPrayerWidgetOverlay;
import ca.plugins.toaextended.boss.zebak.ZebakSceneOverlay;
import ca.plugins.toaextended.challenge.QuickProceedSwaps;
import ca.plugins.toaextended.challenge.apmeken.Apmeken;
import ca.plugins.toaextended.challenge.apmeken.ApmekenOverlay;
import ca.plugins.toaextended.challenge.het.Het;
import ca.plugins.toaextended.challenge.het.HetOverlay;
import ca.plugins.toaextended.challenge.scabaras.ScabarasAdditionPuzzle;
import ca.plugins.toaextended.challenge.scabaras.ScabarasLightPuzzle;
import ca.plugins.toaextended.challenge.scabaras.ScabarasMatchingPuzzle;
import ca.plugins.toaextended.challenge.scabaras.ScabarasObeliskPuzzle;
import ca.plugins.toaextended.challenge.scabaras.ScabarasOverlay;
import ca.plugins.toaextended.challenge.scabaras.ScabarasSequencePuzzle;
import ca.plugins.toaextended.hud.FadeDisabler;
import ca.plugins.toaextended.hud.HpOrbManager;
import ca.plugins.toaextended.nexus.PathLevelTracker;
import ca.plugins.toaextended.pointstracker.PointsTracker;
import ca.plugins.toaextended.tomb.SarcophagusRecolorer;
import ca.plugins.toaextended.util.RaidStateTracker;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class ToaExtendedModule extends AbstractModule
{

	@Override
	protected void configure()
	{
		final Multibinder<PluginLifecycleComponent> lifecycleComponents =
			Multibinder.newSetBinder(binder(), PluginLifecycleComponent.class);

		lifecycleComponents.addBinding().to(RaidStateTracker.class);
		lifecycleComponents.addBinding().to(PathLevelTracker.class);

		lifecycleComponents.addBinding().to(PointsTracker.class);

		lifecycleComponents.addBinding().to(Baba.class);
		lifecycleComponents.addBinding().to(BabaSceneOverlay.class);

		lifecycleComponents.addBinding().to(Kephri.class);
		lifecycleComponents.addBinding().to(KephriSceneOverlay.class);

		lifecycleComponents.addBinding().to(Akkha.class);
		lifecycleComponents.addBinding().to(AkkhaSceneOverlay.class);
		lifecycleComponents.addBinding().to(AkkhaPrayerWidgetOverlay.class);
		lifecycleComponents.addBinding().to(AkkhaPrayerInfoboxOverlay.class);
		lifecycleComponents.addBinding().to(AkkhaMemoryBlast.class);
		lifecycleComponents.addBinding().to(AkkhaFinalStand.class);

		lifecycleComponents.addBinding().to(Zebak.class);
		lifecycleComponents.addBinding().to(ZebakSceneOverlay.class);
		lifecycleComponents.addBinding().to(ZebakPrayerWidgetOverlay.class);
		lifecycleComponents.addBinding().to(ZebakPrayerInfoboxOverlay.class);

		lifecycleComponents.addBinding().to(WardenP2.class);
		lifecycleComponents.addBinding().to(WardenP2SceneOverlay.class);
		lifecycleComponents.addBinding().to(WardenP2PrayerWidgetOverlay.class);
		lifecycleComponents.addBinding().to(WardenP2PrayerInfoboxOverlay.class);

		lifecycleComponents.addBinding().to(WardenP3.class);
		lifecycleComponents.addBinding().to(WardenP3SceneOverlay.class);
		lifecycleComponents.addBinding().to(WardenP3PrayerWidgetOverlay.class);
		lifecycleComponents.addBinding().to(WardenP3PrayerInfoboxOverlay.class);

		lifecycleComponents.addBinding().to(Apmeken.class);
		lifecycleComponents.addBinding().to(ApmekenOverlay.class);

		lifecycleComponents.addBinding().to(ScabarasAdditionPuzzle.class);
		lifecycleComponents.addBinding().to(ScabarasLightPuzzle.class);
		lifecycleComponents.addBinding().to(ScabarasMatchingPuzzle.class);
		lifecycleComponents.addBinding().to(ScabarasObeliskPuzzle.class);
		lifecycleComponents.addBinding().to(ScabarasSequencePuzzle.class);
		lifecycleComponents.addBinding().to(ScabarasOverlay.class);

		lifecycleComponents.addBinding().to(Het.class);
		lifecycleComponents.addBinding().to(HetOverlay.class);

		lifecycleComponents.addBinding().to(FadeDisabler.class);
		lifecycleComponents.addBinding().to(HpOrbManager.class);
		lifecycleComponents.addBinding().to(QuickProceedSwaps.class);
		lifecycleComponents.addBinding().to(SarcophagusRecolorer.class);
	}

	@Provides
	@Singleton
	ToaExtendedConfig provideConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(ToaExtendedConfig.class);
	}

}
