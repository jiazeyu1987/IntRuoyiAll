package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import java.util.List;

public record ThirdPartyFeedbackExcelParseResult(int sheetCount, List<ThirdPartyFeedbackExcelRow> rows) {
}
