package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - SRM 供应商引用 Response VO")
@Data
public class SrmSupplierReferenceRespVO {

    @Schema(description = "ERP 供应商编号", example = "2")
    private Long id;

    @Schema(description = "供应商名称", example = "山东瑛泰医疗器械有限公司")
    private String name;
}
