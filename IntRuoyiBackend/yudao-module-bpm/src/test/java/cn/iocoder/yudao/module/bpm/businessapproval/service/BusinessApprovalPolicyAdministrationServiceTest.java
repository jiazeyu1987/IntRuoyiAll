package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordResult;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyResolution;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySwitchModeReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import({
        BusinessApprovalPolicyAdministrationService.class,
        PersistentBusinessApprovalPolicyResolveService.class,
        BusinessApprovalPolicyAdministrationServiceTest.Config.class
})
class BusinessApprovalPolicyAdministrationServiceTest extends BaseDbUnitTest {

    @Resource
    private BusinessApprovalPolicyAdministrationService policyService;

    @Resource
    private PersistentBusinessApprovalPolicyResolveService resolveService;

    @Resource
    private BusinessApprovalPolicyMapper policyMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ApprovalSignatureRecordService signatureRecordService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(122L);
        reset(adminUserApi);
        reset(signatureRecordService);
        when(signatureRecordService.recordReviewSignature(any())).thenReturn(ApprovalSignatureRecordResult.builder()
                .recordId(9001L)
                .signatureImageId(8001L)
                .signatureImageFileUrl("http://signature.local/policy-switch.png")
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void savePolicyStoresDraftWithTenantAndExplicitMode() {
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(directReq());

        BusinessApprovalPolicyDO policyDO = policyMapper.selectById(respVO.getId());
        assertEquals(122L, policyDO.getTenantId());
        assertEquals(BusinessApprovalPolicyMode.DIRECT.name(), policyDO.getPolicyMode());
        assertEquals("DRAFT", policyDO.getStatus());
    }

    @Test
    void publishDirectPolicyDoesNotRequireProcessDefinitionKey() {
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(directReq());

        policyService.publishPolicy(respVO.getId());

        BusinessApprovalPolicyDO policyDO = policyMapper.selectById(respVO.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, policyDO.getStatus());
        assertEquals(null, policyDO.getProcessDefinitionKey());
    }

    @Test
    void publishFormTemplateUpgradeDirectPolicyFailsFastBecauseUpgradeRequiresBpm() {
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(
                formTemplateUpgradeReq(BusinessApprovalPolicyMode.DIRECT));

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> policyService.publishPolicy(respVO.getId()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID, ex.getErrorCode());
        BusinessApprovalPolicyDO policyDO = policyMapper.selectById(respVO.getId());
        assertEquals("DRAFT", policyDO.getStatus());
    }

    @Test
    void publishSignatureRequiredPolicyDoesNotRequireProcessDefinitionKey() {
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(signatureRequiredReq());

        policyService.publishPolicy(respVO.getId());

        BusinessApprovalPolicyDO policyDO = policyMapper.selectById(respVO.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, policyDO.getStatus());
        assertEquals(BusinessApprovalPolicyMode.SIGNATURE_REQUIRED.name(), policyDO.getPolicyMode());
        assertEquals(null, policyDO.getProcessDefinitionKey());
    }

    @Test
    void disablePublishedPolicyRemovesItFromResolution() {
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(bpmRequiredReq("mes-route-version-approval-v1"));
        policyService.publishPolicy(respVO.getId());

        policyService.disablePolicy(respVO.getId());

        BusinessApprovalPolicyDO policyDO = policyMapper.selectById(respVO.getId());
        assertEquals("DISABLED", policyDO.getStatus());
        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> resolveService.resolve(BusinessApprovalPolicyResolveServiceTest.baseContext().build()));
        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void publishBpmRequiredPolicyRequiresProcessDefinitionKey() {
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(bpmRequiredReq(null));

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> policyService.publishPolicy(respVO.getId()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING, ex.getErrorCode());
        assertEquals("DRAFT", policyMapper.selectById(respVO.getId()).getStatus());
    }

    @Test
    void publishFailsWhenExecutorIsNotRegistered() {
        BusinessApprovalPolicySaveReqVO reqVO = directReq();
        reqVO.setEffectExecutorCode("UNKNOWN_EXECUTOR");
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(reqVO);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> policyService.publishPolicy(respVO.getId()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EXECUTOR_MISSING, ex.getErrorCode());
    }

    @Test
    void publishFailsWhenSameActionAlreadyHasPublishedPolicy() {
        policyService.publishPolicy(policyService.savePolicy(directReq()).getId());
        BusinessApprovalPolicyRespVO second = policyService.savePolicy(directReq());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> policyService.publishPolicy(second.getId()));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_CONFLICT, ex.getErrorCode());
    }

    @Test
    void persistentResolveReadsOnlyPublishedPolicy() {
        BusinessApprovalPolicyRespVO respVO = policyService.savePolicy(bpmRequiredReq("mes-route-version-approval-v1"));
        BusinessApprovalException beforePublish = assertThrows(BusinessApprovalException.class,
                () -> resolveService.resolve(BusinessApprovalPolicyResolveServiceTest.baseContext().build()));
        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_NOT_FOUND, beforePublish.getErrorCode());

        policyService.publishPolicy(respVO.getId());

        BusinessApprovalPolicyResolution resolution =
                resolveService.resolve(BusinessApprovalPolicyResolveServiceTest.baseContext().build());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED, resolution.getMode());
        assertEquals("mes-route-version-approval-v1", resolution.getProcessDefinitionKey());
    }

    @Test
    void switchPublishedBpmPolicyToDirectDisablesOldPolicyAndPublishesDirectPolicy() {
        BusinessApprovalPolicyRespVO published = policyService.savePolicy(
                bpmRequiredReq("mes-route-version-approval-v1"));
        policyService.publishPolicy(published.getId());

        BusinessApprovalPolicyRespVO switched = policyService.switchPolicyMode(100L, published.getId(),
                switchModeReq(BusinessApprovalPolicyMode.DIRECT));

        BusinessApprovalPolicyDO oldPolicy = policyMapper.selectById(published.getId());
        BusinessApprovalPolicyDO newPolicy = policyMapper.selectById(switched.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_DISABLED, oldPolicy.getStatus());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, newPolicy.getStatus());
        assertEquals(BusinessApprovalPolicyMode.DIRECT.name(), newPolicy.getPolicyMode());
        assertEquals("mes-route-version-approval-v1", newPolicy.getProcessDefinitionKey());

        BusinessApprovalPolicyResolution resolution =
                resolveService.resolve(BusinessApprovalPolicyResolveServiceTest.baseContext().build());
        assertEquals(BusinessApprovalPolicyMode.DIRECT, resolution.getMode());
        verifyPolicySwitchSignatureRecorded(newPolicy, BusinessApprovalPolicyMode.DIRECT);
    }

    @Test
    void switchPublishedFormTemplateUpgradeBpmPolicyToDirectFailsFastAndKeepsOldPolicyPublished() {
        BusinessApprovalPolicyRespVO published = policyService.savePolicy(
                formTemplateUpgradeReq(BusinessApprovalPolicyMode.BPM_REQUIRED));
        policyService.publishPolicy(published.getId());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> policyService.switchPolicyMode(100L, published.getId(),
                        switchModeReq(BusinessApprovalPolicyMode.DIRECT)));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID, ex.getErrorCode());
        BusinessApprovalPolicyDO oldPolicy = policyMapper.selectById(published.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, oldPolicy.getStatus());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED.name(), oldPolicy.getPolicyMode());
        verifyNoInteractions(adminUserApi);
        verifyNoInteractions(signatureRecordService);
    }

    @Test
    void switchPublishedDirectPolicyWithInheritedProcessKeyToBpmPublishesBpmPolicyAndRecordsSignature() {
        BusinessApprovalPolicyRespVO published = policyService.savePolicy(
                bpmRequiredReq("mes-route-version-approval-v1"));
        policyService.publishPolicy(published.getId());
        BusinessApprovalPolicyRespVO direct = policyService.switchPolicyMode(100L, published.getId(),
                switchModeReq(BusinessApprovalPolicyMode.DIRECT));
        clearInvocations(adminUserApi, signatureRecordService);

        BusinessApprovalPolicyRespVO switched = policyService.switchPolicyMode(100L, direct.getId(),
                switchModeReq(BusinessApprovalPolicyMode.BPM_REQUIRED));

        BusinessApprovalPolicyDO oldPolicy = policyMapper.selectById(direct.getId());
        BusinessApprovalPolicyDO newPolicy = policyMapper.selectById(switched.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_DISABLED, oldPolicy.getStatus());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, newPolicy.getStatus());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED.name(), newPolicy.getPolicyMode());
        assertEquals("mes-route-version-approval-v1", newPolicy.getProcessDefinitionKey());

        BusinessApprovalPolicyResolution resolution =
                resolveService.resolve(BusinessApprovalPolicyResolveServiceTest.baseContext().build());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED, resolution.getMode());
        assertEquals("mes-route-version-approval-v1", resolution.getProcessDefinitionKey());
        verifyPolicySwitchSignatureRecorded(newPolicy, BusinessApprovalPolicyMode.BPM_REQUIRED);
    }

    @Test
    void switchPublishedDirectPolicyWithoutCurrentProcessKeyInheritsLatestHistoricalBpmKey() {
        BusinessApprovalPolicyRespVO historicalBpm = policyService.savePolicy(
                bpmRequiredReq("mes-route-version-approval-v1"));
        policyService.publishPolicy(historicalBpm.getId());
        policyService.disablePolicy(historicalBpm.getId());
        BusinessApprovalPolicyRespVO direct = policyService.savePolicy(directReq());
        policyService.publishPolicy(direct.getId());

        BusinessApprovalPolicyRespVO switched = policyService.switchPolicyMode(100L, direct.getId(),
                switchModeReq(BusinessApprovalPolicyMode.BPM_REQUIRED));

        BusinessApprovalPolicyDO oldPolicy = policyMapper.selectById(direct.getId());
        BusinessApprovalPolicyDO newPolicy = policyMapper.selectById(switched.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_DISABLED, oldPolicy.getStatus());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, newPolicy.getStatus());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED.name(), newPolicy.getPolicyMode());
        assertEquals("mes-route-version-approval-v1", newPolicy.getProcessDefinitionKey());
        verifyPolicySwitchSignatureRecorded(newPolicy, BusinessApprovalPolicyMode.BPM_REQUIRED);
    }

    @Test
    void switchPublishedDirectPolicyWithoutHistoryUsesExecutorBpmProcessDefinitionKey() {
        BusinessApprovalPolicyRespVO direct = policyService.savePolicy(directReq());
        policyService.publishPolicy(direct.getId());

        BusinessApprovalPolicyRespVO switched = policyService.switchPolicyMode(100L, direct.getId(),
                switchModeReq(BusinessApprovalPolicyMode.BPM_REQUIRED));

        BusinessApprovalPolicyDO oldPolicy = policyMapper.selectById(direct.getId());
        BusinessApprovalPolicyDO newPolicy = policyMapper.selectById(switched.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_DISABLED, oldPolicy.getStatus());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, newPolicy.getStatus());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED.name(), newPolicy.getPolicyMode());
        assertEquals("mes-route-version-approval-v1", newPolicy.getProcessDefinitionKey());
        verifyPolicySwitchSignatureRecorded(newPolicy, BusinessApprovalPolicyMode.BPM_REQUIRED);
    }

    @Test
    void switchPublishedDirectPolicyToBpmRequiresRecoverableProcessDefinitionKeyAndKeepsOldPolicyPublished() {
        BusinessApprovalPolicySaveReqVO directReq = directReq();
        directReq.setEffectExecutorCode("UNCONFIGURED_EXECUTOR");
        BusinessApprovalPolicyRespVO published = policyService.savePolicy(directReq);
        policyService.publishPolicy(published.getId());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> policyService.switchPolicyMode(100L, published.getId(),
                        switchModeReq(BusinessApprovalPolicyMode.BPM_REQUIRED)));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING, ex.getErrorCode());
        BusinessApprovalPolicyDO oldPolicy = policyMapper.selectById(published.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, oldPolicy.getStatus());
        assertEquals(BusinessApprovalPolicyMode.DIRECT.name(), oldPolicy.getPolicyMode());
        verifyNoInteractions(adminUserApi);
        verifyNoInteractions(signatureRecordService);
    }

    @Test
    void switchPublishedPolicyRequiresElectronicSignaturePasswordAndKeepsOldPolicyPublished() {
        BusinessApprovalPolicyRespVO published = policyService.savePolicy(
                bpmRequiredReq("mes-route-version-approval-v1"));
        policyService.publishPolicy(published.getId());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> policyService.switchPolicyMode(100L, published.getId(),
                        switchModeReq(BusinessApprovalPolicyMode.DIRECT, " ")));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_SIGNATURE_PASSWORD_REQUIRED, ex.getErrorCode());
        BusinessApprovalPolicyDO oldPolicy = policyMapper.selectById(published.getId());
        assertEquals(BusinessApprovalPolicy.STATUS_PUBLISHED, oldPolicy.getStatus());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED.name(), oldPolicy.getPolicyMode());
        verifyNoInteractions(adminUserApi);
        verifyNoInteractions(signatureRecordService);
    }

    private void verifyPolicySwitchSignatureRecorded(BusinessApprovalPolicyDO newPolicy,
            BusinessApprovalPolicyMode targetMode) {
        verify(adminUserApi).validatePassword(100L, "signature-pass");
        ArgumentCaptor<ApprovalTaskReviewContext> captor =
                ArgumentCaptor.forClass(ApprovalTaskReviewContext.class);
        verify(signatureRecordService).recordReviewSignature(captor.capture());
        ApprovalTaskReviewContext context = captor.getValue();
        assertEquals(100L, context.getLoginUserId());
        assertEquals(ApprovalModuleCode.BPM, context.getModuleCode());
        assertEquals("BUSINESS_APPROVAL_POLICY_SWITCH", context.getSourceTaskType());
        assertEquals(String.valueOf(newPolicy.getId()), context.getSourceTaskId());
        assertEquals("MES:ROUTE_VERSION:PUBLISH:READY_TO_PUBLISH", context.getBusinessKey());
        assertEquals(null, context.getProcessInstanceId());
        assertEquals(ApprovalTaskReviewResult.APPROVE, context.getResult());
        assertEquals(targetMode.name(), context.getReason());
        assertEquals("signature-pass", context.getSignaturePassword());
        assertEquals(false, context.isGlobalView());
    }

    private BusinessApprovalPolicySaveReqVO directReq() {
        BusinessApprovalPolicySaveReqVO reqVO = baseReq();
        reqVO.setPolicyMode(BusinessApprovalPolicyMode.DIRECT.name());
        reqVO.setProcessDefinitionKey(null);
        return reqVO;
    }

    private BusinessApprovalPolicySaveReqVO bpmRequiredReq(String processDefinitionKey) {
        BusinessApprovalPolicySaveReqVO reqVO = baseReq();
        reqVO.setPolicyMode(BusinessApprovalPolicyMode.BPM_REQUIRED.name());
        reqVO.setProcessDefinitionKey(processDefinitionKey);
        return reqVO;
    }

    private BusinessApprovalPolicySaveReqVO signatureRequiredReq() {
        BusinessApprovalPolicySaveReqVO reqVO = baseReq();
        reqVO.setPolicyMode(BusinessApprovalPolicyMode.SIGNATURE_REQUIRED.name());
        reqVO.setProcessDefinitionKey(null);
        return reqVO;
    }

    private BusinessApprovalPolicySaveReqVO formTemplateUpgradeReq(BusinessApprovalPolicyMode mode) {
        BusinessApprovalPolicySaveReqVO reqVO = new BusinessApprovalPolicySaveReqVO();
        reqVO.setDataDomain("FORM_CENTER");
        reqVO.setSystemCode("FORM_CENTER");
        reqVO.setObjectType("FORM_TEMPLATE");
        reqVO.setActionCode("UPGRADE");
        reqVO.setObjectState("DRAFT");
        reqVO.setPolicyMode(mode.name());
        reqVO.setProcessDefinitionKey(
                mode == BusinessApprovalPolicyMode.BPM_REQUIRED ? "form-template-upgrade-v1" : null);
        reqVO.setEffectExecutorCode("FORM_TEMPLATE_UPGRADE");
        reqVO.setRemark("form template upgrade");
        return reqVO;
    }

    private BusinessApprovalPolicySwitchModeReqVO switchModeReq(BusinessApprovalPolicyMode mode) {
        return switchModeReq(mode, "signature-pass");
    }

    private BusinessApprovalPolicySwitchModeReqVO switchModeReq(BusinessApprovalPolicyMode mode,
            String signaturePassword) {
        BusinessApprovalPolicySwitchModeReqVO reqVO = new BusinessApprovalPolicySwitchModeReqVO();
        reqVO.setPolicyMode(mode.name());
        reqVO.setSignaturePassword(signaturePassword);
        return reqVO;
    }

    private BusinessApprovalPolicySaveReqVO baseReq() {
        BusinessApprovalPolicySaveReqVO reqVO = new BusinessApprovalPolicySaveReqVO();
        reqVO.setDataDomain("MES");
        reqVO.setSystemCode("MES");
        reqVO.setObjectType("ROUTE_VERSION");
        reqVO.setActionCode("PUBLISH");
        reqVO.setObjectState("READY_TO_PUBLISH");
        reqVO.setEffectExecutorCode("MES_ROUTE_VERSION_PUBLISH");
        reqVO.setRemark("route version publish");
        return reqVO;
    }

    @TestConfiguration
    static class Config {

        @Bean
        BusinessApprovalEffectExecutorRegistry businessApprovalEffectExecutorRegistry() {
            return new BusinessApprovalEffectExecutorRegistry(List.of(
                    new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH", "mes-route-version-approval-v1"),
                    new RecordingExecutor("FORM_TEMPLATE_UPGRADE", "form-template-upgrade-v1"),
                    new RecordingExecutor("FORM_TEMPLATE_OBSOLETE", "form-template-obsolete-v1"),
                    new RecordingExecutor("UNCONFIGURED_EXECUTOR")));
        }

        @Bean
        AdminUserApi adminUserApi() {
            return mock(AdminUserApi.class);
        }

        @Bean
        ApprovalSignatureRecordService approvalSignatureRecordService() {
            return mock(ApprovalSignatureRecordService.class);
        }

    }

}
