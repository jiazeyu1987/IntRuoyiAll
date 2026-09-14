package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProProductionReportRevisionLogPageReqVO extends PageParam {

    private String workOrderCode;
    private String processKeyword;
    private String actualEmployeeName;
    private String modifiedByName;
    private LocalDateTime modifiedAtStart;
    private LocalDateTime modifiedAtEnd;
}
