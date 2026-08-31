package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesPqcProductionReleasePageQuery {

    private Integer pageNo;
    private Integer pageSize;
    private String viewStatus;
    private String workOrderCode;
    private String batchCode;
}
