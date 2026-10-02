package com.krypton.automending;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Optional compatibility with the Curios API. Everything is done through reflection so the mod
 * works fine when Curios is not installed; any API-shape mismatch is silently ignored and Curios
 * slots are simply skipped.
 */
public class CuriosCompat {

    private static Boolean curiosLoaded = null;

    private static boolean isCuriosLoaded() {
        if (curiosLoaded == null) {
            try {
                Class.forName("top.theillusivec4.curios.api.CuriosApi");
                curiosLoaded = true;
            } catch (ClassNotFoundException e) {
                curiosLoaded = false;
            }
        }
        return curiosLoaded;
    }

    public static List<ItemStack> getCuriosItems(Player player) {
        List<ItemStack> result = new ArrayList<>();
        if (!isCuriosLoaded()) {
            return result;
        }

        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getInventory = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object lazyOptional = getInventory.invoke(null, player);
            Class<?> lazyOptionalClass = lazyOptional.getClass();

            Method isPresent = lazyOptionalClass.getMethod("isPresent");
            boolean present = (Boolean) isPresent.invoke(lazyOptional);
            if (!present) {
                return result;
            }

            Method orElse = lazyOptionalClass.getMethod("orElse", Object.class);
            Object handler = orElse.invoke(lazyOptional, (Object) null);
            if (handler == null) {
                return result;
            }

            Method getCurios = handler.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCurios.invoke(handler);

            for (Entry<String, Object> entry : curiosMap.entrySet()) {
                Object stacksHandler = entry.getValue();
                Method getStacks = stacksHandler.getClass().getMethod("getStacks");
                Object stackHandler = getStacks.invoke(stacksHandler);

                Method getSlots = stackHandler.getClass().getMethod("getSlots");
                Method getStackInSlot = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                int slots = (Integer) getSlots.invoke(stackHandler);

                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlot.invoke(stackHandler, i);
                    if (stack != null && !stack.isEmpty()) {
                        result.add(stack);
                    }
                }
            }
        } catch (Exception ignored) {
            // Curios API not available or changed; skip curios slots silently.
        }

        return result;
    }
}
