package org.ewgf.configuration;

import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;

@Service
public class MessageConsumptionManager {

    private final RabbitListenerEndpointRegistry endpointRegistry;

    public MessageConsumptionManager(RabbitListenerEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    public void pauseAllConsumers() {
        endpointRegistry.getListenerContainers()
                .forEach(MessageListenerContainer::stop);
    }

    public void resumeAllConsumers() {
        endpointRegistry.getListenerContainers()
                .forEach(MessageListenerContainer::start);
    }
}
