package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - NAS 连接参数保存 Request VO")
@Data
public class FileNasConfigSaveReqVO {

    @Schema(description = "NAS 服务器地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "172.30.30.4")
    @NotBlank(message = "NAS 服务器地址不能为空")
    @Size(max = 100, message = "NAS 服务器地址长度不能超过 100 个字符")
    private String server;

    @Schema(description = "NAS 服务器端口", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "445")
    @jakarta.validation.constraints.Min(value = 1, message = "NAS 服务器端口必须大于 0")
    @jakarta.validation.constraints.Max(value = 65535, message = "NAS 服务器端口不能超过 65535")
    private Integer port;

    @Schema(description = "NAS 共享名", requiredMode = Schema.RequiredMode.REQUIRED, example = "it共享")
    @NotBlank(message = "NAS 共享名不能为空")
    @Size(max = 100, message = "NAS 共享名长度不能超过 100 个字符")
    private String share;

    @Schema(description = "NAS 域", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "WORKGROUP")
    @Size(max = 100, message = "NAS 域长度不能超过 100 个字符")
    private String domain;

    @Schema(description = "NAS 用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "int")
    @NotBlank(message = "NAS 用户名不能为空")
    @Size(max = 100, message = "NAS 用户名长度不能超过 100 个字符")
    private String username;

    @Schema(description = "NAS 密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Kdlyx123")
    @NotBlank(message = "NAS 密码不能为空")
    @Size(max = 200, message = "NAS 密码长度不能超过 200 个字符")
    private String password;
}
