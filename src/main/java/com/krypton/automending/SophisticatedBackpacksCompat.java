package com.krypton.automending;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * Optional compatibility with Sophisticated Backpacks. Reads the backpack inventory handler through
 * the backpack wrapper capability via reflection, so the mod stays optional and any mismatch is
 * silently ignored.
 */
public class SophisticatedBackpacksCompat {

    private static Boolean backpacksLoaded = null;
    private static Object backpackWrapperCapability;

    private static boolean isBackpacksLoaded() {
        if (backpacksLoaded == null) {
            try {
                Class<?> capClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper");
                Field capField = capClass.getField("BACKPACK_WRAPPER_CAPABILITY");
                backpackWrapperCapability = capField.get(null);
                backpacksLoaded = true;
            } catch (Exception e) {
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
            Method getCapability = ItemStack.class.getMethod(
                    "getCapability",
                    Class.forName("net.minecraftforge.common.capabilities.Capability"),
                    Class.forName("net.minecraft.core.Direction"));
            Object lazyOptional = getCapability.invoke(container, backpackWrapperCapability, null);
            if (lazyOptional == null) {
                return result;
            }

            Method isPresent = lazyOptional.getClass().getMethod("isPresent");
            if (!(Boolean) isPresent.invoke(lazyOptional)) {
                return result;
            }

            Method orElse = lazyOptional.getClass().getMethod("orElse", Object.class);
            Object wrapper = orElse.invoke(lazyOptional, (Object) null);
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
