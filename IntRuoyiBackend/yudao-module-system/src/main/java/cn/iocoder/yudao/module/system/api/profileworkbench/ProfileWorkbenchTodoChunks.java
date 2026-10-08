package cn.iocoder.yudao.module.system.api.profileworkbench;

import java.util.List;
public final class ProfileWorkbenchTodoChunks {
    private ProfileWorkbenchTodoChunks() { }
    public static ProfileWorkbenchTodoChunkDTO from(List<ProfileWorkbenchTodoRowDTO> fetched, int limit) {
        if (limit != 100 || fetched == null || fetched.size() > limit + 1)
            throw new IllegalStateException("Invalid workbench chunk contract");
        boolean next = fetched.size() > limit;
        List<ProfileWorkbenchTodoRowDTO> rows = List.copyOf(fetched.subList(0, Math.min(fetched.size(), limit)));
        return new ProfileWorkbenchTodoChunkDTO(rows, rows.isEmpty() ? null : rows.get(rows.size() - 1), next);
    }
}
