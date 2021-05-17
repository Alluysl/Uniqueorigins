/*
 * Uniqueorigins
 * Copyright (C) 2021 Valoeghese
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package valoeghese.uniqueorigins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import valoeghese.uniqueorigins.Uniqueorigins.UniquifierProperties;

public class UniqueState extends PersistentState implements UniquifierProperties {
	public UniqueState() {
		super("uniqueorigindata");
	}

	private CompoundTag impl = new CompoundTag();

	@Override
	public void fromTag(CompoundTag tag) {
		this.impl = tag;
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		return this.impl;
	}

	@Override
	public int getOriginCount(Identifier identifier) {
		String str = identifier.toString();

		if (this.impl.contains(str, INT)) {
			return this.impl.getInt(str);
		} else {
			return 0;
		}
	}

	@Override
	public void addOriginCount(PlayerEntity player, Identifier origin) {
		String str = origin.toString();
		int count = getOriginCount(origin) + 1;
		this.impl.putInt(str, count);

		if (count > getMaxOriginCount()) {
			this.impl.putInt("maxCount", count);
		}
	}

	@Override
	public int getMaxOriginCount() {
		if (this.impl.contains("maxCount", INT)) {
			return this.impl.getInt("maxCount");
		} else {
			return 0;
		}
	}

	private static final byte INT = 3;
}