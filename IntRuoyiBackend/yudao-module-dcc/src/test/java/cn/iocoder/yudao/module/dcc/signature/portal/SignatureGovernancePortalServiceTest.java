package cn.iocoder.yudao.module.dcc.signature.portal;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyOverview;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyService;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalAdapterRegistry;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalAuthorizationOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalMetrics;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SignatureGovernancePortalServiceTest extends BaseMockitoUnitTest {

    @Mock
    private SignatureGovernancePolicyService policyService;
    @Mock
    private DccElectronicSignatureAuthorizationMapper authorizationMapper;

    @Test
    void overview_aggregatesRegisteredPortalAdaptersAndCurrentAuthorizationState() {
        when(authorizationMapper.selectByUserId(101L)).thenReturn(enabledAuthorization(101L));
        when(policyService.describeModule(SignatureGovernanceModuleCode.DCC)).thenReturn(readyPolicy(
                SignatureGovernanceModuleCode.DCC));
        when(policyService.describeModule(SignatureGovernanceModuleCode.EDHR)).thenReturn(readyPolicy(
                SignatureGovernanceModuleCode.EDHR));

        SignatureGovernancePortalServiceImpl service = new SignatureGovernancePortalServiceImpl(
                policyService,
                new SignatureGovernancePortalAdapterRegistry(List.of(
                        new TestPortalAdapter(SignatureGovernanceModuleCode.DCC, "文件签名", "受控文件签名与授权",
                                "/signature-governance/file-signatures", "文件签名记录",
                                "/signature-governance/authorizations", "用户授权",
                                7L, 12L),
                        new TestPortalAdapter(SignatureGovernanceModuleCode.EDHR, "批记录签名", "批记录签名与工作任务",
                                "/signature-governance/batch-signatures", "批记录签名记录",
                                "/mes/pro/feedback/edhr-work-task", "工作任务",
                                5L, 9L))),
                authorizationMapper);

        SignatureGovernancePortalOverview overview = service.getOverview(101L);

        assertEquals("READY", overview.status());
        assertTrue(overview.ready());
        assertEquals("ENABLED", overview.authorization().status());
        assertTrue(overview.authorization().enabled());
        assertTrue(overview.authorization().blockers().isEmpty());
        assertEquals(2L, overview.summary().moduleTotal());
        assertEquals(2L, overview.summary().readyModuleTotal());
        assertEquals(0L, overview.summary().blockedModuleTotal());
        assertEquals(12L, overview.summary().pendingTotal());
        assertEquals(21L, overview.summary().signatureTotal());
        assertEquals(2, overview.modules().size());
        assertEquals("/signature-governance/file-signatures", overview.modules().get(0).routes().primaryPath());
        assertEquals("/mes/pro/feedback/edhr-work-task", overview.modules().get(1).routes().secondaryPath());
        assertTrue(overview.modules().stream().allMatch(module -> module.ready()));
    }

    @Test
    void overview_marksDisabledAuthorizationAsBlockedAndKeepsModuleCardsVisible() {
        when(authorizationMapper.selectByUserId(101L)).thenReturn(disabledAuthorization(101L));
        when(policyService.describeModule(SignatureGovernanceModuleCode.DCC)).thenReturn(readyPolicy(
                SignatureGovernanceModuleCode.DCC));

        SignatureGovernancePortalServiceImpl service = new SignatureGovernancePortalServiceImpl(
                policyService,
                new SignatureGovernancePortalAdapterRegistry(List.of(new TestPortalAdapter(
                        SignatureGovernanceModuleCode.DCC, "文件签名", "受控文件签名与授权",
                        "/signature-governance/file-signatures", "文件签名记录",
                        "/signature-governance/authorizations", "用户授权",
                        3L, 4L))),
                authorizationMapper);

        SignatureGovernancePortalOverview overview = service.getOverview(101L);

        assertEquals("BLOCKED", overview.status());
        assertFalse(overview.ready());
        assertEquals("DISABLED", overview.authorization().status());
        assertFalse(overview.authorization().enabled());
        assertEquals("SIGNATURE_AUTH_DISABLED", overview.authorization().blockers().get(0).code());
        assertEquals("BLOCKED", overview.modules().get(0).status());
        assertEquals("SIGNATURE_AUTH_DISABLED", overview.modules().get(0).blockers().get(0).code());
    }

    @Test
    void overview_supportsFuturePortalAdaptersWithoutHardcodingModuleCodes() {
        when(authorizationMapper.selectByUserId(101L)).thenReturn(enabledAuthorization(101L));
        when(policyService.describeModule(SignatureGovernanceModuleCode.SHOWROOM)).thenReturn(readyPolicy(
                SignatureGovernanceModuleCode.SHOWROOM));

        SignatureGovernancePortalServiceImpl service = new SignatureGovernancePortalServiceImpl(
                policyService,
                new SignatureGovernancePortalAdapterRegistry(List.of(new TestPortalAdapter(
                        SignatureGovernanceModuleCode.SHOWROOM, "展厅物料签名", "展厅物料签名",
                        "/showroom/admin/product/signatures", "签名管理",
                        "/showroom/admin/product/approval-tasks", "审批待办",
                        1L, 1L))),
                authorizationMapper);

        SignatureGovernancePortalOverview overview = service.getOverview(101L);

        assertEquals(1, overview.modules().size());
        assertEquals(SignatureGovernanceModuleCode.SHOWROOM, overview.modules().get(0).moduleCode());
        assertEquals("展厅物料签名", overview.modules().get(0).moduleName());
    }

    @Test
    void overview_failsFastWhenLoginUserIdIsMissing() {
        SignatureGovernancePortalServiceImpl service = new SignatureGovernancePortalServiceImpl(
                policyService,
                new SignatureGovernancePortalAdapterRegistry(List.of()),
                authorizationMapper);

        assertThrows(IllegalArgumentException.class, () -> service.getOverview(null));
    }

    private static SignatureGovernancePolicyOverview readyPolicy(SignatureGovernanceModuleCode moduleCode) {
        return new SignatureGovernancePolicyOverview(moduleCode, true, true, true,
                "policy-v1", moduleCode.name().toLowerCase() + "-policy-source",
                moduleCode.name().toLowerCase() + "-governance-adapter",
                moduleCode.name().toLowerCase() + "-adapter-v1",
                moduleCode.name().toLowerCase() + "-evidence-v1",
                List.of());
    }

    private static DccElectronicSignatureAuthorizationDO enabledAuthorization(Long userId) {
        return DccElectronicSignatureAuthorizationDO.builder()
                .userId(userId)
                .electronicSignatureEnabled(true)
                .authorizationState("ENABLED")
                .failureCount(0)
                .build();
    }

    private static DccElectronicSignatureAuthorizationDO disabledAuthorization(Long userId) {
        return DccElectronicSignatureAuthorizationDO.builder()
                .userId(userId)
                .electronicSignatureEnabled(false)
                .authorizationState("DISABLED")
                .failureCount(0)
                .build();
    }

    private static final class TestPortalAdapter implements SignatureGovernancePortalAdapter {

        private final SignatureGovernanceModuleCode moduleCode;
        private final String moduleName;
        private final String moduleDescription;
        private final String primaryPath;
        private final String primaryLabel;
        private final String secondaryPath;
        private final String secondaryLabel;
        private final Long pendingCount;
        private final Long signatureCount;

        private TestPortalAdapter(SignatureGovernanceModuleCode moduleCode, String moduleName,
                                  String moduleDescription, String primaryPath, String primaryLabel,
                                  String secondaryPath, String secondaryLabel, Long pendingCount,
                                  Long signatureCount) {
            this.moduleCode = moduleCode;
            this.moduleName = moduleName;
            this.moduleDescription = moduleDescription;
            this.primaryPath = primaryPath;
            this.primaryLabel = primaryLabel;
            this.secondaryPath = secondaryPath;
            this.secondaryLabel = secondaryLabel;
            this.pendingCount = pendingCount;
            this.signatureCount = signatureCount;
        }

        @Override
        public SignatureGovernanceModuleCode getModuleCode() {
            return moduleCode;
        }

        @Override
        public String getModuleName() {
            return moduleName;
        }

        @Override
        public String getModuleDescription() {
            return moduleDescription;
        }

        @Override
        public String getPrimaryRouteLabel() {
            return primaryLabel;
        }

        @Override
        public String getPrimaryRoute() {
            return primaryPath;
        }

        @Override
        public String getSecondaryRouteLabel() {
            return secondaryLabel;
        }

        @Override
        public String getSecondaryRoute() {
            return secondaryPath;
        }

        @Override
        public SignatureGovernancePortalMetrics describeMetrics(Long userId) {
            return SignatureGovernancePortalMetrics.of(pendingCount, signatureCount);
        }
    }
}
