package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 排产工单删除请求")
@Data
public class MesProScheduleOrderDeleteReqVO {

    @Schema(description = "待删除排产工单及确认版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "排产工单不能为空")
    private List<@Valid Item> items;

    @Schema(description = "删除原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "删除原因不能为空")
    private String reason;

    @Data
    public static class Item {

        @Schema(description = "排产工单编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "排产工单编号不能为空")
        private Long id;

        @Schema(description = "用户确认时的最后更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "排产工单最后更新时间不能为空")
        private LocalDateTime expectedUpdateTime;
    }
}

