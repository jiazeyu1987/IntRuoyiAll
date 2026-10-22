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

    private String projectLeader; // 仅展示快照，服务从账号目录解析
    @jakarta.validation.constraints.NotNull(message = "请选择项目负责人账号")
    private Long projectLeaderUserId;
    @jakarta.validation.constraints.NotNull(message = "请选择文件夹模板")
    private Long folderTemplateId;
    @jakarta.validation.constraints.NotNull(message = "请填写项目默认属性")
    private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes defaultAttributes;

    @NotBlank(message = "产品编码不能为空")
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @NotBlank(message = "分类不能为空")
    private String classification;

    private String remark;
    @jakarta.validation.constraints.Size(max = 500, message = "新建申请原因不能超过500字符")
    private String creationReason; // create必填；resubmit使用独立resubmissionReason，不复用或猜值
    @jakarta.validation.constraints.Size(max = 500, message = "重提说明不能超过500字符")
    private String resubmissionReason;
}
