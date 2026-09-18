package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - DCC 项目正式权限批量保存 Request VO")
@Data
public class DccProjectAccessRuleBatchSaveReqVO {

    @Schema(description = "正式权限规则列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "正式权限规则不能为空")
    private List<DccProjectAccessRuleSaveReqVO> rules;
}
