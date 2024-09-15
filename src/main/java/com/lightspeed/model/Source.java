package com.lightspeed.model;

public record Source(
        String tableName,
        String alias,
        boolean isSubquery
) {

    public Source(String tableName, String alias) {
        this(tableName, alias, false);
    }
}
