package eu.jamiehall.percolate.support;

import com.googlecode.cqengine.query.parser.common.ValueParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlecode.cqengine.query.parser.sql.support.StringParser.stripQuotes;
import static java.lang.Double.parseDouble;
import static java.util.regex.Pattern.compile;

/**
 * Parses a human readable byte representation to bytes
 * e.g if given 1MB, it will return 1000000
 */
public class ByteParser extends ValueParser<Long> {

    private final static long   KB_FACTOR  = 1000;
    private final static long   KIB_FACTOR = 1024;
    private final static long   MB_FACTOR  = 1000 * KB_FACTOR;
    private final static long   MIB_FACTOR = 1024 * KIB_FACTOR;
    private final static long   GB_FACTOR  = 1000 * MB_FACTOR;
    private final static long   GIB_FACTOR = 1024 * MIB_FACTOR;
    private final static String PATTERN    = "^(\\d+\\.?\\d*)(KB|KIB|MB|MIB|GB|GIB)$";

    @Override
    protected Long parse(final Class<? extends Long> valueType, final String stringValue) {
        final String  value   = stripQuotes(stringValue);
        final Pattern pattern = compile(PATTERN);
        final Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            double ret = parseDouble(matcher.group(1));
            switch (matcher.group(2)) {
                case "GB":
                    return (long) ret * GB_FACTOR;
                case "GiB":
                    return (long) ret * GIB_FACTOR;
                case "MB":
                    return (long) ret * MB_FACTOR;
                case "MiB":
                    return (long) ret * MIB_FACTOR;
                case "KB":
                    return (long) ret * KB_FACTOR;
                case "KiB":
                    return (long) ret * KIB_FACTOR;
                default:
                    return (long) ret;
            }
        }
        return (long) parseDouble(value);
    }


}
