package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanReqVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_SCOPE_EMPTY;

@Component
public class ScheduleInputAssembler {

    private static final String CAPACITY_MODE_PLANNED = "PLANNED";
    private static final String CAPACITY_MODE_ACTUAL = "ACTUAL";

    private final ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy;

    public ScheduleInputAssembler(ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy) {
        this.scheduleDefaultCompatibilityPolicy = scheduleDefaultCompatibilityPolicy;
    }

    public ScheduleInputContext assemble(MesProAutoSchedulePreviewReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getScheduleOrderIds())) {
            throw exception(PRO_AUTO_SCHEDULE_SCOPE_EMPTY);
        }
        boolean replanMode = reqVO instanceof MesProAutoScheduleReplanReqVO;
        String capacityMode = scheduleDefaultCompatibilityPolicy.businessDefaultCapacityMode(
                reqVO.getRuntimeCapacityBasis(), CAPACITY_MODE_ACTUAL, CAPACITY_MODE_PLANNED);
        return new ScheduleInputContext(
                replanMode,
                capacityMode,
                normalizeRequestStartTime(reqVO.getStartTime(), replanMode),
                scheduleDefaultCompatibilityPolicy.businessDefaultPreserveManualLockedTasks(
                        reqVO.getPreserveManualLockedTasks()));
    }

    private LocalDateTime normalizeRequestStartTime(LocalDateTime requestStartTime, boolean replanMode) {
        if (requestStartTime == null || !replanMode) {
            return requestStartTime;
        }
        return requestStartTime.toLocalDate().atStartOfDay();
    }

    public record ScheduleInputContext(
            boolean replanMode,
            String capacityMode,
            LocalDateTime requestStartTime,
            boolean preserveManualLockedTasks) {
    }

}
