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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Scheduler;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomLogger {

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyy_MM_dd");
    private final String logFolderPath = DataStore.dataLayerFolderPath + File.separator + "Logs";
    private final int secondsBetweenWrites = 300;

    // stringbuilder is not thread safe, stringbuffer is
    private StringBuffer queuedEntries = new StringBuffer();

    CustomLogger() {
        // ensure log folder exists
        File logFolder = new File(this.logFolderPath);
        logFolder.mkdirs();

        // delete any outdated log files immediately
        this.DeleteExpiredLogs();

        // unless disabled, schedule recurring tasks
        int daysToKeepLogs = GriefPrevention.getGlobalConfig().getConfig().logging.loggingDaysToKeep;
        if (daysToKeepLogs > 0) {
            Scheduler scheduler = Sponge.getGame().getScheduler();
            scheduler.createTaskBuilder().async().execute(new EntryWriter()).delay(this.secondsBetweenWrites, TimeUnit.SECONDS).interval(this
                    .secondsBetweenWrites, TimeUnit.SECONDS).submit(GriefPrevention.instance);
            scheduler.createTaskBuilder().async().execute(new ExpiredLogRemover()).delay(1, TimeUnit.DAYS).interval(1, TimeUnit.DAYS)
                    .submit(GriefPrevention
                            .instance);
        }
    }

    private static final Pattern inlineFormatterPattern = Pattern.compile("�.");

    void AddEntry(String entry, CustomLogEntryTypes entryType) {
        // if disabled, do nothing
        int daysToKeepLogs = GriefPrevention.getGlobalConfig().getConfig().logging.loggingDaysToKeep;
        if (daysToKeepLogs == 0) {
            return;
        }

        // if entry type is not enabled, do nothing
        if (!this.isEnabledType(entryType)) {
            return;
        }

        // otherwise write to the in-memory buffer, after removing formatters
        Matcher matcher = inlineFormatterPattern.matcher(entry);
        entry = matcher.replaceAll("");
        String timestamp = this.timestampFormat.format(new Date());
        this.queuedEntries.append(timestamp + " " + entry + "\n");
    }

    private boolean isEnabledType(CustomLogEntryTypes entryType) {
        if (entryType == CustomLogEntryTypes.Exception) {
            return true;
        }
        if (entryType == CustomLogEntryTypes.SocialActivity && !GriefPrevention.getGlobalConfig().getConfig().logging.loggingSocialActions) {
            return false;
        }
        if (entryType == CustomLogEntryTypes.SuspiciousActivity && !GriefPrevention.getGlobalConfig().getConfig().logging.loggingSuspiciousActivity) {
            return false;
        }
        if (entryType == CustomLogEntryTypes.AdminActivity && !GriefPrevention.getGlobalConfig().getConfig().logging.loggingAdminActivity) {
            return false;
        }
        if (entryType == CustomLogEntryTypes.Debug && !GriefPrevention.getGlobalConfig().getConfig().logging.loggingDebug) {
            return false;
        }

        return true;
    }

    void WriteEntries() {
        try {
            // if nothing to write, stop here
            if (this.queuedEntries.length() == 0) {
                return;
            }

            // determine filename based on date
            String filename = this.filenameFormat.format(new Date()) + ".log";
            String filepath = this.logFolderPath + File.separator + filename;
            File logFile = new File(filepath);

            // dump content
            Files.append(this.queuedEntries.toString(), logFile, Charset.forName("UTF-8"));

            // in case of a failure to write the above due to exception,
            // the unwritten entries will remain the buffer for the next write
            // to retry
            this.queuedEntries.setLength(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void DeleteExpiredLogs() {
        try {
            // get list of log files
            File logFolder = new File(this.logFolderPath);
            File[] files = logFolder.listFiles();

            // delete any created before x days ago
            int daysToKeepLogs = GriefPrevention.getGlobalConfig().getConfig().logging.loggingDaysToKeep;
            Calendar expirationBoundary = Calendar.getInstance();
            expirationBoundary.add(Calendar.DATE, -daysToKeepLogs);
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    continue; // skip any folders
                }

                String filename = file.getName().replace(".log", "");
                String[] dateParts = filename.split("_"); // format is
                // yyyy_MM_dd
                if (dateParts.length != 3) {
                    continue;
                }

                try {
                    int year = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]) - 1;
                    int day = Integer.parseInt(dateParts[2]);

                    Calendar filedate = Calendar.getInstance();
                    filedate.set(year, month, day);
                    if (filedate.before(expirationBoundary)) {
                        file.delete();
                    }
                } catch (NumberFormatException e) {
                    // throw this away - effectively ignoring any files without
                    // the correct filename format
                    GriefPrevention.AddLogEntry("Ignoring an unexpected file in the abridged logs folder: " + file.getName(),
                            CustomLogEntryTypes.Debug, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // transfers the internal buffer to a log file
    private class EntryWriter implements Runnable {

        @Override
        public void run() {
            WriteEntries();
        }
    }

    private class ExpiredLogRemover implements Runnable {

        @Override
        public void run() {
            DeleteExpiredLogs();
        }
    }
}
