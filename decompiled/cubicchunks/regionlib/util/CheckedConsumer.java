package cubicchunks.regionlib.util;

public interface CheckedConsumer<T, E extends Throwable> {
   void accept(T var1) throws E;
}
