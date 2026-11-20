package cn.iocoder.yudao.module.dcc.service.file;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TYPE_UPLOAD_FORBIDDEN;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;

@Component
public class DccControlledFileEditPermissionSupport {
    @Resource
    private DccControlledFileCategoryPermissionSupport permissions;
    @Resource
    private DccFileTypeTaxonomyAdminService taxonomy;

    public void assertEditableProjectCategory(Long actorId, Long projectCodeId, Long categoryId) {
        if (!permissions.hasEditableProjectCategory(actorId, projectCodeId, categoryId))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }

    public void assertUploadProjectCategory(Long actorId, Long projectCodeId, Long categoryId, Long typeId) {
        if (permissions.hasEditableProjectCategory(actorId, projectCodeId, categoryId)) return;
        if (!permissions.isLibrary(projectCodeId)
                && !permissions.hasCategoryPermission(categoryId, actorId, DccFileCategoryPermissionActionEnum.EDIT)) {
            var path = taxonomy.validateCategoryMapping(typeId, categoryId, true);
            String name = java.util.stream.Stream.of(path.level1(), path.level2(), path.level3(), path.level4(), path.level5())
                    .filter(value -> value != null && !value.isBlank()).reduce((previous, current) -> current).orElseThrow();
            throw exception(CONTROLLED_FILE_TYPE_UPLOAD_FORBIDDEN, name);
        }
        throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }
}
