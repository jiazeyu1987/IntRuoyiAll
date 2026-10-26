package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.WorkbenchVisibilityVersionMapper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service @DS("master")
public class ProfileWorkbenchTodoReadTransaction {
    private static final int CHUNK_SIZE = 100;
    private static final long BUDGET_NANOS = 10_000_000_000L;
    private final WorkbenchSourceRegistry registry;
    private final WorkbenchVisibilityVersionMapper versions;
    private final WorkbenchQueryTokens tokens;
    public ProfileWorkbenchTodoReadTransaction(WorkbenchSourceRegistry registry, WorkbenchVisibilityVersionMapper versions, WorkbenchQueryTokens tokens) {
        this.registry = registry; this.versions = versions; this.tokens = tokens;
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10,rollbackFor=Exception.class)
    public WorkbenchContextResponse issue(long tenant, long user, WorkbenchContextRequest request, List<String> ids, long start) {
        long version = shared(tenant, user); checkBudget(start);
        var issued = tokens.issue(tenant, user, version, request.registryRevision(), ids, request.querySpec(), request.contextEpoch());
        tokens.requireAlive(issued.claim()); checkBudget(start);
        return new WorkbenchContextResponse(3, request.registryRevision(), ids, request.contextEpoch(), Long.toString(version),
            issued.queryVersion(), issued.claim().expiresAt(), issued.claim().querySpec());
    }
    private long shared(long tenant, long user) {
        Long value = versions.shared(tenant, user);
        if (value == null || value < 1) throw VISIBILITY_CONTEXT_MISSING.exception();
        return value;
    }
    private QueryContext query(WorkbenchQueryTokens.Claim claim) {
        tokens.requireAlive(claim);
        if (shared(claim.tenant(), claim.user()) != claim.visibilityVersion()) throw VISIBILITY_VERSION_CONFLICT.exception();
        return claim.querySpec().context(claim.tenant(), claim.user(), claim.visibilityVersion());
    }
    private record Totals(long business, long hidden, long filtered, Map<String, Long> bySource) { }
    private Totals totals(QueryContext q, List<String> ids, long start) {
        long business = 0, hidden = 0, filtered = 0;
        Map<String, Long> counts = new HashMap<>();
        for (String id : ids) {
            SourceAdapter source = registry.require(id);
            long all = checkedCount(source, q.partition(null), start);
            long hiddenCount = checkedCount(source, q.partition("hidden"), start);
            // All partitions share this transaction's snapshot. Without filters, the
            // visible/hidden count is already known; sorting does not change it.
            long selected = q.getTaskType() == null && q.getFilterField() == null
                ? ("hidden".equals(q.getVisibility()) ? hiddenCount : all - hiddenCount)
                : checkedCount(source, q, start);
            if (hiddenCount > all || selected > ("hidden".equals(q.getVisibility()) ? hiddenCount : all - hiddenCount)) throw SOURCE_CONTRACT_INVALID.exception();
            try { business = Math.addExact(business, all); hidden = Math.addExact(hidden, hiddenCount); filtered = Math.addExact(filtered, selected); }
            catch (ArithmeticException failure) { throw SOURCE_CONTRACT_INVALID.exception(failure); }
            counts.put(id, selected);
        }
        // HTTP counts are JSON numbers consumed as exact JavaScript integers, not NumberSerializer strings.
        if (business >= 9_007_199_254_740_991L || hidden >= 9_007_199_254_740_991L || filtered >= 9_007_199_254_740_991L)
            throw SOURCE_CONTRACT_INVALID.exception();
        return new Totals(business, hidden, filtered, counts);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10,rollbackFor=Exception.class)
    public ProfileWorkbenchTodoCountRespVO count(String token, WorkbenchQueryTokens.Claim claim, long start) {
        QueryContext q = query(claim); Totals totals = totals(q, claim.sources(), start);
        tokens.requireAlive(claim); checkBudget(start);
        return new ProfileWorkbenchTodoCountRespVO(3, claim.registryRevision(), token, claim.contextEpoch(), Long.toString(claim.visibilityVersion()),
            totals.filtered, totals.business, totals.hidden, LocalDateTime.now());
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10,rollbackFor=Exception.class)
    public ProfileWorkbenchTodoPageRespVO page(String token, WorkbenchQueryTokens.Claim claim, int pageNo, int pageSize, long start) {
        if (pageNo < 1 || pageSize < 10 || pageSize > 100) throw INVALID_QUERY.exception();
        QueryContext q = query(claim); Comparator<TaskSummary> comparator = SortContract.comparator(q);
        Totals totals = totals(q, claim.sources(), start);
        long maxPage = totals.filtered == 0 ? 1 : (totals.filtered - 1) / pageSize + 1;
        int effective = (int) Math.min(pageNo, maxPage);
        long offset = Math.multiplyExact((long) effective - 1, pageSize);
        int expectedSize = (int) Math.min(pageSize, Math.max(0, totals.filtered - offset));
        PriorityQueue<SourceBuffer> heap = new PriorityQueue<>((a,b) -> comparator.compare(a.head(), b.head()));
        for (String id : claim.sources()) if (totals.bySource.get(id) > 0) {
            SourceBuffer buffer = new SourceBuffer(registry.require(id), totals.bySource.get(id), q, start);
            buffer.load(); heap.add(buffer);
        }
        long consumed = 0; TaskSummary previous = null; List<TaskSummary> rows = new ArrayList<>(expectedSize);
        while (consumed < offset + expectedSize) {
            checkBudget(start);
            if (heap.isEmpty()) throw SOURCE_CONTRACT_INVALID.exception();
            SourceBuffer buffer = heap.remove(); TaskSummary row = buffer.head();
            if (previous != null && comparator.compare(previous,row) >= 0) throw SOURCE_CONTRACT_INVALID.exception();
            previous = row; if (consumed >= offset) rows.add(row);
            consumed++; buffer.index++; buffer.consumed++;
            if (buffer.index < buffer.rows.size()) heap.add(buffer);
            else if (buffer.hasNext && consumed < offset + expectedSize) { buffer.load(); heap.add(buffer); }
            else if (!buffer.hasNext && buffer.consumed != buffer.expected) throw SOURCE_CONTRACT_INVALID.exception();
        }
        tokens.requireAlive(claim); checkBudget(start);
        return new ProfileWorkbenchTodoPageRespVO(3, claim.registryRevision(), token, claim.contextEpoch(), Long.toString(claim.visibilityVersion()),
            List.copyOf(rows), totals.filtered, totals.business, totals.hidden, effective, pageSize, LocalDateTime.now());
    }
    private static long checkedCount(SourceAdapter source, QueryContext q, long start) {
        checkBudget(start); q.validate(); long value;
        try { value = source.count(q); }
        catch (ServiceException failure) { throw failure; }
        catch (RuntimeException failure) { throw SOURCE_QUERY_FAILED.exception(failure); }
        checkBudget(start); if (value < 0) throw SOURCE_CONTRACT_INVALID.exception(); return value;
    }
    static void checkBudget(long start) { if (System.nanoTime() - start >= BUDGET_NANOS) throw QUERY_BUDGET_EXCEEDED.exception(); }
    private static final class SourceBuffer {
        final SourceAdapter source; final long expected; final QueryContext q; final long start;
        List<TaskSummary> rows = List.of(); ChunkCursor after; int index; long consumed; boolean hasNext;
        SourceBuffer(SourceAdapter source, long expected, QueryContext q, long start) { this.source=source;this.expected=expected;this.q=q;this.start=start; }
        TaskSummary head() { return rows.get(index); }
        void load() {
            checkBudget(start); TaskChunk chunk;
            try { chunk = source.readChunk(q, after, CHUNK_SIZE); }
            catch (ServiceException failure) { throw failure; }
            catch (RuntimeException failure) { throw SOURCE_QUERY_FAILED.exception(failure); }
            checkBudget(start);
            if (chunk == null || chunk.rows() == null || chunk.rows().isEmpty() || chunk.rows().size() > CHUNK_SIZE
                    || (chunk.hasNext() && chunk.rows().size() != CHUNK_SIZE)) throw SOURCE_CONTRACT_INVALID.exception();
            ChunkCursor last = after;
            for (TaskSummary row : chunk.rows()) {
                if (row == null || row.getClass() != TaskSummary.class || !source.sourceId().equals(row.getSourceId())
                        || row.getBusinessId() == null || row.getBusinessId().isBlank() || row.getBusinessId().length() > 80
                        || !(source.sourceId() + ":" + row.getBusinessId()).equals(row.getTaskKey()) || row.getTaskKey().length() > 160
                        || row.getRowOrderKey() == null || row.getRowOrderKey().isEmpty()
                        || !source.descriptor().label().equals(row.getSource()) || !source.descriptor().taskType().equals(row.getTaskType())
                        || row.getDetail() == null || row.getStatusLabel() == null || row.getAction() == null
                        || !source.descriptor().actionKind().equals(row.getAction().actionKind())
                        || row.getAction().payloadVersion() != source.descriptor().payloadVersion()) throw SOURCE_CONTRACT_INVALID.exception();
                RowOrderContract.validate(row.getRowOrderKey());
                row.setSourceRank(source.descriptor().sortRank());
                SortContract.validateTime(row.getCreatedAt()); SortContract.validateTime(row.getDueAt());
                if (last != null && SortContract.compare(last,row.cursor(),q) >= 0) throw SOURCE_CONTRACT_INVALID.exception();
                last = row.cursor();
            }
            if (!Objects.equals(last,chunk.after())) throw SOURCE_CONTRACT_INVALID.exception();
            long fetchedEnd = Math.addExact(consumed, chunk.rows().size());
            if (fetchedEnd > expected || (chunk.hasNext() ? fetchedEnd >= expected : fetchedEnd != expected)) throw SOURCE_CONTRACT_INVALID.exception();
            rows = chunk.rows(); after = last; hasNext = chunk.hasNext(); index = 0;
        }
    }
}
