package us.bpsm.textlineiterable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.io.Closer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LineIterableTest {

    static int count(Iterable<?> os) {
        int n = 0;
        for (Object ignored : os) {
            n++;
        }
        return n;
    }

    @Test
    public void emptyContainsNoLines() {
        assertFalse(new TextLineIterable("").iterator().hasNext());
    }

    @Test
    public void lastEolCanBeMissing() {
        assertEquals(1, count(new TextLineIterable("one\n")));
        assertEquals(1, count(new TextLineIterable("one")));
        assertEquals(1, count(new TextLineIterable("\n")));
        assertEquals("", new TextLineIterable("\n").iterator().next());
    }

    @Test
    public void variousEolStylesSupported() {
        Iterator<String> li = new TextLineIterable("one\rtwo\r\nthree\nfour").iterator();
        assertEquals("one", li.next());
        assertEquals("two", li.next());
        assertEquals("three", li.next());
        assertEquals("four", li.next());
        assertFalse(li.hasNext());
    }

    private File someFile() {
        return new File(getClass().getResource("example.txt").getFile());
    }

    @Test
    public void usageExample() throws IOException {
        final Charset utf8 = Charset.forName("UTF-8");
        final Closer closer = Closer.create();
        try {
            // The file contains ten lines of the form:
            //   1,one
            //   2,two
            //   ...
            //   10,ten
            final TextLineIterable tlit =
                    closer.register(new TextLineIterable(someFile(), utf8));

            // tlit is an Iterable: we can process lines using a for(:) loop
            int numberOfCharacters = 0;
            for (String line: tlit) {
                numberOfCharacters += line.length();
            }
            assertEquals(60, numberOfCharacters);

            // We can make use of FluentIterable's methods to do complex
            // processing on our lines without ever needing to have them all
            // in memory at once.

            // We're going to gather a list of the names of even numbers
            // whose names contain an odd number of characters.
            final List<String> names;
            names = tlit.transform(new Function<String,List<String>>() {
                // split each line into value and name (list of two strings)
                public List<String> apply(String s) {
                    return Arrays.asList(s.split(","));
                }
            }).filter(new Predicate<List<String>>() {
                // select only those with even value and odd name length
                public boolean apply(List<String> strings) {
                    int value = Integer.parseInt(strings.get(0));
                    String name = strings.get(1);
                    return value % 2 == 0 && name.length() % 2 != 0;
                }
            }).transform(new Function<List<String>,String>() {
                // extract only the name from each (name, value) pair.
                public String apply(List<String> strings) {
                    return strings.get(1);
                }
            }).toList();
            assertEquals(Arrays.asList("two", "six", "eight", "ten"), names);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

}
