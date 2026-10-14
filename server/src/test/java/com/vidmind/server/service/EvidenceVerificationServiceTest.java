package com.vidmind.server.service;

import com.vidmind.server.dto.AnalysisResult;
import com.vidmind.server.dto.VideoContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceVerificationServiceTest {

    private final EvidenceVerificationService service = new EvidenceVerificationService();
    private final VideoContext context = new VideoContext(
            "lesson.mp4",
            "总结课程",
            List.of(new VideoContext.VideoSegment(
                    120_000,
                    180_000,
                    "接下来讲解二叉树的前序遍历",
                    List.of("前序遍历：根节点、左子树、右子树"),
                    List.of("frame_000125.jpg"))));

    @Test
    void acceptsVerbatimEvidenceAtTheDeclaredTimestamp() {
        AnalysisResult.Evidence evidence = new AnalysisResult.Evidence(
                125_000, "OCR", "根节点、左子树、右子树", "前序遍历顺序");

        assertTrue(service.supported(context, evidence));
        assertTrue(service.supportsClaim(context, "前序遍历顺序", evidence));
    }

    @Test
    void rejectsTextThatOnlyLooksSimilarToTheSource() {
        AnalysisResult.Evidence evidence = new AnalysisResult.Evidence(
                125_000, "OCR", "根节点左子树不存在，因此应跳过", "前序遍历顺序");

        assertFalse(service.supported(context, evidence));
    }

    @Test
    void rejectsUnknownSourceContainingARecognizedSubstring() {
        assertFalse(service.supported(context, new AnalysisResult.Evidence(
                125_000, "NOT_OCR", "根节点、左子树、右子树", "前序遍历顺序")));
        assertFalse(service.supported(context, new AnalysisResult.Evidence(
                125_000, "ASR_UNVERIFIED", "二叉树", "课程主题")));
    }

    @Test
    void treatsTimeWindowsAsHalfOpenAndAcceptsCombinedSources() {
        assertTrue(service.supported(context, new AnalysisResult.Evidence(
                120_000, "asr+ocr", "二叉树", "课程主题")));
        assertFalse(service.supported(context, new AnalysisResult.Evidence(
                180_000, "ASR", "二叉树", "课程主题")));
    }
}
