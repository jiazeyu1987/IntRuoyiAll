package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Schema(description = "管理后台 - 注册证上传提交 Request VO")
@Data
public class DccRegistrationCertificateUploadSubmitReqVO {

    @Schema(description = "DCC项目代码编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "DCC项目代码不能为空")
    @Positive(message = "DCC项目代码不能为空")
    private Long projectCodeId;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公司名称不能为空")
    @Size(max = 255, message = "公司名称长度不能超过255个字符")
    private String companyName;

    @Schema(description = "注册证号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "注册证号不能为空")
    @Size(max = 128, message = "注册证号长度不能超过128个字符")
    private String certificateNo;

    @Schema(description = "首次获证日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "首次获证日期不能为空")
    @DateTimeFormat(iso = DATE)
    private LocalDate firstObtainedDate;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效日期不能为空")
    @DateTimeFormat(iso = DATE)
    private LocalDate effectiveDate;

    @Schema(description = "有效期至", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期至不能为空")
    @DateTimeFormat(iso = DATE)
    private LocalDate expiryDate;

    @Schema(description = "类别", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类别不能为空")
    @Size(max = 64, message = "类别长度不能超过64个字符")
    private String classification;

    @Schema(description = "备注")
    @Size(max = 1024, message = "备注长度不能超过1024个字符")
    private String remark;

    @Schema(description = "注册证文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "注册证文件不能为空")
    private MultipartFile file;

    public DccRegistrationCertificateUploadCommand toCommand() {
        return new DccRegistrationCertificateUploadCommand(
                projectCodeId, companyName, certificateNo, firstObtainedDate,
                effectiveDate, expiryDate, classification, remark, file);
    }
}
