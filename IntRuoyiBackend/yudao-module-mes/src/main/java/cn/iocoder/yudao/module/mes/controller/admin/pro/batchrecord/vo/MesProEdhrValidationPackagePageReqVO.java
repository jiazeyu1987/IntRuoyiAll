package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 验证包分页 Request VO")
@Data
public class MesProEdhrValidationPackagePageReqVO extends PageParam {

    @Schema(description = "验证包编码", example = "EDHR-VAL-20260618010101001")
    private String packageCode;

    @Schema(description = "验证包名称", example = "瑛泰 eDHR 验证包")
    private String packageName;

    @Schema(description = "客户项目名称", example = "瑛泰商业化验证")
    private String customerProjectName;

    @Schema(description = "验证包状态", example = "BLOCKED")
    private String validationStatus;
}
