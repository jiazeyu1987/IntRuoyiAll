package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 电子批记录 Word 导入预检 Response VO")
@Data
public class BatchRecordReportImportPreflightRespVO {

    @Schema(description = "识别路线", example = "B")
    private String routeKey;

    @Schema(description = "批记录名称", example = "球囊扩张压力泵")
    private String batchRecordName;

    @Schema(description = "批记录定义 ID", example = "1001")
    private Long batchRecordDefinitionId;

    @Schema(description = "当前批记录版本 ID", example = "2001")
    private Long currentBatchRecordVersionId;

    @Schema(description = "当前批记录版本号", example = "V2.0")
    private String currentBatchRecordVersionNo;

    @Schema(description = "当前批记录版本状态", example = "APPROVED")
    private String currentBatchRecordVersionStatus;

    @Schema(description = "最新已生成批记录版本 ID", example = "2002")
    private Long latestBatchRecordVersionId;

    @Schema(description = "最新已生成批记录版本号", example = "V3.0")
    private String latestBatchRecordVersionNo;

    @Schema(description = "最新已生成批记录版本状态", example = "PENDING_APPROVAL")
    private String latestBatchRecordVersionStatus;

    @Schema(description = "当前批记录版本是否仍有主批记录表单", example = "true")
    private Boolean currentBatchRecordHasMainReports;

    @Schema(description = "路线治理状态：CREATE_REQUIRED/UPGRADE_REQUIRED/DUPLICATE_BLOCKED", example = "UPGRADE_REQUIRED")
    private String routeGovernanceStatus;

    @Schema(description = "是否需要确认工艺路线升版本", example = "true")
    private Boolean routeUpgradeRequired;

    @Schema(description = "重复同名工艺路线")
    private List<DuplicateRouteRespVO> duplicateRoutes;

    @Schema(description = "当前工艺路线 ID", example = "3001")
    private Long currentRouteId;

    @Schema(description = "当前工艺路线编码", example = "ROUTE202607120001")
    private String currentRouteCode;

    @Schema(description = "当前工艺路线名称", example = "球囊扩张压力泵方案")
    private String currentRouteName;

    @Schema(description = "当前工艺路线版本 ID", example = "4001")
    private Long currentRouteVersionId;

    @Schema(description = "当前工艺路线版本号", example = "V1")
    private String currentRouteVersionNo;

    @Schema(description = "当前工艺路线版本是否活跃", example = "true")
    private Boolean currentRouteVersionActive;

    @Schema(description = "是否存在历史业务引用", example = "true")
    private Boolean hasHistoricalReferences;

    @Schema(description = "历史业务引用清单")
    private List<ReferenceBlockerRespVO> referenceBlockers;

    @Schema(description = "当前允许的导入动作", example = "[\"UPGRADE\"]")
    private List<String> allowedActions;

    @Schema(description = "推荐导入动作", example = "UPGRADE")
    private String recommendedAction;

    @Schema(description = "下一版本号", example = "V2.0")
    private String nextVersionNo;

    @Schema(description = "可选择重建的产线项")
    private List<BatchRecordReportImportRouteProductRespVO> routeProductOptions;

    @Schema(description = "历史业务引用项")
    @Data
    public static class ReferenceBlockerRespVO {

        @Schema(description = "版本号", example = "V1.0")
        private String versionNo;

        @Schema(description = "引用名称", example = "存在批记录执行")
        private String referenceName;

        @Schema(description = "引用数量", example = "1")
        private Long count;

        @Schema(description = "处理入口", example = "eDHR 批记录 > 批次执行")
        private String cleanupEntrance;

        @Schema(description = "处理方式", example = "删除或作废执行记录")
        private String cleanupAction;
    }

    @Schema(description = "重复同名工艺路线")
    @Data
    public static class DuplicateRouteRespVO {

        @Schema(description = "工艺路线 ID", example = "3001")
        private Long routeId;

        @Schema(description = "工艺路线编码", example = "RT000030")
        private String routeCode;

        @Schema(description = "工艺路线名称", example = "球囊扩张压力泵")
        private String routeName;

        @Schema(description = "当前工艺路线版本 ID", example = "4001")
        private Long routeVersionId;

        @Schema(description = "当前工艺路线版本号", example = "V2")
        private String routeVersionNo;
    }
}
