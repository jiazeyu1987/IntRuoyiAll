package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 一线设备账号当前模板 Response VO")
@Data
public class MesFrontlineTemplateRespVO {

    @Schema(description = "模板编号")
    private String templateNo;
    @Schema(description = "模板类型")
    private String templateType;
    @Schema(description = "工艺路线工序编号")
    private Long routeProcessId;
    @Schema(description = "工序编号")
    private Long processId;
    @Schema(description = "实际填写员工编号")
    private Long actualEmployeeId;

}
