package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FormInstanceSubmitReqVO {

    private Map<String, Object> formData;

    private Map<String, List<Long>> startUserSelectAssignees;

}
