package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SrmTenderCommitteeReqVO {

    @NotNull(message = "招标项目编号不能为空")
    private Long projectId;

    @NotBlank(message = "专家产生方式不能为空")
    private String applicationMethod;

    @NotBlank(message = "要求专业类型不能为空")
    private String requiredSpecialtyType;

    @NotNull(message = "要求专家人数不能为空")
    private Integer requiredExpertCount;

    @NotEmpty(message = "专家列表不能为空")
    private List<Long> expertIds;
}
