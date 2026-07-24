package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 交付项目创建 Request VO")
@Data
public class MesProEdhrDeliveryProjectCreateReqVO {

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "瑛泰 eDHR 商业化交付")
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "瑛泰医疗")
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @Schema(description = "客户现场", requiredMode = Schema.RequiredMode.REQUIRED, example = "上海工厂")
    @NotBlank(message = "客户现场不能为空")
    private String siteName;

    @Schema(description = "系统范围", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "系统范围不能为空")
    private String systemScope;

    @Schema(description = "验证范围", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证范围不能为空")
    private String validationScope;

    @Schema(description = "发布标签", requiredMode = Schema.RequiredMode.REQUIRED, example = "edhr-commercial-t6")
    @NotBlank(message = "发布标签不能为空")
    private String releaseTag;

    @Schema(description = "数据库结构版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "schema-20260618")
    @NotBlank(message = "数据库结构版本不能为空")
    private String schemaVersion;

    @Schema(description = "目标环境", requiredMode = Schema.RequiredMode.REQUIRED, example = "test")
    @NotBlank(message = "目标环境不能为空")
    private String targetEnvironment;

    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED, example = "QA负责人")
    @NotBlank(message = "负责人不能为空")
    private String ownerName;

    @Schema(description = "负责部门", example = "质量/IT")
    private String ownerDepartment;

    @Schema(description = "备注")
    private String remark;
}
