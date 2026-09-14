package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest.DccRegistrationCertificateAccessRequestCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 国内注册证访问申请提交请求参数")
@Data
public class DccRegistrationCertificateAccessRequestSubmitReqVO {

    @Schema(description = "注册证主档 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Positive
    private Long certificateId;

    @Schema(description = "申请类型：查看旧证或下载文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 32)
    private String requestType;

    @Schema(description = "申请用途", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 512)
    private String purpose;

    @Schema(description = "下载申请项目代码 ID")
    @Positive
    private Long projectCodeId;

    @Schema(description = "下载申请注册证业务文件 ID 列表")
    private List<@Positive Long> businessFileIds;

    public DccRegistrationCertificateAccessRequestCommand toCommand() {
        return new DccRegistrationCertificateAccessRequestCommand(
                certificateId, requestType, purpose, projectCodeId, businessFileIds);
    }
}
