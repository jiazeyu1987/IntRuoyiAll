package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class EdhrRecordChangeRequestReqVO {

    private Long batchExecutionId;
    private Long executionId;
    private Long sourceArchiveId;

    @NotBlank(message = "原因分类不能为空")
    private String reasonCategory;

    @NotBlank(message = "原因说明不能为空")
    private String reasonText;

    @NotBlank(message = "电子签名密码不能为空")
    private String password;

    private String comment;

    private Map<String, List<Long>> startUserSelectAssignees;

}
