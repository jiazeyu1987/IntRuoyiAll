package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionSignatureTimeReqVO {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonDeserialize(using = MesProBatchRecordExecutionSignatureTimeDeserializer.class)
    private LocalDateTime selectedSignedAt;

    private String selectedTimeZone;

    private String selectedTimeReason;
}
