package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrTravelerGenerateReqVO {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotNull(message = "eDHR批次执行ID不能为空")
    private Long batchExecutionId;

    @NotNull(message = "路线工序ID不能为空")
    private Long routeProcessId;

    private String serialNo;

    private String requestId;

    private String remark;
}
