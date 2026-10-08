package cn.iocoder.yudao.module.showroom.workflow.service;

import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomWorkbenchTodoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class ShowroomAssignmentWorkbenchSource implements ProfileWorkbenchTodoSource {
    @Resource private ShowroomWorkbenchTodoMapper mapper;
    @Override public ProfileWorkbenchTodoSourceId sourceId() { return ProfileWorkbenchTodoSourceId.SHOWROOM_ASSIGNMENT; }
    @Override public long count(ProfileWorkbenchTodoQueryDTO query) {
        Long invalid = mapper.countInvalidNotifications(query);
        if (invalid == null || invalid != 0) throw new IllegalStateException("Showroom assignment notification relation is invalid");
        Long count = mapper.countAssignment(query);
        if (count == null || count < 0) throw new IllegalStateException("Invalid workbench source count");
        return count;
    }
    @Override public ProfileWorkbenchTodoChunkDTO readChunk(ProfileWorkbenchTodoQueryDTO query, ProfileWorkbenchTodoRowDTO after, int limit) {
        if (limit != 100) throw new IllegalArgumentException("Workbench chunk limit must be 100");
        return ProfileWorkbenchTodoChunks.from(mapper.chunkAssignment(query, after, limit + 1), limit);
    }
    @Override public Map<String, String> navigation(ProfileWorkbenchTodoRowDTO row) {
        return Map.of("assignmentId", row.getBusinessId());
    }
    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing formal workbench navigation identity");
        return value;
    }
    private static void put(Map<String, String> nav, String key, String value) { if (value != null) nav.put(key, value); }
}
