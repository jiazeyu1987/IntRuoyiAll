package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 表单中心策略槽位 Request VO")
@Data
public class FormPolicySlotReqVO {

    @Schema(description = "槽位编码")
    @NotBlank
    private String slotCode;

    @Schema(description = "是否必填")
    @NotNull
    private Boolean required;

    @Schema(description = "模板稳定编号")
    @NotNull
    private Long templateId;

}
