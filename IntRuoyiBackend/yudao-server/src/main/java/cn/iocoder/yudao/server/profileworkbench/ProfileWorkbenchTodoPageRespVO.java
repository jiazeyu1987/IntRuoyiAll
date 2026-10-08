package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.profileworkbench.ProfileWorkbenchTodoRowDTO;
import java.time.LocalDateTime;
import java.util.List;
public record ProfileWorkbenchTodoPageRespVO(List<ProfileWorkbenchTodoRowDTO> list, long total,
        long businessTotal, long hiddenTotal, int effectivePageNo, int pageSize, LocalDateTime readAt) { }
