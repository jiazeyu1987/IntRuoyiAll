package cn.iocoder.yudao.module.mes.controller.admin.md.item.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 物料产品金蝶按编码同步 Request VO")
@Data
public class MesKingdeeItemCodeSyncReqVO {

    @Schema(description = "物料编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "物料编码列表不能为空")
    private List<String> itemCodes;

}
