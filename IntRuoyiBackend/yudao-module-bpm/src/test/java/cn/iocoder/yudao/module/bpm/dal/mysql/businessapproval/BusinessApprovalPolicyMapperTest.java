package cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessApprovalPolicyMapperTest extends BaseDbUnitTest {

    @Resource
    private BusinessApprovalPolicyMapper policyMapper;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(122L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void selectPageWithoutStatusFilterReturnsOnlyLatestPolicyVersionPerBusinessAction() {
        insertPolicy(BusinessApprovalPolicyMode.BPM_REQUIRED, BusinessApprovalPolicy.STATUS_DISABLED);
        insertPolicy(BusinessApprovalPolicyMode.DIRECT, BusinessApprovalPolicy.STATUS_DISABLED);
        BusinessApprovalPolicyDO latest =
                insertPolicy(BusinessApprovalPolicyMode.BPM_REQUIRED, BusinessApprovalPolicy.STATUS_PUBLISHED);

        BusinessApprovalPolicyPageReqVO defaultPageReq = new BusinessApprovalPolicyPageReqVO();
        defaultPageReq.setTenantId(122L);
        PageResult<BusinessApprovalPolicyDO> defaultPage = policyMapper.selectPage(defaultPageReq);

        assertEquals(1L, defaultPage.getTotal());
        assertEquals(latest.getId(), defaultPage.getList().get(0).getId());
        assertEquals(BusinessApprovalPolicyMode.BPM_REQUIRED.name(), defaultPage.getList().get(0).getPolicyMode());

        BusinessApprovalPolicyPageReqVO disabledPageReq = new BusinessApprovalPolicyPageReqVO();
        disabledPageReq.setTenantId(122L);
        disabledPageReq.setStatus(BusinessApprovalPolicy.STATUS_DISABLED);
        PageResult<BusinessApprovalPolicyDO> disabledPage = policyMapper.selectPage(disabledPageReq);

        assertEquals(2L, disabledPage.getTotal());
    }

    @Test
    void selectPublishedByActionReturnsAllObjectStatePolicyForConcreteState() {
        insertPolicy(BusinessApprovalPolicyMode.BPM_REQUIRED, BusinessApprovalPolicy.STATUS_PUBLISHED,
                BusinessApprovalPolicy.OBJECT_STATE_ALL);

        var policies = policyMapper.selectPublishedByAction(122L, "MES", "ROUTE", "ROUTE_VERSION",
                "PUBLISH", "READY_TO_PUBLISH");

        assertEquals(1, policies.size());
        assertEquals(BusinessApprovalPolicy.OBJECT_STATE_ALL, policies.get(0).getObjectState());
    }

    @Test
    void selectPageWithApprovalSwitchScopeReturnsTopLevelSwitchablePolicies() {
        insertPolicy("DCC", "DCC", "CONTROLLED_FILE", "PUBLISH", "READY_TO_PUBLISH",
                BusinessApprovalPolicyMode.BPM_REQUIRED, BusinessApprovalPolicy.STATUS_PUBLISHED,
                "dcc-controlled-file-approval", "DCC_PUBLISH");
        insertPolicy("FORM_CENTER", "FORM_CENTER", "FORM_TEMPLATE", "UPGRADE", "DRAFT",
                BusinessApprovalPolicyMode.DIRECT, BusinessApprovalPolicy.STATUS_PUBLISHED,
                "form-template-upgrade-v1", "FORM_TEMPLATE_UPGRADE");
        insertPolicy("MES", "MES", "BATCH_RECORD_VERSION", "PUBLISH", "PRECHECK_PASSED",
                BusinessApprovalPolicyMode.BPM_REQUIRED, BusinessApprovalPolicy.STATUS_PUBLISHED,
                "mes-batch-record-version-approval-v1", "MES_BATCH_RECORD_VERSION_PUBLISH");
        insertPolicy("MES", "MES", "EDHR_ROUTE_FORM", "EDHR_RF_542_FORM", "ACTIVE",
                BusinessApprovalPolicyMode.DIRECT, BusinessApprovalPolicy.STATUS_PUBLISHED,
                null, "MES_EDHR_ROUTE_FORM_FILL");

        BusinessApprovalPolicyPageReqVO reqVO = new BusinessApprovalPolicyPageReqVO();
        reqVO.setTenantId(122L);
        reqVO.setApprovalSwitchScope(true);

        PageResult<BusinessApprovalPolicyDO> page = policyMapper.selectPage(reqVO);

        Set<String> objectTypes = page.getList().stream()
                .map(BusinessApprovalPolicyDO::getObjectType)
                .collect(Collectors.toSet());
        assertEquals(3L, page.getTotal());
        assertTrue(objectTypes.contains("CONTROLLED_FILE"));
        assertTrue(objectTypes.contains("FORM_TEMPLATE"));
        assertTrue(objectTypes.contains("BATCH_RECORD_VERSION"));
        assertFalse(objectTypes.contains("EDHR_ROUTE_FORM"));
        assertTrue(page.getList().stream().anyMatch(policy ->
                "FORM_TEMPLATE".equals(policy.getObjectType())
                        && BusinessApprovalPolicyMode.DIRECT.name().equals(policy.getPolicyMode())));
    }

    private BusinessApprovalPolicyDO insertPolicy(BusinessApprovalPolicyMode policyMode, String status) {
        return insertPolicy(policyMode, status, "READY_TO_PUBLISH");
    }

    private BusinessApprovalPolicyDO insertPolicy(BusinessApprovalPolicyMode policyMode, String status,
            String objectState) {
        BusinessApprovalPolicyDO policy = BusinessApprovalPolicyDO.builder()
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("ROUTE")
                .objectType("ROUTE_VERSION")
                .actionCode("PUBLISH")
                .objectState(objectState)
                .policyMode(policyMode.name())
                .processDefinitionKey(BusinessApprovalPolicyMode.BPM_REQUIRED.equals(policyMode)
                        ? "mes-route-version-approval-v1" : null)
                .effectExecutorCode("ROUTE_VERSION_PUBLISH")
                .status(status)
                .build();
        policyMapper.insert(policy);
        return policy;
    }

    private BusinessApprovalPolicyDO insertPolicy(String dataDomain, String systemCode, String objectType,
            String actionCode, String objectState, BusinessApprovalPolicyMode policyMode, String status,
            String processDefinitionKey, String effectExecutorCode) {
        BusinessApprovalPolicyDO policy = BusinessApprovalPolicyDO.builder()
                .tenantId(122L)
                .dataDomain(dataDomain)
                .systemCode(systemCode)
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState(objectState)
                .policyMode(policyMode.name())
                .processDefinitionKey(processDefinitionKey)
                .effectExecutorCode(effectExecutorCode)
                .status(status)
                .build();
        policyMapper.insert(policy);
        return policy;
    }

}
