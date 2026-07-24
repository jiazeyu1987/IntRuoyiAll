package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MesProEdhrWorkTaskOverdueProcessResult {

    private int scannedCount;

    private int overdueCount;

    private int skippedCount;

    private String skippedReason;
}
