package com.github.marcellokim.issuetracker.ui;

import java.util.Objects;

abstract class PlaceholderView {

    private final String viewId;

    protected PlaceholderView(String viewId) {
        this.viewId = Objects.requireNonNull(viewId, "viewId");
    }

    public String viewId() {
        return viewId;
    }
}
