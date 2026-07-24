package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FormTemplateObsoleteReqVO {

    @NotBlank(message = "作废原因不能为空")
    private String reason;

    private Map<String, List<Long>> startUserSelectAssignees;

}
