package eu.jamiehall.percolate.support;

import com.googlecode.cqengine.query.parser.common.ValueParser;

import java.lang.reflect.Method;

public class FallbackValueParser extends ValueParser<Object> {

    private final StringParser stringParser;

    public FallbackValueParser(final StringParser stringParser) {
        this.stringParser = stringParser;
    }

    @Override
    protected Object parse(final Class<?> valueType, String stringValue) {
        try {
            stringValue = stringParser.parse(String.class, stringValue);
            Method valueOf = valueType.getMethod("valueOf", String.class);
            return valueType.cast(valueOf.invoke(null, stringValue));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse value using a valueOf() method in class '" + valueType.getName() + "': " + stringValue, exception);
        }
    }
}
