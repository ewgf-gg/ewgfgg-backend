package org.ewgf.configuration;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

public class LoggingRetryListener implements RetryListener {
    private static final Logger logger = LoggerFactory.getLogger(LoggingRetryListener.class);

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable == null) {
            logger.info("Transaction succeeded after {} attempts", context.getRetryCount() + 1);
        } else {
            logger.error("Transaction failed after {} attempts. Final exception: {}",
                    context.getRetryCount() + 1,
                    extractDeadlockDetails(throwable));
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        logger.warn("Retry attempt #{} failed due to: {}",
                context.getRetryCount(),
                extractDeadlockDetails(throwable));
    }

    private String extractDeadlockDetails(Throwable throwable) {
        if (throwable instanceof PSQLException psqlException) {
            return String.format("SQL State: %s, Message: %s, Detail: %s",
                    psqlException.getSQLState(),
                    psqlException.getMessage(),
                    psqlException.getServerErrorMessage() != null ?
                            psqlException.getServerErrorMessage().getDetail() : "No detail");
        }
        return throwable.getMessage();
    }
}

