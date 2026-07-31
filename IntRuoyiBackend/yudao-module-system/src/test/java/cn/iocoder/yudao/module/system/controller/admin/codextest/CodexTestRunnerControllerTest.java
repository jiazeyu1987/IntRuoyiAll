package cn.iocoder.yudao.module.system.controller.admin.codextest;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerStatusRespVO;
import cn.iocoder.yudao.module.system.service.codextest.CodexTestArtifactService;
import cn.iocoder.yudao.module.system.service.codextest.CodexTestRunnerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodexTestRunnerControllerTest {

    private static final String RUNNER_TOKEN = "runner-token";
    private static final long MANAGEMENT_TENANT_ID = 1L;

    @Mock
    private CodexTestRunnerService codexTestRunnerService;
    @Mock
    private CodexTestArtifactService codexTestArtifactService;
    @InjectMocks
    private CodexTestRunnerController controller;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void registerRunner_bindsManagementTenantHeaderDuringServiceCall() {
        CodexTestRunnerRegisterReqVO reqVO = new CodexTestRunnerRegisterReqVO();
        CodexTestRunnerRegisterRespVO respVO = new CodexTestRunnerRegisterRespVO();
        respVO.setRunnerSessionId(10L);
        when(codexTestRunnerService.registerRunner(eq(reqVO), eq(RUNNER_TOKEN))).thenAnswer(invocation -> {
            assertEquals(MANAGEMENT_TENANT_ID, TenantContextHolder.getRequiredTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            return respVO;
        });

        CommonResult<CodexTestRunnerRegisterRespVO> result =
                controller.registerRunner(reqVO, RUNNER_TOKEN, MANAGEMENT_TENANT_ID);

        assertEquals(0, result.getCode());
        assertEquals(10L, result.getData().getRunnerSessionId());
    }

    @Test
    void registerRunner_withoutManagementTenantFailsFast() {
        CodexTestRunnerRegisterReqVO reqVO = new CodexTestRunnerRegisterReqVO();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> controller.registerRunner(reqVO, RUNNER_TOKEN, null));

        assertEquals(CODEX_TEST_RESULT_SCHEMA_INVALID.getCode(), ex.getCode());
    }

    @Test
    void getRunnerStatus_returnsDiagnosticStatus() {
        CodexTestRunnerStatusRespVO respVO = new CodexTestRunnerStatusRespVO();
        respVO.setOnline(false);
        respVO.setStatus("STALE");
        respVO.setMessage("Runner 心跳已过期");
        when(codexTestRunnerService.getRunnerStatus()).thenReturn(respVO);

        CommonResult<CodexTestRunnerStatusRespVO> result = controller.getRunnerStatus();

        assertEquals(0, result.getCode());
        assertEquals("STALE", result.getData().getStatus());
        assertEquals("Runner 心跳已过期", result.getData().getMessage());
    }

}
