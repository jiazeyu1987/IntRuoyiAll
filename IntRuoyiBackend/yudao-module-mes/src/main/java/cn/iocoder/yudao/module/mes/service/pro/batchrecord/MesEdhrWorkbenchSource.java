package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class MesEdhrWorkbenchSource implements ProfileWorkbenchTodoSource {
    @Resource private MesWorkbenchTodoMapper mapper;
    @Override public ProfileWorkbenchTodoSourceId sourceId() { return ProfileWorkbenchTodoSourceId.EDHR_WORK_TASK; }
    @Override public long count(ProfileWorkbenchTodoQueryDTO query) {

        Long count = mapper.countEdhr(query);
        if (count == null || count < 0) throw new IllegalStateException("Invalid workbench source count");
        return count;
    }
    @Override public ProfileWorkbenchTodoChunkDTO readChunk(ProfileWorkbenchTodoQueryDTO query, ProfileWorkbenchTodoRowDTO after, int limit) {
        if (limit != 100) throw new IllegalArgumentException("Workbench chunk limit must be 100");
        return ProfileWorkbenchTodoChunks.from(mapper.chunkEdhr(query, after, limit + 1), limit);
    }
    @Override public Map<String, String> navigation(ProfileWorkbenchTodoRowDTO row) {
        Map<String, String> nav = new java.util.LinkedHashMap<>();
        nav.put("id", row.getBusinessId()); nav.put("taskType", required(row.getDomainTaskType()));
        // The existing formal FILL navigator accepts structured batch/task identity without actionUrl.
        // Preserve that source contract; the navigator remains responsible for route validation.
        put(nav, "actionUrl", row.getActionUrl());
        put(nav, "batchExecutionId", row.getBatchExecutionId()); put(nav, "batchTaskId", row.getBatchTaskId()); put(nav, "executionId", row.getExecutionId());
        put(nav, "businessScopeType", row.getBusinessScopeType()); put(nav, "businessScopeId", row.getBusinessScopeId()); return Map.copyOf(nav);
    }
    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing formal workbench navigation identity");
        return value;
    }
    private static void put(Map<String, String> nav, String key, String value) { if (value != null) nav.put(key, value); }
}
