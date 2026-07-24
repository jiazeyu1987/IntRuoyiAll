package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - MES eDHR 记录本条目 Response VO")
@Data
public class MesProEdhrRecordbookEntryRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "条目编码")
    private String entryCode;

    @Schema(description = "记录本 ID")
    private Long recordbookId;

    @Schema(description = "记录本编码")
    private String recordbookCode;

    @Schema(description = "模板 ID")
    private Long templateId;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板版本")
    private String templateVersion;

    @Schema(description = "条目字段定义 JSON")
    private String entrySchemaJson;

    @Schema(description = "条目状态")
    private String status;

    @Schema(description = "版本")
    private Integer version;

    @Schema(description = "条目标题")
    private String entryTitle;

    @Schema(description = "条目正文")
    private Map<String, Object> entryContent;

    @Schema(description = "受控标签编码")
    private List<String> tagCodes;

    @Schema(description = "标签快照 JSON")
    private String tagSnapshotJson;

    @Schema(description = "提交人")
    private Long submittedBy;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "正文锁定时间")
    private LocalDateTime lockedAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
