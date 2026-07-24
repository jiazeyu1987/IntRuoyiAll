package cn.iocoder.yudao.module.dcc.service.category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DccFileCategoryImportResult {

    private int totalCount;
    private int createdCount;
    private int adoptedCount;
    private int updatedCount;

}
