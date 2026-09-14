package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionGoldenFingerBulkVoidReqVO {

    @Valid
    @NotNull(message = "当前筛选条件不能为空")
    private EdhrBatchExecutionPageReqVO filter;

    @NotBlank(message = "原因分类不能为空")
    private String reasonCategory;

    @NotBlank(message = "原因说明不能为空")
    private String reasonText;

    @NotBlank(message = "电子签名密码不能为空")
    private String password;

    private String comment;
}
