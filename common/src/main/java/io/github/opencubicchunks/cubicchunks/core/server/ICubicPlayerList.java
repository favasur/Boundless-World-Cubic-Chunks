package io.github.opencubicchunks.cubicchunks.core.server;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.ICubicPlayerList
// 1.21: interface opt-in. PlayerList doesn't yet implement it. Modders implementing this
// will get per-tick vertical view distance queries from CubeProviderServer.getVerticalViewDistance().
public interface ICubicPlayerList {
    int getVerticalViewDistance();

    int getRawVerticalViewDistance();

    void setVerticalViewDistance(int distance);
}
