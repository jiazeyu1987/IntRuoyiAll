package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineParameterAuditResult;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProcessPoolSubmitEventResult {

    private Long feedbackId;
    private Long recordbookEntryId;
    private Long recordbookEventId;
    private Long processPoolEventId;
    private MesFrontlineParameterAuditResult parameterAuditResult;
}
