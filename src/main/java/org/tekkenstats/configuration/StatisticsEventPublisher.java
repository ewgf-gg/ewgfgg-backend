package org.tekkenstats.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.tekkenstats.events.ReplayProcessingCompletedEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@Slf4j
public class StatisticsEventPublisher
{

    private final ApplicationEventPublisher eventPublisher;
    private final AtomicLong lastEventPublishTime;
    private final AtomicBoolean isPublishing;

    public StatisticsEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.lastEventPublishTime = new AtomicLong(0);
        this.isPublishing = new AtomicBoolean(false);
    }

    public void tryPublishEvent(ReplayProcessingCompletedEvent event)
    {
        long currentTime = System.currentTimeMillis();
        if (!isPublishing.compareAndSet(false, true)) {
            log.info("Another thread is currently publishing an event");
            return;
        }

        try {
                eventPublisher.publishEvent(event);
                lastEventPublishTime.set(currentTime);
                log.info("Successfully published statistics event.");

        } finally
        {
            isPublishing.set(false);
        }
    }


}
