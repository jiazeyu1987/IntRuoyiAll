package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

public interface MesProBatchRecordJimuReportGateway {

    String ensureElectronicBatchRecordCategoryId();

    String findElectronicBatchRecordCategoryId();

    MesProBatchRecordGeneratedReport saveOrUpdateReport(MesProBatchRecordJimuReportSaveReq saveReq);

    MesProBatchRecordReportInfo getReportInfoByCode(String reportCode);

    MesProBatchRecordReportInfo getReportInfo(String reportId);

    String getReportJson(String reportId);

    void updateReportJson(String reportId, String jsonStr);

    void renameReportName(String reportId, String reportName);

    void deleteReport(String reportId);

    int deleteReportsByCategoryId(String categoryId);

    String buildDesignerPath(String reportId);

    String buildPreviewPath(String reportId);
}
