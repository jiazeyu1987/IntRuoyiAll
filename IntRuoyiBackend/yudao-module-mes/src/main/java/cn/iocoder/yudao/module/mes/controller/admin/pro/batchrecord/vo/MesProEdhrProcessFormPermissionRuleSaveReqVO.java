package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrProcessFormPermissionRuleSaveReqVO {

    @NotNull(message = "工艺路线工序不能为空")
    private Long routeProcessId;

    @NotBlank(message = "批记录报表不能为空")
    private String batchRecordReportId;

    @Valid
    @NotNull(message = "填写规则不能为空")
    private CandidateRule fillRule;

    @Valid
    private List<SignatureRule> signatureRules;

    @Data
    @Accessors(chain = true)
    public static class CandidateRule {

        @NotBlank(message = "候选来源类型不能为空")
        private String candidateSourceType;

        private List<Long> candidateSourceIds;

        @NotBlank(message = "完成策略不能为空")
        private String completionPolicy;

        @Min(value = 1, message = "处理时限必须大于 0")
        private Integer dueMinutes;

        private Boolean enabled;

        private String remark;
    }

    @Data
    @Accessors(chain = true)
    public static class SignatureRule {

        @NotBlank(message = "签名位不能为空")
        private String signatureCellKey;

        @NotBlank(message = "签名角色不能为空")
        private String signatureRole;

        @Valid
        @NotNull(message = "签名候选规则不能为空")
        private CandidateRule rule;
    }
}
