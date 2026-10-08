package cn.iocoder.yudao.module.system.api.profileworkbench;

import java.util.List;
public record ProfileWorkbenchTodoChunkDTO(List<ProfileWorkbenchTodoRowDTO> rows,
        ProfileWorkbenchTodoRowDTO after, boolean hasNext) { }
