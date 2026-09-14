package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrBatchRecordFormPermissionRuleSaveReqVO {

    @NotBlank(message = "批记录报表不能为空")
    private String batchRecordReportId;

    @Valid
    private MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule fillRule;

    @Valid
    private List<FillAssignment> fillAssignments;

    @Data
    @Accessors(chain = true)
    public static class FillAssignment {

        @NotBlank(message = "辅助行标识不能为空")
        private String scopeKey;

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
}
