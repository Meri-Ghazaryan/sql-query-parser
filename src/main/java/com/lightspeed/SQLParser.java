package com.lightspeed;

import com.lightspeed.exception.SQLParseException;
import com.lightspeed.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lightspeed.SqlClauseConstants.*;
import static com.lightspeed.utils.ExtractUtil.extractAfter;
import static com.lightspeed.utils.ExtractUtil.extractBetween;

public class SQLParser {
    private static final String SUBQUERY_PATTERN_REGEX = "\\((SELECT.*)\\)\\s*AS\\s*(\\w+)";
    private static final String COMMA_AND_WHITESPACE_PATTERN_REGEX = ",\\s*";
    private static final String JOINS_PATTERN_REGEX = "(INNER|LEFT|RIGHT|FULL)\\s+JOIN\\s+(.*?)\\s+ON\\s+(.*?)\\s+(WHERE|GROUP BY|HAVING|ORDER BY|LIMIT|OFFSET|$)";
    private static final String SUBQUERIES_ENCLOSED_WITHIN_PARENTHESES_REGEX = "\\((SELECT.*)\\)";
    private static final String ALIAS_REGEX = "\\s+AS\\s+|\\s+";
    private static final String AND_CONDITION_REGEX = "\\s+AND\\s+";
    private static final String AGGREGATE_FUNCTIONS_REGEX = "(COUNT|SUM|AVG|MIN|MAX)\\((.*?)\\)\\s*(>|<|=|>=|<=)\\s*(\\d+)";
    private static final String WHITESPACE_REGEX = "\\s+";
    private static final String AND_OR_CONDITIONS_REGEX = "\\s+AND\\s+|\\s+OR\\s+";

    public static Query parseQuery(String query) {

        // Columns (include aggregate functions and subqueries)
        var columns = extractBetween(query, SELECT, FROM);
        if (columns == null || columns.isEmpty()) {
            throw new SQLParseException("Error: Missing columns in SELECT clause.");
        }

        // FROM clause (can include subqueries)
        var fromClause = extractBetween(query, FROM, "(WHERE|LEFT JOIN|RIGHT JOIN|INNER JOIN|GROUP BY|HAVING|ORDER BY|LIMIT|OFFSET|$)");
        if (fromClause == null || fromClause.isEmpty()) {
            throw new SQLParseException("Error: Missing FROM clause.");
        }

        // WHERE conditions (optional, with potential subqueries)
        var whereClause = extractBetween(query, WHERE, "(GROUP BY|HAVING|ORDER BY|LIMIT|OFFSET|$)");

        // GROUP BY clause (optional)
        var groupByClause = extractBetween(query, GROUP_BY, "(HAVING|ORDER BY|LIMIT|OFFSET|$)");

        // HAVING clause (optional, with potential subqueries)
        var havingClause = extractBetween(query, HAVING, "(ORDER BY|LIMIT|OFFSET|$)");

        // ORDER BY clause (optional)
        var orderByClause = extractBetween(query, ORDER_BY, "(LIMIT|OFFSET|$)");

        return new Query(
                parseColumns(columns),
                parseSources(fromClause),
                parseJoins(query),
                whereClause != null ? parseWhereClauses(whereClause) : null,
                groupByClause != null ? List.of(groupByClause.split(COMMA_AND_WHITESPACE_PATTERN_REGEX)) : Collections.emptyList(),
                havingClause != null ? parseHavingClauses(havingClause) : null,
                orderByClause != null ? parseSorts(orderByClause) : null,
                retrieveLimit(query),
                retrieveOffset(query)
        );
    }

    private static List<String> parseColumns(String columns) {
        var columnsList = new ArrayList<String>();
        var subqueryPattern = Pattern.compile(SUBQUERY_PATTERN_REGEX, Pattern.CASE_INSENSITIVE);
        var matcher = subqueryPattern.matcher(columns);

        if (matcher.find()) {
            var subquery = matcher.group(1);
            var alias = matcher.group(2);
            columnsList.add("SUBQUERY: " + alias);
            parseQuery(subquery);
        } else {
            columnsList.addAll(List.of(columns.split(COMMA_AND_WHITESPACE_PATTERN_REGEX)));
        }
        return columnsList;
    }

    private static List<Source> parseSources(String fromClause) {
        List<Source> sources = new ArrayList<>();
        var subqueryPattern = Pattern.compile(SUBQUERY_PATTERN_REGEX, Pattern.CASE_INSENSITIVE);
        var matcher = subqueryPattern.matcher(fromClause);

        if (matcher.find()) {
            var subquery = matcher.group(1);
            var alias = matcher.group(2);
            sources.add(new Source(subquery, alias, true));
            parseQuery(subquery);
        } else {
            var sourcesArray = fromClause.split(COMMA_AND_WHITESPACE_PATTERN_REGEX);
            sources = Arrays.stream(sourcesArray)
                    .map(sourceStr -> {
                        var parts = sourceStr.split(ALIAS_REGEX);
                        return parts.length == 2 ? new Source(parts[0], parts[1]) : new Source(parts[0], null);
                    })
                    .collect(Collectors.toList());
        }
        return sources;
    }

