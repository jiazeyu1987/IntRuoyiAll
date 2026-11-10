package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccNewFileInitialVersionTest {

    @Test
    void newFileUsesSpecifiedValidInitialRevisionAndIteration() {
        assertEquals("A/1", DccWindchillVersionNumber.initialForNewFile("a/1"));
        assertEquals("B/1", DccWindchillVersionNumber.initialForNewFile("B/1"));
        assertEquals("A/1", DccWindchillVersionNumber.initialForNewFile(null));
        for (int iteration = 1; iteration <= 9; iteration++) {
            assertEquals("A/" + iteration, DccWindchillVersionNumber.initialForNewFile("a/" + iteration));
        }
        assertEquals("AH/9", DccWindchillVersionNumber.initialForNewFile("ah/9"));
        for (String invalid : new String[]{"A/0", "A/10", "A/01", "I/1", "O/2", "AI/3", "ao/9", "A/2-1"}) {
            assertThrows(IllegalArgumentException.class, () -> DccWindchillVersionNumber.initialForNewFile(invalid), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> DccWindchillVersionNumber.initialForNewFile("V1.0"));
    }

    @Test
    void storedLegacyInitialIsNotReclassifiedByGuessing() {
        assertEquals(null, DccWindchillVersionNumber.parseStoredInitial("V1.0"));
        assertEquals("A/1", DccWindchillVersionNumber.parseStoredInitial("A/1").display());
        assertEquals(null, DccWindchillVersionNumber.parseStoredInitial("V1.1"));
    }

    @Test
    void versionNumberAdvancesOnlyOnExplicitServerOperation() {
        assertEquals("A/2", DccWindchillVersionNumber.parse("A/1").nextIteration().display());
        assertEquals("B/1", DccWindchillVersionNumber.parse("A/9").nextRevision().display());
        assertEquals("AA/1", DccWindchillVersionNumber.parse("Z/2").nextRevision().display());
        assertEquals("J/1", DccWindchillVersionNumber.parse("H/9").nextIteration().display());
        assertEquals("P/1", DccWindchillVersionNumber.parse("N/9").nextRevision().display());
        assertEquals("AJ/1", DccWindchillVersionNumber.parse("AH/9").nextRevision().display());
    }

    @Test
    void historyComparatorOrdersWindchillRevisionAndIteration() {
        assertTrue(DccControlledFileVersion.parse("A/2")
                .compareTo(DccControlledFileVersion.parse("A/1")) > 0);
        assertTrue(DccControlledFileVersion.parse("B/1")
                .compareTo(DccControlledFileVersion.parse("A/99")) > 0);
        assertTrue(DccControlledFileVersion.parse("AA/1")
                .compareTo(DccControlledFileVersion.parse("Z/9")) > 0);
    }

    @Test
    void publicSubmitContractRequiresAnExplicitInitialVersionNumber() throws NoSuchFieldException {
        Field versionNo = DccControlledFileSubmitReqVO.class.getDeclaredField("versionNo");
        assertTrue(versionNo.isAnnotationPresent(NotBlank.class),
                "new upload must state its initial version instead of silently generating another value");
    }

    @Test
    void newLifecycleMigrationDoesNotRewriteHistoricalMasters() throws Exception {
        String sql = Files.readString(Path.of("..", "sql", "mysql",
                "20260906_dcc_new_file_lifecycle_p1.sql"), StandardCharsets.UTF_8).toLowerCase();
        assertFalse(sql.contains("update `dcc_controlled_file_master`"));
        assertTrue(sql.contains("information_schema.columns"));
    }
}
