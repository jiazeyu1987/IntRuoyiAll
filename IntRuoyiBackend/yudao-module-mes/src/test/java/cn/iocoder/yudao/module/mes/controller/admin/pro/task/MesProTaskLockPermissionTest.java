package cn.iocoder.yudao.module.mes.controller.admin.pro.task;

import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.MesProTaskLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.MesProTaskSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProTaskLockPermissionTest {

    @Test
    void lockAndUnlock_shouldUseScheduleUpdatePermission() throws Exception {
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:update')",
                MesProTaskController.class.getMethod("lockTask", MesProTaskLockReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:update')",
                MesProTaskController.class.getMethod("unlockTask", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void taskEditing_shouldStillRequireTaskUpdatePermission() throws Exception {
        assertEquals("@ss.hasPermission('mes:pro-task:update')",
                MesProTaskController.class.getMethod("updateTask", MesProTaskSaveReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
    }
}
