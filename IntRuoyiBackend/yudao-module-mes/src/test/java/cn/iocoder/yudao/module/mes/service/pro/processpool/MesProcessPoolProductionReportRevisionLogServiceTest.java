package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProProductionReportRevisionLogPageReqVO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolProductionReportRevisionLogServiceTest {

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper revisionMapper;
    @Mock
    private MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper;
    @Mock
    private MesProProcessPoolTimelineReadMapper timelineReadMapper;
    @Mock
    private MesTeamLeaderScopeService scopeService;

    private MesProcessPoolProductionReportRevisionLogService service;

    @BeforeEach
    void setUp() {
        service = new MesProcessPoolProductionReportRevisionLogService(
                eventMapper, revisionMapper, revisionDiffMapper, timelineReadMapper, scopeService);
    }

    @Test
    void returnsReadableLogsWithSnapshotActorAndBusinessFieldChanges() {
        when(eventMapper.selectById(176L)).thenReturn(event());
        when(revisionMapper.selectListByEventId(176L)).thenReturn(List.of(revision()));
        when(revisionDiffMapper.selectListByRevisionIds(List.of(701L))).thenReturn(List.of(
                diff(1L, "OUTPUT_QUANTITY", "完成数量", "4", "6"),
                diff(2L, "DEVICE_PARAMETERS.pressure", "压力（kPa）", "20", "25")
        ));

        List<MesProcessPoolProductionReportRevisionLogBO> logs = service.getLogs(176L, 3001L);

        assertEquals(1, logs.size());
        MesProcessPoolProductionReportRevisionLogBO log = logs.get(0);
        assertEquals("王组长", log.getModifiedByName());
        assertEquals(LocalDateTime.of(2026, 8, 7, 9, 30), log.getModifiedAt());
        assertEquals("录入时数量填错", log.getChangeReason());
        assertTrue(log.getSignatureConfirmed());
        assertEquals(2, log.getChanges().size());
        assertEquals("完成数量", log.getChanges().get(0).getFieldName());
        assertEquals("4", log.getChanges().get(0).getBeforeValue());
        assertEquals("6", log.getChanges().get(0).getAfterValue());
        verify(scopeService).assertCanAccessEmployee(3001L, "PRODUCTION", 964L);
    }

    @Test
    void pagesProductionReportRevisionLogsByCurrentLeaderResponsibleEmployees() {
        MesProProductionReportRevisionLogPageReqVO reqVO = new MesProProductionReportRevisionLogPageReqVO()
                .setWorkOrderCode("WO-001");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(scopeService.listResponsibleEmployeeIds(3001L, "PRODUCTION")).thenReturn(Set.of(964L));
        when(revisionMapper.selectProductionReportRevisionLogCount(reqVO, List.of(964L))).thenReturn(1L);
        when(revisionMapper.selectProductionReportRevisionLogPage(reqVO, List.of(964L), 0, 10))
                .thenReturn(List.of(revision()));
        when(eventMapper.selectById(176L)).thenReturn(event());
        when(timelineReadMapper.selectTimelineDetailById(176L)).thenReturn(timelineEvent());
        when(revisionDiffMapper.selectListByRevisionIds(List.of(701L))).thenReturn(List.of(
                diff(1L, "OUTPUT_QUANTITY", "完成数量", "4", "6")));

        PageResult<MesProcessPoolProductionReportRevisionLogBO> page =
                service.getProductionReportRevisionPage(reqVO, 3001L);

        assertEquals(1L, page.getTotal());
        MesProcessPoolProductionReportRevisionLogBO row = page.getList().get(0);
        assertEquals(701L, row.getRevisionId());
        assertEquals(176L, row.getEventId());
        assertEquals("WO-001", row.getWorkOrderCode());
        assertEquals("组装", row.getProcessName());
        assertEquals("张三", row.getActualEmployeeName());
        assertEquals(1, row.getFieldCount());
        assertEquals("完成数量", row.getChangeSummary());
        verify(scopeService).assertCanAccessEmployee(3001L, "PRODUCTION", 964L);
    }

    @Test
    void loadsProductionReportRevisionDetailByRevisionIdWithCurrentLeaderScope() {
        when(revisionMapper.selectById(701L)).thenReturn(revision());
        when(eventMapper.selectById(176L)).thenReturn(event());
        when(timelineReadMapper.selectTimelineDetailById(176L)).thenReturn(timelineEvent());
        when(revisionDiffMapper.selectListByRevisionIds(List.of(701L))).thenReturn(List.of(
                diff(1L, "OUTPUT_QUANTITY", "完成数量", "4", "6")));

        MesProcessPoolProductionReportRevisionLogBO detail =
                service.getProductionReportRevisionDetail(701L, 3001L);

        assertEquals(701L, detail.getRevisionId());
        assertEquals("WO-001", detail.getWorkOrderCode());
        assertEquals("完成数量", detail.getChanges().get(0).getFieldName());
        verify(scopeService).assertCanAccessEmployee(3001L, "PRODUCTION", 964L);
    }

    @Test
    void rendersLegacyLossDetailJsonAsReadableText() {
        when(eventMapper.selectById(176L)).thenReturn(event());
        when(revisionMapper.selectListByEventId(176L)).thenReturn(List.of(revision()));
        when(revisionDiffMapper.selectListByRevisionIds(List.of(701L))).thenReturn(List.of(diff(
                1L,
                "LOSS_DETAILS",
                "损耗明细",
                "[{\"reasonName\":\"正常损耗\",\"quantity\":2}]",
                "[{\"reasonName\":\"正常损耗\",\"quantity\":1},{\"reasonName\":\"设备故障\",\"quantity\":1}]"
        )));

        MesProcessPoolProductionReportRevisionLogBO.FieldChange change =
                service.getLogs(176L, 3001L).get(0).getChanges().get(0);

        assertEquals("正常损耗 2", change.getBeforeValue());
        assertEquals("正常损耗 1；设备故障 1", change.getAfterValue());
    }

    @Test
    void failsFastWhenPersistedSignatureSnapshotCannotProvideActorIdentity() {
        MesProProcessPoolEventRevisionDO malformed = revision().setRevisionSignatureSnapshot("{}");
        when(eventMapper.selectById(176L)).thenReturn(event());
        when(revisionMapper.selectListByEventId(176L)).thenReturn(List.of(malformed));
        when(revisionDiffMapper.selectListByRevisionIds(List.of(701L))).thenReturn(List.of(
                diff(1L, "OUTPUT_QUANTITY", "完成数量", "4", "6")));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.getLogs(176L, 3001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void returnsAnActualEmptyListWhenTheReportHasNeverBeenCorrected() {
        when(eventMapper.selectById(176L)).thenReturn(event());
        when(revisionMapper.selectListByEventId(176L)).thenReturn(List.of());

        assertTrue(service.getLogs(176L, 3001L).isEmpty());

        verify(revisionDiffMapper, never()).selectListByRevisionIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void rejectsOutOfScopeLeaderBeforeReadingRevisionContent() {
        ServiceException denied = new ServiceException(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED);
        when(eventMapper.selectById(176L)).thenReturn(event());
        doThrow(denied).when(scopeService).assertCanAccessEmployee(3001L, "PRODUCTION", 964L);

        ServiceException actual = assertThrows(ServiceException.class, () -> service.getLogs(176L, 3001L));

        assertSame(denied, actual);
        verify(revisionMapper, never()).selectListByEventId(176L);
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder()
                .id(176L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .actualEmployeeId(964L)
                .build();
    }

    private static ProcessPoolTimelineEventReadDO timelineEvent() {
        return new ProcessPoolTimelineEventReadDO()
                .setId(176L)
                .setWorkOrderCode("WO-001")
                .setWorkOrderName("压力泵生产工单")
                .setProcessCode("PROC-10")
                .setProcessName("组装")
                .setActualEmployeeUserName("张三")
                .setSubmittedAt(LocalDateTime.of(2026, 8, 7, 8, 30));
    }

    private static MesProProcessPoolEventRevisionDO revision() {
        return MesProProcessPoolEventRevisionDO.builder()
                .id(701L)
                .eventId(176L)
                .changeReason("录入时数量填错")
                .revisionSignatureId(9102L)
                .revisionSignatureUserId(3001L)
                .revisionSignatureSnapshot("{\"actorId\":3001,\"actorName\":\"王组长\"}")
                .modifiedByUserId(3001L)
                .serverRevisionTime(LocalDateTime.of(2026, 8, 7, 9, 30))
                .revisionStatus(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE)
                .build();
    }

    private static MesProProcessPoolEventRevisionDiffDO diff(
            Long id, String fieldCode, String fieldName, String beforeValue, String afterValue) {
        return MesProProcessPoolEventRevisionDiffDO.builder()
                .id(id)
                .revisionId(701L)
                .eventId(176L)
                .fieldCode(fieldCode)
                .fieldName(fieldName)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .build();
    }
}
