package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccApprovalVersionBindingTest {

    @Test
    void initialApprovalSubjectUsesConcreteIterationLabel() {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .versionNo(DccWindchillVersionNumber.initialForNewFile("client-value"))
                .build();

        assertEquals("A/1", file.getVersionNo());
    }

    @Test
    void approvalAdapterReadsTheConcreteVersionRecord() throws Exception {
        String adapter = Files.readString(Path.of("src", "main", "java", "cn", "iocoder",
                        "yudao", "module", "dcc", "approval", "DccApprovalTaskAdapter.java"),
                StandardCharsets.UTF_8);
        assertTrue(adapter.contains("file.getVersionNo()"));
    }
}
