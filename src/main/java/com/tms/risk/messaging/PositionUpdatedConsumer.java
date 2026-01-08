package com.tms.risk.messaging;

import com.tms.common.config.kafka.KafkaTopics;
import com.tms.common.observability.logging.CorrelationIdFilter;
import com.tms.risk.service.RiskEvaluationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionUpdatedConsumer {

    private final RiskEvaluationService riskEvaluationService;

    @KafkaListener(
        topics = KafkaTopics.POSITIONS_UPDATED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @CircuitBreaker(name = "kafka-consumer", fallbackMethod = "handlePositionUpdatedFallback")
    @Retry(name = "kafka-consumer")
    public void handlePositionUpdated(ConsumerRecord<String, Map<String, Object>> record,
                                       Acknowledgment acknowledgment) {
        Map<String, Object> positionEvent = record.value();
        String positionId = (String) positionEvent.get("positionId");
        String correlationId = (String) positionEvent.get("correlationId");

        try {
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);
            log.info("Received position update for risk evaluation: positionId={}, partition={}, offset={}",
                positionId, record.partition(), record.offset());

            riskEvaluationService.evaluatePosition(positionEvent);

            acknowledgment.acknowledge();
            log.debug("Risk evaluation completed for position: positionId={}", positionId);

        } catch (Exception e) {
            log.error("Failed to evaluate risk for position: positionId={}", positionId, e);
            throw e;
        } finally {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        }
    }

    public void handlePositionUpdatedFallback(ConsumerRecord<String, Map<String, Object>> record,
                                               Acknowledgment acknowledgment,
                                               Exception e) {
        Map<String, Object> positionEvent = record.value();
        String positionId = (String) positionEvent.get("positionId");

        log.error("Circuit breaker open, skipping risk evaluation: positionId={}", positionId, e);
        acknowledgment.acknowledge();
    }
}
