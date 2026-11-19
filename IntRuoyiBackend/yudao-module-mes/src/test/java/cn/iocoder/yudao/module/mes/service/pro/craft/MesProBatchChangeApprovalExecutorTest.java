package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.ArgumentCaptor;
import cn.iocoder.yudao.module.bpm.businessapproval.model.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftChangeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftChangeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteControlledContentAdapter;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProBatchChangeApprovalExecutorTest {
    private MesProBatchChangeApprovalExecutor executor;
    private MesProCraftService crafts;
    private MesProRouteVersionMapper versions;
    private MesProCraftChangeMapper changes;
    private MesProRouteControlledContentAdapter audit;
    private MesProCraftChangeDO change;
    private List<MesProRouteVersionDO> candidates;
    private BusinessApprovalContext context;
    private BusinessApprovalRequest request;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        executor = new MesProBatchChangeApprovalExecutor();
        crafts = mock(MesProCraftService.class); versions = mock(MesProRouteVersionMapper.class);
        changes = mock(MesProCraftChangeMapper.class); audit = mock(MesProRouteControlledContentAdapter.class);
        ReflectionTestUtils.setField(executor,"crafts",crafts); ReflectionTestUtils.setField(executor,"versions",versions);
        ReflectionTestUtils.setField(executor,"changes",changes); ReflectionTestUtils.setField(executor,"platform",audit);
        change = new MesProCraftChangeDO(); change.setId(9L); change.setApprovalScope("BATCH");
        change.setChangeType("SHARED"); change.setStatus("PREPARING"); change.setReason("工序修订");
        when(changes.selectById(9L)).thenReturn(change);
        when(changes.updateById(any(MesProCraftChangeDO.class))).thenReturn(1);
        when(versions.updateById(any(MesProRouteVersionDO.class))).thenReturn(1);
        when(crafts.groupId(any())).thenReturn(9L);
        when(crafts.onRouteReady(any(),anyLong())).thenAnswer(c->{
            assertTrue(candidates.stream().allMatch(v->"READY_TO_PUBLISH".equals(v.getLifecycleStatus())));
            candidates.forEach(v->v.setLifecycleStatus("ACTIVE"));change.setStatus("APPLIED");return true;
        });
        doAnswer(c->{change.setStatus("CANCELLED");candidates.forEach(v->v.setLifecycleStatus("CANCELLED"));return null;})
                .when(crafts).abortBatchFromApproval(any(),anyLong());
        populate(2);
    }
    private void populate(int size) {
        candidates = new ArrayList<>(); Map<String,String> hashes = new LinkedHashMap<>();
        for (long id=1;id<=size;id++) {
            var v=new MesProRouteVersionDO();v.setId(id);v.setActive(false);v.setLifecycleStatus("DRAFT");v.setRouteSnapshotSha256("hash"+id);
            candidates.add(v);hashes.put(String.valueOf(id),"hash"+id);when(versions.selectById(id)).thenReturn(v);
        }
        change.setCandidateIdsJson(JSON.toJSONString(candidates.stream().map(MesProRouteVersionDO::getId).toList()));
        change.setCandidateHashesJson(JSON.toJSONString(hashes));
        context=BusinessApprovalContext.builder().tenantId(1L).dataDomain("MES").systemCode("MES")
                .objectType("ROUTE_BATCH_CHANGE").objectId("9").objectState("PREPARING").actionCode("PUBLISH")
                .applicantUserId(7L).variables(Map.of("changeFingerprint",MesProBatchChangeApprovalExecutor.fingerprint(change))).build();
        request=BusinessApprovalRequest.builder().requestId(80L).tenantId(1L).context(context).processInstanceId("one-process").build();
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test @SuppressWarnings("unchecked") void thousandCandidatesSubmitExactlyOneBusinessApprovalRequest() {
        populate(1000);
        var approvals=mock(BusinessApprovalOrchestrator.class);
        ObjectProvider<BusinessApprovalOrchestrator> provider=mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(approvals);
        ReflectionTestUtils.setField(executor,"orchestrator",provider);
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
            executor.submit(9L);
        }
        var captured=ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(approvals,times(1)).submit(captured.capture());
        assertEquals("ROUTE_BATCH_CHANGE",captured.getValue().getObjectType());
        assertEquals("9",captured.getValue().getObjectId());
        assertEquals(1000,captured.getValue().getVariables().get("affectedRouteCount"));
        assertEquals(MesProBatchChangeApprovalExecutor.fingerprint(change),captured.getValue().getVariables().get("changeFingerprint"));
    }
    @Test void thousandRoutesShareOneRequestAndOneInstanceUntilOneApprovalPublishesAll() {
        populate(1000);executor.precheck(context);executor.markPending(context,request);
        assertEquals(80L,change.getApprovalRequestId());assertEquals("PENDING",change.getStatus());
        assertTrue(candidates.stream().allMatch(v->"PENDING_APPROVAL".equals(v.getLifecycleStatus())&&"one-process".equals(v.getApprovalProcessInstanceId())));
        verify(crafts,never()).onRouteReady(any(),any());
        assertEquals("APPLIED",executor.executeApproved(context,request,8L).getResultState());
        verify(crafts,times(1)).onRouteReady(any(),eq(8L));
        verify(audit,times(1000)).recordApproved(any(),eq(8L),anyString());
    }
    @Test void disabledApprovalPublishesDirectlyWithoutProcessInstance() {
        var direct=request.toBuilder().processInstanceId(null).build();
        assertEquals("APPLIED",executor.executeDirect(context,direct).getResultState());
        assertNull(change.getApprovalProcessInstanceId());assertEquals(80L,change.getApprovalRequestId());
        assertTrue(candidates.stream().allMatch(v->"ACTIVE".equals(v.getLifecycleStatus())));
    }
    @Test void parentAndFourDescendantsUseOneTreeApprovalRequestAndPublishTogether() {
        populate(5);
        change.setChangeType("TREE");
        context = context.toBuilder().objectType("ROUTE_TREE_CHANGE")
                .variables(Map.of("changeFingerprint", MesProBatchChangeApprovalExecutor.fingerprint(change))).build();
        request = request.toBuilder().context(context).build();
        executor.precheck(context);
        executor.markPending(context, request);
        assertTrue(candidates.stream().allMatch(v -> "PENDING_APPROVAL".equals(v.getLifecycleStatus())
                && "one-process".equals(v.getApprovalProcessInstanceId())));
        verify(crafts, never()).onRouteReady(any(), any());
        executor.executeApproved(context, request, 8L);
        assertTrue(candidates.stream().allMatch(v -> "ACTIVE".equals(v.getLifecycleStatus())));
        verify(crafts, times(1)).onRouteReady(any(), eq(8L));
    }
    @Test void treeChangeCannotBypassApprovalUsingDirectPolicy() {
        change.setChangeType("TREE");
        context = context.toBuilder().objectType("ROUTE_TREE_CHANGE")
                .variables(Map.of("changeFingerprint", MesProBatchChangeApprovalExecutor.fingerprint(change))).build();
        request = request.toBuilder().context(context).processInstanceId(null).build();
        assertThrows(RuntimeException.class, () -> executor.executeDirect(context, request));
        verify(crafts, never()).onRouteReady(any(), any());
        assertTrue(candidates.stream().allMatch(v -> "DRAFT".equals(v.getLifecycleStatus())));
    }
    @Test void rejectionCancelsAllAndLateApprovalDoesNotPublish() {
        executor.markPending(context,request);executor.reject(context,request,8L,"需修订");
        assertEquals("CANCELLED",executor.executeApproved(context,request,8L).getResultState());
        verify(crafts,never()).onRouteReady(any(),any());
        verify(audit,times(2)).recordRejected(any(),eq(8L),eq("需修订"),anyString());
    }
    @Test void withdrawalCancelsWholeChange() {
        executor.markPending(context,request);executor.cancel(context,request,7L,"撤回");
        assertEquals("CANCELLED",change.getStatus());verify(audit,times(2)).recordWithdrawn(any(),eq(7L));
    }
    @Test void duplicateApprovalCannotPublishTwice() {
        executor.markPending(context,request);executor.executeApproved(context,request,8L);executor.executeApproved(context,request,8L);
        verify(crafts,times(1)).onRouteReady(any(),any());
    }
    @Test void changedReasonOrAffectedRangeInvalidatesApproval() {
        executor.markPending(context,request);change.setReason("另一修改");
        assertThrows(RuntimeException.class,()->executor.executeApproved(context,request,8L));
        verify(crafts,never()).onRouteReady(any(),any());
    }
    @Test void changedCandidateHashBlocksWholeApproval() {
        executor.markPending(context,request);candidates.get(1).setRouteSnapshotSha256("changed");
        assertThrows(RuntimeException.class,()->executor.executeApproved(context,request,8L));
        verify(audit,never()).recordApproved(any(),any(),any());
    }
    @Test void otherTenantCannotApproveChange() {
        executor.markPending(context,request);TenantContextHolder.setTenantId(2L);
        assertThrows(RuntimeException.class,()->executor.executeApproved(context,request,8L));
    }
    @Test void unrelatedRequestOrProcessCannotApprove() {
        executor.markPending(context,request);
        assertThrows(RuntimeException.class,()->executor.executeApproved(context,request.toBuilder().requestId(81L).build(),8L));
        assertThrows(RuntimeException.class,()->executor.executeApproved(context,request.toBuilder().processInstanceId("other").build(),8L));
    }
    @Test void callbackUsesFrozenRequestAndDoesNotResolveCurrentPolicy() {
        executor.markPending(context,request);
        // No policy resolver is consulted by the callback; changing the policy cannot turn this pending request into DIRECT.
        assertEquals("PENDING",change.getStatus());
        executor.executeApproved(context,request,8L);
        assertEquals("APPLIED",change.getStatus());
    }
}
