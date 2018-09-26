package eu.jamiehall.percolate.listener;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.logical.And;
import com.googlecode.cqengine.query.logical.Or;
import com.googlecode.cqengine.query.option.OrderByOption;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.query.parser.common.QueryParser;
import eu.jamiehall.percolate.FilterBaseListener;
import eu.jamiehall.percolate.FilterLexer;
import eu.jamiehall.percolate.FilterParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

import static com.googlecode.cqengine.query.QueryFactory.*;
import static com.googlecode.cqengine.query.parser.common.ParserUtils.getParentContextOfType;
import static com.googlecode.cqengine.query.parser.common.ParserUtils.validateExpectedNumberOfChildQueries;

public class FilterQueryListener<O> extends FilterBaseListener {
    protected final QueryParser<O>                               queryParser;
    protected final Map<ParserRuleContext, Collection<Query<O>>> childQueries     = new HashMap<>();
    protected       OrderByOption<O>                             orderByOption    = null;
    protected       int                                          numQueriesParsed = 0;

    public FilterQueryListener(QueryParser<O> queryParser) {
        this.queryParser = queryParser;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void exitComparatorExpression(final ComparatorExpressionContext context) {
        final Attribute<O, Comparable> attribute = queryParser.getAttribute(context.attributeName(), Comparable.class);
        final Comparable               value     = queryParser.parseValue(attribute, context.queryParameter());
        switch (context.operator.getType()) {
            case FilterLexer.GT:
                addParsedQuery(context, greaterThan(attribute, value));
                break;
            case FilterLexer.GT_EQ:
                addParsedQuery(context, greaterThanOrEqualTo(attribute, value));
                break;
            case FilterLexer.LT:
                addParsedQuery(context, lessThan(attribute, value));
                break;
            case FilterLexer.LT_EQ:
                addParsedQuery(context, lessThanOrEqualTo(attribute, value));
                break;
            case FilterLexer.EQ:
                addParsedQuery(context, equal(attribute, value));
                break;
            case FilterLexer.NOT_EQ1:
            case FilterLexer.NOT_EQ2:
                addParsedQuery(context, not(equal(attribute, value)));
                break;
            default:
                throw new RuntimeException("Unexpected comparison operator");
        }
    }

    @Override
    public void exitBinaryExpression(final BinaryExpressionContext context) {
        switch (context.operator.getType()) {
            case FilterLexer.AND:
                addParsedQuery(context, new And<>(childQueries.get(context)));
                break;
            case FilterLexer.OR:
                addParsedQuery(context, new Or<>(childQueries.get(context)));
                break;
            default:
                throw new RuntimeException("Unexpected binary operator: " + context.operator.getText());

        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public void exitBetweenQuery(final BetweenQueryContext ctx) {
        final Attribute<O, Comparable>  attribute       = queryParser.getAttribute(ctx.attributeName(), Comparable.class);
        final List<? extends ParseTree> queryParameters = ctx.queryParameter();
        final Comparable                lowerValue      = queryParser.parseValue(attribute, queryParameters.get(0));
        final Comparable                upperValue      = queryParser.parseValue(attribute, queryParameters.get(1));
        addParsedQuery(ctx, between(attribute, lowerValue, upperValue));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void exitNotBetweenQuery(final NotBetweenQueryContext ctx) {
        final Attribute<O, Comparable>  attribute       = queryParser.getAttribute(ctx.attributeName(), Comparable.class);
        final List<? extends ParseTree> queryParameters = ctx.queryParameter();
        final Comparable                lowerValue      = queryParser.parseValue(attribute, queryParameters.get(0));
        final Comparable                upperValue      = queryParser.parseValue(attribute, queryParameters.get(1));
        addParsedQuery(ctx, not(between(attribute, lowerValue, upperValue)));
    }

    @Override
    public void exitInQuery(final InQueryContext ctx) {
        final Attribute<O, Object>      attribute       = queryParser.getAttribute(ctx.attributeName(), Object.class);
        final List<? extends ParseTree> queryParameters = ctx.queryParameter();
        final Collection<Object>        values          = new ArrayList<>(queryParameters.size());
        for (final ParseTree queryParameter : queryParameters) {
            final Object value = queryParser.parseValue(attribute, queryParameter);
            values.add(value);
        }
        addParsedQuery(ctx, in(attribute, values));
    }

    @Override
    public void exitNotInQuery(NotInQueryContext ctx) {
        final Attribute<O, Object>      attribute       = queryParser.getAttribute(ctx.attributeName(), Object.class);
        final List<? extends ParseTree> queryParameters = ctx.queryParameter();
        final Collection<Object>        values          = new ArrayList<>(queryParameters.size());
        for (ParseTree queryParameter : queryParameters) {
            final Object value = queryParser.parseValue(attribute, queryParameter);
            values.add(value);
        }
        addParsedQuery(ctx, not(in(attribute, values)));
    }


    @Override
    public void exitMatchesRegexQuery(MatchesRegexQueryContext ctx) {
        final Attribute<O, String> attribute = queryParser.getAttribute(ctx.attributeName(), String.class);
        String                     value     = queryParser.parseValue(attribute, ctx.stringQueryParameter());
        // Add escape character '\' before regex keywords
        final StringBuilder builder = new StringBuilder(value.length() * 2);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ("[](){}.*+?$^|#\\".indexOf(c) != -1) {
                builder.append("\\");
            }
            builder.append(c);
        }
        value = builder.toString();

        // Replace like keywords by regex keywords.
        value = value.replace("_", ".");
        value = value.replace("%", ".*");
        addParsedQuery(ctx, matchesRegex(attribute, value));
    }

    @Override
    public void exitHasQuery(HasQueryContext ctx) {
        final Attribute<O, Object> attribute = queryParser.getAttribute(ctx.attributeName(), Object.class);
        addParsedQuery(ctx, has(attribute));
    }

    @Override
    public void exitNotHasQuery(NotHasQueryContext ctx) {
        final Attribute<O, Object> attribute = queryParser.getAttribute(ctx.attributeName(), Object.class);
        addParsedQuery(ctx, not(has(attribute)));
    }

    /**
     * Adds the given query to a list of child queries which have not yet been wrapped in a parent query.
     */
    private void addParsedQuery(ParserRuleContext currentContext, Query<O> parsedQuery) {
        // Retrieve the possibly null parent query...
        final ParserRuleContext    parentContext    = getParentContextOfType(currentContext, BinaryExpressionContext.class);
        final Collection<Query<O>> childrenOfParent = childQueries.computeIfAbsent(parentContext, k -> new ArrayList<>());
        // parentContext will be null if this is root query
        childrenOfParent.add(parsedQuery);
        numQueriesParsed++;
    }

    /**
     * Can be called when parsing has finished, to retrieve the parsed query.
     */
    public Query<O> getParsedQuery() {
        final Collection<Query<O>> rootQuery = childQueries.get(null);
        if (rootQuery == null) {
            // There was no WHERE clause...
            return all(this.queryParser.getObjectType());
        }
        validateExpectedNumberOfChildQueries(1, rootQuery.size());
        return rootQuery.iterator().next();
    }

    /**
     * Can be called when parsing has finished, to retrieve the {@link QueryOptions}, which may include an
     * {@link OrderByOption} if found in the string query.
     *
     * @return The parsed {@link QueryOptions}
     */
    public QueryOptions getQueryOptions() {
        return noQueryOptions();
    }

}
