package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportStageInitializationResult {

    private List<MesProductionReleaseReportUploadTaskReceipt> reportUploadTasks;
    private String reportSnapshotHash;
}
