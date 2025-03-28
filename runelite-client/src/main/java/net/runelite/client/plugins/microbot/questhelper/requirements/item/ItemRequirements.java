/*
 *  * Copyright (c) 2020, Zoinkwiz <https://github.com/Zoinkwiz>
 *  * Copyright (c) 2019, Trevor <https://github.com/Trevor159>
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  *
 *  * 1. Redistributions of source code must retain the above copyright notice, this
 *  *    list of conditions and the following disclaimer.
 *  * 2. Redistributions in binary form must reproduce the above copyright notice,
 *  *    this list of conditions and the following disclaimer in the documentation
 *  *    and/or other materials provided with the distribution.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package net.runelite.client.plugins.microbot.questhelper.requirements.item;


import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.client.plugins.microbot.questhelper.QuestHelperConfig;
import net.runelite.client.plugins.microbot.questhelper.questhelpers.QuestUtil;
import net.runelite.client.plugins.microbot.questhelper.requirements.util.LogicType;
import net.runelite.client.plugins.microbot.questhelper.util.Utils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public class ItemRequirements extends ItemRequirement {
    @Getter
    ArrayList<ItemRequirement> itemRequirements = new ArrayList<>();

    @Getter
    LogicType logicType;

    public ItemRequirements(ItemRequirement... requirements) {
        this("", requirements);
    }

    public ItemRequirements(String name, ItemRequirement... itemRequirements) {
        super(name, itemRequirements[0].getId(), -1);

        assert (Utils.varargsNotNull(itemRequirements));

        this.itemRequirements.addAll(Arrays.asList(itemRequirements));
        this.logicType = LogicType.AND;
    }

    public ItemRequirements(LogicType logicType, String name, ItemRequirement... itemRequirements) {
        super(name, itemRequirements[0].getId(), -1);

        assert (Utils.varargsNotNull(itemRequirements));

        this.itemRequirements.addAll(Arrays.asList(itemRequirements));
        this.logicType = logicType;
    }

    public ItemRequirements(LogicType logicType, String name, List<ItemRequirement> itemRequirements) {
        super(name, itemRequirements.get(0).getId(), -1);

        assert (itemRequirements.stream().noneMatch(Objects::isNull));

        this.itemRequirements.addAll(itemRequirements);
        this.logicType = logicType;
    }

    public ItemRequirements(LogicType logicType, ItemRequirement... requirements) {
        this(logicType, "", requirements);
    }

    @Override
    public boolean isActualItem() {
        return LogicType.OR.test(getItemRequirements().stream(), item -> !item.getAllIds().contains(-1) && item.getQuantity() >= 0);
    }

    @Override
    public boolean check(Client client) {
        return check(client, false);
    }

    @Override
    public boolean check(Client client, boolean checkConsideringSlotRestrictions) {
        Predicate<ItemRequirement> predicate = r -> r.check(client, checkConsideringSlotRestrictions);
        int successes = (int) itemRequirements.stream().filter(Objects::nonNull).filter(predicate).count();
        hadItemLastCheck = logicType.compare(successes, itemRequirements.size());
        return hadItemLastCheck;
    }

    @Override
    public boolean check(Client client, boolean checkConsideringSlotRestrictions, List<Item> items) {
        Predicate<ItemRequirement> predicate = r -> r.check(client, checkConsideringSlotRestrictions, items);
        int successes = (int) itemRequirements.stream().filter(Objects::nonNull).filter(predicate).count();
        hadItemLastCheck = logicType.compare(successes, itemRequirements.size());
        return hadItemLastCheck;
    }

    @Override
    public Color getColor(Client client, QuestHelperConfig config) {
        return this.check(client, true) ? config.passColour() : config.failColour();
    }

    @Override
    public Color getColorConsideringBank(Client client, boolean checkConsideringSlotRestrictions,
                                         List<Item> bankItems, QuestHelperConfig config) {
        Color color = config.failColour();
        if (!this.isActualItem() && this.getItemRequirements() == null) {
            color = Color.GRAY;
        } else if (this.check(client, checkConsideringSlotRestrictions)) {
            color = config.passColour();
        }

        if (color == config.failColour() && bankItems != null) {
            if (check(client, false, bankItems)) {
                color = Color.WHITE;
            }
        }

        return color;
    }

    @Override
    public ItemRequirement copy() {
        ItemRequirements newItem = new ItemRequirements(getLogicType(), getName(), getItemRequirements());
        newItem.addAlternates(alternateItems);
        newItem.setDisplayItemId(getDisplayItemId());
        newItem.setExclusiveToOneItemType(exclusiveToOneItemType);
        newItem.setHighlightInInventory(highlightInInventory);
        newItem.setDisplayMatchedItemName(isDisplayMatchedItemName());
        newItem.setConditionToHide(getConditionToHide());
        newItem.setQuestBank(getQuestBank());
        newItem.setTooltip(getTooltip());
        newItem.logicType = logicType;
        newItem.additionalOptions = additionalOptions;

        return newItem;
    }

    @Override
    public List<Integer> getAllIds() {
        return itemRequirements.stream()
                .map(ItemRequirement::getAllIds)
                .flatMap(Collection::stream)
                .collect(QuestUtil.collectToArrayList());
    }

    @Override
    public ItemRequirement equipped() {
        ItemRequirements newItem = (ItemRequirements) copy();

        newItem.itemRequirements.forEach((itemRequirement -> itemRequirement.setEquip(true)));
        equip = true;
        return newItem;
    }


    @Override
    public void setEquip(boolean shouldEquip) {
        itemRequirements.forEach((itemRequirement -> itemRequirement.setEquip(true)));
        equip = shouldEquip;
    }

    @Override
    public boolean checkBank(Client client) {
        return logicType.test(getItemRequirements().stream(), item -> item.checkBank(client) || item.check(client, false));
    }
}
