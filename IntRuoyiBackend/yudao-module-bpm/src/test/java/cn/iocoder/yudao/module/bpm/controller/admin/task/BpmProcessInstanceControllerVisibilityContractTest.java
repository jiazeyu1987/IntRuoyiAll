package cn.iocoder.yudao.module.bpm.controller.admin.task;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BpmProcessInstanceControllerVisibilityContractTest {

    private static final Path CONTROLLER_PATH = Path.of(
            "src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/task/BpmProcessInstanceController.java");
    private static final Path ERROR_CODE_PATH = Path.of(
            "src/main/java/cn/iocoder/yudao/module/bpm/enums/ErrorCodeConstants.java");

    @Test
    void fullDetailRoutesRequireBpmAdminOrParticipant() throws Exception {
        String source = Files.readString(CONTROLLER_PATH, StandardCharsets.UTF_8);

        assertTrue(source.contains("PermissionApi"));
        assertTrue(source.contains("RoleCodeEnum.BPM_ADMIN.getCode()"));
        assertTrue(source.contains(
                "hasAnyRolesOrSuperAdmin(loginUserId, RoleCodeEnum.BPM_ADMIN.getCode())"));
        assertTrue(source.contains("assertCanReadProcessInstance(getLoginUserId(), processInstance)"));
        assertTrue(source.contains("assertCanReadProcessInstance(getLoginUserId(), historicProcessInstance)"));
        assertTrue(source.contains("assertCanReadProcessInstance(getLoginUserId(), id)"));
        assertTrue(source.contains("hasTaskParticipant(loginUserId, processInstance.getId())"));
    }

    @Test
    void approvalDetailKeepsDefinitionPreviewUngated() throws Exception {
        String source = Files.readString(CONTROLLER_PATH, StandardCharsets.UTF_8);

        assertTrue(source.contains("if (StrUtil.isNotBlank(reqVO.getProcessInstanceId()))"));
        assertTrue(source.contains(
                "assertCanReadProcessInstance(getLoginUserId(), reqVO.getProcessInstanceId())"));
    }

    @Test
    void accessDeniedErrorCodeIsExplicit() throws Exception {
        String source = Files.readString(ERROR_CODE_PATH, StandardCharsets.UTF_8);

        assertTrue(source.contains("PROCESS_INSTANCE_ACCESS_DENIED"));
    }

}
