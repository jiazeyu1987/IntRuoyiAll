package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR OQ/PQ 执行 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrOqPqRunRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "用例ID")
    private Long caseId;

    @Schema(description = "用例类型")
    private String caseType;

    @Schema(description = "执行编号")
    private String runCode;

    @Schema(description = "执行状态")
    private String runStatus;

    @Schema(description = "执行环境")
    private String executionEnvironment;

    @Schema(description = "发布标签")
    private String releaseTag;

    @Schema(description = "数据库结构版本")
    private String schemaVersion;

    @Schema(description = "执行人")
    private String executorName;

    @Schema(description = "复核人")
    private String reviewerName;

    @Schema(description = "执行时间")
    private LocalDateTime executedAt;

    @Schema(description = "PQ真实业务路径")
    private String realBusinessPath;

    @Schema(description = "PQ真实测试数据来源")
    private String realTestDataSource;

    @Schema(description = "目标环境证明")
    private String targetEnvironmentProof;

    @Schema(description = "附件或证据标识")
    private String attachmentEvidence;

    @Schema(description = "证据校验值")
    private String evidenceChecksum;

    @Schema(description = "开放偏差数")
    private Integer openDeviationCount;

    @Schema(description = "执行结论")
    private String conclusion;

    @Schema(description = "阻断原因")
    private String blockedReason;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
