package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 自动排产依赖线 Request VO")
@Data
public class MesProAutoScheduleDependencyReqVO {

    @Schema(description = "工单编号列表")
    private List<Long> workOrderIds;

    @Schema(description = "任务编号列表")
    private List<Long> taskIds;

}
