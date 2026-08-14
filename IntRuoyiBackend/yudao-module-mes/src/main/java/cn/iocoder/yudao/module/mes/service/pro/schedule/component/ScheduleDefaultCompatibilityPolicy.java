package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteServiceImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 排产默认值和历史兼容边界。
 */
@Component
public class ScheduleDefaultCompatibilityPolicy {

    private static final BigDecimal DEFAULT_SHIFT_HOURS = new BigDecimal("10.5");

    public BigDecimal defaultShiftHoursWhenMissing() {
        return DEFAULT_SHIFT_HOURS;
    }

    public BigDecimal shiftHoursOrDefault(BigDecimal shiftHours) {
        return shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0
                ? defaultShiftHoursWhenMissing()
                : shiftHours;
    }

    public String businessDefaultCapacityMode(String capacityMode, String actualMode, String plannedMode) {
        return actualMode.equalsIgnoreCase(capacityMode) ? actualMode : plannedMode;
    }

    public boolean businessDefaultPreserveManualLockedTasks(Boolean preserveManualLockedTasks) {
        return preserveManualLockedTasks == null || Boolean.TRUE.equals(preserveManualLockedTasks);
    }

    public BigDecimal historicalSnapshotScheduleQuantity(BigDecimal scheduledQuantity, BigDecimal workOrderQuantity) {
        if (scheduledQuantity != null && scheduledQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return scheduledQuantity;
        }
        return workOrderQuantity;
    }

    public <T> T historicalReadRouteMapIgnoreDeleted(Callable<T> callable) {
        return TenantUtils.executeIgnore(callable);
    }

    public boolean warnDefaultRouteScheduleConfig(MesProRouteScheduleConfigDO scheduleConfig) {
        return scheduleConfig != null
                && MesProRouteServiceImpl.DEFAULT_SCHEDULE_CONFIG_VERSION.equals(scheduleConfig.getConfigVersion());
    }

    @SuppressWarnings("unchecked")
    public boolean warnDefaultResourceSnapshot(MesProScheduleOrderProcessDO process) {
        if (process == null || StrUtil.isBlank(process.getResourceSnapshotJson())) {
            return false;
        }
        Map<String, Object> payload = JsonUtils.parseObject(process.getResourceSnapshotJson(), Map.class);
        if (payload == null) {
            return false;
        }
        Object configVersion = payload.get("configVersion");
        return MesProRouteServiceImpl.DEFAULT_SCHEDULE_CONFIG_VERSION.equals(configVersion);
    }

    public boolean failFastMissingRouteScheduleConfig(MesProRouteScheduleConfigDO scheduleConfig) {
        return scheduleConfig == null;
    }

    public boolean failFastMissingCalendarOrCapacity(boolean missingCalendarOrCapacity) {
        return missingCalendarOrCapacity;
    }

}
