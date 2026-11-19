package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.businessapproval.model.*;
import cn.iocoder.yudao.module.bpm.businessapproval.service.*;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftChangeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.audit.MesVersionOperateLogService;
import cn.iocoder.yudao.module.mes.service.pro.route.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real approval executor, group coordinator and lifecycle services, backed by H2 transactions.
 * Mapper, projection and BPM boundaries are test doubles; this is not browser/Flowable E2E. */
class MesProSharedApprovalTransactionTest {
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;
    private BusinessApprovalBpmEventListener listener;
    private MesProRouteVersionBusinessApprovalEffectExecutor executor;
    private MesProRouteVersionPublishProjectionServiceImpl projection;
    private MesProRouteControlledContentAdapter audit;
    private MesProRouteVersionMapper versions;
    private BpmProcessInstanceApi bpm;
    private MesProCraftService batchGroup;
    private MesProCraftChangeDO batchChange;
    private MesProCraftChangeMapper batchChanges;
    private final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        TenantContextHolder.setTenantId(1L);
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
        // Keep this fixture alive across mapper calls, each of which obtains its own connection.
        source.setUrl(source.getUrl() + ";DB_CLOSE_DELAY=-1");
        transactionManager = new DataSourceTransactionManager(source);
        jdbc = new JdbcTemplate(source);
        jdbc.execute("create table route_version(id bigint primary key, body varchar(20000))");
        jdbc.execute("create table change_state(id bigint primary key, phase varchar(30))");
        jdbc.execute("create table approval(id bigint primary key, status varchar(30), event_key varchar(100), result_state varchar(30))");
        jdbc.execute("create table audit(kind varchar(30), version_id bigint)");
        jdbc.update("insert into change_state values(1,'PENDING'),(2,'old master')");
        for (long id : List.of(1L, 2L, 101L, 102L)) {
            MesProRouteVersionDO v = new MesProRouteVersionDO();
            v.setId(id); v.setRouteId(id > 100 ? id - 100 : id);
            v.setLifecycleStatus(id > 100 ? "DRAFT" : "ACTIVE"); v.setActive(id < 100);
            v.setSourceRouteVersionId(id > 100 ? id - 100 : null);
            v.setRouteSnapshotJson(canonicalizer.canonicalize(snapshot(v.getRouteId(), id > 100)));
            v.setRouteSnapshotSha256(canonicalizer.sha256(v.getRouteSnapshotJson()));
            v.setRouteSnapshotFormatVersion(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
            jdbc.update("insert into route_version values(?,?)", id, JSON.toJSONString(v));
        }
        versions = mock(MesProRouteVersionMapper.class);
        when(versions.selectById(anyLong())).thenAnswer(c -> version(c.getArgument(0)));
        when(versions.selectActiveByRouteId(anyLong())).thenAnswer(c -> version(c.getArgument(0)));
        when(versions.selectActiveByRouteIdForUpdate(anyLong())).thenAnswer(c -> version(c.getArgument(0)));
        when(versions.updateById(any(MesProRouteVersionDO.class))).thenAnswer(c -> {
            MesProRouteVersionDO patch = c.getArgument(0);
            JSONObject body = JSON.parseObject(JSON.toJSONString(version(patch.getId())));
            body.putAll(JSON.parseObject(JSON.toJSONString(patch)));
            return jdbc.update("update route_version set body=? where id=?", body.toJSONString(), patch.getId());
        });
        when(versions.updateApprovalFieldsToDraft(anyLong())).thenAnswer(c -> {
            MesProRouteVersionDO v = version(c.getArgument(0));
            v.setLifecycleStatus("DRAFT"); v.setApprovalProcessInstanceId(null);
            return jdbc.update("update route_version set body=? where id=?", JSON.toJSONString(v), v.getId());
        });
        var change = new MesProCraftChangeDO();
        change.setId(1L); change.setChangeType("SHARED"); change.setCandidateIdsJson("[101,102]");
        change.setCandidateHashesJson(JSON.toJSONString(Map.of(
                "101", version(101).getRouteSnapshotSha256(), "102", version(102).getRouteSnapshotSha256(),
                "101:source", version(1).getRouteSnapshotSha256(), "102:source", version(2).getRouteSnapshotSha256())));
        var changes = mock(MesProCraftChangeMapper.class);
        batchChange = change;
        batchChanges = changes;
        when(changes.selectById(1L)).thenAnswer(c -> { change.setStatus(phase(1)); return change; });
        when(changes.updateById(any(MesProCraftChangeDO.class))).thenAnswer(c ->
                jdbc.update("update change_state set phase=? where id=1", ((MesProCraftChangeDO)c.getArgument(0)).getStatus()));
        var crafts = mock(MesProCraftMapper.class);
        when(crafts.lockSequence(1L)).thenReturn(10L);
        var shared = mock(MesProSharedChangeService.class);
        doAnswer(c -> { jdbc.update("update change_state set phase='new master' where id=2"); return null; })
                .when(shared).apply(any());
        ObjectProvider<MesProSharedChangeService> sharedProvider = mock(ObjectProvider.class);
        when(sharedProvider.getObject()).thenReturn(shared);
        ObjectProvider<MesProRouteVersionLifecycleService> lifecycleProvider = mock(ObjectProvider.class);
        audit = mock(MesProRouteControlledContentAdapter.class);
        doAnswer(c -> log("SUBMITTED", c.getArgument(0))).when(audit).recordSubmitted(any(), any(), any());
        doAnswer(c -> log("APPROVED", c.getArgument(0))).when(audit).recordApproved(any(), any(), any());
        doAnswer(c -> log("PUBLISHED", c.getArgument(1))).when(audit).recordPublished(any(), any(), any());
        doAnswer(c -> log("CANCELLED", c.getArgument(0))).when(audit).recordCancelled(any(), any());
        var groupTarget = new MesProCraftService();
        bpm = mock(BpmProcessInstanceApi.class);
        wire(groupTarget, "craftMapper", crafts, "changeMapper", changes, "versionMapper", versions,
                "lifecycle", lifecycleProvider, "sharedChanges", sharedProvider, "platform", audit,
                "bpmApi", bpm);
        MesProCraftService group = transactional(groupTarget);
        batchGroup = group;
        projection = mock(MesProRouteVersionPublishProjectionServiceImpl.class);
        when(projection.projectCandidate(any())).thenAnswer(c -> {
            MesProRouteVersionDO v = c.getArgument(0);
            return new MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult(v.getRouteSnapshotJson(), Set.of(10L));
        });
        var lifecycleTarget = new MesProRouteVersionLifecycleServiceImpl();
        wire(lifecycleTarget, "craftService", group, "routeVersionMapper", versions,
                "deviceIdentitySnapshots", mock(MesProRouteDeviceIdentitySnapshotService.class),
                "publishProjectionService", projection, "routeSnapshotCanonicalizer", canonicalizer,
                "platformAdapter", audit, "versionOperateLogService", mock(MesVersionOperateLogService.class));
        MesProRouteVersionLifecycleService lifecycle = transactional(lifecycleTarget);
        when(lifecycleProvider.getObject()).thenReturn(lifecycle);
        var executorTarget = new MesProRouteVersionBusinessApprovalEffectExecutor();
        wire(executorTarget, "craftService", group, "lifecycleService", lifecycle,
                "routeVersionMapper", versions, "platformAdapter", audit);
        executor = transactional(executorTarget);
        var store = mock(BusinessApprovalRequestStore.class);
        when(store.findByProcessInstanceId(anyString())).thenAnswer(c ->
                Optional.of(request(Long.parseLong(((String)c.getArgument(0)).substring(4)))));
        when(store.update(any())).thenAnswer(c -> {
            BusinessApprovalRequest r = c.getArgument(0);
            jdbc.update("update approval set status=?, event_key=?, result_state=? where id=?",
                    r.getStatus().name(), r.getLastEventKey(), r.getResultState(), r.getRequestId());
            return r;
        });
        listener = new BusinessApprovalBpmEventListener(store, new BusinessApprovalEffectExecutorRegistry(List.of(executor)));
        for (long id : List.of(101L, 102L)) {
            jdbc.update("insert into approval(id,status) values(?,'PENDING_BPM')", id);
            executor.markPending(request(id).getContext(), request(id));
        }
    }

    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    private MesProBatchChangeApprovalExecutor prepareBatch() {
        batchChange.setApprovalScope("BATCH");
        jdbc.update("update change_state set phase='PREPARING' where id=1");
        for (long id : List.of(101L,102L)) {
            var v=version(id);v.setLifecycleStatus("DRAFT");v.setApprovalProcessInstanceId(null);
            jdbc.update("update route_version set body=? where id=?",JSON.toJSONString(v),id);
        }
        jdbc.update("delete from audit");
        var target=new MesProBatchChangeApprovalExecutor();
        wire(target,"crafts",batchGroup,"changes",batchChanges,"versions",versions,"platform",audit);
        return transactional(target);
    }
    private BusinessApprovalRequest batchRequest() {
        var context=BusinessApprovalContext.builder().tenantId(1L).dataDomain("MES").systemCode("MES")
                .objectType("ROUTE_BATCH_CHANGE").objectId("1").objectState("PREPARING").actionCode("PUBLISH")
                .applicantUserId(7L).variables(Map.of("changeFingerprint",MesProBatchChangeApprovalExecutor.fingerprint(batchChange))).build();
        return BusinessApprovalRequest.builder().requestId(501L).tenantId(1L).context(context).processInstanceId("batch-instance").build();
    }
    @Test void oneBatchApprovalPublishesBothRoutesInTheSameTransaction() {
        var batch=prepareBatch();var request=batchRequest();
        batch.markPending(request.getContext(),request);
        assertOldPublishedState();
        assertEquals("APPLIED",batch.executeApproved(request.getContext(),request,8L).getResultState());
        assertEquals("ACTIVE",version(101).getLifecycleStatus());assertEquals("ACTIVE",version(102).getLifecycleStatus());
        assertEquals("new master",phase(2));assertEquals(2,auditCount("PUBLISHED"));
    }
    @Test void batchSecondProjectionFailureRollsBackBothApprovalTransitionsAndPublication() {
        var batch=prepareBatch();var request=batchRequest();
        batch.markPending(request.getContext(),request);
        doThrow(new IllegalStateException("second projection failed")).when(projection)
                .projectCandidate(argThat(v->v.getId()==102L));
        assertThrows(IllegalStateException.class,()->batch.executeApproved(request.getContext(),request,8L));
        assertOldPublishedState();assertEquals("PENDING",phase(1));
        assertEquals("PENDING_APPROVAL",version(101).getLifecycleStatus());
        assertEquals("PENDING_APPROVAL",version(102).getLifecycleStatus());
        assertEquals(0,auditCount("APPROVED"));assertEquals(0,auditCount("PUBLISHED"));
    }

