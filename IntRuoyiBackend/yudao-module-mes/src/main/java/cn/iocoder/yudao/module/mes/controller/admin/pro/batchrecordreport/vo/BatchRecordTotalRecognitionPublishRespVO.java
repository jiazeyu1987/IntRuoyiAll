package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产批记录总识别 JSON 发布 Response VO")
@Data
public class BatchRecordTotalRecognitionPublishRespVO {

    @Schema(description = "发布动作：CREATED_ROUTE=新建路线，UPDATED_ROUTE=更新已有路线候选", example = "UPDATED_ROUTE")
    private String action;

    @Schema(description = "DCC 项目代码 ID", example = "1001")
    private Long dccProjectCodeId;

    @Schema(description = "DCC 项目代码", example = "PQC-ID-001")
    private String projectCode;

    @Schema(description = "DCC 项目名称", example = "球囊扩张压力泵")
    private String projectName;

    @Schema(description = "工艺路线 ID", example = "2001")
    private Long routeId;

    @Schema(description = "工艺路线编码", example = "ROUTE202609180001")
    private String routeCode;

    @Schema(description = "工艺路线名称", example = "球囊扩张压力泵")
    private String routeName;

    @Schema(description = "当前正式路线版本 ID", example = "3001")
    private Long routeVersionId;

    @Schema(description = "当前正式路线版本号", example = "V1")
    private String routeVersionNo;

    @Schema(description = "写入的路线候选版本 ID", example = "3002")
    private Long routeCandidateVersionId;

    @Schema(description = "写入的路线候选版本号", example = "V2")
    private String routeCandidateVersionNo;

    @Schema(description = "JSON 工序数量", example = "14")
    private Integer processCount;

    @Schema(description = "本次同步到候选生产配置的工序数量", example = "14")
    private Integer updatedProcessCount;

    @Schema(description = "已保存 JSON 的 SHA-256")
    private String recognitionJsonSha256;

    @Schema(description = "发布时间")
    private LocalDateTime updateTime;
}
