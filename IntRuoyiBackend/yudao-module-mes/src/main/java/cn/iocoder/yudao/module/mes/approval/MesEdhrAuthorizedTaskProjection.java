package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import java.util.Objects;

/** Internal, server-only pair retaining the complete row from the authorized query. */
public record MesEdhrAuthorizedTaskProjection(MesProEdhrWorkTaskDO task, MesProEdhrWorkTaskRespVO display) {
    public MesEdhrAuthorizedTaskProjection {
        Objects.requireNonNull(task, "EDHR_AUTHORIZED_TASK_REQUIRED");
        Objects.requireNonNull(display, "EDHR_AUTHORIZED_TASK_DISPLAY_REQUIRED");
        if (task.getId() == null || !Objects.equals(task.getId(), display.getId())) {
            throw new IllegalStateException("EDHR_AUTHORIZED_TASK_DISPLAY_IDENTITY_CONFLICT");
        }
    }
}
