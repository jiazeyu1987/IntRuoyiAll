package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 表单中心业务动作上下文 Request VO")
@Data
public class BusinessActionContextReqVO {

    private Long tenantId;

    @NotBlank(message = "数据域不能为空")
    private String dataDomain;

    @NotBlank(message = "系统编码不能为空")
    private String systemCode;

    @NotBlank(message = "对象类型不能为空")
    private String objectType;

    @NotBlank(message = "对象编号不能为空")
    private String objectId;

    @NotBlank(message = "对象版本不能为空")
    private String objectVersion;

    @NotBlank(message = "动作编码不能为空")
    private String actionCode;

    @NotBlank(message = "对象状态不能为空")
    private String objectState;

    private String orgCode;

    private String deptCode;

    private List<String> roleCodes;

    private String productCode;

    private String categoryCode;

    private String reason;

}
