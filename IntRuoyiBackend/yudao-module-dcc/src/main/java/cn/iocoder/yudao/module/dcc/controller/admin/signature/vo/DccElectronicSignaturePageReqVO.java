package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - DCC电子签名记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccElectronicSignaturePageReqVO extends PageParam {

    @Schema(description = "受控文件ID", example = "900")
    private Long controlledFileId;

    @Schema(hidden = true)
    private Set<Long> controlledFileIds;

    @Schema(description = "文件编号", example = "DCC-SOP-001")
    private String fileNumber;

    @Schema(description = "修订ID", example = "900")
    private Long revisionId;

    @Schema(description = "版本号", example = "A.1")
    private String versionNo;

    @Schema(description = "签名人用户ID", example = "99")
    private Long signerUserId;

    @Schema(description = "任务动作结果", example = "APPROVED")
    private String taskActionResult;

    @Schema(hidden = true)
    private String persistentActionType;

    @Schema(description = "签名含义编码", example = "REVIEW_APPROVE")
    private String meaningCode;

    @Schema(description = "受控副本哈希状态", example = "NOT_APPLICABLE")
    private String controlledCopyHashStatus;

    @Schema(description = "证据哈希短码", example = "6f2c91ab03d4")
    private String evidenceHashShort;

    @Schema(description = "证据状态", example = "VALID")
    private String evidenceStatus;

    @Schema(description = "签名时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] signedAt;

    @Schema(description = "快速过滤")
    private QuickFilter quickFilter;
}
