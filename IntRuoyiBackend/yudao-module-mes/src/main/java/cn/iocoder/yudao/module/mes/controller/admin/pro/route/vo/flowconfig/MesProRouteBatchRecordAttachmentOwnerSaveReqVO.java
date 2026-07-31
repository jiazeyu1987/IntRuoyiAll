package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线批记录附件负责人保存 Request VO")
@Data
@Accessors(chain = true)
public class MesProRouteBatchRecordAttachmentOwnerSaveReqVO {

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "路线候选版本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    @NotNull(message = "路线版本编号不能为空")
    private Long routeVersionId;

    @Schema(description = "批记录附件负责人配置列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "批记录附件负责人配置不能为空")
    @Valid
    private List<MesProRouteBatchRecordAttachmentOwnerItemSaveReqVO> items;
}
