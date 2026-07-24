package cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - SRM 供应商门户申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmSupplierPortalApplicationPageReqVO extends PageParam {

    @Schema(description = "申请编号")
    private Long id;

    @Schema(description = "企业名称")
    private String companyName;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "申请状态")
    private String applicationStatus;
}
