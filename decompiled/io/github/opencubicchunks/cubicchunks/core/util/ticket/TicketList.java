package io.github.opencubicchunks.cubicchunks.core.util.ticket;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TicketList {
   private final Cube cube;
   private int tickRefs = 0;
   @Nonnull
   private List<ITicket> tickets = Lists.newArrayListWithCapacity(1);

   public TicketList(@Nullable Cube cube) {
      this.cube = cube;
   }

   public void remove(ITicket ticket) {
      if (this.cube != null) {
         if (this.tickets.remove(ticket) && ticket.shouldTick()) {
            this.tickRefs--;

            assert this.tickRefs >= 0;

            if (this.tickRefs == 0) {
               ((ICubicWorldInternal.Server)this.cube.getWorld()).removeForcedCube(this.cube);
            }
         }
      }
   }

   public void add(ITicket ticket) {
      if (this.cube != null) {
         if (!this.tickets.contains(ticket)) {
            this.tickets.add(ticket);
            this.tickRefs = this.tickRefs + (ticket.shouldTick() ? 1 : 0);
            if (ticket.shouldTick()) {
               assert this.tickRefs > 0;

               if (this.tickRefs == 1) {
                  ((ICubicWorldInternal.Server)this.cube.getWorld()).addForcedCube(this.cube);
               }
            }
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
