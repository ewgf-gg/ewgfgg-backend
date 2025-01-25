package org.ewgf.utils;

import org.ewgf.events.ReplayProcessingCompletedEvent;
import org.ewgf.events.StatisticsEventPublisher;
import org.ewgf.repositories.CharacterStatsRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
public class EventPublisherUtils {

    private final CharacterStatsRepository characterStatsRepository;
    private final StatisticsEventPublisher eventPublisher;

    private EventPublisherUtils(CharacterStatsRepository characterStatsRepository, StatisticsEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.characterStatsRepository = characterStatsRepository;
    }

    public void publishEventForAllGameVersions()
    {
        Optional<List<Integer>> gameVersions = characterStatsRepository.findAllGameVersions();
        gameVersions.ifPresent(integers -> eventPublisher.tryPublishEvent(
                new ReplayProcessingCompletedEvent(new HashSet<>(integers))));
    }
}
