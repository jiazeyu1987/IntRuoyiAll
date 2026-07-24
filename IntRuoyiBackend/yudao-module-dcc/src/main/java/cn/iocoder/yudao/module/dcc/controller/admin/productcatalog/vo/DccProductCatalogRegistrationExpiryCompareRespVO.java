package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - DCC 产品目录注册证有效期比对 Response VO")
@Data
public class DccProductCatalogRegistrationExpiryCompareRespVO {

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "原 sheet 行号")
    private Integer originalRowNo;

    @Schema(description = "比对状态：MATCH/MISMATCH/FETCH_FAILED/NO_LINK/UNSUPPORTED")
    private String status;

    @Schema(description = "本地有效期至")
    private String localExpiryDate;

    @Schema(description = "外站有效期至")
    private String remoteExpiryDate;

    @Schema(description = "状态说明")
    private String message;
}
