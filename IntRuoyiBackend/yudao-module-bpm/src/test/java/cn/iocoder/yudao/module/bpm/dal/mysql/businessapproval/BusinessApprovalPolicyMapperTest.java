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

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
