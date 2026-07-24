package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class EdhrRecordChangeApproveReqVO {

    @NotNull(message = "变更事件不能为空")
    private Long changeEventId;

    @NotBlank(message = "电子签名密码不能为空")
    private String password;

    private String comment;

}
