package cubicchunks.regionlib.util;

public interface CheckedBiConsumer<T, U, E extends Throwable> {
   void accept(T var1, U var2) throws E;
}
