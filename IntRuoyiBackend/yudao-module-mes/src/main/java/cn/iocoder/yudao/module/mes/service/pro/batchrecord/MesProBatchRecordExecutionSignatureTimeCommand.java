package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionSignatureTimeCommand {

    private LocalDateTime selectedSignedAt;

    private String selectedTimeZone;

    private String selectedTimeReason;
}
