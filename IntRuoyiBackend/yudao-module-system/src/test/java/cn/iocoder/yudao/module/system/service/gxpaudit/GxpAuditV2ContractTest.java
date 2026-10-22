package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GxpAuditV2ContractTest {

    @Test
    void commandMustExposeV2ProtocolFields() {
        Set<String> fields = Set.of(GxpAuditCommand.class.getDeclaredFields()).stream()
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertTrue(fields.contains("eventSchemaVersion"));
        assertTrue(fields.contains("transactionId"));
        assertTrue(fields.contains("resultStatus"));
        assertTrue(fields.contains("reasonCode"));
        assertTrue(fields.contains("reasonSource"));
        assertTrue(fields.contains("authenticatedActor"));
        assertTrue(fields.contains("performedBy"));
        assertTrue(fields.contains("sourceType"));
        assertTrue(fields.contains("sourceLocator"));
        assertTrue(fields.contains("links"));
        assertTrue(fields.contains("evidences"));
        assertTrue(fields.contains("relationManifest"));
        assertTrue(fields.contains("evidenceManifest"));
        assertTrue(fields.contains("statePayloadHash"));
    }

    @Test
    void eventMustPersistV2ProtocolFieldsWithoutUsingLegacyBaseAuditColumns() {
        Set<String> fields = Set.of(GxpAuditEventDO.class.getDeclaredFields()).stream()
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertTrue(fields.contains("eventSchemaVersion"));
        assertTrue(fields.contains("canonicalizationVersion"));
        assertTrue(fields.contains("resultStatus"));
        assertTrue(fields.contains("transactionId"));
        assertTrue(fields.contains("authenticatedActorJson"));
        assertTrue(fields.contains("performedByJson"));
        assertTrue(fields.contains("sourceType"));
        assertTrue(fields.contains("sourceLocator"));
        assertTrue(fields.contains("relationManifestJson"));
        assertTrue(fields.contains("evidenceManifestJson"));
        assertTrue(fields.contains("statePayloadHash"));
    }
}
