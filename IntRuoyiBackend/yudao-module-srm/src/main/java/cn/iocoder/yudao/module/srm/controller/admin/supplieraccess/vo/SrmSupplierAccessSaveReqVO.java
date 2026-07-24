package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - SRM 供应商准入新增/修改 Request VO")
@Data
public class SrmSupplierAccessSaveReqVO {

    @Schema(description = "准入档案编号", example = "1")
    private Long id;

    @Schema(description = "ERP 供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "ERP 供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "准入备注", example = "测试租户准入申请")
    private String accessRemark;

    @Schema(description = "门户联系人", example = "张三")
    private String portalContactName;

    @Schema(description = "门户联系电话", example = "13800138000")
    private String portalContactPhone;

    @Schema(description = "资质到期日")
    private LocalDate qualificationExpireDate;
}
