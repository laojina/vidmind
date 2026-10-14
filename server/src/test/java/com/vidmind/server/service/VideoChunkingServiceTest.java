package com.vidmind.server.service;

import com.vidmind.server.dto.VideoContext;
import com.vidmind.server.utils.DeepSeekUtils;
import com.vidmind.server.utils.EmbeddingUtils;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideoChunkingServiceTest {
    @Test
    void neighboringWindowsShareBoundaryTextWithoutChangingEvidenceTimestamps() {
        var models = mock(DeepSeekUtils.class);
        when(models.summarizeChunk(any())).thenThrow(new IllegalStateException("offline"));
        var service = new VideoChunkingService(models, mock(EmbeddingUtils.class), mock(AgentTelemetry.class));
        var before = segment(240_000, "boundary before");
        var after = segment(300_000, "boundary after");
        var chunks = service.build(List.of(before, after));
        assertEquals(2, chunks.size());
        assertEquals(List.of(before, after), chunks.get(0).rawSegments());
        assertEquals(List.of(before, after), chunks.get(1).rawSegments());
        assertEquals(0, chunks.get(0).startTime());
        assertEquals(360_000, chunks.get(0).endTime());
        assertEquals(240_000, chunks.get(1).startTime());
        assertTrue(chunks.get(0).segmentSummary().contains("boundary after"));
        var retrieval = new VideoEvidenceRetrievalService(models, mock(EmbeddingUtils.class),
                mock(QdrantVectorStore.class), mock(AgentTelemetry.class));
        assertEquals(2, retrieval.retrieve(7L, "boundary", chunks).size());
        assertThrows(IllegalArgumentException.class, () -> service.build(null));
    }

    @Test
    void sparseTimelineDoesNotGenerateEmptyWindows() {
        var models = mock(DeepSeekUtils.class);
        when(models.summarizeChunk(any())).thenThrow(new IllegalStateException("offline"));
        var service = new VideoChunkingService(models, mock(EmbeddingUtils.class), mock(AgentTelemetry.class));
        assertEquals(1, service.build(List.of(segment(3_000_000_000L, "late"))).size());
        verify(models, times(1)).summarizeChunk(any());
    }

    private VideoContext.VideoSegment segment(long start, String text) {
        return new VideoContext.VideoSegment(start, start + 60_000, text, List.of(), List.of());
    }
}
