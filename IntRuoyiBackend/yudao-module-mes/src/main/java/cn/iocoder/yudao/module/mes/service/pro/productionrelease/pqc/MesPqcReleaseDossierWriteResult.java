package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class MesPqcReleaseDossierWriteResult {

    private List<Long> batchRecordEvidenceIds;
    private List<Long> processInspectionEvidenceIds;
    private List<Long> lossReportEvidenceIds;
    private String lossReportStatus;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private String sourceSnapshotHash;
}
