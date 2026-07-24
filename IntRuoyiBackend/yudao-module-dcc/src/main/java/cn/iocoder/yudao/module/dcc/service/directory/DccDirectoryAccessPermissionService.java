package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;

import java.util.Set;

public interface DccDirectoryAccessPermissionService {

    boolean hasDirectoryManagementPermission(Long userId);

    Set<Long> getAuthorizedDirectoryIds(Long userId, DccAccessTypeEnum accessType);
}
