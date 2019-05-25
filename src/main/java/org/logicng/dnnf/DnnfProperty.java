package org.logicng.dnnf;

public enum DnnfProperty {
    DECOMPOSABLE("decomposable"),
    DETERMINISTIC("deterministic"),
    SMOOTH("smooth");

    private final String description;

    DnnfProperty(final String description) {
        this.description = description;
    }

    public String description() {
        return "DnnfProperty{description=" + this.description + "}";
    }
}
