package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.iocoder.yudao.module.system.api.profileworkbench.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class MesWorkOrderWorkbenchSource implements ProfileWorkbenchTodoSource {
    @Resource private MesWorkbenchTodoMapper mapper;
    @Override public ProfileWorkbenchTodoSourceId sourceId() { return ProfileWorkbenchTodoSourceId.WORK_ORDER; }
    @Override public long count(ProfileWorkbenchTodoQueryDTO query) {

        Long count = mapper.countWorkOrder(query);
        if (count == null || count < 0) throw new IllegalStateException("Invalid workbench source count");
        return count;
    }
    @Override public ProfileWorkbenchTodoChunkDTO readChunk(ProfileWorkbenchTodoQueryDTO query, ProfileWorkbenchTodoRowDTO after, int limit) {
        if (limit != 100) throw new IllegalArgumentException("Workbench chunk limit must be 100");
        return ProfileWorkbenchTodoChunks.from(mapper.chunkWorkOrder(query, after, limit + 1), limit);
    }
    @Override public Map<String, String> navigation(ProfileWorkbenchTodoRowDTO row) {
        return row.getCode() == null ? Map.of() : Map.of("code", row.getCode());
    }
    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing formal workbench navigation identity");
        return value;
    }
    private static void put(Map<String, String> nav, String key, String value) { if (value != null) nav.put(key, value); }
}
