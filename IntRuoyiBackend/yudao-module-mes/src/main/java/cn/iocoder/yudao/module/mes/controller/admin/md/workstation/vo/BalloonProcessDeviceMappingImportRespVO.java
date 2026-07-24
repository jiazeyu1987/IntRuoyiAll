package cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 球囊工序工作站设备关系同步 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalloonProcessDeviceMappingImportRespVO {

    @Schema(description = "工序数量", example = "33")
    private Integer processCount;

    @Schema(description = "设备数量", example = "31")
    private Integer machineryCount;

    @Schema(description = "设备工序明细数量", example = "53")
    private Integer machineryProcessCount;

    @Schema(description = "复用工作站数量", example = "18")
    private Integer reusedWorkstationCount;

    @Schema(description = "新建工作站数量", example = "15")
    private Integer createdWorkstationCount;

    @Schema(description = "设备绑定数量", example = "53")
    private Integer machineryBindingCount;

    @Schema(description = "人工工序数量", example = "3")
    private Integer manualProcessCount;

    @Schema(description = "忽略的占位符行数量", example = "4")
    private Integer ignoredPlaceholderRowCount;

    @Schema(description = "新建设备数量", example = "0")
    private Integer createdMachineryCount;

    @Schema(description = "更新设备数量", example = "0")
    private Integer updatedMachineryCount;

    @Schema(description = "被忽略的产能冲突对数量", example = "2")
    private Integer ignoredCapacityConflictPairCount;

    @Schema(description = "被忽略的产能冲突详情")
    private List<IgnoredCapacityConflictPair> ignoredCapacityConflictPairs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "被忽略的产能冲突详情")
    public static class IgnoredCapacityConflictPair {

        @Schema(description = "工序名称", example = "外管拉伸2")
        private String processName;

        @Schema(description = "设备编码", example = "A03388")
        private String machineryCode;

        @Schema(description = "冲突来源行号")
        private List<Integer> sourceRowNos;

        @Schema(description = "冲突的10.5小时日产能原始值")
        private List<String> dailyCapacities;
    }
}
