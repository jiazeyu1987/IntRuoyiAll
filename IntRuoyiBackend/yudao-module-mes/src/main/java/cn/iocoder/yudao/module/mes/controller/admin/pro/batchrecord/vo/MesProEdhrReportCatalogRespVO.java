package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR 报表目录 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrReportCatalogRespVO {

    private Long id;
    private String reportCode;
    private String reportName;
    private String reportCategory;
    private String businessPurpose;
    private String primaryDimensions;
    private String relatedDimensions;
    private String dataSourceSummary;
    private String permissionPolicy;
    private String exportPolicy;
    private String status;
    private String acceptanceStatus;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
