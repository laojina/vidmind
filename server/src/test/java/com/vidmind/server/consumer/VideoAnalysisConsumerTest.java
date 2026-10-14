package com.vidmind.server.consumer;

import com.vidmind.server.dto.*;
import com.vidmind.server.service.*;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoAnalysisConsumerTest {
    private final AiService ai = mock(AiService.class);
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final AgentCheckpointService checkpoints = mock(AgentCheckpointService.class);
    private final RocketMQTemplate mq = mock(RocketMQTemplate.class);
    private final FailedAnalysisTaskService failures = mock(FailedAnalysisTaskService.class);
    private final MediaService media = mock(MediaService.class);
    private final TaskEventService events = mock(TaskEventService.class);
    private VideoAnalysisConsumer consumer;
    private final AnalysisTaskMsg msg = new AnalysisTaskMsg(7L, AnalysisTaskMsg.START_ANALYSIS,
            "media-7", "summarize", "GENERAL");

    @BeforeEach
    void setup() {
        consumer = new VideoAnalysisConsumer(ai, redisson, redis, checkpoints, mq, failures, media, events, "dead");
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(media.exists(7L)).thenReturn(true);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(1L);
    }

    @Test
    void lockContentionRequestsRedeliveryInsteadOfAcknowledging() {
        when(lock.tryLock()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> consumer.onMessage(msg));
        verifyNoInteractions(ai);
        verify(redis, never()).delete(anyCollection());
        verify(lock, never()).unlock();
    }

    @Test
    void persistedResultPreventsRepeatedModelWork() {
        var result = new AnalysisResult("done", List.of(), List.of(), List.of(), List.of());
        when(checkpoints.loadResult(7L, "summarize", AnalysisMode.GENERAL))
                .thenReturn(new AgentState("summarize", null, result, null, 1));
        consumer.onMessage(msg);
        verifyNoInteractions(ai);
        verify(values, never()).increment(anyString());
        verify(lock).unlock();
    }

    @Test
    void transientFailureKeepsActiveMarkerAndRequestsRetry() {
        doThrow(new IllegalStateException("provider unavailable")).when(ai).asyncAnalyze(7L, "summarize", AnalysisMode.GENERAL);
        assertThrows(IllegalStateException.class, () -> consumer.onMessage(msg));
        verify(redis, never()).delete(anyCollection());
        verifyNoInteractions(failures, mq);
        verify(lock).unlock();
    }

    @Test
    void exhaustedAttemptRoutesToDeadLetter() {
        when(values.increment(anyString())).thenReturn(3L);
        doThrow(new IllegalStateException("provider unavailable")).when(ai).asyncAnalyze(7L, "summarize", AnalysisMode.GENERAL);
        consumer.onMessage(msg);
        verify(failures).record(eq(msg), eq(3L), any());
        verify(mq).convertAndSend("dead", msg);
    }

    @Test
    void deadLetterHandoffRetryDoesNotExecuteModelAgain() {
        when(values.increment(anyString())).thenReturn(4L);
        doThrow(new IllegalStateException("broker unavailable")).when(mq).convertAndSend("dead", msg);
        assertThrows(IllegalStateException.class, () -> consumer.onMessage(msg));
        verifyNoInteractions(ai);
        verify(redis, never()).delete(anyCollection());
        verify(lock).unlock();
    }

    @Test
    void cleanupFailureStillReleasesLock() {
        doThrow(new IllegalStateException("redis unavailable")).when(redis).delete(anyCollection());
        assertThrows(IllegalStateException.class, () -> consumer.onMessage(msg));
        verify(lock).unlock();
    }
}
