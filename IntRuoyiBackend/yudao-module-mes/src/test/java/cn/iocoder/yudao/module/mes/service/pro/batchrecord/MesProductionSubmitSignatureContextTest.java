package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MesProductionSubmitSignatureContextTest {
    @Test
    void incompleteIdentityCannotBecomeSigningSubject() {
        for (Long invalidId : new Long[] {null, 0L, -1L}) {
            assertServiceException(() -> new MesProductionSubmitSignatureContext(invalidId, 2L, 3L, "one"),
                    MesProductionSubmitSignatureContext.CONTEXT_INVALID);
            assertServiceException(() -> new MesProductionSubmitSignatureContext(1L, invalidId, 3L, "one"),
                    MesProductionSubmitSignatureContext.CONTEXT_INVALID);
            assertServiceException(() -> new MesProductionSubmitSignatureContext(1L, 2L, invalidId, "one"),
                    MesProductionSubmitSignatureContext.CONTEXT_INVALID);
        }
        for (String invalidKey : new String[] {null, "", " \t\n"}) {
            assertServiceException(() -> new MesProductionSubmitSignatureContext(1L, 2L, 3L, invalidKey),
                    MesProductionSubmitSignatureContext.CONTEXT_INVALID);
        }
    }

    @Test
    void exactIdentityHasUnambiguousStableEncoding() {
        var first = new MesProductionSubmitSignatureContext(1L, 2L, 3L, "submit|a\nb");
        assertEquals(first.sourceName(), new MesProductionSubmitSignatureContext(
                1L, 2L, 3L, "submit|a\nb").sourceName());
        assertEquals(first.projectionSourceName(), new MesProductionSubmitSignatureContext(
                1L, 2L, 3L, "submit|a\nb").projectionSourceName());
        org.junit.jupiter.api.Assertions.assertTrue(new MesProductionSubmitSignatureContext(
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, "k".repeat(128))
                .projectionSourceName().length() <= 128);
        for (var other : List.of(new MesProductionSubmitSignatureContext(2L, 2L, 3L, "submit|a\nb"),
                new MesProductionSubmitSignatureContext(1L, 3L, 3L, "submit|a\nb"),
                new MesProductionSubmitSignatureContext(1L, 2L, 4L, "submit|a\nb"),
                new MesProductionSubmitSignatureContext(1L, 2L, 3L, "submit|a\\nb"))) {
            assertNotEquals(first.sourceName(), other.sourceName());
            assertNotEquals(first.projectionSourceName(), other.projectionSourceName());
        }
    }
}
