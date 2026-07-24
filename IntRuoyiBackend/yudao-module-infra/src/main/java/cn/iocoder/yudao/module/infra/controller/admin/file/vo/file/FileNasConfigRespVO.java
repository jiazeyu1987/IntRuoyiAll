package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - NAS 连接参数 Response VO")
@Data
@Accessors(chain = true)
public class FileNasConfigRespVO {

    @Schema(description = "NAS 服务器地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "172.30.30.4")
    private String server = "";

    @Schema(description = "NAS 服务器端口", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "445")
    private Integer port;

    @Schema(description = "NAS 共享名", requiredMode = Schema.RequiredMode.REQUIRED, example = "it共享")
    private String share = "";

    @Schema(description = "NAS 域", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "WORKGROUP")
    private String domain = "";

    @Schema(description = "NAS 用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "int")
    private String username = "";

    @Schema(description = "NAS 密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Kdlyx123")
    private String password = "";
}
