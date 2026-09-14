package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DCC controlled print creation request.
 */
@Data
public class DccControlledFilePrintCreateReqVO {

    @NotBlank(message = "打印用途不能为空")
    private String purpose;

    @NotNull(message = "份数不能为空")
    @Min(value = 1, message = "份数必须大于 0")
    private Integer copies;

    @NotBlank(message = "接收部门不能为空")
    private String receivingDepartment;

    @NotBlank(message = "使用位置不能为空")
    private String useLocation;

}
