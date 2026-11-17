package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.system.api.workbench.*;
import org.springframework.stereotype.Component;

/** Deviation duties are a separate category; eligibility and navigation remain eDHR-owned. */
@Component
public class MesDeviationWorkbenchSource extends MesEdhrWorkbenchSource {
    @Override public SourceDescriptor descriptor() {
        return new SourceDescriptor("EDHR_DEVIATION_TASK", "mes", 1, 3, "eDHR偏差任务", "偏差",
                "mes.deviation.policy", "mes.deviation.open", 1);
    }
    @Override public long count(QueryContext query) {
        query.validate();
        Long count = mapper.countDeviation(query);
        if (count == null || count < 0) throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
        return count;
    }
    @Override public TaskChunk readChunk(QueryContext query, ChunkCursor after, int limit) {
        query.validate();
        if (limit != 100) throw WorkbenchFailure.INVALID_QUERY.exception();
        return TaskChunk.from(mapper.chunkDeviation(query, after, limit + 1).stream()
                .map(row -> project(row, query)).toList(), limit);
    }
    @Override public TaskSummary resolveVisibilityIdentity(QueryContext query, String businessId) {
        query.validate();
        if (businessId == null || !businessId.matches("[1-9][0-9]*"))
            throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
        long id;
        try { id = Long.parseLong(businessId); }
        catch (NumberFormatException invalid) { throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception(invalid); }
        Long status = mapper.lockEdhrIdentity(query, id);
        var rows = mapper.identityDeviation(query, businessId, status);
        if (rows.size() != 1) throw WorkbenchFailure.TASK_NOT_ELIGIBLE.exception();
        return project(rows.get(0), query);
    }
}
