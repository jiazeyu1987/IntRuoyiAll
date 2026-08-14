package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionLogBO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 生产报工修改记录 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPoolProductionReportRevisionLogRespVO {

    @Schema(description = "修改人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modifiedByName;
    @Schema(description = "修改时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime modifiedAt;
    @Schema(description = "修改原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String changeReason;
    @Schema(description = "是否已完成电子签名", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean signatureConfirmed;
    @Schema(description = "业务字段修改明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FieldChangeRespVO> changes;

    public static ProcessPoolProductionReportRevisionLogRespVO from(
            MesProcessPoolProductionReportRevisionLogBO source) {
        return ProcessPoolProductionReportRevisionLogRespVO.builder()
                .modifiedByName(source.getModifiedByName())
                .modifiedAt(source.getModifiedAt())
                .changeReason(source.getChangeReason())
                .signatureConfirmed(source.getSignatureConfirmed())
                .changes(source.getChanges().stream().map(FieldChangeRespVO::from).toList())
                .build();
    }

    @Schema(description = "管理后台 - MES 生产报工字段修改明细 Response VO")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldChangeRespVO {

        @Schema(description = "业务字段名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String fieldName;
        @Schema(description = "修改前", requiredMode = Schema.RequiredMode.REQUIRED)
        private String beforeValue;
        @Schema(description = "修改后", requiredMode = Schema.RequiredMode.REQUIRED)
        private String afterValue;

        private static FieldChangeRespVO from(MesProcessPoolProductionReportRevisionLogBO.FieldChange source) {
            return FieldChangeRespVO.builder()
                    .fieldName(source.getFieldName())
                    .beforeValue(source.getBeforeValue())
                    .afterValue(source.getAfterValue())
                    .build();
        }
    }
}
