package cn.iocoder.yudao.module.dcc.service.directory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DccDirectoryImportResult {

    private int importedCount;
    private int rootCount;

}
