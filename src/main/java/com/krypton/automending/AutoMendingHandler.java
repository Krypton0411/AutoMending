package com.krypton.automending;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Auto Mending core handler.
 *
 * <p>Every SCAN_INTERVAL ticks, while the player has any experience points, it looks for the most
 * damaged item carrying the Mending enchantment across the player's inventory, armor, offhand,
 * Curios slots, shulker boxes and Sophisticated Backpacks, then spends up to XP_PER_TICK experience
 * points to restore DURABILITY_PER_XP durability per point.
 *
 * <p>Reconstructed from a Vineflower decompilation of the original release jar
 * {@code auto-mending-1.20.1-1.0.2.jar}; API names translated from SRG/intermediary to the
 * official (mojmap) names used by the 1.20.1 Forge dev environment.
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
    public void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END) {
            Player player = event.player;
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

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (this.isRepairableMendingItem(stack)) {
                float dmgPct = (float) stack.getDamageValue() / stack.getMaxDamage();
                if (dmgPct > bestDamagePct) {
                    bestDamagePct = dmgPct;
                    bestStack = stack;
                }
            }
        }

        for (ItemStack stack : inventory.armor) {
            if (this.isRepairableMendingItem(stack)) {
                float dmgPct = (float) stack.getDamageValue() / stack.getMaxDamage();
                if (dmgPct > bestDamagePct) {
                    bestDamagePct = dmgPct;
                    bestStack = stack;
                }
            }
        }

        ItemStack offhand = inventory.offhand.get(0);
        if (this.isRepairableMendingItem(offhand)) {
            float dmgPct = (float) offhand.getDamageValue() / offhand.getMaxDamage();
            if (dmgPct > bestDamagePct) {
                bestDamagePct = dmgPct;
                bestStack = offhand;
            }
        }

        List<ItemStack> curiosItems = CuriosCompat.getCuriosItems(player);
        for (ItemStack stack : curiosItems) {
            if (this.isRepairableMendingItem(stack)) {
                float dmgPct = (float) stack.getDamageValue() / stack.getMaxDamage();
                if (dmgPct > bestDamagePct) {
                    bestDamagePct = dmgPct;
                    bestStack = stack;
                }
            }
        }

        // Shulker boxes (and any container that stores items under BlockEntityTag) in the main inventory.
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack container = inventory.getItem(i);
            if (!container.isEmpty() && container.hasTag()) {
                for (ItemStack contained : this.getContainerItems(container)) {
                    if (this.isRepairableMendingItem(contained)) {
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
                if (this.isRepairableMendingItem(contained)) {
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
            if (this.isRepairableMendingItem(contained)) {
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
                if (this.isRepairableMendingItem(contained)) {
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

    private boolean isRepairableMendingItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.isDamageableItem()
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, stack) > 0;
    }

    /** Reads the Items list out of a container's BlockEntityTag (used for shulker boxes). */
    private List<ItemStack> getContainerItems(ItemStack container) {
        List<ItemStack> items = new ArrayList<>();
        CompoundTag tag = container.getTag();
        if (tag != null && tag.contains("BlockEntityTag")) {
            CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
            if (blockEntityTag.contains("Items")) {
                ListTag itemsTag = blockEntityTag.getList("Items", 10);
                for (int i = 0; i < itemsTag.size(); i++) {
                    CompoundTag itemTag = itemsTag.getCompound(i);
                    ItemStack stack = ItemStack.of(itemTag);
                    if (!stack.isEmpty()) {
                        items.add(stack);
                    }
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
     * <p>This is the vanilla 1.20.1 formula (identical to {@code Player.getXpNeededForNextLevel()}):
     * below 15: 7 + 2L; 15..29: 37 + (L-15)*5; 30+: 112 + (L-30)*9. The decompiled original
     * contained mutually exclusive branches and dead code left over from an older formula; the
     * vanilla formula is used here so the mod matches the game's own experience bar.
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
