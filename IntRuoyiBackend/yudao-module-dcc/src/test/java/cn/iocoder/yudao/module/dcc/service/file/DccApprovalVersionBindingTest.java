package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccApprovalVersionBindingTest {

    @Test
    void initialApprovalSubjectRejectsClientControlledVersionAndUsesConcreteIterationLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> DccWindchillVersionNumber.initialForNewFile("client-value"));
        DccControlledFileDO file = DccControlledFileDO.builder()
                .versionNo(DccWindchillVersionNumber.initialForNewFile("A/1"))
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
