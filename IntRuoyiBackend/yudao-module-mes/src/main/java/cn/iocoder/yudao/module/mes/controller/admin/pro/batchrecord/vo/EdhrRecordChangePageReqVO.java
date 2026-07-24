package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EdhrRecordChangePageReqVO extends PageParam {

    private String changeType;
    private String targetScope;
    private Long batchExecutionId;
    private Long executionId;
    private String changeStatus;

}
