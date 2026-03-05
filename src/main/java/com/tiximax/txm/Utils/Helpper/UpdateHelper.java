package com.tiximax.txm.Utils.Helpper;

import java.util.function.Consumer;

public class UpdateHelper {
 private UpdateHelper() {
        // Prevent instantiate
    }

    public static <T> void applyIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
    public static void applyIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value);
        }
    }

    public static void applyIfTrue(Boolean value, Runnable action) {
        if (Boolean.TRUE.equals(value)) {
            action.run();
        }
    }
}
