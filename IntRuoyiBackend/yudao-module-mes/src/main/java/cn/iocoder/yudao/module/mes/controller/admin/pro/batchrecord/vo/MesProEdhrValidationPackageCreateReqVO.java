package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 验证包创建 Request VO")
@Data
public class MesProEdhrValidationPackageCreateReqVO {

    @Schema(description = "验证包名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "瑛泰 eDHR 验证包")
    @NotBlank(message = "验证包名称不能为空")
    private String packageName;

    @Schema(description = "客户项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "瑛泰商业化验证")
    @NotBlank(message = "客户项目名称不能为空")
    private String customerProjectName;

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

    @Schema(description = "验证负责人", requiredMode = Schema.RequiredMode.REQUIRED, example = "验证负责人")
    @NotBlank(message = "验证负责人不能为空")
    private String validationOwnerName;

    @Schema(description = "QA负责人", requiredMode = Schema.RequiredMode.REQUIRED, example = "QA负责人")
    @NotBlank(message = "QA负责人不能为空")
    private String qaOwnerName;

    @Schema(description = "备注")
    private String remark;
}
