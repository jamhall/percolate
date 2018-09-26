package eu.jamiehall.percolate;

import com.googlecode.cqengine.query.parser.common.ParseResult;
import com.googlecode.cqengine.query.parser.common.QueryParser;
import com.googlecode.cqengine.query.parser.sql.support.DateMathParser;
import eu.jamiehall.percolate.FilterParser.StartContext;
import eu.jamiehall.percolate.exception.InvalidQueryException;
import eu.jamiehall.percolate.listener.FilterQueryListener;
import eu.jamiehall.percolate.support.ByteParser;
import eu.jamiehall.percolate.support.FallbackValueParser;
import eu.jamiehall.percolate.support.StringParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.antlr.v4.runtime.CharStreams.fromStream;

/**
 * A query parser for a given percolate expression
 *
 * @param <O> - the object type that will be filtered from the given query
 */
public class FilterQuery<O> extends QueryParser<O> {

    public FilterQuery(final Class<O> objectType) {
        super(objectType);
        registerValueParser(Date.class, new DateMathParser(new Date()));
        registerValueParser(String.class, new StringParser());
        registerValueParser(Long.class, new ByteParser());
        registerFallbackValueParser(new FallbackValueParser(new StringParser()));
    }

    /**
     * Parse the query
     *
     * @param query the query to be parsed
     * @return the parsed the result
     */
    @Override
    public ParseResult<O> parse(final String query) {
        try {
            if (query == null) {
                throw new InvalidQueryException("Query was null");
            }

            final CharStream             stream       = createStream(query);
            final FilterLexer            lexer        = createLexer(stream);
            final FilterParser           parser       = createParser(new CommonTokenStream(lexer));
            final StartContext           queryContext = parser.start();
            final ParseTreeWalker        walker       = new ParseTreeWalker();
            final FilterQueryListener<O> listener     = new FilterQueryListener<>(this);

            walker.walk(listener, queryContext);

            return new ParseResult<>(listener.getParsedQuery(), listener.getQueryOptions());
        } catch (InvalidQueryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidQueryException("Failed to parse query", exception);
        }
    }


    /**
     * Create a new lexer
     *
     * @param stream the character stream
     * @return the lexer
     */
    private FilterLexer createLexer(final CharStream stream) {
        final FilterLexer lexer = new FilterLexer(stream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER);
        return lexer;
    }

    /**
     * Create a new parser
     *
     * @param input the query input
     * @return the parser
     */
    private FilterParser createParser(final TokenStream input) {
        final FilterParser parser = new FilterParser(input);
        parser.removeErrorListeners();
        parser.addErrorListener(SYNTAX_ERROR_LISTENER);
        return parser;
    }

    /**
     * Create a new character stream a given query
     *
     * @param query the query
     * @return a character stream
     */
    private CharStream createStream(final String query) throws IOException {
        final InputStream stream = new ByteArrayInputStream(query.getBytes());
        return fromStream(stream);
    }
}
