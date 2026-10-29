package cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 当前生产工序 SOP 文件 Response VO")
@Data
public class MesProProcessSopRespVO {

    @Schema(description = "工序编号")
    private Long processId;

    @Schema(description = "SOP 文件地址，未绑定时为空")
    private String fileUrl;
}
