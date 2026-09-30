package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 项目代码与产品目录联合新建申请 Request VO")
@Data
public class DccProjectProductCreateReqVO {

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @NotBlank(message = "项目代码不能为空")
    private String projectCode;

    @NotBlank(message = "项目负责人不能为空")
    private String projectLeader;

    @NotBlank(message = "产品编码不能为空")
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @NotBlank(message = "分类不能为空")
    private String classification;

    private String remark;
}
