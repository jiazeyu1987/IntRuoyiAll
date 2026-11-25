package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - MES 自动排产重排 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesProAutoScheduleReplanReqVO extends MesProAutoSchedulePreviewReqVO {

    public interface Apply extends jakarta.validation.groups.Default {}

    @Schema(description = "重排应用幂等键")
    @jakarta.validation.constraints.NotBlank(groups = Apply.class, message = "重排幂等键不能为空")
    @jakarta.validation.constraints.Size(max = 128, message = "重排幂等键不能超过128字符")
    @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_-]+", message = "重排幂等键仅允许字母、数字、下划线和连字符")
    private String idempotencyKey;
}
