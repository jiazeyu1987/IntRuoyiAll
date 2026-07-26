package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线批记录附件负责人明细保存 Request VO")
@Data
@Accessors(chain = true)
public class MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO {

    @Schema(description = "附件配置编码", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "INCOMING_INSPECTION_REPORT")
    @NotBlank(message = "批记录附件编码不能为空")
    private String attachmentCode;

    @Schema(description = "候选来源类型：USERS/ROLE", requiredMode = Schema.RequiredMode.REQUIRED, example = "ROLE")
    @NotBlank(message = "批记录附件负责人来源不能为空")
    private String candidateSourceType;

    @Schema(description = "候选来源 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "批记录附件负责人不能为空")
    private List<Long> candidateSourceIds;

    @Schema(description = "候选来源名称快照")
    private List<String> candidateSourceNames;

    @Schema(description = "备注")
    private String remark;
}
