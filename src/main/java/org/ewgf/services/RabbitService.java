package org.ewgf.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.ewgf.utils.DateTimeUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.ewgf.events.ReplayProcessingCompletedEvent;
import org.ewgf.models.*;
import org.ewgf.repositories.BattleRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.ewgf.utils.Constants.TIMESTAMP_HEADER;

@Slf4j
@Service
public class RabbitService {

    private final BattleProcessingService battleProcessingService;

    public RabbitService(BattleProcessingService battleProcessingService) {
        this.battleProcessingService = battleProcessingService;
    }

    @RabbitListener(queues = "#{rabbitMQConfig.queueName}",
            containerFactory = "rabbitListenerContainerFactory")
    public void receiveMessage(List<Battle> battles, @Header(TIMESTAMP_HEADER) String timestamp) {
        log.info("Received {} battles from RabbitMQ, timestamp: {}", battles.size(), timestamp);
        long start = System.currentTimeMillis();

        battleProcessingService.processBattlesAsync(battles);
        log.info("Total operation time: {} ms", System.currentTimeMillis() - start);
    }
}
