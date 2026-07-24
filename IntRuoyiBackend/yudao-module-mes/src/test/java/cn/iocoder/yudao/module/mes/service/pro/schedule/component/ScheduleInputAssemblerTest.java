package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanReqVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_SCOPE_EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleInputAssemblerTest {

    private final ScheduleInputAssembler assembler =
            new ScheduleInputAssembler(new ScheduleDefaultCompatibilityPolicy());

    @Test
    void assemble_shouldDefaultPreviewContextWithoutSideEffects() {
        MesProAutoSchedulePreviewReqVO reqVO = new MesProAutoSchedulePreviewReqVO();
        reqVO.setScheduleOrderIds(List.of(501L));
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 10, 30));
        reqVO.setRuntimeCapacityBasis("PLANNED");

        ScheduleInputAssembler.ScheduleInputContext context = assembler.assemble(reqVO);

        assertFalse(context.replanMode());
        assertEquals("PLANNED", context.capacityMode());
        assertEquals(LocalDateTime.of(2026, 5, 14, 10, 30), context.requestStartTime());
        assertTrue(context.preserveManualLockedTasks());
    }

    @Test
    void assemble_shouldNormalizeReplanStartTimeToDateStartAndActualCapacity() {
        MesProAutoScheduleReplanReqVO reqVO = new MesProAutoScheduleReplanReqVO();
        reqVO.setScheduleOrderIds(List.of(501L));
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 10, 30));
        reqVO.setRuntimeCapacityBasis("actual");
        reqVO.setPreserveManualLockedTasks(Boolean.FALSE);

        ScheduleInputAssembler.ScheduleInputContext context = assembler.assemble(reqVO);

        assertTrue(context.replanMode());
        assertEquals("ACTUAL", context.capacityMode());
        assertEquals(LocalDateTime.of(2026, 5, 14, 0, 0), context.requestStartTime());
        assertFalse(context.preserveManualLockedTasks());
    }

    @Test
    void assemble_shouldRejectEmptyScheduleOrderScope() {
        MesProAutoSchedulePreviewReqVO reqVO = new MesProAutoSchedulePreviewReqVO();
        reqVO.setStartTime(LocalDateTime.of(2026, 5, 14, 10, 30));
        reqVO.setRuntimeCapacityBasis("PLANNED");

        ServiceException ex = assertThrows(ServiceException.class, () -> assembler.assemble(reqVO));

        assertEquals(PRO_AUTO_SCHEDULE_SCOPE_EMPTY.getCode(), ex.getCode());
    }

}
