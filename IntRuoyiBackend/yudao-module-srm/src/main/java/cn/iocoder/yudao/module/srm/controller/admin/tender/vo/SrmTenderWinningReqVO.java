package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmTenderWinningReqVO {

    @NotNull(message = "招标项目编号不能为空")
    private Long projectId;

    @NotNull(message = "中标候选编号不能为空")
    private Long candidateId;

    private String winningRemark;
}
