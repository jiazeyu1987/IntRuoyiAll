package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrWorkTaskStatsRespVO {

    private Long todoCount;

    private Long fillCount;

    private Long reviewCount;

    private Long reworkCount;

    private Long archiveCount;

    private Long overdueCount;

    private Long doneCount;
}
