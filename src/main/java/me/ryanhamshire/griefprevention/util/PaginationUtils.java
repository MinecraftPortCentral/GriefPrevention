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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// A bad hack at attempting to track active pages
// Remove once SpongeAPI can support this feature
public class PaginationUtils {

    // The active page resets when a new command is entered by same player
    private final static Map<UUID, Integer> activePageMap = new HashMap<>();

    public static void updateActiveCommand(UUID uuid, String command, String args) {
        if (command.equalsIgnoreCase("callback") || command.equalsIgnoreCase("gpreload")) {
            // ignore
            return;
        }

        if (command.equalsIgnoreCase("page")) {
            final Integer activePage = activePageMap.get(uuid);
            if (activePage != null) {
                try {
                    final Integer page = Integer.parseInt(args);
                    activePageMap.put(uuid, page);
                } catch (Throwable t) {
                    
                }
            }
            return;
        }

        if (command.equalsIgnoreCase("pagination")) {
            final Integer activePage = activePageMap.get(uuid);
            if (activePage != null) {
                final boolean isNext = args.contains("next");
                if (isNext) {
                    activePageMap.put(uuid, activePage + 1);
                } else if (activePage != 1) {
                    activePageMap.put(uuid, activePage - 1);
                }
            }
            return;
        }

        resetActivePage(uuid);
    }

    public static void resetActivePage(UUID uuid) {
        activePageMap.put(uuid, 1);
    }

    public static Integer getActivePage(UUID uuid) {
        return activePageMap.get(uuid);
    }

    public static void removeActivePageData(UUID uuid) {
        activePageMap.remove(uuid);
    }
}
