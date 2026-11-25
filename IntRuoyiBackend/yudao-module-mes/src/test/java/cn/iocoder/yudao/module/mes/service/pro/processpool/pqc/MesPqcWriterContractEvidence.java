package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Exports only synthetic unit-test writer values for the real MySQL contract test. */
public final class MesPqcWriterContractEvidence {
    private MesPqcWriterContractEvidence() {}
    public static void record(String source, List<MesPqcInspectionTaskDO> tasks) {
        assertFalse(tasks.isEmpty());
        var keys=tasks.stream().map(MesPqcInspectionTaskDO::getInspectionRuleKey).toList();
        assertTrue(keys.stream().allMatch(key -> key != null && !key.isBlank()));
        try {
            Path directory=Path.of("target/database-contract-writers");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(source+".json"),JsonUtils.toJsonString(keys));
        } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
    }
}
