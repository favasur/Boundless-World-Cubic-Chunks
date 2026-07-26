package cubicchunks.regionlib.util;

public interface CheckedFunction<T, R, E extends Throwable> {
   R apply(T var1) throws E;
}
