package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 交付项目分页 Request VO")
@Data
public class MesProEdhrDeliveryProjectPageReqVO extends PageParam {

    @Schema(description = "项目编码", example = "EDHR-DEL-20260618090000001")
    private String projectCode;

    @Schema(description = "项目名称", example = "瑛泰 eDHR 商业化交付")
    private String projectName;

    @Schema(description = "客户名称", example = "瑛泰医疗")
    private String customerName;

    @Schema(description = "项目状态", example = "BLOCKED")
    private String projectStatus;
}
