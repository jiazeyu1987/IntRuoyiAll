package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import org.springframework.web.multipart.MultipartFile;

public interface ThirdPartyFeedbackImportService {

    ThirdPartyFeedbackImportResult importWorkbook(MultipartFile file);

    ThirdPartyFeedbackImportResult importDirectWorkReportWorkbook(MultipartFile file);

    ThirdPartyFeedbackImportResult simulateImportWorkbook(Integer processCount);
}
