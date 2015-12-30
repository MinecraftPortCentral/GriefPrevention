/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
package me.ryanhamshire.GriefPrevention;

import com.google.common.io.Files;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//loads ignore data from file into a hash map
public class IgnoreLoaderThread extends Thread {

    private UUID playerToLoad;
    private ConcurrentHashMap<UUID, Boolean> destinationMap;

    public IgnoreLoaderThread(UUID playerToLoad, ConcurrentHashMap<UUID, Boolean> destinationMap) {
        this.playerToLoad = playerToLoad;
        this.destinationMap = destinationMap;
        this.setPriority(MIN_PRIORITY);
    }

    @Override
    public void run() {
        File ignoreFile = new File(DataStore.playerDataFolderPath + File.separator + this.playerToLoad + ".ignore");

        // if the file doesn't exist, there's nothing to do here
        if (!ignoreFile.exists())
            return;

        boolean needRetry = false;
        int retriesRemaining = 5;
        Exception latestException = null;
        do {
            try {
                needRetry = false;

                // read the file content and immediately close it
                List<String> lines = Files.readLines(ignoreFile, Charset.forName("UTF-8"));

                // each line is one ignore. asterisks indicate administrative
                // ignores
                for (String line : lines) {
                    boolean adminIgnore = false;
                    if (line.startsWith("*")) {
                        adminIgnore = true;
                        line = line.substring(1);
                    }
                    try {
                        UUID ignoredUUID = UUID.fromString(line);
                        this.destinationMap.put(ignoredUUID, adminIgnore);
                    } catch (IllegalArgumentException e) {
                    } // if a bad UUID, ignore the line
                }
            }

            // if there's any problem with the file's content, retry up to 5
            // times with 5 milliseconds between
            catch (Exception e) {
                latestException = e;
                needRetry = true;
                retriesRemaining--;
            }

            try {
                if (needRetry)
                    Thread.sleep(5);
            } catch (InterruptedException exception) {
            }

        } while (needRetry && retriesRemaining >= 0);

        // if last attempt failed, log information about the problem
        if (needRetry) {
            GriefPrevention.AddLogEntry("Retry attempts exhausted.  Unable to load ignore data for player \"" + playerToLoad.toString() + "\": "
                    + latestException.toString());
            latestException.printStackTrace();
        }
    }
}
