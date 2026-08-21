package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest {

    private static final long ACTIVE_ORDER_ID = 101L;
    private static final long PRODUCT_ID = 102L;
    private static final long ROUTE_ID = 103L;
    private static final long ROUTE_VERSION_ID = 104L;
    private static final long ROUTE_PROCESS_ID = 105L;
    private static final long PROCESS_ID = 106L;
    private static final long TASK_ID = 107L;
    private static final long EVENT_ID = 108L;
    private static final long REVIEW_ID = 109L;
    private static final long REGULATION_VERSION_ID = 110L;

    private MesPqcInspectionTaskMapper taskMapper;
    private MesPqcProcessInspectionAggregateDetailMapper aggregateMapper;
    private MesProProcessPoolEventMapper eventMapper;
    private MesProProcessPoolPqcRecordMapper recordMapper;
    private MesProcessPoolSubmissionReviewMapper reviewMapper;
    private MesQaInspectionRegulationMapper regulationMapper;
    private MesQaInspectionRegulationVersionMapper versionMapper;
    private MesQaInspectionRegulationItemMapper itemMapper;
    private DccProjectCodeMapper dccProjectCodeMapper;
    private ActiveOrderSnapshotResolver activeOrderSnapshotResolver;
    private MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort qaProvenancePort;
    private MesTeamLeaderActiveOrderReleaseProcessInspectionReader reader;

    @BeforeEach
    void setUp() {
        taskMapper = mock(MesPqcInspectionTaskMapper.class);
        aggregateMapper = mock(MesPqcProcessInspectionAggregateDetailMapper.class);
        eventMapper = mock(MesProProcessPoolEventMapper.class);
        recordMapper = mock(MesProProcessPoolPqcRecordMapper.class);
        reviewMapper = mock(MesProcessPoolSubmissionReviewMapper.class);
        regulationMapper = mock(MesQaInspectionRegulationMapper.class);
        versionMapper = mock(MesQaInspectionRegulationVersionMapper.class);
        itemMapper = mock(MesQaInspectionRegulationItemMapper.class);
        dccProjectCodeMapper = mock(DccProjectCodeMapper.class);
        activeOrderSnapshotResolver = mock(ActiveOrderSnapshotResolver.class);
        qaProvenancePort = mock(MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.class);
        when(activeOrderSnapshotResolver.requireEffective(ACTIVE_ORDER_ID)).thenReturn(
                new ActiveOrderSnapshotResolver.ActiveOrderSnapshot(ACTIVE_ORDER_ID, 100L, ROUTE_ID,
                        ROUTE_VERSION_ID, 205L, 202L, REGULATION_VERSION_ID));
        DccProjectCodeDO defaultProject = DccProjectCodeDO.builder().id(205L).productMasterId(11L)
                .projectCode("BOUND-ID").projectName("正式绑定项目").status("ENABLE").build();
        MesQaInspectionRegulationDO defaultRegulation = new MesQaInspectionRegulationDO()
                .setId(202L).setDccProjectCodeId(205L)
                .setOwnerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .setLifecycleStatus("PUBLISHED").setCurrentVersionId(REGULATION_VERSION_ID);
        MesQaInspectionRegulationVersionDO defaultVersion = new MesQaInspectionRegulationVersionDO()
                .setId(REGULATION_VERSION_ID).setRegulationId(202L).setLifecycleStatus("PUBLISHED")
                .setPublishedAt(LocalDateTime.of(2026, 8, 9, 10, 0));
        when(dccProjectCodeMapper.selectById(205L)).thenReturn(defaultProject);
        when(regulationMapper.selectById(202L)).thenReturn(defaultRegulation);
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(defaultVersion);
        when(qaProvenancePort.verify(any(), any(), any())).thenAnswer(invocation -> verifiedProvenance(
                invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        reader = new MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl(taskMapper, aggregateMapper,
                eventMapper, recordMapper, reviewMapper, regulationMapper, versionMapper, itemMapper,
                activeOrderSnapshotResolver, dccProjectCodeMapper, qaProvenancePort);
    }

    @Test
    void readsExactAggregateEventRecordReviewAndPublishedQaByDccOwnership() {
        MesPqcInspectionTaskDO task = task();
        MesPqcProcessInspectionAggregateDetailDO aggregate = aggregate(EVENT_ID);
        MesProProcessPoolEventDO event = new MesProProcessPoolEventDO().setId(EVENT_ID);
        MesProProcessPoolPqcRecordDO record = new MesProProcessPoolPqcRecordDO().setId(201L);
        MesProcessPoolSubmissionReviewDO review = new MesProcessPoolSubmissionReviewDO().setId(REVIEW_ID);
        MesQaInspectionRegulationDO regulation = new MesQaInspectionRegulationDO()
                .setId(202L).setDccProjectCodeId(205L).setLifecycleStatus("PUBLISHED")
                .setOwnerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .setCurrentVersionId(REGULATION_VERSION_ID);
        MesQaInspectionRegulationVersionDO version = new MesQaInspectionRegulationVersionDO()
                .setId(REGULATION_VERSION_ID).setRegulationId(202L).setLifecycleStatus("PUBLISHED")
                .setPublishedAt(LocalDateTime.of(2026, 8, 9, 10, 0));
        MesQaInspectionRegulationItemDO item = new MesQaInspectionRegulationItemDO().setId(203L);
        when(taskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(task));
        when(aggregateMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(aggregate));
        when(eventMapper.selectById(EVENT_ID)).thenReturn(event);
        when(recordMapper.selectByEventId(EVENT_ID)).thenReturn(record);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
        when(regulationMapper.selectById(202L)).thenReturn(regulation);
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(version);
        when(itemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(item));
        DccProjectCodeDO dccProject = DccProjectCodeDO.builder().id(205L).productMasterId(11L)
                .projectCode("ID").projectName("球囊扩张压力泵").status("ENABLE").build();
        when(dccProjectCodeMapper.selectById(205L)).thenReturn(dccProject);
        when(qaProvenancePort.verify(dccProject, regulation, version))
                .thenReturn(verifiedProvenance(dccProject, regulation, version));

        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.SourceBundle result = reader.read(command());

        assertEquals(1, result.getSources().size());
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source = result.getSources().get(0);
        assertSame(task, source.getTask());
        assertEquals(List.of(aggregate), source.getAggregateDetails());
        assertSame(event, source.getEvent());
        assertSame(record, source.getPqcRecord());
        assertSame(review, source.getReview());
        assertSame(regulation, source.getRegulation());
        assertSame(version, source.getRegulationVersion());
        assertEquals(List.of(item), source.getRegulationItems());
        assertSame(dccProject, source.getDccProject());
        assertEquals("ID", source.getRouteProjectCode());
        assertEquals("DCC_QA_PROJECT_RELATION", source.getQaDccProvenance().getProvenanceType());
        assertEquals(REGULATION_VERSION_ID, source.getQaDccProvenance().getRegulationVersionId());
        verify(regulationMapper).selectById(202L);
    }

    @Test
    void dccOwnedQaWithoutVerifiedDirectProvenanceIsNotExposedAsFormalQa() {
        MesPqcInspectionTaskDO task = task();
        MesQaInspectionRegulationDO regulation = new MesQaInspectionRegulationDO()
                .setId(202L).setDccProjectCodeId(205L).setLifecycleStatus("PUBLISHED")
                .setOwnerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .setCurrentVersionId(REGULATION_VERSION_ID);
        MesQaInspectionRegulationVersionDO version = new MesQaInspectionRegulationVersionDO()
                .setId(REGULATION_VERSION_ID).setRegulationId(202L).setLifecycleStatus("PUBLISHED")
                .setPublishedAt(LocalDateTime.of(2026, 8, 9, 10, 0));
        DccProjectCodeDO dccProject = DccProjectCodeDO.builder().id(205L).productMasterId(11L)
                .projectCode("ID").projectName("球囊扩张压力泵").status("ENABLE").build();
        when(taskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(task));
        when(aggregateMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of());
        when(dccProjectCodeMapper.selectById(205L)).thenReturn(dccProject);
        when(regulationMapper.selectById(202L)).thenReturn(regulation);
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(version);
        when(qaProvenancePort.verify(dccProject, regulation, version)).thenReturn(
                new MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution()
                        .setBlockerType("PQC_DCC_QA_PROVENANCE_REQUIRED")
                        .setBlockerMessage("QA 版本缺少正式 DCC 项目来源关系"));

        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source =
                reader.read(command()).getSources().get(0);

        assertNull(source.getRegulation());
        assertNull(source.getRegulationVersion());
        assertEquals("PQC_DCC_QA_PROVENANCE_REQUIRED", source.getQaDccProvenance().getBlockerType());
        verify(itemMapper, never()).selectListByVersionId(any());
    }

    @Test
    void frozenDccIdentityDoesNotScanEnabledProjectsOrDeriveFromProductCodes() {
        when(taskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(task()));
        when(aggregateMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of());

        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.SourceBundle result = reader.read(command());

        assertEquals(205L, result.getSources().get(0).getDccProject().getId());
        assertEquals("BOUND-ID", result.getSources().get(0).getRouteProjectCode());
        assertEquals(REGULATION_VERSION_ID, result.getSources().get(0).getRegulationVersion().getId());
        verify(dccProjectCodeMapper, never()).selectEnabledList();
    }

    @Test
    void activeOrderRouteSnapshotMismatchFailsFastBeforeReadingInspectionSources() {
        when(activeOrderSnapshotResolver.requireEffective(ACTIVE_ORDER_ID)).thenReturn(
                new ActiveOrderSnapshotResolver.ActiveOrderSnapshot(ACTIVE_ORDER_ID, 100L, ROUTE_ID + 1,
                        ROUTE_VERSION_ID, 205L, 202L, REGULATION_VERSION_ID));

        assertThrows(IllegalStateException.class, () -> reader.read(command()));

        verify(taskMapper, never()).selectListByActiveOrderId(any());
        verify(dccProjectCodeMapper, never()).selectById(any());
    }

    @Test
    void ambiguousAggregateEventIdsDoNotSelectAnArbitrarySignedSource() {
        MesPqcInspectionTaskDO task = task();
        when(taskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(task));
        when(aggregateMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(
                List.of(aggregate(EVENT_ID), aggregate(EVENT_ID + 1)));

        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.SourceBundle result = reader.read(command());

        assertEquals(1, result.getSources().size());
        assertNull(result.getSources().get(0).getEvent());
        assertNull(result.getSources().get(0).getPqcRecord());
        verify(eventMapper, never()).selectById(any());
        verify(recordMapper, never()).selectByEventId(any());
    }

    private MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command() {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand()
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setProductId(PRODUCT_ID)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID);
    }

    private MesPqcInspectionTaskDO task() {
        return new MesPqcInspectionTaskDO()
                .setId(TASK_ID)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setRouteProcessId(ROUTE_PROCESS_ID)
                .setProcessId(PROCESS_ID);
    }

    private MesPqcProcessInspectionAggregateDetailDO aggregate(long eventId) {
        return new MesPqcProcessInspectionAggregateDetailDO()
                .setId(eventId + 1000)
                .setPqcTaskId(TASK_ID)
                .setEventId(eventId)
                .setReviewId(REVIEW_ID);
    }

    private MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution verifiedProvenance(
            DccProjectCodeDO project,
            MesQaInspectionRegulationDO regulation,
            MesQaInspectionRegulationVersionDO version) {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution()
                .setDccProjectCodeId(project.getId())
                .setRegulationId(regulation.getId())
                .setRegulationVersionId(version.getId())
                .setProvenanceType("DCC_QA_PROJECT_RELATION")
                .setProvenanceId("relation-" + version.getId())
                .setProvenanceSnapshotHash("provenance-hash-" + version.getId());
    }
}
