package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckoutReqVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccCheckoutCheckinLifecycleTest {

    @Test
    void checkoutReasonAndCheckinTicketAreExplicitContracts() throws Exception {
        assertTrue(DccControlledFileCheckoutReqVO.class.getDeclaredField("reason") != null);
        assertTrue(DccControlledFileCheckinReqVO.class.getDeclaredField("uploadTicket") != null);
        assertTrue(DccControlledFileCheckinReqVO.class.getDeclaredField("sessionId") != null);
        assertTrue(DccControlledFileCheckinReqVO.class.getDeclaredField("changeDescription") != null);
    }

    @Test
    void checkinAutomaticallyCreatesNextIterationAndPreservesBaseVersion() {
        DccWindchillVersionNumber base = DccWindchillVersionNumber.parse("A/1");
        assertEquals("A/2", base.nextIteration().display());
        assertEquals("A/1", base.display());
    }

    @Test
    void p2MigrationStoresCheckoutHistoryAndImmutableHashEvidence() throws Exception {
        String sql = Files.readString(Path.of("..", "sql", "mysql", "20260906_dcc_new_file_lifecycle_p2.sql"),
                StandardCharsets.UTF_8).toLowerCase();
        assertTrue(sql.contains("dcc_controlled_file_checkout"));
        assertTrue(sql.contains("base_source_sha256"));
        assertTrue(sql.contains("checkin_source_sha256"));
        assertTrue(sql.contains("predecessor_controlled_file_id"));
        assertTrue(sql.contains("unique key `uk_dcc_checkout_active_master`"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(!sql.contains("add column if not exists"));
        assertTrue(sql.contains("create procedure ensure_dcc_p2_column"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("drop procedure if exists ensure_dcc_p2_column"));
        assertTrue(!sql.contains("add column if not exists"));
    }

    @Test
    void p3MigrationUsesMysql80SafeIdempotentColumnProcedure() throws Exception {
        String sql = Files.readString(Path.of("..", "sql", "mysql", "20260906_dcc_new_file_lifecycle_p3.sql"),
                StandardCharsets.UTF_8).toLowerCase();
        assertTrue(sql.contains("revision_base_active_controlled_file_id"));
        assertTrue(sql.contains("create procedure ensure_dcc_p3_column"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("drop procedure if exists ensure_dcc_p3_column"));
        assertTrue(!sql.contains("add column if not exists"));
    }
}
