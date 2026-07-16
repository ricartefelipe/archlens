package io.archlens.domain;

public enum UserRole {
    ADMIN,
    ARCHITECT,
    VIEWER;

    public boolean canWrite() {
        return this == ADMIN || this == ARCHITECT;
    }
}
