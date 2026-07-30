package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventRevisionFieldChangeBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventRevisionUpdateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFragmentOriginalField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工序池原始记录修改 Request VO")
@Data
@Accessors(chain = true)
public class ProcessPoolEventRevisionUpdateReqVO {

    @Schema(description = "工序池提交事件ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "提交事件不能为空")
    private Long eventId;

    @Schema(description = "修改后的原始 payload JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修改后的原始 payload 不能为空")
    private String afterPayload;

    @Schema(description = "修改原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修改原因不能为空")
    private String changeReason;

    @Schema(description = "重新电子签名ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9002")
    @NotNull(message = "重新电子签名不能为空")
    private Long revisionSignatureId;

    @Schema(description = "重新电子签名用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "重新电子签名用户不能为空")
    private Long revisionSignatureUserId;

    @Schema(description = "重新电子签名快照", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "重新电子签名快照不能为空")
    private String revisionSignatureSnapshot;

    @Schema(description = "修改人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "修改人不能为空")
    private Long modifiedByUserId;

    @Schema(description = "字段级修改明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "字段级修改明细不能为空")
    private List<FieldChangeReqVO> changedFields;

    public MesProcessPoolEventRevisionUpdateReqBO toBO() {
        return MesProcessPoolEventRevisionUpdateReqBO.builder()
                .eventId(eventId)
                .afterPayload(afterPayload)
                .changeReason(changeReason)
                .revisionSignatureId(revisionSignatureId)
                .revisionSignatureUserId(revisionSignatureUserId)
                .revisionSignatureSnapshot(revisionSignatureSnapshot)
                .modifiedByUserId(modifiedByUserId)
                .changedFields(changedFields.stream()
                        .map(FieldChangeReqVO::toBO)
                        .toList())
                .build();
    }

    @Schema(description = "管理后台 - MES 工序池原始记录字段修改明细 Request VO")
    @Data
    @Accessors(chain = true)
    public static class FieldChangeReqVO {

        @Schema(description = "模板字段编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "字段编码不能为空")
        private String fieldCode;

        @Schema(description = "模板字段名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "字段名称不能为空")
        private String fieldName;

        @Schema(description = "修改前字段值")
        private String beforeValue;

        @Schema(description = "修改后字段值")
        private String afterValue;

        @Schema(description = "是否影响数量片段/FIFO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "是否影响数量片段不能为空")
        private Boolean affectsQuantityFragment;

        @Schema(description = "来源数量片段ID；影响数量片段时必填", example = "8001")
        private Long sourceQuantityFragmentId;

        @Schema(description = "原始字段枚举", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "原始字段枚举不能为空")
        private MesProcessPoolFragmentOriginalField originalField;

        public MesProcessPoolEventRevisionFieldChangeBO toBO() {
            return MesProcessPoolEventRevisionFieldChangeBO.builder()
                    .fieldCode(fieldCode)
                    .fieldName(fieldName)
                    .beforeValue(beforeValue)
                    .afterValue(afterValue)
                    .affectsQuantityFragment(affectsQuantityFragment)
                    .sourceQuantityFragmentId(sourceQuantityFragmentId)
                    .originalField(originalField)
                    .build();
        }

        @JsonIgnore
        @AssertTrue(message = "影响数量片段的字段必须提供来源数量片段ID")
        public boolean isSourceQuantityFragmentPresentWhenAffectsQuantityFragment() {
            return !Boolean.TRUE.equals(affectsQuantityFragment) || sourceQuantityFragmentId != null;
        }
    }
}
