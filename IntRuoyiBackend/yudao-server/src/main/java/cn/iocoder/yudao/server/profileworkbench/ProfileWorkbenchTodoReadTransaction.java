package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProfileWorkbenchTodoReadTransaction {
    private static final int CHUNK_SIZE = 100;
    private static final long BUDGET_NANOS = 10_000_000_000L;
    private final Map<ProfileWorkbenchTodoSourceId, ProfileWorkbenchTodoSource> sources;
    public ProfileWorkbenchTodoReadTransaction(List<ProfileWorkbenchTodoSource> beans) {
        EnumMap<ProfileWorkbenchTodoSourceId, ProfileWorkbenchTodoSource> registered = new EnumMap<>(ProfileWorkbenchTodoSourceId.class);
        for (ProfileWorkbenchTodoSource source : beans) {
            if (registered.put(source.sourceId(), source) != null) throw new IllegalStateException("Duplicate workbench source");
        }
        if (registered.size() != ProfileWorkbenchTodoSourceId.values().length) throw new IllegalStateException("Missing workbench source");
        sources = Collections.unmodifiableMap(registered);
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 10, rollbackFor = Exception.class)
    public ProfileWorkbenchTodoCountRespVO count(ProfileWorkbenchTodoQueryDTO query, List<ProfileWorkbenchTodoSourceId> ids, long start) {
        long total = 0;
        for (ProfileWorkbenchTodoSourceId id : ids) total = Math.addExact(total, checkedCount(sources.get(id), query, start));
        checkBudget(start);
        return new ProfileWorkbenchTodoCountRespVO(total, LocalDateTime.now());
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 10, rollbackFor = Exception.class)
    public ProfileWorkbenchTodoPageRespVO page(ProfileWorkbenchTodoQueryDTO query, List<ProfileWorkbenchTodoSourceId> ids,
            int pageNo, int pageSize, long start) {
        if (pageNo < 1 || pageSize < 10 || pageSize > 100) throw new IllegalArgumentException("Invalid workbench page bounds");
        Comparator<ProfileWorkbenchTodoRowDTO> comparator = ProfileWorkbenchTodoSortContract.comparator(query);
        long business = 0, hidden = 0, total = 0;
        EnumMap<ProfileWorkbenchTodoSourceId, Long> filteredCounts = new EnumMap<>(ProfileWorkbenchTodoSourceId.class);
        ProfileWorkbenchTodoQueryDTO base = new ProfileWorkbenchTodoQueryDTO();
        base.setUserId(query.getUserId()); base.setTenantId(query.getTenantId());
        ProfileWorkbenchTodoQueryDTO hiddenQuery = new ProfileWorkbenchTodoQueryDTO();
        BeanUtils.copyProperties(base, hiddenQuery); hiddenQuery.setVisibility("hidden");
        for (ProfileWorkbenchTodoSourceId id : ids) {
            ProfileWorkbenchTodoSource source = sources.get(id);
            long baseCount = checkedCount(source, base, start);
            long hiddenCount = checkedCount(source, hiddenQuery, start);
            long count = checkedCount(source, query, start);
            if (hiddenCount > baseCount || count > ("hidden".equals(query.getVisibility()) ? hiddenCount : baseCount - hiddenCount))
                throw new IllegalStateException("Inconsistent workbench source counts");
            business = Math.addExact(business, baseCount); hidden = Math.addExact(hidden, hiddenCount);
            total = Math.addExact(total, count); filteredCounts.put(id, count);
        }
        long maxPage = total == 0 ? 1 : (total - 1) / pageSize + 1;
        int effective = (int) Math.min(pageNo, maxPage);
        long offset = Math.multiplyExact((long) effective - 1, pageSize);
        PriorityQueue<SourceBuffer> heap = new PriorityQueue<>((a, b) -> comparator.compare(a.head(), b.head()));
        for (ProfileWorkbenchTodoSourceId id : ids) {
            if (filteredCounts.get(id) > 0) {
                SourceBuffer buffer = new SourceBuffer(sources.get(id), filteredCounts.get(id), query, comparator, start);
                buffer.load();
                if (buffer.rows.isEmpty()) throw new IllegalStateException("Count predicted missing workbench rows");
                heap.add(buffer);
            }
        }
        int expectedSize = (int) Math.min(pageSize, Math.max(0, total - offset));
        long consumed = 0;
        List<ProfileWorkbenchTodoRowDTO> result = new ArrayList<>(expectedSize);
        ProfileWorkbenchTodoRowDTO previous = null;
        while (consumed < offset + expectedSize) {
            checkBudget(start);
            if (heap.isEmpty()) throw new IllegalStateException("Workbench source ended before counted prefix");
            SourceBuffer buffer = heap.remove();
            ProfileWorkbenchTodoRowDTO row = buffer.head();
            if (previous != null && comparator.compare(previous, row) >= 0) throw new IllegalStateException("Duplicate or unordered workbench row");
            previous = row;
            if (consumed >= offset) {
                row.setNavigation(buffer.source.navigation(row));
                result.add(row);
            }
            consumed++;
            buffer.index++;
            buffer.consumed++;
            if (buffer.index < buffer.rows.size()) heap.add(buffer);
            else if (buffer.hasNext && consumed < offset + expectedSize) {
                buffer.load(); heap.add(buffer);
            } else if (!buffer.hasNext && buffer.consumed != buffer.expected) {
                throw new IllegalStateException("Workbench source count differs from exhausted rows");
            }
        }
        checkBudget(start);
        return new ProfileWorkbenchTodoPageRespVO(List.copyOf(result), total, business, hidden, effective, pageSize, LocalDateTime.now());
    }
    private static long checkedCount(ProfileWorkbenchTodoSource source, ProfileWorkbenchTodoQueryDTO query, long start) {
        checkBudget(start);
        long count = source.count(query);
        checkBudget(start);
        if (count < 0) throw new IllegalStateException("Invalid workbench count");
        return count;
    }
    private static void checkBudget(long start) {
        if (System.nanoTime() - start >= BUDGET_NANOS) throw new IllegalStateException("Workbench query exceeded ten-second budget");
    }
    private static final class SourceBuffer {
        final ProfileWorkbenchTodoSource source;
        final long expected;
        final ProfileWorkbenchTodoQueryDTO query;
        final Comparator<ProfileWorkbenchTodoRowDTO> comparator;
        final long start;
        List<ProfileWorkbenchTodoRowDTO> rows = List.of();
        ProfileWorkbenchTodoRowDTO after;
        int index;
        long consumed;
        boolean hasNext;
        SourceBuffer(ProfileWorkbenchTodoSource source, long expected, ProfileWorkbenchTodoQueryDTO query,
                Comparator<ProfileWorkbenchTodoRowDTO> comparator, long start) {
            this.source = source; this.expected = expected; this.query = query; this.comparator = comparator; this.start = start;
        }
        ProfileWorkbenchTodoRowDTO head() { return rows.get(index); }
        void load() {
            checkBudget(start);
            ProfileWorkbenchTodoChunkDTO chunk = source.readChunk(query, after, CHUNK_SIZE);
            checkBudget(start);
            if (chunk == null || chunk.rows() == null || chunk.rows().isEmpty() || chunk.rows().size() > CHUNK_SIZE
                    || (chunk.hasNext() && chunk.rows().size() != CHUNK_SIZE))
                throw new IllegalStateException("Invalid workbench chunk");
            ProfileWorkbenchTodoRowDTO last = after;
            for (ProfileWorkbenchTodoRowDTO row : chunk.rows()) {
                if (row == null || row.getSourceId() != source.sourceId() || row.getNumericId() == null || row.getNumericId() <= 0
                        || !Long.toString(row.getNumericId()).equals(row.getBusinessId())
                        || !(source.sourceId().label + ":" + row.getBusinessId()).equals(row.getTaskKey())
                        || !source.sourceId().label.equals(row.getSource()) || !source.sourceId().taskType.equals(row.getTaskType())
                        || row.getDetail() == null || row.getStatusLabel() == null)
                    throw new IllegalStateException("Invalid canonical workbench row");
                if (last != null && comparator.compare(last, row) >= 0) throw new IllegalStateException("Workbench chunk cursor did not advance");
                last = row;
            }
            if (chunk.after() == null || !last.equals(chunk.after())) throw new IllegalStateException("Invalid workbench end cursor");
            long fetchedEnd = Math.addExact(consumed, chunk.rows().size());
            if (fetchedEnd > expected || (chunk.hasNext() ? fetchedEnd >= expected : fetchedEnd != expected))
                throw new IllegalStateException("Workbench chunk disagrees with exact count");
            rows = chunk.rows(); after = last; hasNext = chunk.hasNext(); index = 0;
        }
    }
}
