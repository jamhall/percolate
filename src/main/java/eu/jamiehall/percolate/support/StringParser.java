package eu.jamiehall.percolate.support;

import com.googlecode.cqengine.query.parser.common.ValueParser;

public class StringParser extends ValueParser<String> {

    @Override
    public String parse(final Class<? extends String> valueType, final String stringValue) {
        return stringValue.replaceAll("\\\\'", "'")
                .replaceAll("^\'|\'$", "");
    }
}