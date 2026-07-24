package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrBatchRecordFormPermissionRuleSaveReqVO {

    @NotBlank(message = "批记录报表不能为空")
    private String batchRecordReportId;

    @Valid
    @NotNull(message = "填写规则不能为空")
    private MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule fillRule;
}
