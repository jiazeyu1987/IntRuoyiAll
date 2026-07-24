package cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - SRM 供应商资格校验 Response VO")
@Data
public class SrmSupplierEligibilityRespVO {

    @Schema(description = "ERP 供应商编号", example = "2")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "山东瑛泰医疗器械有限公司")
    private String supplierName;

    @Schema(description = "是否通过校验", example = "true")
    private Boolean eligible;

    @Schema(description = "准入状态", example = "APPROVED")
    private String accessStatus;

    @Schema(description = "准入状态文案", example = "已通过")
    private String accessStatusLabel;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "未处理高风险数量", example = "0")
    private Long openHighRiskCount;

    @Schema(description = "阻断原因", example = "供应商存在未处理高风险")
    private String blockedReason;

    @Schema(description = "未处理高风险来源摘要")
    private List<String> openHighRiskSources;

    @Schema(description = "校验时间")
    private LocalDateTime checkedTime;
}
