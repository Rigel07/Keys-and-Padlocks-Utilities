package com.utilitymachines.lock;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Candado colocado: código, material y (opcionalmente) quién lo puso, para la alarma de la ganzúa. */
public record LockInfo(String code, LockTier tier, @Nullable UUID owner, String ownerName) {
    public LockInfo(String code, LockTier tier) {
        this(code, tier, null, "");
    }
}
