package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES eDHR 主数据追溯校验 Request VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordDomainTraceVerifyReqVO {

    @Schema(description = "执行记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    @NotNull(message = "执行记录编号不能为空")
    private Long executionId;

    @Schema(description = "调用方期望的主数据追溯 hash", example = "e3b0c44298fc1c149afbf4c8996fb924...")
    private String expectedDomainTraceHash;
}
