package cn.iocoder.yudao.module.showroom.foundation.contract;

import java.util.List;

/**
 * Frozen V1 approval route: editor -> department supervisor -> publicity approver.
 */
public final class ShowroomApprovalRouteContract {

    private static final List<String> FIXED_ROUTE = ShowroomRoleModelContract.fixedApprovalRoute();

    private ShowroomApprovalRouteContract() {
    }

    public static List<String> fixedRoute() {
        return FIXED_ROUTE;
    }

    public static void validatePrerequisites(Long editorUserId, Long submitterDeptId,
                                             Long departmentSupervisorUserId, Long gaoxinApproverUserId) {
        if (editorUserId == null) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: editor user is required");
        }
        if (gaoxinApproverUserId == null) {
            throw new IllegalStateException("SHOWROOM_ROLE_BINDING_MISSING: publicity approver is required");
        }
    }

    public static boolean shouldSkipSupervisorStep(Long submitterDeptId, Long departmentSupervisorUserId) {
        return submitterDeptId == null || departmentSupervisorUserId == null;
    }

}
