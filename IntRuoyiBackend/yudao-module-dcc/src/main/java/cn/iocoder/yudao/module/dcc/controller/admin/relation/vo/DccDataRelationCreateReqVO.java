package cn.iocoder.yudao.module.dcc.controller.admin.relation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - DCC 三方明确关联新增 Request VO")
@Data
public class DccDataRelationCreateReqVO {

    @NotNull(message = "DCC 产品目录不能为空")
    private Long productCatalogId;

    @NotNull(message = "DCC 项目代码不能为空")
    private Long projectCodeId;

    @NotNull(message = "注册证不能为空")
    private Long registrationCertificateId;

    private String relationRemark;
}
