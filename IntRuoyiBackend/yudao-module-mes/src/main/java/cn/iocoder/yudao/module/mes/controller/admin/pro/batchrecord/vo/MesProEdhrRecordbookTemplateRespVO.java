package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 记录本模板 Response VO")
@Data
public class MesProEdhrRecordbookTemplateRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板版本")
    private String templateVersion;

    @Schema(description = "记录本类型")
    private String recordbookType;

    @Schema(description = "条目字段定义 JSON")
    private String entrySchemaJson;

    @Schema(description = "标签策略 JSON")
    private String tagPolicyJson;

    @Schema(description = "模板状态")
    private String status;

    @Schema(description = "启用人")
    private Long activeBy;

    @Schema(description = "启用时间")
    private LocalDateTime activeAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
