package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Flow2-only source snapshot. It is embedded in the production fact event and
 * must never be treated as a formal batch-record entry.
 */
@Data
@Accessors(chain = true)
public class MesProFrontlineRecordbookSourceSnapshot {

    private Long recordbookId;
    private String entryTitle;
    private Map<String, Object> entryContent;
    private List<String> tagCodes;
    private String idempotencyKey;
    private String remark;

}
