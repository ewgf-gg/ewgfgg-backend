package org.ewgf.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class RabbitService {

    private static final Logger logger = LogManager.getLogger(RabbitService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BattleProcessingService battleProcessingService;

    private final JavaType battleListType = objectMapper.getTypeFactory()
            .constructCollectionType(ArrayList.class, Battle.class);

    public RabbitService(BattleProcessingService battleProcessingService) {
        this.battleProcessingService = battleProcessingService;
    }

    @RabbitListener(queues = "#{rabbitMQConfig.queueName}",
            containerFactory = "rabbitListenerContainerFactory")
    public void receiveMessage(String message, @Header(TIMESTAMP_HEADER) String unixTimestamp) throws Exception
    {
        logger.info("Received Battle Data from RabbitMQ, Timestamped: {}", unixTimestamp);

        long startTime = System.currentTimeMillis();
        List<Battle> battles = objectMapper.readValue(message, battleListType);
        battleProcessingService.processBattlesAsync(battles);

        logger.info("Total Operation Time: {} ms", System.currentTimeMillis() - startTime);
    }
}
