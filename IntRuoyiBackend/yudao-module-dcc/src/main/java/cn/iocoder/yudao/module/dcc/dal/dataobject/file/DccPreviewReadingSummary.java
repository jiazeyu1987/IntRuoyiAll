package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DccPreviewReadingSummary {
    private LocalDateTime firstOpenedAt;
    private LocalDateTime lastOpenedAt;
    private Long accumulatedSeconds;
    private Long openedSessionCount;
}
