package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesPqcProductionReleasePageQuery {

    private Integer pageNo;
    private Integer pageSize;
    private String listFilters;
    private String viewStatus;
    private String workOrderCode;
    private String batchCode;
    private Long applicationId;
    private Long pqcReleaseWorkTaskId;
}
