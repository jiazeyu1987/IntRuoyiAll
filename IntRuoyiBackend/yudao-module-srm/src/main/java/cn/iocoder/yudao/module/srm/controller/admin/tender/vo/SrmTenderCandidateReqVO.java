package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SrmTenderCandidateReqVO {

    @NotNull(message = "招标项目编号不能为空")
    private Long projectId;

    @NotEmpty(message = "候选投标不能为空")
    private List<Long> submissionIds;
}
