package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    private MesQaInspectionRegulationItemEquipmentMapper equipmentMapper;
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
        equipmentMapper = mock(MesQaInspectionRegulationItemEquipmentMapper.class);
        reader = new MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl(taskMapper, aggregateMapper,
                eventMapper, recordMapper, reviewMapper, regulationMapper, versionMapper, itemMapper,
                equipmentMapper);
    }

    @Test
    void readsExactAggregateEventRecordReviewAndPublishedQaGraph() {
        MesPqcInspectionTaskDO task = task();
        MesPqcProcessInspectionAggregateDetailDO aggregate = aggregate(EVENT_ID);
        MesProProcessPoolEventDO event = new MesProProcessPoolEventDO().setId(EVENT_ID);
        MesProProcessPoolPqcRecordDO record = new MesProProcessPoolPqcRecordDO().setId(201L);
        MesProcessPoolSubmissionReviewDO review = new MesProcessPoolSubmissionReviewDO().setId(REVIEW_ID);
        MesQaInspectionRegulationDO regulation = new MesQaInspectionRegulationDO()
                .setId(202L).setCurrentVersionId(REGULATION_VERSION_ID);
        MesQaInspectionRegulationVersionDO version = new MesQaInspectionRegulationVersionDO()
                .setId(REGULATION_VERSION_ID);
        MesQaInspectionRegulationItemDO item = new MesQaInspectionRegulationItemDO().setId(203L);
        MesQaInspectionRegulationItemEquipmentDO equipment =
                new MesQaInspectionRegulationItemEquipmentDO().setId(204L);
        when(taskMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(task));
        when(aggregateMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(aggregate));
        when(eventMapper.selectById(EVENT_ID)).thenReturn(event);
        when(recordMapper.selectByEventId(EVENT_ID)).thenReturn(record);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
        when(regulationMapper.selectPublishedByRouteProcess(PRODUCT_ID, ROUTE_ID, ROUTE_VERSION_ID,
                ROUTE_PROCESS_ID, PROCESS_ID)).thenReturn(regulation);
        when(versionMapper.selectById(REGULATION_VERSION_ID)).thenReturn(version);
        when(itemMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(item));
        when(equipmentMapper.selectListByVersionId(REGULATION_VERSION_ID)).thenReturn(List.of(equipment));

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
        assertEquals(List.of(equipment), source.getRegulationItemEquipment());
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
}
