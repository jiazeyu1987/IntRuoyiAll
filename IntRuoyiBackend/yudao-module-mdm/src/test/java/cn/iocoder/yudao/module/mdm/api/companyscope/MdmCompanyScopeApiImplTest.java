package cn.iocoder.yudao.module.mdm.api.companyscope;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_USER_COMPANY_SCOPE_DENIED;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmCompanyScopeApiImplTest {

    @Mock
    private MdmCompanyScopeService companyScopeService;

    private MdmCompanyScopeApiImpl api;

    @BeforeEach
    void setUp() {
        api = new MdmCompanyScopeApiImpl();
        ReflectionTestUtils.setField(api, "companyScopeService", companyScopeService);
    }

    @Test
    void validateUserCompanyAccessDelegatesExactIds() {
        api.validateUserCompanyAccess(101L, 301L);

        verify(companyScopeService).validateUserCompanyAccess(101L, 301L);
    }

    @Test
    void validateUserCompanyAccessBatchDelegatesExactBatchAndPropagatesFailure() {
        List<Long> companyIds = List.of(302L, 301L);
        api.validateUserCompanyAccessBatch(101L, companyIds);
        verify(companyScopeService).validateUserCompanyAccessBatch(101L, companyIds);

        ServiceException denied = exception(MDM_USER_COMPANY_SCOPE_DENIED);
        doThrow(denied).when(companyScopeService).validateUserCompanyAccessBatch(101L, companyIds);

        assertSame(denied, assertThrows(ServiceException.class,
                () -> api.validateUserCompanyAccessBatch(101L, companyIds)));
    }

    @Test
    void getEnabledCompanyIdsForUserReturnsUnchangedStableSet() {
        Set<Long> companyIds = Set.of(301L, 302L);
        when(companyScopeService.getEnabledCompanyIdsForUser(101L)).thenReturn(companyIds);

        assertSame(companyIds, api.getEnabledCompanyIdsForUser(101L));
    }

    @Test
    void validateUserCompanyAccessPropagatesExplicitDenial() {
        ServiceException denied = exception(MDM_USER_COMPANY_SCOPE_DENIED);
        doThrow(denied).when(companyScopeService).validateUserCompanyAccess(101L, 301L);

        assertSame(denied, assertThrows(ServiceException.class,
                () -> api.validateUserCompanyAccess(101L, 301L)));
    }

    @Test
    void resolveRecipientUserIdsReturnsUnchangedStableSet() {
        Set<Long> recipients = Set.of(1001L, 1002L);
        when(companyScopeService.resolveRecipientUserIds(301L, List.of(201L),
                "dcc:registration-certificate:notify")).thenReturn(recipients);

        assertSame(recipients, api.resolveRecipientUserIds(301L, List.of(201L),
                "dcc:registration-certificate:notify"));
    }

}
