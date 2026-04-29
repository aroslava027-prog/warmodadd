package com.example.ftbwar;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod("ftbwar")
public class FTBWarMod {

    @Mod.EventBusSubscriber(modid = "ftbwar", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {

        @SubscribeEvent
        static void registerCommands(RegisterCommandsEvent event) {
            Commands.register(event.getDispatcher());
        }
    }
}
