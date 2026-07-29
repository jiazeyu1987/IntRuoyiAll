package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplatePayloadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "管理后台 - MES 一线固定模板 payload 校验 Request VO")
@Data
public class MesProFrontlineTemplatePayloadReqVO {

    @Schema(description = "生产工单编号", example = "10")
    private Long workOrderId;

    @Schema(description = "工艺路线编号", example = "20")
    private Long routeId;

    @Schema(description = "工序编号", example = "30")
    private Long processId;

    @Schema(description = "工艺路线工序编号", example = "40")
    private Long routeProcessId;

    @Schema(description = "实际作业员工编号", example = "50")
    private Long actualEmployeeId;

    @Schema(description = "模板编码", example = "PRODUCTION_SIMPLIFIED")
    private String templateCode;

    @Schema(description = "模板字段值")
    private Map<String, Object> fieldValues;

    private Object submitTime;

    private Object submittedAt;

    private Object feedbackTime;

    public FrontlineTemplatePayloadCommand toCommand() {
        Map<String, Object> commandFieldValues = new LinkedHashMap<>();
        if (fieldValues != null) {
            commandFieldValues.putAll(fieldValues);
        }
        if (submitTime != null) {
            commandFieldValues.put("submitTime", submitTime);
        }
        if (submittedAt != null) {
            commandFieldValues.put("submittedAt", submittedAt);
        }
        if (feedbackTime != null) {
            commandFieldValues.put("feedbackTime", feedbackTime);
        }
        return new FrontlineTemplatePayloadCommand(
                workOrderId, routeId, processId, routeProcessId, actualEmployeeId, templateCode, commandFieldValues);
    }
}
