package cn.iocoder.yudao.module.dcc.service.directory;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DccDirectoryDeleteSubtreeResult {

    private final int directoryCount;
    private final int controlledFileCount;
    private final int masterCount;
    private final int infraFileCount;

}
