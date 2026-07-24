package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 记录本创建 Request VO")
@Data
public class MesProEdhrRecordbookCreateReqVO {

    @Schema(description = "模板 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模板 ID 不能为空")
    private Long templateId;

    @Schema(description = "记录本编码")
    private String recordbookCode;

    @Schema(description = "记录本名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "记录本名称不能为空")
    private String recordbookName;

    @Schema(description = "责任人")
    private Long ownerUserId;

    @Schema(description = "责任部门")
    private Long ownerDeptId;

    @Schema(description = "业务范围")
    private String businessScope;

    @Schema(description = "业务对象类型")
    private String businessObjectType;

    @Schema(description = "业务对象 ID")
    private Long businessObjectId;

    @Schema(description = "业务对象编码")
    private String businessObjectCode;

    @Schema(description = "备注")
    private String remark;
}
