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
package me.ryanhamshire.griefprevention.configuration;

import me.ryanhamshire.griefprevention.configuration.category.ConfigCategory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ConfigSerializable
public class ClaimTemplateConfig extends ConfigCategory {

    @Setting(value = "name", comment = "The template name.")
    public String templateName;
    @Setting(value = "description", comment = "A description to help describe the template.")
    public String templateDescription = "";
    @Setting(value = ClaimStorageData.MAIN_OWNER_UUID, comment = "The owner uuid that created this template.")
    public UUID ownerUniqueId;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_CREATED, comment = "The date and time this template was created.")
    public String dateCreated = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_ACCESSORS, comment = "The accessors associated with subdivision.")
    public ArrayList<UUID> accessors = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BUILDERS, comment = "The builders associated with subdivision.")
    public ArrayList<UUID> builders = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_CONTAINERS, comment = "The containers associated with subdivision.")
    public ArrayList<UUID> containers = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_MANAGERS, comment = "The coowners associated with subdivision.")
    public ArrayList<UUID> coowners = new ArrayList<>();

    public ClaimTemplateConfig() {

    }

    public UUID getOwnerUniqueId() {
        return this.ownerUniqueId;
    }

    public String getDateCreated() {
        return this.dateCreated;
    }

    public List<UUID> getAccessors() {
        return this.accessors;
    }


    public List<UUID> getBuilders() {
        return this.builders;
    }


    public List<UUID> getContainers() {
        return this.containers;
    }

    public List<UUID> getCoowners() {
        return this.coowners;
    }

    public String getTemplateName() {
        return this.templateName;
    }

    public void setTemplateName(String template) {
        this.templateName = template;
    }

    public String getTemplateDescription() {
        return this.templateDescription;
    }

    public void setTemplateDescription(String description) {
        this.templateDescription = description;
    }
}