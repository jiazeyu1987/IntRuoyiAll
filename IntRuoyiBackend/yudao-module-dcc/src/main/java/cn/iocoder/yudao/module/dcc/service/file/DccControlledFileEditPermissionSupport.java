package cn.iocoder.yudao.module.dcc.service.file;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;

@Component
public class DccControlledFileEditPermissionSupport {
    @Resource
    private DccControlledFileCategoryPermissionSupport permissions;

    public void assertEditableProjectCategory(Long actorId, Long projectCodeId, Long categoryId) {
        if (!permissions.hasEditableProjectCategory(actorId, projectCodeId, categoryId))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }
}
