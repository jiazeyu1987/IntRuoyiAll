package cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 生产工序设备产能 Response VO")
@Data
public class MesProProcessMachineryRespVO {

    @Schema(description = "设备编号", example = "1")
    private Long machineryId;

    @Schema(description = "设备编码", example = "A03190")
    private String machineryCode;

    @Schema(description = "设备名称", example = "球囊成型机")
    private String machineryName;

    @Schema(description = "设备状态", example = "2")
    private Integer machineryStatus;

    @Schema(description = "单台班次产能", example = "100")
    private BigDecimal shiftCapacity;

    @Schema(description = "当前可用班次产能", example = "100")
    private BigDecimal availableShiftCapacity;

    @Schema(description = "是否维修中", example = "false")
    private Boolean underRepair;

    @Schema(description = "可用状态：NORMAL/REPAIR", example = "NORMAL")
    private String availabilityStatus;

    @Schema(description = "可用状态原因", example = "维修工单（WX-001）")
    private String availabilityReason;

}
