package com.vidmind.server.service;

import com.vidmind.server.dto.AnalysisTaskMsg;
import com.vidmind.server.mapper.AgentCheckpointMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisTaskLifecycleServiceTest {
    private final AgentCheckpointMapper mapper = mock(AgentCheckpointMapper.class);
    private final AnalysisTaskLifecycleService service = new AnalysisTaskLifecycleService(mapper);
    private final Map<String, String> states = new HashMap<>();
    private final Map<String, String> identities = new HashMap<>();

    @BeforeEach
    void durableJournal() {
        doAnswer(call -> {
            states.putIfAbsent(call.getArgument(1), "QUEUED");
            identities.putIfAbsent(call.getArgument(1), call.getArgument(2));
            return null;
        }).when(mapper).registerTask(anyLong(), anyString(), anyString());
        when(mapper.findStage(anyLong(), anyString())).thenAnswer(call -> states.get(call.getArgument(1)));
        when(mapper.findPayload(anyLong(), anyString())).thenAnswer(call -> identities.get(call.getArgument(1)));
        when(mapper.transitionTask(anyLong(), anyString(), anyString(), anyString())).thenAnswer(call -> {
            String key = call.getArgument(1);
            return states.replace(key, call.getArgument(2), call.getArgument(3)) ? 1 : 0;
        });
    }

    @Test
    void completedGenerationCannotBeReopenedByRegistrationOrRedelivery() {
        var msg = message();
        service.register(msg);
        assertNotNull(msg.getTaskId());
        assertTrue(service.begin(msg));
        service.complete(msg);
        service.complete(msg);
        service.register(msg);
        assertFalse(service.begin(msg));
        assertThrows(IllegalStateException.class, () -> service.retry(msg));
    }

    @Test
    void transientFailureCanResumeAndDeadLetterIsTerminal() {
        var msg = message();
        service.register(msg);
        assertTrue(service.begin(msg));
        service.retry(msg);
        assertTrue(service.begin(msg));
        service.deadLetter(msg);
        assertFalse(service.begin(msg));
    }

    @Test
    void runningCheckpointCanResumeAfterCallerReacquiresDistributedLock() {
        var msg = message();
        service.register(msg);
        assertTrue(service.begin(msg));
        assertTrue(service.begin(msg));
        service.complete(msg);
        assertFalse(service.begin(msg));
    }

    @Test
    void distinctReplayGenerationDoesNotOverwritePriorTerminalRecord() {
        var old = message();
        service.register(old);
        service.begin(old);
        service.deadLetter(old);
        var replay = message();
        service.register(replay);
        assertNotEquals(old.getTaskId(), replay.getTaskId());
        assertTrue(service.begin(replay));
        assertFalse(service.begin(old));
    }

    @Test
    void mismatchedGoalAndMalformedIdAreRejected() {
        var msg = message();
        service.register(msg);
        msg.setUserGoal("different goal");
        assertThrows(IllegalArgumentException.class, () -> service.begin(msg));
        var invalid = message();
        invalid.setTaskId("not-a-uuid");
        assertThrows(IllegalArgumentException.class, () -> service.register(invalid));
        var negativeMedia = new AnalysisTaskMsg(-1L, AnalysisTaskMsg.START_ANALYSIS, "x", "goal");
        assertThrows(IllegalArgumentException.class, () -> service.register(negativeMedia));
    }

    @Test
    void legacyMessagesRemainCompatibleAndQueuedStateCannotJumpToCompleted() {
        assertTrue(service.begin(message()));
        var msg = message();
        service.register(msg);
        assertThrows(IllegalStateException.class, () -> service.complete(msg));
    }

    private AnalysisTaskMsg message() {
        return new AnalysisTaskMsg(7L, AnalysisTaskMsg.START_ANALYSIS, "media-7", "summarize", "GENERAL");
    }
}
