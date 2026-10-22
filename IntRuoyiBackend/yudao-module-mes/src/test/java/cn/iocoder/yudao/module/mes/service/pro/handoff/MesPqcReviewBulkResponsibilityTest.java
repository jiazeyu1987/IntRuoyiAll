package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesPqcReviewBulkResponsibilityTest {
    private MesActiveOrderHandoffOwnerResolverTest fixture;
    private MesActiveOrderHandoffTaskMapper handoffs;
    private MesProcessPoolActiveOrderMapper orders;
    private List<MesProProcessPoolEventDO> events;
    private List<MesPqcInspectionTaskDO> pqc;
    private List<MesActiveOrderHandoffTaskDO> original, reviews;

    @BeforeEach void setup() { fixture=new MesActiveOrderHandoffOwnerResolverTest();fixture.setup(); }
    @AfterEach void clear() { fixture.clear(); }
    private void prepare(int count) {
        var base=fixture.frozenResponsibilityFixture();fixture.allowUser(343L,"mes:pro-process-pool-team-leader:review");
        handoffs=(MesActiveOrderHandoffTaskMapper)ReflectionTestUtils.getField(fixture.service,"handoffTasks");
        orders=(MesProcessPoolActiveOrderMapper)ReflectionTestUtils.getField(fixture.service,"orders");
        var order=orders.selectById(413L);clearInvocations(orders);
        when(orders.selectBatchIds(anyCollection())).thenReturn(List.of(order));
        events=new ArrayList<>();pqc=new ArrayList<>();original=new ArrayList<>();reviews=new ArrayList<>();
        for(int i=0;i<count;i++) {
            long eventId=176L+i, taskId=55L+i;
            events.add(MesActiveOrderHandoffOwnerResolverTest.pqcEvent().setId(eventId).setFeedbackSourceId(taskId));
            var task=MesActiveOrderHandoffOwnerResolverTest.pqcTask().setId(taskId).setSubmittedEventId(eventId);pqc.add(task);
            var source=JsonUtils.parseObject(JsonUtils.toJsonString(base),MesActiveOrderHandoffTaskDO.class);
            source.setId(1000L+i).setSourceId(taskId).setRoundId(taskId);
            var snapshot=(com.fasterxml.jackson.databind.node.ObjectNode)JsonUtils.parseTree(source.getResponsibilitySnapshotJson());
            snapshot.put("sourceId",taskId).put("roundId",taskId);source.setResponsibilitySnapshotJson(snapshot.toString());original.add(source);
            var review=new MesActiveOrderHandoffTaskDO().setId(2000L+i).setActiveOrderId(413L).setWorkOrderId(274L)
                    .setRouteProcessId(91L).setSourceType("PROCESS_POOL_EVENT").setSourceId(eventId).setRoundId(eventId)
                    .setTaskType("PQC_REVIEW").setStatus("TODO").setCandidateUserSnapshot("343");review.setTenantId(1L);
            review.setActionUrl("/mes/pro/process-pool/pqc-leader?activeOrderId=413&eventId="+eventId+"&handoffTaskId="+(2000L+i)+"&roundId="+eventId+"&handoffType=PQC_REVIEW");
            var frozen=new LinkedHashMap<String,Object>();frozen.put("identityDomain","SYSTEM_USER");frozen.put("activeOrderId",413L);frozen.put("workOrderId",274L);
            frozen.put("routeProcessId",91L);frozen.put("processId",15L);frozen.put("pqcTaskId",taskId);frozen.put("qaProcessId",task.getQaProcessId());
            frozen.put("routeVersionId",task.getRouteVersionId());frozen.put("regulationVersionId",task.getRegulationVersionId());
            frozen.put("sourceId",eventId);frozen.put("roundId",eventId);frozen.put("sourceType","PROCESS_POOL_EVENT");frozen.put("userIds",List.of(343L));
            review.setResponsibilitySnapshotJson(JsonUtils.toJsonString(frozen));reviews.add(review);
        }
        when(fixture.pqcTasks.selectBatchIds(anyCollection())).thenReturn(pqc);
        when(handoffs.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(original);
    }

    @Test void all1140MembersAreCheckedWithConstantTableReadsAndOneCurrentLeaderCheck() {
        prepare(1140);var ids=fixture.service.frozenPqcReviewEventIds(reviews,events,343L);
        assertEquals(1140,ids.size());assertTrue(ids.contains(1315L));
        verify(fixture.pqcTasks).selectBatchIds(anyCollection());verify(fixture.pqcTasks,never()).selectById(any());
        verify(orders).selectBatchIds(Set.of(413L));verify(orders,never()).selectById(any());
        verify(handoffs).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(fixture.users).selectById(343L);verify(fixture.permissions).hasAnyPermissions(343L,"mes:pro-process-pool-team-leader:review");
        verifyNoInteractions(fixture.scopes);
    }

    @Test void completedOriginalHandoffStillRequiresItsExactSubmittedEvent() {
        prepare(1);original.get(0).setStatus("DONE").setCompletionSourceId(176L);
        assertEquals(Set.of(176L),fixture.service.frozenPqcReviewEventIds(reviews,events,343L));
        original.get(0).setCompletionSourceId(177L);
        assertThrows(RuntimeException.class,()->fixture.service.frozenPqcReviewEventIds(reviews,events,343L));
    }

    @ParameterizedTest @ValueSource(strings={"missingEvent","missingTask","missingOrder","missingHandoff","duplicateHandoff","foreignEvent","foreignTask","foreignOrder","foreignHandoff","wrongStage","wrongSource","wrongRule","wrongReviewSnapshot","disabledLeader","missingLeader","noPermission","wrongCandidate"})
    void bulkReadRetainsEveryFrozenResponsibilityGuard(String defect) {
        prepare(2);
        switch(defect) {
            case "missingEvent" -> events.remove(1);
            case "missingTask" -> pqc.remove(1);
            case "missingOrder" -> when(orders.selectBatchIds(anyCollection())).thenReturn(List.of());
            case "missingHandoff" -> original.remove(1);
            case "duplicateHandoff" -> original.add(original.get(1));
            case "foreignEvent" -> events.get(1).setTenantId(2L);
            case "foreignTask" -> pqc.get(1).setTenantId(2L);
            case "foreignOrder" -> orders.selectBatchIds(Set.of(413L)).get(0).setTenantId(2L);
            case "foreignHandoff" -> original.get(1).setTenantId(2L);
            case "wrongStage" -> pqc.get(1).setRouteProcessId(92L);
            case "wrongSource" -> pqc.get(1).setSubmittedEventId(999L);
            case "wrongRule" -> original.get(1).setResponsibilitySnapshotJson(original.get(1).getResponsibilitySnapshotJson().replace("\"scopeId\":98","\"scopeId\":99"));
            case "wrongReviewSnapshot" -> reviews.get(1).setResponsibilitySnapshotJson(reviews.get(1).getResponsibilitySnapshotJson().replace("\"pqcTaskId\":56","\"pqcTaskId\":999"));
            case "disabledLeader" -> fixture.users.selectById(343L).setStatus(1);
            case "missingLeader" -> when(fixture.users.selectById(343L)).thenReturn(null);
            case "noPermission" -> when(fixture.permissions.hasAnyPermissions(343L,"mes:pro-process-pool-team-leader:review")).thenReturn(false);
            case "wrongCandidate" -> reviews.get(1).setCandidateUserSnapshot("349");
            default -> throw new IllegalArgumentException(defect);
        }
        assertThrows(RuntimeException.class,()->fixture.service.frozenPqcReviewEventIds(reviews,events,343L));
    }

    @Test void signedCorrectionRoundIsStillBoundToItsFrozenSourceAndCurrentPermissionsPerRequest() {
        prepare(1);var review=reviews.get(0);review.setSourceType(MesActiveOrderHandoffContract.EVENT_SIGNED_REVISION).setRoundId(800L);
        review.setActionUrl(review.getActionUrl().replace("roundId=176","roundId=800"));
        review.setResponsibilitySnapshotJson(review.getResponsibilitySnapshotJson().replace("\"roundId\":176","\"roundId\":800").replace("\"sourceType\":\"PROCESS_POOL_EVENT\"","\"sourceType\":\"PROCESS_POOL_EVENT_SIGNED_REVISION\""));
        assertEquals(Set.of(176L),fixture.service.frozenPqcReviewEventIds(reviews,events,343L));
        when(fixture.permissions.hasAnyPermissions(343L,"mes:pro-process-pool-team-leader:review")).thenReturn(false);
        assertThrows(RuntimeException.class,()->fixture.service.frozenPqcReviewEventIds(reviews,events,343L));
    }
}
