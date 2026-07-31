package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MesProFrontlineRecordbookEntryPayload {

    private Long feedbackId;
    private Long recordbookId;
    private String entryTitle;
    private Map<String, Object> entryContent;
    private List<String> tagCodes;
    private String idempotencyKey;
    private String remark;

}
