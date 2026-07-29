package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProFrontlineRecordbookEntryResult {

    private Long recordbookEntryId;
    private Long recordbookEventId;

}