    @Test void waitsForEveryApprovalThenPublishesTogetherAndIgnoresDuplicateEvents() {
        BusinessApprovalRequest first = event(101, BpmProcessInstanceStatusEnum.APPROVE);
        assertEquals("READY_TO_PUBLISH", first.getResultState());
        assertEquals("PENDING_APPROVAL", version(102).getLifecycleStatus());
        assertOldPublishedState();
        assertEquals(first.getLastEventKey(), event(101, BpmProcessInstanceStatusEnum.APPROVE).getLastEventKey());
        assertEquals(1, auditCount("APPROVED"));
        BusinessApprovalRequest last = event(102, BpmProcessInstanceStatusEnum.APPROVE);
        assertEquals("ACTIVE", last.getResultState());
        for (long id : List.of(101L, 102L)) assertEquals("ACTIVE", version(id).getLifecycleStatus());
        for (long id : List.of(1L, 2L)) assertEquals("SUPERSEDED", version(id).getLifecycleStatus());
        assertEquals("new master", phase(2)); assertEquals("APPLIED", phase(1));
        event(102, BpmProcessInstanceStatusEnum.APPROVE);
        assertEquals(2, auditCount("APPROVED")); assertEquals(2, auditCount("PUBLISHED"));
    }

    @Test void rejectionAfterOneApprovalCancelsWholeBatchAndReportsActualVersionState() {
        event(101, BpmProcessInstanceStatusEnum.APPROVE);
        BusinessApprovalRequest rejected = event(102, BpmProcessInstanceStatusEnum.REJECT);
        assertCancelled();
        assertEquals(BusinessApprovalRequestStatus.REJECTED, rejected.getStatus());
        assertEquals(version(102).getLifecycleStatus(), rejected.getResultState());
    }

