package cn.iocoder.yudao.module.dcc.service.position;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DccApprovalPositionImportResult {

    private int totalCount;
    private int createdCount;
    private int adoptedCount;
    private int updatedCount;
    private int disabledCount;

}
