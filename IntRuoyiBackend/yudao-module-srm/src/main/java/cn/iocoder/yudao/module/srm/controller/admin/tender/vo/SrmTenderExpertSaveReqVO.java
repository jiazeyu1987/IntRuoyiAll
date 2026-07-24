package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SrmTenderExpertSaveReqVO {

    @NotBlank(message = "专家姓名不能为空")
    private String expertName;

    @NotBlank(message = "专家专业类型不能为空")
    private String specialtyType;
}
