package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - DCC 产品目录注册证有效期比对 Request VO")
@Data
public class DccProductCatalogRegistrationExpiryCompareReqVO {

    @Schema(description = "当前页行键")
    @Valid
    @NotEmpty(message = "当前页行键不能为空")
    private List<RowKey> rows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowKey {

        @Schema(description = "数据来源")
        @NotBlank(message = "数据来源不能为空")
        private String dataSource;

        @Schema(description = "原 sheet 行号")
        @NotNull(message = "原 sheet 行号不能为空")
        private Integer originalRowNo;
    }
}
