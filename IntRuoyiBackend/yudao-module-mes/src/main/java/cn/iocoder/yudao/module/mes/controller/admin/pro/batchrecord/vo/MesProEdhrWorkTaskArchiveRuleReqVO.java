package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrWorkTaskArchiveRuleReqVO {

    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @NotNull(message = "最终归档责任人不能为空")
    private Long assigneeUserId;

    @NotNull(message = "处理时限不能为空")
    @Min(value = 1, message = "处理时限必须大于 0")
    private Integer dueMinutes;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    private String remark;
}
