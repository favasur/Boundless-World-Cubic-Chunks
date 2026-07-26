package io.github.opencubicchunks.cubicchunks.core.util.ticket;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList
public class TicketList {
    private final Cube cube;
    private int tickRefs = 0;
    @Nonnull
    private final List<ITicket> tickets = new ArrayList<>(1);

    public TicketList(@Nullable Cube cube) {
        this.cube = cube;
    }

    public void remove(ITicket ticket) {
        if (this.cube == null) {
            return;
        }
        if (this.tickets.remove(ticket) && ticket.shouldTick()) {
            this.tickRefs--;
            if (this.tickRefs < 0) {
                CubicChunks.LOGGER.warn("TicketList tickRefs went negative for cube {}", this.cube.getCoords());
                this.tickRefs = 0;
            }
        }
    }

    public void add(ITicket ticket) {
        if (this.cube == null) {
            return;
        }
        if (!this.tickets.contains(ticket)) {
            this.tickets.add(ticket);
            if (ticket.shouldTick()) {
                this.tickRefs++;
            }
        }
    }

    public boolean contains(ITicket ticket) {
        return this.tickets.contains(ticket);
    }

    public boolean shouldTick() {
        return this.tickRefs > 0;
    }

    public boolean canUnload() {
        return this.tickets.isEmpty();
    }

    public boolean anyMatch(Predicate<ITicket> predicate) {
        return this.tickets.stream().anyMatch(predicate);
    }
}
