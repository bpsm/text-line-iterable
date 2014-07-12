package us.bpsm.textlineiterable;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * An easy way to iterate over all lines in a source of text while using
 * memory proportional to the length of the longest line.
 * <p>
 * For example:
 * <p>
 * {@code
 *  @Test
 *  public void usageExample() throws IOException {
 *      final Charset utf8 = Charset.forName("UTF-8");
 *      final Closer closer = Closer.create();
 *      try {
 *          // The file contains ten lines of the form:
 *          //   1,one
 *          //   2,two
 *          //   ...
 *          //   10,ten
 *          final TextLineIterable tlit =
 *                  closer.register(new TextLineIterable(someFile(), utf8));
 *
 *           // tlit is an Iterable: we can process lines using a for(:) loop
 *          int numberOfCharacters = 0;
 *          for (String line: tlit) {
 *              numberOfCharacters += line.length();
 *          }
 *          assertEquals(60, numberOfCharacters);
 *
 *          // We can make use of FluentIterable's methods to do complex
 *          // processing on our lines without ever needing to have them all
 *          // in memory at once.
 *
 *          // We're going to gather a list of even numbers whose names
 *          // contain an odd number of characters.
 *
 *          final List<String> names;
 *          names = tlit.transform(new Function<String,List<String>>() {
 *              // split each line into value and name (list of two strings)
 *              public List<String> apply(String s) {
 *                  return Arrays.asList(s.split(","));
 *              }
 *          }).filter(new Predicate<List<String>>() {
 *              // select only those with even value and odd name length
 *              public boolean apply(List<String> strings) {
 *                  int value = Integer.parseInt(strings.get(0));
 *                  String name = strings.get(1);
 *                  return value % 2 == 0 && name.length() % 2 != 0;
 *              }
 *          }).transform(new Function<List<String>,String>() {
 *              // extract only the name from each (name, value) pair.
 *              public String apply(List<String> strings) {
 *                  return strings.get(1);
 *              }
 *          }).toList();
 *          assertEquals(Arrays.asList("two", "six", "eight", "ten"), names);
 *      } catch (Throwable e) {
 *          throw closer.rethrow(e);
 *      } finally {
 *          closer.close();
 *      }
 *  }
 * }
 */
public class TextLineIterable extends FluentIterable<String> implements Closeable {
    final CharSource charSource;
    final Set<LineIterator> awaitingClose;


    public TextLineIterable(CharSource charSource) {
        this.charSource = charSource;
        this.awaitingClose = Sets.newHashSet();
    }

    public TextLineIterable(File textFile, Charset charset) {
        this(Files.asCharSource(textFile, charset));
    }

    public TextLineIterable(CharSequence text) {
        this(CharSource.wrap(text));
    }

    /**
     * Returns a ClosableIterator over the lines of this TextLineIterable.
     * The Strings returned by the iterator <strong>do not</strong> include
     * their terminating EOL sequence (\n, \r\n or \r as per
     * {@link java.io.BufferedReader#readLine()}).
     * @return a CloseableIterator producing 0 or more Strings.
     */
    @Override
    public synchronized CloseableIterator<String> iterator() {
        try {
            return new LineIterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This closes all Iterators created from this instance that have
     * not already been closed.
     * @throws IOException
     */
    @Override
    public synchronized void close() throws IOException {
        IOException failure = null;
        /* We iterate over a copy of awaitingClose because li.close() will
        remove elements from awaitingClose as a side-effect. */
        for (LineIterator li: ImmutableSet.copyOf(awaitingClose)) {
            try {
                li.close();
            } catch (IOException e) {
                failure = e;
            }
        }
        assert awaitingClose.isEmpty():
                "LineIterator.close() must de-register its receiver.";
        if (failure != null) {
            throw failure;
        }
    }

    static void closeQuietly(Closeable resource) {
        try {
            resource.close();
        } catch (IOException ignored) {
            ; // suppress
        }
    }

    class LineIterator implements CloseableIterator<String> {
        final BufferedReader reader;
        String peek = null;
        String msg = null;

        LineIterator() throws IOException {
            BufferedReader reader = charSource.openBufferedStream();
            try {
                peek = reader.readLine();
            } catch (IOException e) {
                assert peek == null;
                msg = e.getMessage();
                closeQuietly(reader);
                throw e;
            }
            this.reader = reader;
            awaitingClose.add(this);
        }



        @Override
        public void close() throws IOException {
            try {
                reader.close();
            } finally {
                synchronized (TextLineIterable.this) {
                    awaitingClose.remove(this);
                }
                peek = null;
            }
        }

        @Override
        public boolean hasNext() {
            return peek != null;
        }

        @Override
        public String next() {
            if (peek == null) {
                throw new NoSuchElementException(msg);
            }
            final String result = peek;
            try {
                peek = reader.readLine();
            } catch (IOException e) {
                msg = e.getMessage();
                closeQuietly(this);
                assert peek == null;
            }
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
