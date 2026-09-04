package com.example.demonicascension.client;

/**
 * Client-side mirror of the eclipse's server-wide active window. Rendering code
 * (fog, the eclipse disc, the sky-cancelling mixin) all read this directly rather
 * than each tracking their own copy of {@link com.example.demonicascension.network.EclipseStatePayload}.
 */
public class EclipseClientState {

    private static long activeUntilGameTime = 0L;

    public static void setActiveUntil(long gameTime) {
        activeUntilGameTime = gameTime;
    }

    public static boolean isActive(long currentGameTime) {
        return currentGameTime < activeUntilGameTime;
    }
}
