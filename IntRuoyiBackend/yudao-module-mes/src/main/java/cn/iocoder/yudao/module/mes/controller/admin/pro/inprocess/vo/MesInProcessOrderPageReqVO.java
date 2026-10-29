package cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MesInProcessOrderPageReqVO extends PageParam {
    @Size(max = 100) private String workOrderCode;
    @Size(max = 100) private String productName;
    @Size(max = 100) private String batchCode;
    @Size(max = 100) private String equipment;
}
