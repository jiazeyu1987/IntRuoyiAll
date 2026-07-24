package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionSpecialNodeSkipReqVO {

    @NotNull(message = "任务 ID 不能为空")
    private Long taskId;

    @NotBlank(message = "跳过原因不能为空")
    private String reason;

    @NotBlank(message = "签名密码不能为空")
    private String password;

    private List<EdhrBatchExecutionSpecialNodeAttachmentVO> attachments;
}
