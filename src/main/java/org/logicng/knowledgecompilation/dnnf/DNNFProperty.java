package org.logicng.knowledgecompilation.dnnf;

public enum DNNFProperty {
    DECOMPOSABLE("decomposable"),
    DETERMINISTIC("deterministic"),
    SMOOTH("smooth");

    private final String description;

    DNNFProperty(final String description) {
        this.description = description;
    }

    public String description() {
        return "DnnfProperty{description=" + this.description + "}";
    }
}
