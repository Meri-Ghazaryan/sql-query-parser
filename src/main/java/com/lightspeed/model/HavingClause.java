package com.lightspeed.model;

public record HavingClause(
        String function,
        String field,
        String operator,
        String value,
        String logicalOperator,
        String subquery
) {

    public HavingClause(String subquery) {
        this(null, null, null, null, null, subquery);
    }

    public HavingClause(String function, String field, String operator, String value, String logicalOperator) {
        this(function, field, operator, value, logicalOperator, null);
    }

    @Override
    public String toString() {
        if (this.subquery() != null) {
            return "HAVING Subquery: " + this.subquery();
        }
        return this.function() + "(" + this.field() + ") " + this.operator() + " " + this.value() +
                (this.logicalOperator() != null ? " " + this.logicalOperator() : "");
    }
}
