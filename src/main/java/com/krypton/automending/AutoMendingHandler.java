package com.krypton.automending;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Auto Mending core handler.
 *
 * <p>Every SCAN_INTERVAL ticks, while the player has any experience points, it looks for the most
 * damaged item carrying the Mending enchantment across the player's inventory, armor, offhand,
 * Curios slots, shulker boxes and Sophisticated Backpacks, then spends up to XP_PER_TICK experience
 * points to restore DURABILITY_PER_XP durability per point.
 *
 * <p>NeoForge 1.21.1 port of the original Forge 1.20.1 mod. API adaptations for 1.21.1:
 * Enchantments.MENDING is now a ResourceKey (looked up via the level registry), and item NBT access
 * was replaced by the Data Components API (shulker box contents live in DataComponents.CONTAINER).
 */
public class AutoMendingHandler {

    /** Max experience points consumed per repair tick. */
    private static final int XP_PER_TICK = 5;
    /** Durability restored per experience point. */
    private static final int DURABILITY_PER_XP = 2;
    /** How often (in ticks) the inventory scan runs. */
    private static final int SCAN_INTERVAL = 2;

    private int tickCounter = 0;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            this.tickCounter++;
            if (this.tickCounter % SCAN_INTERVAL == 0) {
                if (this.hasXpToSpend(player)) {
                    ItemStack targetStack = this.findMostDamagedMendingItem(player);
                    if (targetStack != null && !targetStack.isEmpty()) {
                        this.repairItem(player, targetStack);
                    }
                }
            }
        }
    }

    private boolean hasXpToSpend(Player player) {
        return player.experienceLevel > 0
                || player.totalExperience > 0
                || player.experienceProgress > 0.001F;
    }

    /**
     * Scans every candidate slot for the item with the highest damage percentage that is damageable
     * and enchanted with Mending. Priority: main inventory, armor, offhand, Curios slots, then
     * container content (shulker boxes) in the main inventory, backpacks in the main inventory,
     * the backpack in the offhand, and backpacks inside Curios slots.
     */
    private ItemStack findMostDamagedMendingItem(Player player) {
        ItemStack bestStack = null;
        float bestDamagePct = -1.0F;
        Inventory inventory = player.getInventory();
        Level level = player.level();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (this.isRepairableMendingItem(stack, level)) {
                float dmgPct = (float) stack.getDamageValue() / stack.getMaxDamage();
                if (dmgPct > bestDamagePct) {
                    bestDamagePct = dmgPct;
                    bestStack = stack;
                }
            }
        }

        for (ItemStack stack : inventory.armor) {
            if (this.isRepairableMendingItem(stack, level)) {
                float dmgPct = (float) stack.getDamageValue() / stack.getMaxDamage();
                if (dmgPct > bestDamagePct) {
                    bestDamagePct = dmgPct;
                    bestStack = stack;
                }
            }
        }

        ItemStack offhand = inventory.offhand.get(0);
        if (this.isRepairableMendingItem(offhand, level)) {
            float dmgPct = (float) offhand.getDamageValue() / offhand.getMaxDamage();
            if (dmgPct > bestDamagePct) {
                bestDamagePct = dmgPct;
                bestStack = offhand;
            }
        }

        List<ItemStack> curiosItems = CuriosCompat.getCuriosItems(player);
        for (ItemStack stack : curiosItems) {
            if (this.isRepairableMendingItem(stack, level)) {
                float dmgPct = (float) stack.getDamageValue() / stack.getMaxDamage();
                if (dmgPct > bestDamagePct) {
                    bestDamagePct = dmgPct;
                    bestStack = stack;
                }
            }
        }

        // Shulker boxes (any item with a CONTAINER data component) in the main inventory.
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack container = inventory.getItem(i);
            if (!container.isEmpty()) {
                for (ItemStack contained : this.getContainerItems(container)) {
                    if (this.isRepairableMendingItem(contained, level)) {
                        float dmgPct = (float) contained.getDamageValue() / contained.getMaxDamage();
                        if (dmgPct > bestDamagePct) {
                            bestDamagePct = dmgPct;
                            bestStack = contained;
                        }
                    }
                }
            }
        }

        // Sophisticated Backpacks in the main inventory.
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            for (ItemStack contained : SophisticatedBackpacksCompat.getBackpackItems(inventory.getItem(i))) {
                if (this.isRepairableMendingItem(contained, level)) {
                    float dmgPct = (float) contained.getDamageValue() / contained.getMaxDamage();
                    if (dmgPct > bestDamagePct) {
                        bestDamagePct = dmgPct;
                        bestStack = contained;
                    }
                }
            }
        }

        // Backpack in the offhand.
        for (ItemStack contained : SophisticatedBackpacksCompat.getBackpackItems(inventory.offhand.get(0))) {
            if (this.isRepairableMendingItem(contained, level)) {
                float dmgPct = (float) contained.getDamageValue() / contained.getMaxDamage();
                if (dmgPct > bestDamagePct) {
                    bestDamagePct = dmgPct;
                    bestStack = contained;
                }
            }
        }

        // Backpacks inside Curios slots.
        for (ItemStack curiosStack : curiosItems) {
            for (ItemStack contained : SophisticatedBackpacksCompat.getBackpackItems(curiosStack)) {
                if (this.isRepairableMendingItem(contained, level)) {
                    float dmgPct = (float) contained.getDamageValue() / contained.getMaxDamage();
                    if (dmgPct > bestDamagePct) {
                        bestDamagePct = dmgPct;
                        bestStack = contained;
                    }
                }
            }
        }

        return bestStack;
    }

    private boolean isRepairableMendingItem(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.isDamageableItem()) {
            return false;
        }
        // 1.21.1: Enchantments.MENDING is a ResourceKey; resolve it through the level's registry.
        Holder<Enchantment> mending = level.registryAccess().holderOrThrow(Enchantments.MENDING);
        return stack.getEnchantments().getLevel(mending) > 0;
    }

    /** Reads the contained items out of a container's CONTAINER data component (shulker boxes). */
    private List<ItemStack> getContainerItems(ItemStack container) {
        List<ItemStack> items = new ArrayList<>();
        ItemContainerContents contents = container.get(DataComponents.CONTAINER);
        if (contents != null) {
            for (ItemStack stack : contents.nonEmptyItems()) {
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }
        return items;
    }

    private void repairItem(Player player, ItemStack stack) {
        int xpCost = this.convertToXpPoints(player);
        if (xpCost > 0) {
            int xpToUse = Math.min(XP_PER_TICK, xpCost);
            int repairAmount = xpToUse * DURABILITY_PER_XP;
            int currentDmg = stack.getDamageValue();
            int toRepair = Math.min(repairAmount, currentDmg);
            if (toRepair > 0) {
                stack.setDamageValue(currentDmg - toRepair);
                this.removeXpPoints(player, xpToUse);
            }
        }
    }

    /** Converts the player's current level + progress into a total experience-point count. */
    private int convertToXpPoints(Player player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        int totalPoints = 0;
        for (int lvl = 0; lvl < level; lvl++) {
            totalPoints += this.getXpNeededForLevel(lvl);
        }
        return totalPoints + (int) (this.getXpNeededForLevel(level) * progress);
    }

    /**
     * Experience points needed to go from {@code level} to {@code level + 1}.
     *
     * <p>This is the vanilla formula (identical to {@code Player.getXpNeededForNextLevel()},
     * unchanged since 1.19.2): below 15: 7 + 2L; 15..29: 37 + (L-15)*5; 30+: 112 + (L-30)*9.
     */
    private int getXpNeededForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
    }

    /** Deducts experience points and pushes the updated experience bar to the client. */
    private void removeXpPoints(Player player, int points) {
        if (points > 0) {
            int xpForNextLevel = this.getXpNeededForLevel(player.experienceLevel);
            int progressInPoints = (int) (xpForNextLevel * player.experienceProgress);
            if (progressInPoints >= points) {
                player.experienceProgress = (float) (progressInPoints - points) / xpForNextLevel;
            } else {
                int remaining = points - progressInPoints;
                player.experienceProgress = 0.0F;
                while (remaining > 0 && player.experienceLevel > 0) {
                    player.experienceLevel--;
                    int xpForPrevLevel = this.getXpNeededForLevel(player.experienceLevel);
                    if (xpForPrevLevel >= remaining) {
                        player.experienceProgress = (float) (xpForPrevLevel - remaining) / xpForPrevLevel;
                        remaining = 0;
                    } else {
                        remaining -= xpForPrevLevel;
                    }
                }
            }

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetExperiencePacket(
                        player.experienceProgress, player.totalExperience, player.experienceLevel));
            }
        }
    }
}
