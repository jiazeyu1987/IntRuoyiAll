package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignatureCreationProtocolRegistryTest {
    @Test void v1UsesExactExistingCreationSource() {
        var protocol = SignatureCreationProtocolRegistry.require("V1");
        assertEquals("signature.record.create", protocol.operationId());
        assertEquals("SERVICE_METHOD", protocol.sourceType());
        assertEquals("ElectronicSignatureServiceImpl.sign", protocol.sourceLocator());
    }
    @Test void v2UsesOnlyTheTrustedTransactionExecutor() {
        var protocol = SignatureCreationProtocolRegistry.require("V2");
        assertEquals("V2", protocol.evidenceFormatVersion());
        assertEquals("signature.record.create", protocol.operationId());
        assertEquals("SERVICE_METHOD", protocol.sourceType());
        assertEquals("ElectronicSignatureTransactionExecutor.executeAuthenticated", protocol.sourceLocator());
        assertNotEquals(SignatureCreationProtocolRegistry.require("V1").sourceLocator(), protocol.sourceLocator());
    }
    @Test void unsupportedProtocolsStayClosed() {
        for (String version : new String[] {null, "", "v1", "v2", " V2", "V2 ", "unknown"})
            assertThrows(IllegalArgumentException.class, () -> SignatureCreationProtocolRegistry.require(version));
    }
}
