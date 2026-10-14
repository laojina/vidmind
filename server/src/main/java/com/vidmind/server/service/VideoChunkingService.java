package com.vidmind.server.service;

import com.vidmind.server.dto.VideoChunk;
import com.vidmind.server.dto.VideoContext;
import com.vidmind.server.utils.DeepSeekUtils;
import com.vidmind.server.utils.EmbeddingUtils;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** 将长视频整理成可持久化、可检索的五分钟知识块。 */
@Service
public class VideoChunkingService {

    private static final long CHUNK_MS = 5 * 60 * 1000L;
    private static final long OVERLAP_MS = 60_000L;
    private static final Logger log = LoggerFactory.getLogger(VideoChunkingService.class);

    private final DeepSeekUtils deepSeekUtils;
    private final EmbeddingUtils embeddingUtils;
    private final AgentTelemetry telemetry;

    public VideoChunkingService(DeepSeekUtils deepSeekUtils,
                                EmbeddingUtils embeddingUtils,
                                AgentTelemetry telemetry) {
        this.deepSeekUtils = deepSeekUtils;
        this.embeddingUtils = embeddingUtils;
        this.telemetry = telemetry;
    }

    public List<VideoChunk> build(List<VideoContext.VideoSegment> segments) {
        if (segments == null) throw new IllegalArgumentException("video segments are required");
        if (segments.isEmpty()) return List.of();

        List<VideoContext.VideoSegment> orderedSegments = segments.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(VideoContext.VideoSegment::startMs))
                .toList();
        if (orderedSegments.isEmpty()) return List.of();

        List<VideoChunk> chunks = new ArrayList<>();
        // Only visit occupied core windows. Include adjacent text while preserving original timestamps.
        TreeSet<Long> coreWindows = new TreeSet<>();
        orderedSegments.forEach(segment -> coreWindows.add(segment.startMs() / CHUNK_MS * CHUNK_MS));
        for (long coreStart : coreWindows) {
            long chunkStart = Math.max(0, coreStart - OVERLAP_MS);
            long end = Math.addExact(coreStart, CHUNK_MS + OVERLAP_MS);
            List<VideoContext.VideoSegment> rawSegments = orderedSegments.stream()
                    .filter(segment -> segment.startMs() < end && segment.endMs() > chunkStart)
                    .toList();
            if (rawSegments.isEmpty()) continue;

            VideoChunk.ChunkSummary summary = summarize(rawSegments);
            List<String> keywords = normalizeTexts(summary.keywords());
            String embeddingText = summary.segmentSummary() + "\n" + String.join(" ", keywords);
            chunks.add(new VideoChunk(
                    chunkStart,
                    end,
                    summary.segmentSummary(),
                    keywords,
                    rawSegments,
                    embed(embeddingText)));
        }
        log.info("video_chunks_built segments={} chunks={} overlapMs={}",
                orderedSegments.size(), chunks.size(), OVERLAP_MS);
        return chunks;
    }

    private VideoChunk.ChunkSummary summarize(List<VideoContext.VideoSegment> segments) {
        try {
            return deepSeekUtils.summarizeChunk(segments);
        } catch (RuntimeException e) {
            log.warn("video_chunk_summary_fallback segmentCount={}", segments.size(), e);
            telemetry.incrementCurrent("summaryFallbacks", 1);
            String rawText = segments.stream()
                    .map(segment -> segment.transcript() + " "
                            + String.join(" ", normalizeTexts(segment.ocrTexts())))
                    .filter(text -> !text.isBlank())
                    .collect(java.util.stream.Collectors.joining(" "));
            String summary = rawText.length() <= 500 ? rawText : rawText.substring(0, 500);
            return new VideoChunk.ChunkSummary(summary, List.of());
        }
    }

    private List<Double> embed(String text) {
        try {
            return embeddingUtils.embed(text);
        } catch (RuntimeException e) {
            log.warn("video_chunk_embedding_fallback textChars={}", text.length(), e);
            telemetry.incrementCurrent("embeddingFallbacks", 1);
            return List.of();
        }
    }

    private List<String> normalizeTexts(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
