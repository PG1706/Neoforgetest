package com.example.demonicascension.event;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.command.DemonCommand;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = DemonicAscension.MODID)
public class ModCommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DemonCommand.register(event.getDispatcher());
    }
}