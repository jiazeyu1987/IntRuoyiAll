package cn.iocoder.yudao.module.dcc.service.position;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;

import java.util.Set;

public final class DccUploaderDerivedPositionSupport {

    private static final Set<String> DIRECT_MANAGER_POSITION_NAMES = Set.of(
            "编制人直接主管"
    );
    private static final Set<String> DEPARTMENT_SCOPED_POSITION_NAMES = Set.of(
            "部门负责人",
            "编制部门负责人"
    );
    private DccUploaderDerivedPositionSupport() {
    }

    public static boolean isUploaderDerivedPosition(DccApprovalPositionDO position) {
        return position != null && isUploaderDerivedPositionName(position.getName());
    }

    public static boolean isUploaderDerivedPositionName(String positionName) {
        String normalizedName = StrUtil.trimToEmpty(positionName);
        return DIRECT_MANAGER_POSITION_NAMES.contains(normalizedName)
                || DEPARTMENT_SCOPED_POSITION_NAMES.contains(normalizedName);
    }

    public static boolean isDirectManagerPositionName(String positionName) {
        return DIRECT_MANAGER_POSITION_NAMES.contains(StrUtil.trimToEmpty(positionName));
    }

    public static boolean isDepartmentScopedPositionName(String positionName) {
        return DEPARTMENT_SCOPED_POSITION_NAMES.contains(StrUtil.trimToEmpty(positionName));
    }

}
