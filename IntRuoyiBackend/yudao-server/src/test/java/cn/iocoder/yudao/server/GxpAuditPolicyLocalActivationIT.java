package cn.iocoder.yudao.server;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyActivationCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyActivationResult;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyActivationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = YudaoServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.quartz.auto-startup=false",
                "spring.task.scheduling.enabled=false",
                "spring.main.lazy-initialization=true"
        })
@ActiveProfiles("local")
class GxpAuditPolicyLocalActivationIT {

    private static final long TENANT_ID = 1L;
    private static final long ACTOR_ID = 1L;
    private static final String POLICY_VERSION = "2026-09-approved-02";
    private static final String APPROVAL_REFERENCE = "CODEX-IMPLEMENTATION-20260908";
    private static final String COVERAGE_REPORT_HASH =
            "5f1fa67f533bab3a9814ffbc9180016c243559ec597148a37af43988a0900a20";

    @Resource
    private GxpAuditPolicyActivationService activationService;

    @BeforeEach
    void setContext() {
        TenantContextHolder.setTenantId(TENANT_ID);
        LoginUser loginUser = new LoginUser();
        loginUser.setId(ACTOR_ID);
        loginUser.setTenantId(TENANT_ID);
        loginUser.setInfo(Map.of("username", "admin", LoginUser.INFO_KEY_NICKNAME, "瑛泰管理员"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterAll
    static void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void activateLocalApprovedPolicyThroughService() {
        GxpAuditPolicyActivationResult result = activationService.activate(
                new GxpAuditPolicyActivationCommand(
                        TENANT_ID,
                        ACTOR_ID,
                        APPROVAL_REFERENCE,
                        "gxp20-local-policy-activation-20260926",
                        COVERAGE_REPORT_HASH));
        assertEquals(POLICY_VERSION, result.policyVersion());
    }
}
