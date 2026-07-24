package cn.iocoder.yudao.module.dcc.service.upload;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileUploadPolicyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileUploadPolicyMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_EXCEEDED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_UPLOAD_SIZE_POLICY_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DccUploadSizePolicyServiceTest extends BaseMockitoUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 10, 0);

    @Mock
    private DccControlledFileUploadPolicyMapper uploadPolicyMapper;

    @InjectMocks
    private DccUploadSizePolicyServiceImpl uploadSizePolicyService;

    @Test
    void validateUploadSize_failsClosedWhenPolicyIsMissing() {
        when(uploadPolicyMapper.selectList()).thenReturn(List.of());

        assertServiceException(() -> uploadSizePolicyService.validateUploadSize(10L, "SOURCE_FILE", 1L, NOW),
                DCC_UPLOAD_SIZE_POLICY_MISSING);
    }

    @Test
    void resolveEffectivePolicy_usesCategoryPurposeThenCategoryThenPurposeThenGlobal() {
        when(uploadPolicyMapper.selectList()).thenReturn(List.of(
                policy("GLOBAL", null, null, "global", 1000L),
                policy("PURPOSE", null, "SOURCE_FILE", "purpose", 900L),
                policy("CATEGORY", 10L, null, "category", 800L),
                policy("CATEGORY_PURPOSE", 10L, "SOURCE_FILE", "category-purpose", 700L)));

        DccUploadSizePolicyMatch match = uploadSizePolicyService.resolveEffectivePolicy(10L, "SOURCE_FILE", NOW);

        assertEquals("category-purpose", match.policyCode());
        assertEquals(DccUploadSizePolicyScopeType.CATEGORY_PURPOSE, match.scopeType());
        assertEquals(700L, match.maxBytes());
    }

    @Test
    void resolveEffectivePolicy_usesCategoryBeforePurposeWhenCategoryPurposeIsAbsent() {
        when(uploadPolicyMapper.selectList()).thenReturn(List.of(
                policy("GLOBAL", null, null, "global", 1000L),
                policy("PURPOSE", null, "SOURCE_FILE", "purpose", 900L),
                policy("CATEGORY", 10L, null, "category", 800L)));

        DccUploadSizePolicyMatch match = uploadSizePolicyService.resolveEffectivePolicy(10L, "SOURCE_FILE", NOW);

        assertEquals("category", match.policyCode());
        assertEquals(DccUploadSizePolicyScopeType.CATEGORY, match.scopeType());
    }

    @Test
    void validateUploadSize_rejectsExceededFileSize() {
        when(uploadPolicyMapper.selectList()).thenReturn(List.of(
                policy("GLOBAL", null, null, "global", 100L)));

        assertServiceException(() -> uploadSizePolicyService.validateUploadSize(null, "SOURCE_FILE", 101L, NOW),
                DCC_UPLOAD_SIZE_EXCEEDED, 101L, 100L);
    }

    @Test
    void validateUploadSize_acceptsFileSizeEqualToMaxBytes() {
        when(uploadPolicyMapper.selectList()).thenReturn(List.of(
                policy("GLOBAL", null, null, "global", 100L)));

        DccUploadSizePolicyMatch match = uploadSizePolicyService.validateUploadSize(null, "SOURCE_FILE", 100L, NOW);

        assertEquals("global", match.policyCode());
        assertEquals(100L, match.maxBytes());
    }

    @Test
    void validateUploadSize_rejectsDisabledExpiredFutureOrNonPositiveMatchedPolicyWithoutLowerFallback() {
        assertInvalidMatchedPolicyFailsClosed(policy("CATEGORY", 10L, null, "disabled-category", 800L,
                Boolean.FALSE, NOW.minusHours(1), NOW.plusHours(1)));
        assertInvalidMatchedPolicyFailsClosed(policy("CATEGORY", 10L, null, "expired-category", 800L,
                Boolean.TRUE, NOW.minusHours(2), NOW.minusMinutes(1)));
        assertInvalidMatchedPolicyFailsClosed(policy("CATEGORY", 10L, null, "future-category", 800L,
                Boolean.TRUE, NOW.plusMinutes(1), NOW.plusHours(2)));
        assertInvalidMatchedPolicyFailsClosed(policy("CATEGORY", 10L, null, "zero-category", 0L,
                Boolean.TRUE, NOW.minusHours(1), NOW.plusHours(1)));
    }

    private void assertInvalidMatchedPolicyFailsClosed(DccControlledFileUploadPolicyDO invalidPolicy) {
        when(uploadPolicyMapper.selectList()).thenReturn(List.of(
                policy("GLOBAL", null, null, "global", 1000L),
                invalidPolicy));

        assertServiceException(() -> uploadSizePolicyService.validateUploadSize(10L, "SOURCE_FILE", 1L, NOW),
                DCC_UPLOAD_SIZE_POLICY_MISSING);
    }

    private DccControlledFileUploadPolicyDO policy(String scopeType, Long categoryId, String purpose,
                                                   String code, Long maxBytes) {
        return policy(scopeType, categoryId, purpose, code, maxBytes,
                Boolean.TRUE, NOW.minusHours(1), NOW.plusHours(1));
    }

    private DccControlledFileUploadPolicyDO policy(String scopeType, Long categoryId, String purpose,
                                                   String code, Long maxBytes, Boolean enabled,
                                                   LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        return DccControlledFileUploadPolicyDO.builder()
                .id((long) code.hashCode())
                .policyCode(code)
                .scopeType(scopeType)
                .categoryId(categoryId)
                .purpose(purpose)
                .maxBytes(maxBytes)
                .enabled(enabled)
                .priority(1)
                .policyVersion("v1")
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .changeReason("T07 test")
                .build();
    }

}
