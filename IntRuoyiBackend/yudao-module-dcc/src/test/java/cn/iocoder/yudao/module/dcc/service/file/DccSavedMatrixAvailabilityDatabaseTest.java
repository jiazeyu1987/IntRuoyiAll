package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_INACTIVE;

/** Actual saved/executed matrix selection against changed H2 category facts, not a source-string test. */
class DccSavedMatrixAvailabilityDatabaseTest extends DccWorkingApplicationTypeChangeDatabaseTest {
    @Resource DccFileTypeTaxonomyMapper taxonomyRows;

    @ParameterizedTest
    @CsvSource({"TARGET,DISABLED", "TARGET,REBOUND", "REJECTED,DISABLED", "REJECTED,REBOUND"})
    void unavailableSavedConfigurationRejectsPreviewAndConfirmation(String source, String damage) throws Exception {
        boolean target = source.equals("TARGET");
        if (target) confirmedState(true); else rejectedCopies();
        long category = target ? 3L : 2L;
        var request = target ? targetRequest(22L) : rejectedPreviewRequest();
        var expected = target ? targetExpectation(22L) : executedExpectation();
        if (damage.equals("DISABLED"))
            jdbc.update("UPDATE dcc_file_category SET active=0 WHERE tenant_id=1 AND id=?", category);
        else
            jdbc.update("UPDATE dcc_file_category SET file_type_taxonomy_id=9 WHERE tenant_id=1 AND id=?", category);
        var current = original(22L);
        var rejected = original(20L);
        var attributes = jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");

        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> tx().execute(s -> baselines.preview(99L, request)),
                        "Saved canonical is not authorization to use a disabled/rebound current category"),
                () -> assertThrows(IllegalStateException.class,
                        () -> tx().execute(s -> baselines.confirm(99L, 5L, category, 22L, "NEW", expected)),
                        "Final confirmation must reject an unavailable saved type without replacing its matrix"),
                () -> {
                    if (target) assertThrows(IllegalStateException.class,
                            () -> tx().execute(s -> baselines.savedTypeChangeSource(99L, files.selectById(22L))),
                            "Ordinary working-save source selection must apply the same availability gate");
                });
        assertEquals(current, original(22L));
        assertEquals(rejected, original(20L));
        assertEquals(attributes, jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
    }

    @ParameterizedTest
    @CsvSource({"TARGET,LEAF", "TARGET,PARENT", "REJECTED,LEAF", "REJECTED,PARENT"})
    void inactiveSavedTypePathRejectsPreviewAndConfirmation(String source, String damage) throws Exception {
        boolean target = source.equals("TARGET");
        if (target) confirmedState(true); else rejectedCopies();
        long category = target ? 3L : 2L;
        long leaf = target ? 8L : 6L;
        long parent = 0L;
        for (int level = 1; level <= 5; level++) {
            long id = level == 5 ? leaf : 100L + level;
            jdbc.update("INSERT INTO dcc_file_type_taxonomy(id,parent_id,level_no,code,name,active,sort,tenant_id,deleted) VALUES(?,?,?,?,?,1,0,1,0)",
                    id, parent, level, "C10-PATH-" + id, "C10 path level " + level);
            parent = id;
        }
        var taxonomies = new DccFileTypeTaxonomyAdminServiceImpl();
        wire(taxonomies, "taxonomyMapper", taxonomyRows, "categoryMapper", categories);
        wire(baselines, "taxonomies", taxonomies);
        assertEquals(category, taxonomies.resolveActiveCategoryId(leaf), "The actual path must be valid before disabling it");
        var request = target ? targetRequest(22L) : rejectedPreviewRequest();
        var expected = target ? targetExpectation(22L) : executedExpectation();
        jdbc.update("UPDATE dcc_file_type_taxonomy SET active=0 WHERE tenant_id=1 AND id=?", damage.equals("LEAF") ? leaf : 102L);
        assertEquals(FILE_TYPE_TAXONOMY_INACTIVE.getCode(),
                assertThrows(ServiceException.class, () -> taxonomies.resolveActiveCategoryId(leaf)).getCode());
        var current = original(22L);
        var attributes = jdbc.queryForList("SELECT * FROM dcc_project_application_attributes");
        assertAll(
                () -> assertEquals(FILE_TYPE_TAXONOMY_INACTIVE.getCode(), assertThrows(ServiceException.class,
                        () -> tx().execute(s -> baselines.preview(99L, request))).getCode()),
                () -> assertEquals(FILE_TYPE_TAXONOMY_INACTIVE.getCode(), assertThrows(ServiceException.class,
                        () -> tx().execute(s -> baselines.confirm(99L, 5L, category, 22L, "NEW", expected))).getCode()),
                () -> {
                    if (target) assertEquals(FILE_TYPE_TAXONOMY_INACTIVE.getCode(), assertThrows(ServiceException.class,
                            () -> tx().execute(s -> baselines.savedTypeChangeSource(99L, files.selectById(22L)))).getCode());
                });
        assertEquals(current, original(22L));
        assertEquals(attributes, jdbc.queryForList("SELECT * FROM dcc_project_application_attributes"));
    }
}
