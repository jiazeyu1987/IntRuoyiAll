package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Real H2 persistence using shipped source schema: DB numeric/time precision must not break replay hashes. */
class MesLocalTestRunSourceHashPersistenceTest {
    @Test void persistedPickAndReplenishmentHashesSurviveReloadAndOperationalMetadataChanges() throws Exception {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        String ddl;
        try (var input = new ClassPathResource("sql/mes_edhr_completion_source_producer_tables.sql").getInputStream()) {
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String kind : List.of("pick", "replenishment")) {
            String table = "erp_kingdee_production_" + kind + "_list";
            for (String name : List.of(table, table + "_item")) {
                // The two issue headers share the explicit V1 field schema; the fixture ships only the pick header.
                String schemaName = name.equals("erp_kingdee_production_replenishment_list")
                        ? "erp_kingdee_production_pick_list" : name;
                var match = Pattern.compile("CREATE TABLE " + schemaName + " \\([\\s\\S]*?\\r?\\n\\);").matcher(ddl);
                assertTrue(match.find(), name);
                jdbc.execute(match.group().replace(schemaName, name).replace("uk_erp_kingdee_prod_pick_list_source", "uk_" + name + "_source"));
            }
            jdbc.update("INSERT INTO " + table + " (id,tenant_id,source_form_id,source_fid,source_bill_no,document_status,source_modify_time,last_sync_time) "
                    + "VALUES(1,165,'LOCAL_TEST_RUN','LOCAL_TEST_RUN:1:P','LT-RUN-A-P','LOCAL_TEST',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            jdbc.update("INSERT INTO " + table + "_item (id,tenant_id,production_" + kind + "_list_id,source_form_id,source_fid,source_entry_id,source_line_key,source_bill_no,material_number,material_name,requested_quantity,actual_quantity,source_modify_time,last_sync_time) "
                    + "VALUES(2,165,1,'LOCAL_TEST_RUN','LOCAL_TEST_RUN:1:P','101','LOCAL_TEST_RUN:1:P:101','LT-RUN-A-P','MAT','物料',100.00,100.00,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        }
        String pickHash = pickHash(jdbc);
        String replenishmentHash = replenishmentHash(jdbc);
        assertEquals(pickHash, pickHash(jdbc));
        assertEquals(replenishmentHash, replenishmentHash(jdbc));
        for (String kind : List.of("pick", "replenishment")) {
            String table = "erp_kingdee_production_" + kind + "_list";
            jdbc.update("UPDATE " + table + " SET last_sync_time=DATEADD('DAY',1,last_sync_time),raw_payload='new metadata'");
            jdbc.update("UPDATE " + table + "_item SET requested_quantity=100.000000,last_sync_time=DATEADD('DAY',1,last_sync_time),raw_payload='new metadata'");
        }
        assertEquals(pickHash, pickHash(jdbc));
        assertEquals(replenishmentHash, replenishmentHash(jdbc));
        jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET actual_quantity=101");
        jdbc.update("UPDATE erp_kingdee_production_replenishment_list_item SET material_number='OTHER'");
        assertNotEquals(pickHash, pickHash(jdbc));
        assertNotEquals(replenishmentHash, replenishmentHash(jdbc));
    }
    private String pickHash(JdbcTemplate jdbc) {
        var header = jdbc.queryForObject("SELECT * FROM erp_kingdee_production_pick_list", new BeanPropertyRowMapper<>(ErpKingdeeProductionPickListDO.class));
        var items = jdbc.query("SELECT * FROM erp_kingdee_production_pick_list_item", new BeanPropertyRowMapper<>(ErpKingdeeProductionPickListItemDO.class));
        return MesLocalTestRunSourceGuard.pickSnapshotHash(header, items);
    }
    private String replenishmentHash(JdbcTemplate jdbc) {
        var header = jdbc.queryForObject("SELECT * FROM erp_kingdee_production_replenishment_list", new BeanPropertyRowMapper<>(ErpKingdeeProductionReplenishmentListDO.class));
        var items = jdbc.query("SELECT * FROM erp_kingdee_production_replenishment_list_item", new BeanPropertyRowMapper<>(ErpKingdeeProductionReplenishmentListItemDO.class));
        return MesLocalTestRunSourceGuard.replenishmentSnapshotHash(header, items);
    }
}
