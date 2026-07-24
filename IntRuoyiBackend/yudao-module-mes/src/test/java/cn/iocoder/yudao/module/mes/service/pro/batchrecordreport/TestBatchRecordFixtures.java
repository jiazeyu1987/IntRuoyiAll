package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;

import java.time.LocalDateTime;
import java.util.List;

final class TestBatchRecordFixtures {

    private TestBatchRecordFixtures() {
    }

    static MesProBatchRecordParsedTable parsedTable(int index, String title) {
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(index)
                .tableTitle(title)
                .rowCount(1)
                .columnCount(1)
                .rows(List.of(List.of(MesProBatchRecordParsedCell.builder()
                        .text(title)
                        .rowSpan(1)
                        .colSpan(1)
                        .build())))
                .build();
    }

    static MesProBatchRecordGeneratedReport generatedReport(String reportId, String reportCode, String reportName) {
        return new MesProBatchRecordGeneratedReport(reportId, reportCode, reportName);
    }

    static MesProBatchRecordReportInfo reportInfo(String reportId, String reportCode, String reportName,
                                                  LocalDateTime updateTime) {
        return new MesProBatchRecordReportInfo(reportId, reportCode, reportName, updateTime);
    }

    static MesProBatchRecordReportDO metadataReport(Long id, String sampleKey, Integer tableIndex,
                                                    String reportId, String reportCode, String reportName,
                                                    String sourceFileName) {
        MesProBatchRecordReportDO dataObject = new MesProBatchRecordReportDO();
        dataObject.setId(id);
        dataObject.setSampleKey(sampleKey);
        dataObject.setBatchRecordName("棘突球囊");
        dataObject.setRouteKey(MesProBatchRecordRecognitionRouteKeys.LEGACY);
        dataObject.setSourceFileName(sourceFileName);
        dataObject.setSourceFileSha256("sha256");
        dataObject.setSourceTableIndex(tableIndex);
        dataObject.setTableTitle(reportName);
        dataObject.setReportId(reportId);
        dataObject.setReportCode(reportCode);
        dataObject.setReportName(reportName);
        dataObject.setReportCategoryId("category-ebrr");
        dataObject.setLastImportTime(LocalDateTime.now());
        return dataObject;
    }
}
