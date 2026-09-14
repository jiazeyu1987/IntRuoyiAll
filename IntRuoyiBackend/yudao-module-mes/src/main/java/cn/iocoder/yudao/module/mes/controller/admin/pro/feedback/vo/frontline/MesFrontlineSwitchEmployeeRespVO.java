package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 一线设备账号切换实际填写员工 Response VO")
@Data
public class MesFrontlineSwitchEmployeeRespVO {

    @Schema(description = "登录账号用户编号")
    private Long loginUserId;
    @Schema(description = "实际填写员工编号")
    private Long actualEmployeeId;
    @Schema(description = "工艺路线编号")
    private Long routeId;
    @Schema(description = "工艺路线工序编号")
    private Long routeProcessId;
    @Schema(description = "工序编号")
    private Long processId;
    @Schema(description = "是否需要额外验证")
    private Boolean extraVerificationRequired;
    @Schema(description = "切换后当前模板")
    private MesFrontlineTemplateRespVO template;

}