    @Test void withdrawalCancelsWholeBatchAndLateApprovalCannotPublishIt() {
        BusinessApprovalRequest cancelled = event(102, BpmProcessInstanceStatusEnum.CANCEL);
        assertCancelled();
        assertEquals(version(102).getLifecycleStatus(), cancelled.getResultState());
        BusinessApprovalRequest late = event(101, BpmProcessInstanceStatusEnum.APPROVE);
        assertEquals("CANCELLED", late.getResultState());
        assertCancelled();
    }

    @Test void secondProjectionFailureRollsBackPublicationApprovalAndAuditThenAllowsRetry() {
        event(101, BpmProcessInstanceStatusEnum.APPROVE);
        doThrow(new IllegalStateException("second projection failed"))
                .when(projection).projectCandidate(argThat(v -> v.getId() == 102L));
        assertThrows(IllegalStateException.class, () -> event(102, BpmProcessInstanceStatusEnum.APPROVE));
        assertOldPublishedState();
        assertEquals("READY_TO_PUBLISH", version(101).getLifecycleStatus());
        assertEquals("PENDING_APPROVAL", version(102).getLifecycleStatus());
        assertEquals(BusinessApprovalRequestStatus.PENDING_BPM, request(102).getStatus());
        assertNull(request(102).getLastEventKey()); assertEquals(1, auditCount("APPROVED"));
        doAnswer(c -> new MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult(
                ((MesProRouteVersionDO)c.getArgument(0)).getRouteSnapshotJson(), Set.of(10L)))
                .when(projection).projectCandidate(any());
        assertEquals("ACTIVE", event(102, BpmProcessInstanceStatusEnum.APPROVE).getResultState());
        assertEquals("APPLIED", phase(1)); assertEquals(2, auditCount("PUBLISHED"));
    }

