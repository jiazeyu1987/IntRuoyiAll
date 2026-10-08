package cn.iocoder.yudao.module.system.api.profileworkbench;

import java.util.Map;
public interface ProfileWorkbenchTodoSource {
    ProfileWorkbenchTodoSourceId sourceId();
    long count(ProfileWorkbenchTodoQueryDTO query);
    ProfileWorkbenchTodoChunkDTO readChunk(ProfileWorkbenchTodoQueryDTO query, ProfileWorkbenchTodoRowDTO after, int limit);
    Map<String, String> navigation(ProfileWorkbenchTodoRowDTO row);
}
