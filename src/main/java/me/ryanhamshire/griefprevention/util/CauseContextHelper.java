/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.griefprevention.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.common.SpongeImplHooks;

public class CauseContextHelper {

    public static User getEventUser(Event event) {
        final Cause cause = event.getCause();
        final EventContext context = event.getContext();
        // Don't attempt to set user for leaf decay
        if (context.containsKey(EventContextKeys.LEAVES_DECAY)) {
            return null;
        }

        User user = null;
        User fakePlayer = null;
        if (cause != null) {
            user = cause.first(User.class).orElse(null);
            if (user != null && user instanceof EntityPlayer && SpongeImplHooks.isFakePlayer((EntityPlayer) user)) {
                fakePlayer = user;
            }
        }

        // Only check notifier for fire spread
        if (context.containsKey(EventContextKeys.FIRE_SPREAD)) {
            return context.get(EventContextKeys.NOTIFIER).orElse(null);
        }

        if (user == null || fakePlayer != null) {
            // Always use owner for ticking TE's
            // See issue MinecraftPortCentral/GriefPrevention#610 for more information
            if (cause.containsType(TileEntity.class)) {
                user = context.get(EventContextKeys.OWNER)
                        .orElse(context.get(EventContextKeys.NOTIFIER)
                                .orElse(context.get(EventContextKeys.CREATOR)
                                        .orElse(null)));
            } else {
                user = context.get(EventContextKeys.NOTIFIER)
                        .orElse(context.get(EventContextKeys.OWNER)
                                .orElse(context.get(EventContextKeys.CREATOR)
                                        .orElse(null)));
            }
        }

        if (user == null) {
            // fall back to fakeplayer if we still don't have a user
            user = fakePlayer;
            if (event instanceof ExplosionEvent) {
                // Check igniter
                final Living living = context.get(EventContextKeys.IGNITER).orElse(null);
                if (living != null && living instanceof User) {
                    user = (User) living;
                }
            }
        }

        return user;
    }
}