    @Test void publicationAuditFailureRollsBackBothRoutesAndTheirApproval() {
        event(101, BpmProcessInstanceStatusEnum.APPROVE);
        doThrow(new IllegalStateException("audit unavailable")).when(audit)
                .recordPublished(any(), argThat(v -> v.getId() == 102L), any());
        assertThrows(IllegalStateException.class, () -> event(102, BpmProcessInstanceStatusEnum.APPROVE));
        assertOldPublishedState();
        assertEquals("PENDING_APPROVAL", version(102).getLifecycleStatus());
        assertEquals(BusinessApprovalRequestStatus.PENDING_BPM, request(102).getStatus());
    }

    @Test void anotherReviewerRejectionCancelsRemainingApprovalThroughBusinessAuthority() {
        doAnswer(c -> { event(101, BpmProcessInstanceStatusEnum.CANCEL, 9L); return null; })
                .when(bpm).cancelBusinessApprovalProcess(eq(9L), eq("bpm-101"),
                        eq(MesProRouteVersionBusinessApprovalEffectExecutor.EXECUTOR_CODE), eq("101"), anyString());
        event(102, BpmProcessInstanceStatusEnum.REJECT, 9L);
        assertCancelled();
        assertEquals(BusinessApprovalRequestStatus.CANCELED, request(101).getStatus());
        assertEquals(BusinessApprovalRequestStatus.REJECTED, request(102).getStatus());
        verify(bpm, never()).cancelProcessInstance(any(), anyString(), anyString());
    }

    @Test void linkedApprovalCancellationFailureRollsBackRejectionAndWholeBatchCancellation() {
        doThrow(new IllegalStateException("linked BPM cancellation failed")).when(bpm)
                .cancelBusinessApprovalProcess(any(), anyString(), anyString(), anyString(), anyString());
        assertThrows(IllegalStateException.class, () -> event(102, BpmProcessInstanceStatusEnum.REJECT, 9L));
        assertOldPublishedState();
        for (long id : List.of(101L, 102L)) {
            assertEquals("PENDING_APPROVAL", version(id).getLifecycleStatus());
            assertEquals(BusinessApprovalRequestStatus.PENDING_BPM, request(id).getStatus());
        }
        assertEquals(0, auditCount("CANCELLED"));
    }

