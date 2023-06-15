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

import java.util.Comparator;
import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.Prayer;

public final class AttackProjectile implements Comparable<AttackProjectile>
{

	@Getter
	private final Prayer prayer;

	@Getter
	private final int priority;

	@Getter
	private int ticks;

	public AttackProjectile(final Prayer prayer, final int ticks, final int priority)
	{
		this.prayer = prayer;
		this.ticks = ticks;
		this.priority = priority;
	}

	public AttackProjectile(final Prayer prayer, final int ticks)
	{
		this(prayer, ticks, 0);
	}

	public void decrementTicks()
	{
		if (ticks > 0)
		{
			--ticks;
		}
	}

	public boolean isExpired()
	{
		return ticks == 0;
	}

	@Override
	public int compareTo(@NonNull final AttackProjectile projectile)
	{
		return Comparator.comparing(AttackProjectile::getTicks)
			.thenComparing(AttackProjectile::getPriority)
			.compare(this, projectile);
	}

}
