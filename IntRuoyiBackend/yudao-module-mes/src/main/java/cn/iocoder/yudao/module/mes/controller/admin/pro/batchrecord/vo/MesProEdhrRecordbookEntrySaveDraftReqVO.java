package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - MES eDHR 记录本条目保存草稿 Request VO")
@Data
public class MesProEdhrRecordbookEntrySaveDraftReqVO {

    @Schema(description = "条目 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "条目 ID 不能为空")
    private Long id;

    @Schema(description = "条目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "条目标题不能为空")
    private String entryTitle;

    @Schema(description = "条目正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "条目正文不能为空")
    private Map<String, Object> entryContent;

    @Schema(description = "受控标签编码")
    private List<String> tagCodes;

    @Schema(description = "备注")
    private String remark;
}