    private void assertOldPublishedState() {
        assertEquals("ACTIVE", version(1).getLifecycleStatus()); assertEquals("ACTIVE", version(2).getLifecycleStatus());
        assertEquals("old master", phase(2)); assertEquals("PENDING", phase(1));
        assertEquals(0, auditCount("PUBLISHED"));
    }
    private void assertCancelled() {
        assertEquals("CANCELLED", phase(1)); assertEquals("old master", phase(2));
        for (long id : List.of(101L, 102L)) assertEquals("CANCELLED", version(id).getLifecycleStatus());
        for (long id : List.of(1L, 2L)) assertEquals("ACTIVE", version(id).getLifecycleStatus());
        assertEquals(0, auditCount("PUBLISHED"));
    }
    private BusinessApprovalRequest event(long id, BpmProcessInstanceStatusEnum status) {
        return event(id, status, 7L);
    }
    private BusinessApprovalRequest event(long id, BpmProcessInstanceStatusEnum status, Long actorId) {
        var event = new BpmProcessInstanceStatusEvent(this);
        event.setId("bpm-" + id); event.setBusinessKey("BUSINESS_APPROVAL:" + id);
        event.setProcessDefinitionKey(MesProRouteVersionBusinessApprovalEffectExecutor.PROCESS_DEFINITION_KEY);
        event.setStatus(status.getStatus()); event.setActorUserId(actorId); event.setReason("review result");
        // Flowable invokes its status listeners inside the process command transaction.
        return new TransactionTemplate(transactionManager).execute(s -> listener.onProcessStatusEvent(event));
    }
    private BusinessApprovalRequest request(long id) {
        return jdbc.queryForObject("select * from approval where id=?", (rs, row) -> BusinessApprovalRequest.builder()
                .requestId(id).tenantId(1L).policyMode(BusinessApprovalPolicyMode.BPM_REQUIRED)
                .effectExecutorCode(MesProRouteVersionBusinessApprovalEffectExecutor.EXECUTOR_CODE)
                .processDefinitionKey(MesProRouteVersionBusinessApprovalEffectExecutor.PROCESS_DEFINITION_KEY)
                .processInstanceId("bpm-" + id).status(BusinessApprovalRequestStatus.valueOf(rs.getString("status")))
                .lastEventKey(rs.getString("event_key")).resultState(rs.getString("result_state"))
                .context(BusinessApprovalContext.builder().tenantId(1L).dataDomain("MES").systemCode("MES")
                        .objectType("ROUTE_VERSION").objectId(Long.toString(id)).objectVersion("V2")
                        .actionCode("PUBLISH").objectState("DRAFT").applicantUserId(7L).reason("shared change").build())
                .build(), id);
    }
    private MesProRouteVersionDO version(long id) {
        return JSON.parseObject(jdbc.queryForObject("select body from route_version where id=?", String.class, id), MesProRouteVersionDO.class);
    }
    private String phase(long id) { return jdbc.queryForObject("select phase from change_state where id=?", String.class, id); }
    private Object log(String kind, MesProRouteVersionDO v) { jdbc.update("insert into audit values(?,?)", kind, v.getId()); return null; }
    private int auditCount(String kind) { return jdbc.queryForObject("select count(*) from audit where kind=?", Integer.class, kind); }
    private void wire(Object bean, Object... fields) {
        for (int i = 0; i < fields.length; i += 2) ReflectionTestUtils.setField(bean, (String)fields[i], fields[i + 1]);
    }
    @SuppressWarnings("unchecked") private <T> T transactional(T bean) {
        var proxy = new ProxyFactory(bean); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return (T)proxy.getProxy();
    }
    private String snapshot(long routeId, boolean candidate) {
        return """
                {"routeId":%d,"routeCode":"RT-%d","routeName":"Approval test",
                 "configSnapshots":{%s
                  "flowGraph":{"nodes":[{"routeProcessId":10,"processId":20,"sort":1}],"edges":[]},
                  "products":[],"scheduleConfigs":[],"batchUseConfigs":[],"scheduleUseConfigs":[],
                  "productionProcessConfigSchemaVersion":1,
                  "productionProcessConfigs":[{"routeProcessId":10,"processId":20,"overagePercent":0,
                    "lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}]}}
                """.formatted(routeId, routeId, candidate ? "\"craftChangeId\":1," : "");
    }
}
