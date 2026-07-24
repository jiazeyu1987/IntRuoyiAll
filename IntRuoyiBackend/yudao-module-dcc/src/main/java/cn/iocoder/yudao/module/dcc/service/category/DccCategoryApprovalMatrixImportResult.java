package cn.iocoder.yudao.module.dcc.service.category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DccCategoryApprovalMatrixImportResult {

    private Integer totalCount;
    private Integer seededCount;
    private Integer skippedCount;
}
