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

package valoeghese.uniqueorigins.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayer.ConditionedOrigin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import valoeghese.uniqueorigins.Uniqueorigins;
import valoeghese.uniqueorigins.Uniqueorigins.UniquifierProperties;

@Mixin(value = OriginLayer.class, remap = false)
public class MixinOriginLayer {
	private <E extends Collection<Identifier>> E filter(E identifiers, UniquifierProperties properties, Collector<Identifier, ?, E> collector) {
		E other = identifiers.stream()
				.filter(id -> properties.getOriginCount(id) < properties.getMaxOriginCount())
				.collect(collector);

		if (other.isEmpty()) {
			return identifiers;
		} else {
			return other;
		}
	}
	@Inject(at= @At("RETURN"), method = "getRandomOrigins", cancellable = true)
	private void makeOriginsUniqueRandom(PlayerEntity entity, CallbackInfoReturnable<List<Identifier>> info) {
		if (!entity.getEntityWorld().isClient()) {
			UniquifierProperties properties = Uniqueorigins.getOriginData(entity);
			info.setReturnValue(
					filter(info.getReturnValue(), properties, Collectors.toList())
					);
		}
	}

	/*@Inject(at= @At("RETURN"), method = "getOrigins", cancellable = true)
	private void makeOriginsUniqueNormal(PlayerEntity entity, CallbackInfoReturnable<List<Identifier>> info) {
		if (!entity.getEntityWorld().isClient()) {
			UniquifierProperties properties = Uniqueorigins.getOriginData(entity);
			info.setReturnValue(
					filter(info.getReturnValue(), properties, Collectors.toList())
					);
		}
	}*/

	@Shadow
	private Identifier identifier;
	@Shadow
	private List<ConditionedOrigin> conditionedOrigins;
	@Shadow
	private int order;
	@Shadow
	private boolean enabled;
	@Shadow
	private List<Identifier> originsExcludedFromRandom;
	@Shadow
	private boolean doesRandomAllowUnchoosable;
	@Shadow
	private boolean autoChooseIfNoChoice;

	@Overwrite
	public void write(PacketByteBuf buffer) {
		// Old code
		buffer.writeString(identifier.toString());
		buffer.writeInt(order);
		buffer.writeBoolean(enabled);

		OriginLayer ol = (OriginLayer) (Object) this;

		// Replaced Code Starts Here ===================================
		MinecraftServer server = Uniqueorigins.sidedProxy.getCurrentServer(); // get the server
		UniquifierProperties properties = Uniqueorigins.getOriginData(server); // get the properties from the server

		List<ConditionedOrigin> toWrite = new ArrayList<>();

		// Iterate for each conditioned origin and filter out :b:ad ones
		for (ConditionedOrigin origin : conditionedOrigins) {
			toWrite.add(
					new ConditionedOrigin( // filter out the options
							((AccessorConditionedOrigin) origin).getCondition(),
							filter(origin.getOrigins(), properties, Collectors.toList()))
					);
		}

		buffer.writeInt(toWrite.size()); // write the new data
		toWrite.forEach(co -> co.write(buffer));
		// Replaced Code Ends Here ==========================================

		buffer.writeString(ol.getOrCreateTranslationKey());
		buffer.writeString(ol.getMissingOriginNameTranslationKey());
		buffer.writeString(ol.getMissingOriginDescriptionTranslationKey());
		buffer.writeBoolean(ol.isRandomAllowed());

		if(ol.isRandomAllowed()) {
			buffer.writeBoolean(doesRandomAllowUnchoosable);
			buffer.writeInt(originsExcludedFromRandom.size());
			originsExcludedFromRandom.forEach(buffer::writeIdentifier);
		}

		buffer.writeBoolean(ol.hasDefaultOrigin());

		if(ol.hasDefaultOrigin()) {
			buffer.writeIdentifier(ol.getDefaultOrigin());
		}
		buffer.writeBoolean(autoChooseIfNoChoice);
	}

}
