package cn.iocoder.yudao.module.mes.service.dv;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.checkrecord.*;
import cn.iocoder.yudao.module.mes.service.dv.maintenrecord.*;
import cn.iocoder.yudao.module.mes.service.dv.repair.*;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.checkrecord.MesDvCheckRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.maintenrecord.MesDvMaintenRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.repair.MesDvRepairMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.checkrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.maintenrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.repair.MesDvRepairDO;
import cn.iocoder.yudao.module.mes.enums.dv.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MesDvLifecycleTest {
    @Test
    void inspectionCompletionWritesEquipmentDateAndRejectsEmptyDetails() {
        var service = new MesDvCheckRecordServiceImpl();
        var mapper = mock(MesDvCheckRecordMapper.class);
        var lines = mock(MesDvCheckRecordLineService.class);
        var equipment = mock(MesDvMachineryService.class);
        ReflectionTestUtils.setField(service, "checkRecordMapper", mapper);
        ReflectionTestUtils.setField(service, "checkRecordLineService", lines);
        ReflectionTestUtils.setField(service, "machineryService", equipment);
        var time = LocalDateTime.of(2026, 10, 29, 10, 0);
        when(mapper.selectById(1L)).thenReturn(new MesDvCheckRecordDO().setId(1L)
                .setMachineryId(2L).setCheckTime(time).setStatus(MesDvCheckRecordStatusEnum.DRAFT.getStatus()));
        when(lines.getCheckRecordLineListByRecordId(1L)).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.submitCheckRecord(1L));
        verifyNoInteractions(equipment);
        when(lines.getCheckRecordLineListByRecordId(1L)).thenReturn(List.of(new MesDvCheckRecordLineDO()));
        service.submitCheckRecord(1L);
        verify(equipment).updateMachineryLastCheckTime(2L, time);
        verify(mapper).updateById(argThat((MesDvCheckRecordDO row) -> row.getStatus().equals(MesDvCheckRecordStatusEnum.FINISHED.getStatus())));
    }

    @Test
    void maintenanceCompletionWritesEquipmentDateAndRejectsEmptyDetails() {
        var service = new MesDvMaintenRecordServiceImpl();
        var mapper = mock(MesDvMaintenRecordMapper.class);
        var lines = mock(MesDvMaintenRecordLineService.class);
        var equipment = mock(MesDvMachineryService.class);
        ReflectionTestUtils.setField(service, "maintenRecordMapper", mapper);
        ReflectionTestUtils.setField(service, "maintenRecordLineService", lines);
        ReflectionTestUtils.setField(service, "machineryService", equipment);
        var time = LocalDateTime.of(2026, 10, 29, 10, 0);
        when(mapper.selectById(1L)).thenReturn(new MesDvMaintenRecordDO().setId(1L)
                .setMachineryId(2L).setMaintenTime(time).setStatus(MesDvMaintenRecordStatusEnum.PREPARE.getStatus()));
        when(lines.getMaintenRecordLineListByRecordId(1L)).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.submitMaintenRecord(1L));
        verifyNoInteractions(equipment);
        when(lines.getMaintenRecordLineListByRecordId(1L)).thenReturn(List.of(new MesDvMaintenRecordLineDO()));
        service.submitMaintenRecord(1L);
        verify(equipment).updateMachineryLastMaintenTime(2L, time);
        verify(mapper).updateById(argThat((MesDvMaintenRecordDO row) -> row.getStatus().equals(MesDvMaintenRecordStatusEnum.SUBMITTED.getStatus())));
    }

    @Test
    void repairAcceptanceRequiresAwaitingAcceptanceAndRecordsActor() {
        var service = new MesDvRepairServiceImpl();
        var mapper = mock(MesDvRepairMapper.class);
        ReflectionTestUtils.setField(service, "repairMapper", mapper);
        var row = new MesDvRepairDO().setId(1L).setStatus(MesDvRepairStatusEnum.CONFIRMED.getStatus());
        when(mapper.selectById(1L)).thenReturn(row);
        assertThrows(ServiceException.class, () -> service.finishRepair(1L, 1, 3L));
        row.setStatus(MesDvRepairStatusEnum.APPROVING.getStatus());
        service.finishRepair(1L, 1, 3L);
        verify(mapper).updateById(argThat((MesDvRepairDO result) -> result.getStatus().equals(MesDvRepairStatusEnum.FINISHED.getStatus())
                && result.getConfirmUserId().equals(3L) && result.getResult().equals(1) && result.getConfirmDate() != null));
    }
}
