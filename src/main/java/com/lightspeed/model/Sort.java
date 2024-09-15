package com.lightspeed.model;

public record Sort(
        String column,
        String direction) {

    @Override
    public String toString() {
        return this.column() + " " + this.direction();
    }
}
