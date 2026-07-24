package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单中心生效执行 Response VO")
@Data
public class FormEffectExecutionRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "表单实例编号")
    private Long instanceId;

    @Schema(description = "执行编码")
    private String executionCode;

    @Schema(description = "幂等键")
    private String idempotencyKey;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "业务结果引用")
    private String resultRef;

    @Schema(description = "失败原因")
    private String failureReason;

}
