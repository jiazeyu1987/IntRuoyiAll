package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MesProEdhrDeviationPageReqVO extends PageParam {
    private String status;
    private String level;
    private Long batchExecutionId;
    private String search;
    private String sortField;
    private String sortOrder;
    private LocalDateTime initiatedAtStart;
    private LocalDateTime initiatedAtEnd;
}
