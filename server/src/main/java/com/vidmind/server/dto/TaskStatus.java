package com.vidmind.server.dto;

public record TaskStatus(State state, String result, String message) {

    public enum State {
        NOT_STARTED,
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public static TaskStatus of(State state, String message) {
        return new TaskStatus(state, null, message);
    }

    public static TaskStatus completed(String result) {
        return new TaskStatus(State.COMPLETED, result, "任务完成");
    }

    public static TaskStatus completed(AgentState agentState) {
        String markdown = agentState.result().toMarkdown();
        if (agentState.critique() != null && agentState.critique().passed()) {
            return completed(markdown);
        }
        String warning = "分析结果已生成，可结合视频时间戳进一步确认。";
        return new TaskStatus(State.COMPLETED, "> **结果提示：** " + warning + "\n\n" + markdown, warning);
    }
}
