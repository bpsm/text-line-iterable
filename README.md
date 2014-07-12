# Text Line Iterable

An easy way to iterate over all lines in a source of text while using memory proportional to the length of the longest line.

## Installation

This is a Maven project with the following coordinates:

```xml
<dependency>
    <groupId>us.bpsm</groupId>
    <artifactId>text-line-iterator</artifactId>
    <version>1.0.0</version>
</dependency>
```

It is available through the OSS Sonatype Releases repository:

    https://oss.sonatype.org/content/repositories/releases

## License

Text Line Iterator is relased under the
[Eclipse Public License - v 1.0](http://www.eclipse.org/legal/epl-v10.html).

## Usage

```java
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
```

## Dependencies

This code requires a recent version of [Google Guava](https://code.google.com/p/guava-libraries/). The included pom.xml specifies version 17.0 though it may work with older versions as well.
