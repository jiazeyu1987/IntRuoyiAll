package cn.iocoder.yudao.module.dcc.service.download;

import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccDownloadPolicyServiceTest {

    private final DccDownloadPolicyService policyService = new DccDownloadPolicyService();

    @Test
    void decide_requiresActivePublishedCategoryAndDirectoryDownloadSignals() {
        assertTrue(policyService.decide(new DccDownloadPolicyContext(
                901L,
                DccControlledFileStatusEnum.ACTIVE.getStatus(),
                501L,
                true,
                true)).allowed());

        assertDenied("CATEGORY_DOWNLOAD_DENIED", new DccDownloadPolicyContext(
                901L,
                DccControlledFileStatusEnum.ACTIVE.getStatus(),
                501L,
                false,
                true));
        assertDenied("DIRECTORY_DOWNLOAD_DENIED", new DccDownloadPolicyContext(
                901L,
                DccControlledFileStatusEnum.ACTIVE.getStatus(),
                501L,
                true,
                false));
        assertDenied("STATUS_NOT_ACTIVE", new DccDownloadPolicyContext(
                901L,
                DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
                501L,
                true,
                true));
        assertDenied("PUBLISHED_FILE_MISSING", new DccDownloadPolicyContext(
                901L,
                DccControlledFileStatusEnum.ACTIVE.getStatus(),
                null,
                true,
                true));
    }

    @Test
    void policyContextDoesNotExposeFileNumberOrPrefixInput() {
        assertFalse(Arrays.stream(DccDownloadPolicyContext.class.getRecordComponents())
                .anyMatch(component -> "fileNumber".equals(component.getName())));
    }

    private void assertDenied(String reason, DccDownloadPolicyContext context) {
        DccDownloadPolicyDecision decision = policyService.decide(context);
        assertFalse(decision.allowed());
        assertEquals(reason, decision.reason());
    }
}
