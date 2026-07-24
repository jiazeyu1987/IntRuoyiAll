package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产员工作台冒烟测试启动 Request VO")
@Data
public class MesProSchedulerWorkbenchSmokeTestStartReqVO {

    @Schema(description = "是否开启报工审批；默认 false")
    private Boolean feedbackApprovalEnabled;

}
