package us.bpsm.textlineiterable;

import java.io.Closeable;
import java.util.Iterator;

/**
 * A CloseableIterator is an Iterator backed by some non-memory resource.
 * Implementations are expected to close the underlying resource
 * automatically if the the iterator's contents are fully consumed.
 * Partially consumed instances may be closed explicitly by calling
 * {@link #close()}. {@code close()} is idempotent. After {@code close()} is
 * called: {@link #hasNext()} will return false and {@link #next()} will
 * throw {@link java.util.NoSuchElementException}.
 */
public interface CloseableIterator<E> extends Iterator<E>, Closeable {
}
