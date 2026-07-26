package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
abstract class AsyncIOProvider<T> implements Runnable {
   private final ConcurrentLinkedQueue<Consumer<T>> callbacks = new ConcurrentLinkedQueue<>();
   volatile boolean finished = false;

   AsyncIOProvider() {
   }

   void addCallback(Consumer<T> callback) {
      this.callbacks.add(callback);
   }

   void removeCallback(Consumer<T> callback) {
      this.callbacks.remove(callback);
   }

   void runCallbacks() {
      T value = this.get();

      for (Consumer<T> callback : this.callbacks) {
         callback.accept(value);
      }

      this.callbacks.clear();
   }

   boolean isFinished() {
      return this.finished;
   }

   boolean hasCallbacks() {
      return !this.callbacks.isEmpty();
   }

   abstract void runSynchronousPart();

   @Nullable
   abstract T get();
}
