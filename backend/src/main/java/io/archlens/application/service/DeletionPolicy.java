package io.archlens.application.service;

import io.archlens.api.exception.BusinessException;

public final class DeletionPolicy {

    private DeletionPolicy() {
    }

    public static void ensureDeletionAllowed(boolean legalHold) {
        if (legalHold) {
            throw new BusinessException("Retenção legal ativa; exclusão da avaliação está bloqueada");
        }
    }
}
