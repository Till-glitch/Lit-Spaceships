package com.peaceman.alpha.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * Raumanzug-Rüstungsteil: Schützt vor tödlichem Vakuum und Erstickung im Weltraum.
 */
public class SpaceSuitItem extends ArmorItem {

    public SpaceSuitItem(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
