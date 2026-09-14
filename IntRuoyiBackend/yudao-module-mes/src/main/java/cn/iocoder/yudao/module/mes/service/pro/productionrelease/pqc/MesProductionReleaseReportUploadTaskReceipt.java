package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportUploadTaskReceipt {

    private String nodeType;
    private Long batchTaskId;
    private Long workTaskId;
    private List<Long> candidateUserIds;
    private String status;
}
