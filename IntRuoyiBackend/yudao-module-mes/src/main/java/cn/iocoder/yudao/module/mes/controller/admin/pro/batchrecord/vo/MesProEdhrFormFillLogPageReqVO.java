package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrFormFillLogPageReqVO extends PageParam {

    private String batchRecordReportId;
    private String formKeyword;
    private LocalDateTime changedAtStart;
    private LocalDateTime changedAtEnd;
    private Long actorId;
    private String actorName;
    private String batchCode;
    private String workOrderCode;
    private String executionCode;
}
