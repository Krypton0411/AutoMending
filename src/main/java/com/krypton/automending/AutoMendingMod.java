package com.krypton.automending;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AutoMendingMod.MOD_ID)
public class AutoMendingMod {
    public static final String MOD_ID = "auto_mending";

    public AutoMendingMod() {
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.register(new AutoMendingHandler());
    }
}