    private static List<Join> parseJoins(String query) {
        var joins = new ArrayList<Join>();
        var matcher = Pattern.compile(JOINS_PATTERN_REGEX, Pattern.CASE_INSENSITIVE).matcher(query);

        try {
            while (matcher.find()) {
                var type = matcher.group(1).toUpperCase();
                var sourceStr = matcher.group(2);
                var condition = matcher.group(3);
                var parts = sourceStr.split(ALIAS_REGEX);

                var source = parts.length == 2 ? new Source(parts[0], parts[1]) : new Source(parts[0], null);
                joins.add(new Join(type, source, condition));
            }
        } catch (Exception e) {
            throw new SQLParseException("Error: Unable to parse JOIN clause.");
        }
        return joins;
    }

    private static List<WhereClause> parseWhereClauses(String whereClause) {
        List<WhereClause> whereClauses = new ArrayList<>();
        try {
            var subqueryPattern = Pattern.compile(SUBQUERIES_ENCLOSED_WITHIN_PARENTHESES_REGEX, Pattern.CASE_INSENSITIVE);
            var matcher = subqueryPattern.matcher(whereClause);

            if (matcher.find()) {
                var subquery = matcher.group(1);
                whereClauses.add(new WhereClause("SUBQUERY: " + subquery));
                parseQuery(subquery);
            } else {
                whereClauses = Arrays.stream(whereClause.split(AND_CONDITION_REGEX))
                        .map(WhereClause::new)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new SQLParseException("Error: Unable to parse WHERE clause.");
        }
        return whereClauses;
    }

    private static List<HavingClause> parseHavingClauses(String havingClause) {
        List<HavingClause> havingClauses = new ArrayList<>();
        try {
            var subqueryPattern = Pattern.compile(SUBQUERIES_ENCLOSED_WITHIN_PARENTHESES_REGEX, Pattern.CASE_INSENSITIVE);
            var matcher = subqueryPattern.matcher(havingClause);

            if (matcher.find()) {
                var subquery = matcher.group(1);
                havingClauses.add(new HavingClause("SUBQUERY: " + subquery));
                parseQuery(subquery);
            } else {
                var pattern = Pattern.compile(AGGREGATE_FUNCTIONS_REGEX);
                havingClauses = Arrays.stream(havingClause.split(AND_OR_CONDITIONS_REGEX))
                        .map(condition -> {
                            Matcher matcher2 = pattern.matcher(condition);
                            if (matcher2.find()) {
                                return craeteHavingClause(havingClause, matcher2);
                            } else {
                                throw new SQLParseException("Error: Invalid HAVING clause format for condition: " + condition);
                            }
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new SQLParseException("Error: Unable to parse HAVING clause.");
        }
        return havingClauses;
    }

    private static List<Sort> parseSorts(String orderByClause) {
        try {
            return Arrays.stream(orderByClause.split(COMMA_AND_WHITESPACE_PATTERN_REGEX))
                    .map(sortStr -> {
                        var parts = sortStr.split(WHITESPACE_REGEX);
                        return new Sort(parts[0], parts.length == 2 ? parts[1].toUpperCase() : "ASC");
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new SQLParseException("Error: Unable to parse ORDER BY clause.");
        }
    }

    private static Integer retrieveLimit(String query) {

        // LIMIT clause (optional, but usually required for certain queries)
        var limitClause = extractAfter(query, LIMIT);
        if (limitClause != null) {
            try {
                return Integer.parseInt(limitClause.trim());
            } catch (NumberFormatException e) {
                throw new SQLParseException("Error: Invalid number format for LIMIT value.");
            }
        }
        return null;
    }

    private static Integer retrieveOffset(String query) {

        // OFFSET clause (optional)
        var offsetClause = extractAfter(query, OFFSET);
        if (offsetClause != null) {
            try {
                return Integer.parseInt(offsetClause.trim());
            } catch (NumberFormatException e) {
                throw new SQLParseException("Error: Invalid number format for OFFSET value.");
            }
        }
        return null;
    }

    private static HavingClause craeteHavingClause(String havingClause, Matcher matcher) {
        var function = matcher.group(1); // COUNT, SUM, etc.
        var field = matcher.group(2); // field inside parentheses (could be * for COUNT)
        var operator = matcher.group(3); // comparison operator
        var value = matcher.group(4); // value for comparison
        var logicalOperator = havingClause.contains("AND") ? "AND" : havingClause.contains("OR") ? "OR" : null;

        return new HavingClause(
                function,
                field.isEmpty() ? "*" : field,
                operator,
                value,
                logicalOperator
        );
    }
}
