package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchCapacityUnificationAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchPolicySettingsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchShiftHoursRespVO;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MesProSchedulerWorkbenchService {

    MesProSchedulerWorkbenchSummaryRespVO getSummary(LocalDate date);

    MesProSchedulerWorkbenchShiftHoursRespVO getShiftHoursSetting();

    MesProSchedulerWorkbenchShiftHoursRespVO saveShiftHoursSetting(BigDecimal shiftHours);

    MesProSchedulerWorkbenchPolicySettingsRespVO getPolicySettings();

    MesProSchedulerWorkbenchPolicySettingsRespVO savePolicySettings(MesProSchedulerWorkbenchPolicySettingsRespVO reqVO);

    MesProSchedulerWorkbenchCapacityUnificationAuditRespVO getCapacityUnificationAudit();
}
