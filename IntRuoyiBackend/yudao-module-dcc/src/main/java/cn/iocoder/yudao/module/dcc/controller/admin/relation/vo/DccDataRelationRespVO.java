package cn.iocoder.yudao.module.dcc.controller.admin.relation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 三方明确关联 Response VO")
@Data
public class DccDataRelationRespVO {

    private Long id;
    private Long productCatalogId;
    private Long projectCodeId;
    private Long registrationCertificateId;
    private String relationStatus;
    private String relationSource;
    private String relationRemark;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    private LocalDateTime createTime;
}
