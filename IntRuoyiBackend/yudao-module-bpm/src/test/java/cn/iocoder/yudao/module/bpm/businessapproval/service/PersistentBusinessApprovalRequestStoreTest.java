package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(PersistentBusinessApprovalRequestStore.class)
class PersistentBusinessApprovalRequestStoreTest extends BaseDbUnitTest {

    @Resource
    private PersistentBusinessApprovalRequestStore requestStore;

    @Resource
    private BusinessApprovalRequestMapper requestMapper;

    @Test
    void createPendingPersistsContextAndFindsByProcessInstanceId() {
        BusinessApprovalContext context = BusinessApprovalPolicyResolveServiceTest.baseContext().build();
        BusinessApprovalPolicy policy = BusinessApprovalPolicyResolveServiceTest.basePolicy().build();

        BusinessApprovalRequest request = requestStore.createPendingRequest(context, policy);
        BusinessApprovalRequest attached = requestStore.attachProcessInstance(request.getRequestId(), "bpm-1001");

        assertNotNull(attached.getRequestId());
        BusinessApprovalRequestDO requestDO = requestMapper.selectById(attached.getRequestId());
        assertEquals("PENDING_BPM", requestDO.getRequestStatus());
        assertEquals("bpm-1001", requestDO.getProcessInstanceId());
        assertTrue(requestDO.getBusinessContextJson().contains("publish route version"));

        Optional<BusinessApprovalRequest> found = requestStore.findByProcessInstanceId("bpm-1001");
        assertTrue(found.isPresent());
        assertEquals(context.getTenantId(), found.get().getContext().getTenantId());
        assertEquals(context.getObjectId(), found.get().getContext().getObjectId());
        assertEquals(BusinessApprovalRequestStatus.PENDING_BPM, found.get().getStatus());
    }

    @Test
    void createPendingFailsFastWhenSameBusinessActionIsPending() {
        BusinessApprovalContext context = BusinessApprovalPolicyResolveServiceTest.baseContext().build();
        BusinessApprovalPolicy policy = BusinessApprovalPolicyResolveServiceTest.basePolicy().build();
        requestStore.createPendingRequest(context, policy);

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> requestStore.createPendingRequest(context, policy));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PENDING_CONFLICT, ex.getErrorCode());
    }

    @Test
    void terminalUpdatePersistsResultAndReleasesPendingLock() {
        BusinessApprovalContext context = BusinessApprovalPolicyResolveServiceTest.baseContext().build();
        BusinessApprovalPolicy policy = BusinessApprovalPolicyResolveServiceTest.basePolicy().build();
        BusinessApprovalRequest request = requestStore.createPendingRequest(context, policy);
        BusinessApprovalRequest attached = requestStore.attachProcessInstance(request.getRequestId(), "bpm-1002");

        BusinessApprovalRequest approved = attached.withTerminalEvent(BusinessApprovalRequestStatus.APPROVED,
                "bpm-1002:completed", BusinessApprovalEffectResult.completed("PUBLISHED"));
        requestStore.update(approved);

        assertFalse(requestStore.hasPendingRequest(context));
        BusinessApprovalRequest found = requestStore.findByProcessInstanceId("bpm-1002").orElseThrow();
        assertEquals(BusinessApprovalRequestStatus.APPROVED, found.getStatus());
        assertEquals("bpm-1002:completed", found.getLastEventKey());
        assertEquals("PUBLISHED", found.getResultState());
    }

    @Test
    void directRequestDoesNotOccupyPendingLock() {
        BusinessApprovalContext context = BusinessApprovalPolicyResolveServiceTest.baseContext().build();
        BusinessApprovalPolicy policy = BusinessApprovalPolicyResolveServiceTest.basePolicy()
                .mode(cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode.DIRECT)
                .processDefinitionKey(null)
                .build();

        BusinessApprovalRequest request = requestStore.createDirectRequest(context, policy);
        requestStore.update(request.withStatus(BusinessApprovalRequestStatus.DIRECT_EXECUTED,
                BusinessApprovalEffectResult.completed("PUBLISHED")));

        assertFalse(requestStore.hasPendingRequest(context));
        BusinessApprovalRequestDO requestDO = requestMapper.selectById(request.getRequestId());
        assertEquals("DIRECT_EXECUTED", requestDO.getRequestStatus());
        assertEquals("PUBLISHED", requestDO.getResultState());
    }

}
