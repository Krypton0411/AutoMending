package com.krypton.automending;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * Optional compatibility with Sophisticated Backpacks. Reads the backpack inventory handler via
 * reflection, so the mod stays optional and any mismatch is silently ignored.
 *
 * <p>NeoForge 1.21.1 adaptation: the old capability entry point
 * ({@code CapabilityBackpackWrapper.BACKPACK_WRAPPER_CAPABILITY}) was removed in the 1.21.x line;
 * the new entry point is the static factory
 * {@code BackpackWrapper.fromStack(ItemStack)} (returns {@code Noop.INSTANCE} for non-backpack
 * items, which exposes an empty inventory handler).
 */
public class SophisticatedBackpacksCompat {

    private static Boolean backpacksLoaded = null;

    private static boolean isBackpacksLoaded() {
        if (backpacksLoaded == null) {
            try {
                Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper");
                backpacksLoaded = true;
            } catch (ClassNotFoundException e) {
                backpacksLoaded = false;
            }
        }
        return backpacksLoaded;
    }

    public static List<ItemStack> getBackpackItems(ItemStack container) {
        List<ItemStack> result = new ArrayList<>();
        if (!isBackpacksLoaded() || container == null || container.isEmpty()) {
            return result;
        }

        try {
            Class<?> wrapperClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper");
            Method fromStack = wrapperClass.getMethod("fromStack", ItemStack.class);
            Object wrapper = fromStack.invoke(null, container);
            if (wrapper == null) {
                return result;
            }

            Method getInventoryHandler = wrapper.getClass().getMethod("getInventoryHandler");
            Object inventoryHandler = getInventoryHandler.invoke(wrapper);
            if (inventoryHandler == null) {
                return result;
            }

            Method getSlots = inventoryHandler.getClass().getMethod("getSlots");
            Method getStackInSlot = inventoryHandler.getClass().getMethod("getStackInSlot", int.class);
            int slots = (Integer) getSlots.invoke(inventoryHandler);

            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) getStackInSlot.invoke(inventoryHandler, i);
                if (stack != null && !stack.isEmpty()) {
                    result.add(stack);
                }
            }
        } catch (Exception ignored) {
            // Sophisticated Backpacks not available or changed; skip backpack slots silently.
        }

        return result;
    }
}
