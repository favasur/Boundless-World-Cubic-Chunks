package io.github.opencubicchunks.cubicchunks.core.server;

import net.minecraft.server.level.ServerPlayer;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.WatcherPlayerEntry
class WatcherPlayerEntry {
    final ServerPlayer player;

    WatcherPlayerEntry(ServerPlayer player) {
        this.player = player;
    }
}
