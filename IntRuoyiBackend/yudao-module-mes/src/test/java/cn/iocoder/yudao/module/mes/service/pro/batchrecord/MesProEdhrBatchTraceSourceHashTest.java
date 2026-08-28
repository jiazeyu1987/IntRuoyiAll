package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchTraceSourceHashTest {

    @Test
    void persistedExternalWitnessHashIsAcceptedOnlyForDeclaredSourceTypes() {
        String snapshot = "{\"sourceType\":\"MATERIAL_ISSUE\",\"witnessHash\":\"upstream-hash\"}";

        assertTrue(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, snapshot, "upstream-hash"));
        assertFalse(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE, snapshot, "tampered-hash"));
        assertFalse(MesProEdhrBatchTraceSourceHash.isValid(
                MesProEdhrBatchTraceLinkType.WORK_ORDER, snapshot, "upstream-hash"));
    }
}
