package com.example.demonicascension.compat;

import fuzs.fantasticwings.flight.FlightCapability;
import fuzs.fantasticwings.flight.apparatus.FlightApparatus;
import fuzs.fantasticwings.flight.apparatus.FlightApparatusImpl;
import fuzs.fantasticwings.init.ModRegistry;

import net.minecraft.world.entity.player.Player;

public class WingsIntegration {

    /** Grants or removes the evil wings based on demon form state. */
    public static void updateWings(Player player, boolean transformed) {
        FlightCapability flight = ModRegistry.FLIGHT_CAPABILITY.get(player);
        if (flight == null) {
            return;
        }

        if (transformed) {
            flight.setWings(FlightApparatusImpl.EVIL.holder());
        } else {
            flight.setWings(FlightApparatus.FlightApparatusHolder.empty());
        }
    }
}