package cn.iocoder.yudao.module.mes.controller.admin.pro.dccprojectgovernance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES DCC 项目治理状态 Response VO")
@Data
public class MesProDccProjectGovernanceStatusRespVO {

    @Schema(description = "DCC 项目名称", example = "球囊扩张压力泵")
    private String projectName;

    @Schema(description = "DCC 项目代码数量", example = "1")
    private Integer dccProjectCodeCount;

    @Schema(description = "工艺路线状态：OK/MISSING/DUPLICATE", example = "OK")
    private String routeStatus;

    @Schema(description = "工艺路线数量", example = "1")
    private Long routeCount;

    @Schema(description = "工艺路线编码列表")
    private List<String> routeCodes;

    @Schema(description = "主批记录状态：OK/MISSING/DUPLICATE", example = "OK")
    private String mainBatchRecordStatus;

    @Schema(description = "主批记录数量", example = "1")
    private Long mainBatchRecordCount;

    @Schema(description = "主批记录版本列表")
    private List<String> mainBatchRecordVersionNos;

    @Schema(description = "损耗单状态：OK/MISSING/DUPLICATE", example = "OK")
    private String lossReportStatus;

    @Schema(description = "损耗单数量", example = "1")
    private Long lossReportCount;

    @Schema(description = "损耗单编码列表")
    private List<String> lossReportCodes;

    @Schema(description = "过程检验单状态：OK/MISSING/DUPLICATE", example = "OK")
    private String processInspectionStatus;

    @Schema(description = "过程检验单数量", example = "1")
    private Long processInspectionCount;

    @Schema(description = "过程检验单编码列表")
    private List<String> processInspectionCodes;

    @Schema(description = "参数记录表状态：OK/MISSING/DUPLICATE", example = "OK")
    private String parameterRecordStatus;

    @Schema(description = "参数记录表数量", example = "1")
    private Long parameterRecordCount;

    @Schema(description = "参数记录表编码列表")
    private List<String> parameterRecordCodes;

    @Schema(description = "阻断提示")
    private List<String> blockerMessages;
}
