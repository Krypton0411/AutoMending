package com.krypton.automending;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod(AutoMendingMod.MOD_ID)
public class AutoMendingMod {
    public static final String MOD_ID = "auto_mending";

    public AutoMendingMod() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.register(new AutoMendingHandler());
    }
}
