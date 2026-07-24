package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileRecognitionFailureSummaryDO {

    private String failureStage;
    private String failureCode;
    private String failureMessage;
    private Long failureCount;
}
