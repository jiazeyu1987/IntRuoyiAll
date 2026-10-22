package cn.iocoder.yudao.module.mes.service.pro;

import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

/** Explicit unit-port double. Does not verify cryptography or creation audit: see MesSignatureContentProtocolTest. */
public final class MesVerifiedConsumerFixture {
    private MesVerifiedConsumerFixture() {}
    public static <T> T wire(T consumer, ElectronicSignatureQueryService expectedRows, Long expectedTenant) {
        var port = mock(ElectronicSignatureVerifiedFactService.class, invocation -> {
            String operation = invocation.getMethod().getName();
            if ("toString".equals(operation) && invocation.getMethod().getParameterCount() == 0)
                return "ExplicitVerifiedSignaturePort@" + Integer.toHexString(System.identityHashCode(invocation.getMock()));
            if ("hashCode".equals(operation) && invocation.getMethod().getParameterCount() == 0)
                return System.identityHashCode(invocation.getMock());
            if ("equals".equals(operation) && invocation.getMethod().getParameterCount() == 1)
                return invocation.getMock() == invocation.getArgument(0);
            if (!"requireVerifiedFact".equals(operation))
                throw new AssertionError("Unexpected verified-port operation");
            Long id = invocation.getArgument(0);
            var row = expectedRows.getById(id);
            var verification = expectedRows.verifyEvidence(id);
            if (row == null || verification == null || !id.equals(row.id()) || !id.equals(verification.signatureId())
                    || !"MES".equals(row.moduleCode()) || !"MES_BATCH_RECORD".equals(row.subjectType())
                    || !"VALID".equals(verification.verificationStatus())
                    || !"LOCAL_DIGEST_TIME_AND_CREATION_AUDIT".equals(verification.verificationScope())
                    || !"CONSISTENT".equals(verification.digestStatus())
                    || !"CONSISTENT".equals(verification.timeConsistencyStatus())
                    || !"CONSISTENT".equals(verification.auditBindingStatus())
                    || !java.util.Objects.equals(verification.storedContentHash(), verification.calculatedContentHash())
                    || !java.util.Objects.equals(verification.storedEvidenceHash(), verification.calculatedEvidenceHash()))
                throw new IllegalStateException("TEST_VERIFIED_PORT_REJECTED");
            return mock(ElectronicSignatureVerifiedFactService.VerifiedSignatureFact.class, call -> {
                String name = call.getMethod().getName();
                if ("verification".equals(name)) return verification;
                if ("auditEventId".equals(name)) return 1L;
                if ("tenantId".equals(name)) return expectedTenant;
                return row.getClass().getMethod(name).invoke(row);
            });
        });
        ReflectionTestUtils.setField(consumer, "verifiedSignatureFacts", port);
        return consumer;
    }
}