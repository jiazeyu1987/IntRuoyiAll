package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "\u7BA1\u7406\u540E\u53F0 - MES \u8BBE\u5907\u53F0\u8D26\u6700\u7EC8\u7248\u5BF9\u9F50 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvMachineryFinalSyncRespVO {

    @Schema(description = "Excel \u6709\u6548\u8BBE\u5907\u4F7F\u7528\u884C\u6570", example = "83")
    private Integer excelEffectiveRowCount;

    @Schema(description = "\u5FFD\u7565\u7684\u5360\u4F4D\u7B26\u884C\u6570", example = "4")
    private Integer ignoredPlaceholderRowCount;

    @Schema(description = "\u4E3B\u8868\u8BBE\u5907\u6570", example = "31")
    private Integer machineryCount;

    @Schema(description = "\u660E\u7EC6\u884C\u6570", example = "83")
    private Integer processDetailCount;

    @Schema(description = "\u65B0\u521B\u5EFA\u8BBE\u5907\u6570", example = "31")
    private Integer createdCount;

    @Schema(description = "\u66F4\u65B0\u8BBE\u5907\u6570", example = "0")
    private Integer updatedCount;

    @Schema(description = "\u5220\u9664\u65E7\u8BBE\u5907\u6570", example = "40")
    private Integer deletedCount;

    @Schema(description = "\u9ED8\u8BA4\u8BBE\u5907\u7C7B\u578B\u7F16\u7801", example = "DEFAULT-MACHINERY-TYPE")
    private String defaultMachineryTypeCode;

    @Schema(description = "\u9ED8\u8BA4\u8F66\u95F4\u7F16\u7801", example = "AUTO-WSHOP")
    private String defaultWorkshopCode;
}
