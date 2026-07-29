package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateResolveCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 一线固定模板解析 Request VO")
@Data
public class MesProFrontlineTemplateResolveReqVO {

    @Schema(description = "实际作业员工编号", example = "1001")
    private Long actualEmployeeId;

    @Schema(description = "工艺路线工序编号", example = "2001")
    private Long routeProcessId;

    @Schema(description = "工序编号", example = "3001")
    private Long processId;

    @Schema(description = "模板编码", example = "PRODUCTION_SIMPLIFIED")
    private String templateCode;

    public FrontlineTemplateResolveCommand toCommand() {
        return new FrontlineTemplateResolveCommand(actualEmployeeId, routeProcessId, processId, templateCode);
    }
}
