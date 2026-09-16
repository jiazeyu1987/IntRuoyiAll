package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 生产组长数据清理 Request VO")
@Data
public class MesTeamLeaderDataCleanupExecuteReqVO {

    private List<Long> orderIds;

    private List<Integer> orderVersions;

    @AssertTrue(message = "必须确认清理全部生产组长运行数据")
    private Boolean confirm;
}
