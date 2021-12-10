package com.hivemq.extensions.dns.exception;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public class ConfigurationException extends RuntimeException {

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(final @NotNull String message) {
        super(message);
    }

    public ConfigurationException(final @NotNull String message, final @NotNull Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(final @NotNull Throwable cause) {
        super(cause);
    }

    protected ConfigurationException(final @NotNull String message,
                                     final @NotNull Throwable cause,
                                     final boolean enableSuppression,
                                     final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
