package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(description = "管理后台 - MES 自动排产重排预览 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesProAutoScheduleReplanPreviewRespVO extends MesProAutoSchedulePreviewRespVO {

    private List<MesProAutoScheduleProtectedTaskRespVO> protectedTasks;
}
