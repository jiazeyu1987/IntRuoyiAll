package cn.iocoder.yudao.module.showroom.foundation.contract;

import java.util.List;

public final class ShowroomRoleModelContract {

    private static final String EDITOR_ROLE_CODE = "EDITOR";
    private static final String DEPARTMENT_SUPERVISOR_ROLE_CODE = "DEPARTMENT_SUPERVISOR";
    private static final String GAOXIN_APPROVER_ROLE_CODE = "showroom_publicity";
    private static final String FRONTSTAGE_VIEWER_ROLE_CODE = "FRONTSTAGE_VIEWER";

    private static final List<String> FIXED_ROLE_MODEL = List.of(
            EDITOR_ROLE_CODE,
            DEPARTMENT_SUPERVISOR_ROLE_CODE,
            GAOXIN_APPROVER_ROLE_CODE,
            FRONTSTAGE_VIEWER_ROLE_CODE
    );

    private static final List<String> FIXED_APPROVAL_ROUTE = List.of(
            EDITOR_ROLE_CODE,
            DEPARTMENT_SUPERVISOR_ROLE_CODE,
            GAOXIN_APPROVER_ROLE_CODE
    );

    private ShowroomRoleModelContract() {
    }

    public static List<String> fixedRoleModel() {
        return FIXED_ROLE_MODEL;
    }

    public static List<String> fixedApprovalRoute() {
        return FIXED_APPROVAL_ROUTE;
    }

    public static String editorRoleCode() {
        return EDITOR_ROLE_CODE;
    }

    public static String departmentSupervisorRoleCode() {
        return DEPARTMENT_SUPERVISOR_ROLE_CODE;
    }

    public static String gaoxinApproverRoleCode() {
        return GAOXIN_APPROVER_ROLE_CODE;
    }

    public static String frontstageViewerRoleCode() {
        return FRONTSTAGE_VIEWER_ROLE_CODE;
    }
}
