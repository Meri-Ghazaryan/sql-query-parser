package com.lightspeed.model;

import java.util.List;

public record Query(
        List<String> columns,
        List<Source> fromSources,
        List<Join> joins,
        List<WhereClause> whereClauses,
        List<String> groupByColumns,
        List<HavingClause> havingClauses,
        List<Sort> sortColumns,
        Integer limit,
        Integer offset) {

    @Override
    public String toString() {
        return "Query:\n" +
                "Columns: " + (this.columns() != null ? this.columns().toString() : "None") + "\n" +
                "From: " + (this.fromSources() != null ? this.fromSources().toString() : "None") + "\n" +
                "Joins: " + (this.joins() != null ? this.joins().toString() : "None") + "\n" +
                "Where Clauses: " + (this.whereClauses() != null ? this.whereClauses().toString() : "None") + "\n" +
                "Group By: " + (this.groupByColumns() != null ? this.groupByColumns().toString() : "None") + "\n" +
                "Having Clauses: " + (this.havingClauses() != null ? this.havingClauses().toString() : "None") + "\n" +
                "Order By: " + (this.sortColumns() != null ? this.sortColumns().toString() : "None") + "\n" +
                "Limit: " + (this.limit() != null ? this.limit() : "None") + "\n" +
                "Offset: " + (this.offset() != null ? this.offset() : "None");
    }
}
