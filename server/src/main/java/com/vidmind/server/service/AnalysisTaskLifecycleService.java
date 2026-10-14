package com.vidmind.server.service;

import com.vidmind.server.dto.AnalysisMode;
import com.vidmind.server.dto.AnalysisTaskMsg;
import com.vidmind.server.mapper.AgentCheckpointMapper;
import com.vidmind.server.utils.AnalysisTaskKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Durable, generation-scoped task lifecycle. Consumer calls require the task's Redisson lock. */
@Service
public class AnalysisTaskLifecycleService {
    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskLifecycleService.class);
    public enum State { QUEUED, RUNNING, RETRYING, COMPLETED, FAILED, DEAD_LETTERED }
    private final AgentCheckpointMapper mapper;

    public AnalysisTaskLifecycleService(AgentCheckpointMapper mapper) { this.mapper = mapper; }

    public void register(AnalysisTaskMsg msg) {
        validate(msg);
        if (msg.getTaskId() == null) msg.setTaskId(UUID.randomUUID().toString());
        mapper.registerTask(msg.getMediaId(), key(msg), identity(msg));
        requireIdentity(msg);
    }

    /** Returns false for an acknowledged terminal generation; legacy envelopes keep old behavior. */
    public boolean begin(AnalysisTaskMsg msg) {
        validate(msg);
        if (msg.getTaskId() == null) return true;
        requireIdentity(msg);
        State current = state(msg);
        if (current == State.COMPLETED || current == State.FAILED || current == State.DEAD_LETTERED) return false;
        if (current == State.RUNNING) {
            // The caller acquired the lock: a prior worker may have crashed. Resume its checkpoints.
            log.warn("analysis_task_generation_resumed mediaId={} taskId={}", msg.getMediaId(), msg.getTaskId());
            return true;
        }
        transition(msg, current, State.RUNNING);
        return true;
    }

    public void retry(AnalysisTaskMsg msg) { finish(msg, State.RETRYING); }
    public void complete(AnalysisTaskMsg msg) { finish(msg, State.COMPLETED); }
    public void fail(AnalysisTaskMsg msg) { finish(msg, State.FAILED); }
    public void deadLetter(AnalysisTaskMsg msg) { finish(msg, State.DEAD_LETTERED); }

    private void finish(AnalysisTaskMsg msg, State target) {
        if (msg.getTaskId() == null) return;
        transition(msg, State.RUNNING, target);
    }

    private void transition(AnalysisTaskMsg msg, State expected, State target) {
        if (expected != State.QUEUED && expected != State.RETRYING && expected != State.RUNNING) {
            throw new IllegalStateException("任务终态不能回退: " + expected);
        }
        if (mapper.transitionTask(msg.getMediaId(), key(msg), expected.name(), target.name()) != 1) {
            if (state(msg) == target) return; // Repeating the same transition is idempotent.
            throw new IllegalStateException("任务状态转换冲突: " + expected + " -> " + target);
        }
        log.info("analysis_task_transition mediaId={} taskId={} from={} to={}",
                msg.getMediaId(), msg.getTaskId(), expected, target);
    }

    private State state(AnalysisTaskMsg msg) {
        String persisted = mapper.findStage(msg.getMediaId(), key(msg));
        if (persisted == null) throw new IllegalStateException("任务代次尚未登记");
        return State.valueOf(persisted);
    }

    private void requireIdentity(AnalysisTaskMsg msg) {
        if (!identity(msg).equals(mapper.findPayload(msg.getMediaId(), key(msg)))) {
            throw new IllegalArgumentException("任务代次与分析目标或模式不匹配");
        }
    }

    private String identity(AnalysisTaskMsg msg) {
        return msg.getAction() + ":" + AnalysisTaskKeys.goalDigest(msg.getUserGoal(),
                AnalysisMode.fromNullable(msg.getMode()));
    }

    private String key(AnalysisTaskMsg msg) {
        String id = msg.getTaskId();
        if (id == null || !UUID.fromString(id).toString().equalsIgnoreCase(id)) {
            throw new IllegalArgumentException("invalid analysis taskId");
        }
        return "task:lifecycle:" + id;
    }

    private void validate(AnalysisTaskMsg msg) {
        if (msg == null || msg.getMediaId() == null || msg.getMediaId() <= 0 || !msg.hasSupportedAction()) {
            throw new IllegalArgumentException("invalid analysis task");
        }
        AnalysisTaskKeys.goalDigest(msg.getUserGoal(), AnalysisMode.fromNullable(msg.getMode()));
    }
}
