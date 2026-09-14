package cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 工序只读目录设备明细 Response VO")
@Data
public class MesProMesProcessMachineryRespVO {

    @Schema(description = "设备明细编号", example = "1")
    private Long id;

    @Schema(description = "设备排序", example = "1")
    private Integer machinerySortNo;

    @Schema(description = "源表拆分后的设备编码", example = "B09032")
    private String machineryCode;

    @Schema(description = "源表设备名称", example = "超声波清洗机")
    private String machineryName;
}
