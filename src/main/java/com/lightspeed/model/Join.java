package com.lightspeed.model;

public record Join(
        String type,
        Source source,
        String condition
) {

    @Override
    public String toString() {
        return this.type() + " JOIN " + this.source() + " ON " + this.condition();
    }
}
