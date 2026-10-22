package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class MesEdhrSourceCreationBindingSqlContractTest {
    @Test void h2ResourceIsExactlyFrozenNineTablesAndSixIndexes() throws Exception {
        byte[] bytes;
        try (var stream = getClass().getResourceAsStream("/sql/mes_edhr_source_creation_verifier_tables.sql")) {
            assertNotNull(stream); bytes = stream.readAllBytes();
        }
        assertEquals(11447, bytes.length);
        assertEquals("f288c9b1644cf0724e9833cc29b0374e88553a75fbbc96e21b3c41e76583c3f4",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        String sql = new String(bytes, StandardCharsets.UTF_8);
        assertFalse(sql.contains("\r")); assertEquals(9, sql.split("CREATE TABLE ", -1).length - 1);
        assertEquals(6, sql.split("CREATE INDEX ", -1).length - 1);
    }
    @Test void mysqlDraftContainsElevenColumnsAndThreeIndependentUniqueKeys() throws Exception {
        String sql = Files.readString(Path.of("../sql/mysql/20261009_mes_edhr_source_creation_binding.sql"), StandardCharsets.UTF_8);
        assertTrue(sql.contains("DRAFT ONLY")); assertFalse(sql.contains("deleted")); assertFalse(sql.contains("IF NOT EXISTS"));
        var columns = sql.lines().filter(line -> line.matches("  [a-z_]+ (bigint|varchar\\([0-9]+\\)|char\\([0-9]+\\)).*"))
                .map(String::trim).map(line -> line.replaceFirst(",$", "")).toList();
        assertEquals(java.util.List.of("id bigint NOT NULL AUTO_INCREMENT", "tenant_id bigint NOT NULL",
                "owner_protocol varchar(64) NOT NULL", "source_row_id bigint NOT NULL",
                "source_receipt_id varchar(128) NOT NULL", "source_version varchar(64) NOT NULL",
                "source_hash char(64) NOT NULL", "request_hash_version varchar(32) NOT NULL",
                "request_hash char(64) NOT NULL", "creation_event_id bigint NOT NULL",
                "creation_event_hash char(64) NOT NULL"), columns);
        assertEquals(3, sql.lines().filter(line -> line.trim().startsWith("UNIQUE KEY ")).count());
        assertTrue(sql.contains("UNIQUE KEY uk_mes_edhr_creation_row (tenant_id,owner_protocol,source_row_id)"));
        assertTrue(sql.contains("UNIQUE KEY uk_mes_edhr_creation_receipt (tenant_id,owner_protocol,source_receipt_id,source_version)"));
        assertTrue(sql.contains("UNIQUE KEY uk_mes_edhr_creation_event (tenant_id,creation_event_id)"));
        assertTrue(sql.contains("PRIMARY KEY (id)"));
        assertTrue(sql.contains(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;"));
    }
}
