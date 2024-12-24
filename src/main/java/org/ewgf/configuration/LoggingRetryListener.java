package org.ewgf.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

public class LoggingRetryListener implements RetryListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggingRetryListener.class);

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        // Invoked before the first attempt
        return true; // Proceed with the retry operation
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // Invoked after all retries are complete
        if (throwable == null) {
            logger.info("RETRY SUCCEEDED AFTER {} ATTEMPTS", context.getRetryCount());
        } else {
            logger.error("RETRY FAILED AFTER {} ATTEMPTS", context.getRetryCount());
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // Invoked after each failed attempt
        int retryCount = context.getRetryCount();
        logger.warn("RETRY ATTEMPT #{} DUE TO EXCEPTION: {}", retryCount, throwable.getMessage());
    }
}
