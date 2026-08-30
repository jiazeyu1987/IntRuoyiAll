package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.api.companyscope.dto.MdmRoleCompanyScopeCreateReqDTO;
import cn.iocoder.yudao.module.mdm.api.companyscope.dto.MdmUserCompanyScopeCreateReqDTO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmRoleCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmUserCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmRoleCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmUserCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_CONFIG_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_WRITE_RESULT_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_ROLE_COMPANY_SCOPE_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_USER_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_USER_COMPANY_SCOPE_DISABLED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_USER_COMPANY_SCOPE_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DISABLED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_NOT_FOUND;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TENANT_MISMATCH;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmCompanyScopeServiceImplTest {

    private static final Long TENANT_ID = 11L;
    private static final Long USER_ID = 101L;
    private static final Long ROLE_ID = 201L;
    private static final Long COMPANY_ID = 301L;

    @Mock
    private MdmUserCompanyScopeMapper userScopeMapper;
    @Mock
    private MdmRoleCompanyScopeMapper roleScopeMapper;
    @Mock
    private MdmEnterpriseService enterpriseService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private MdmCompanyScopeRecipientResolver recipientResolver;

    private MdmCompanyScopeServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        service = new MdmCompanyScopeServiceImpl();
        ReflectionTestUtils.setField(service, "userScopeMapper", userScopeMapper);
        ReflectionTestUtils.setField(service, "roleScopeMapper", roleScopeMapper);
        ReflectionTestUtils.setField(service, "enterpriseService", enterpriseService);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "roleApi", roleApi);
        ReflectionTestUtils.setField(service, "recipientResolver", recipientResolver);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void validateUserCompanyAccessAllowsOnlyEnabledExplicitMapping() {
        stubEnabledCompany();
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 2, false)));

        assertDoesNotThrow(() -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID));

        verify(userScopeMapper).selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID);
    }

    @Test
    void validateUserCompanyAccessChecksMappingBeforeEnterpriseLookup() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of());

        assertServiceException(() -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID),
                MDM_USER_COMPANY_SCOPE_DENIED);

        verifyNoInteractions(enterpriseService);
    }

    @Test
    void authorizationReadsNormalizeEnterpriseStateFailures() {
        List<Long> batchCompanyIds = List.of(301L, 302L);
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        when(userScopeMapper.selectByTenantUser(TENANT_ID, USER_ID)).thenReturn(List.of(
                userScope(2L, TENANT_ID, USER_ID, 302L,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        when(userScopeMapper.selectByTenantUserAndCompanyIds(TENANT_ID, USER_ID, batchCompanyIds))
                .thenReturn(List.of(
                        userScope(3L, TENANT_ID, USER_ID, 301L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                        userScope(4L, TENANT_ID, USER_ID, 302L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        doThrow(exception(MDM_ENTERPRISE_NOT_FOUND, COMPANY_ID))
                .when(enterpriseService).getEnabledEnterprises(List.of(COMPANY_ID),
                        Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));
        doThrow(exception(MDM_ENTERPRISE_DISABLED, 302L))
                .when(enterpriseService).getEnabledEnterprises(List.of(302L),
                        Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));
        doThrow(exception(MDM_ENTERPRISE_TENANT_MISMATCH, 301L))
                .when(enterpriseService).getEnabledEnterprises(batchCompanyIds,
                        Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));

        assertServiceException(() -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID),
                MDM_USER_COMPANY_SCOPE_DENIED);
        assertServiceException(() -> service.getEnabledCompanyIdsForUser(USER_ID),
                MDM_USER_COMPANY_SCOPE_DENIED);
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, batchCompanyIds),
                MDM_USER_COMPANY_SCOPE_DENIED);
    }

    @Test
    void authorizationReadsPropagateInfrastructureFailureUnchanged() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        IllegalStateException databaseFailure = new IllegalStateException("enterprise database unavailable");
        doThrow(databaseFailure).when(enterpriseService).getEnabledEnterprises(List.of(COMPANY_ID),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID));

        assertSame(databaseFailure, thrown);
    }

    @Test
    void validateUserCompanyAccessBatchValidatesTheCompleteBatchInOneQuery() {
        List<Long> requestedCompanyIds = List.of(302L, 301L);
        List<Long> normalizedCompanyIds = List.of(301L, 302L);
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompanyIds(TENANT_ID, USER_ID, normalizedCompanyIds))
                .thenReturn(List.of(
                        userScope(2L, TENANT_ID, USER_ID, 302L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                        userScope(1L, TENANT_ID, USER_ID, 301L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        when(enterpriseService.getEnabledEnterprises(normalizedCompanyIds,
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())))
                .thenReturn(List.of(enabledCompany(301L), enabledCompany(302L)));

        assertDoesNotThrow(() -> service.validateUserCompanyAccessBatch(USER_ID, requestedCompanyIds));

        verify(userScopeMapper).selectByTenantUserAndCompanyIds(TENANT_ID, USER_ID, normalizedCompanyIds);
        verify(userScopeMapper, never()).selectByTenantUserAndCompany(any(), any(), any());
        verify(enterpriseService).getEnabledEnterprises(normalizedCompanyIds,
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));
    }

    @Test
    void validateUserCompanyAccessBatchRejectsEmptyDuplicateAndInvalidIdsBeforeLookup() {
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, null),
                MDM_COMPANY_SCOPE_FIELD_REQUIRED);
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, List.of()),
                MDM_COMPANY_SCOPE_FIELD_REQUIRED);
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, List.of(301L, 301L)),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, List.of(0L)),
                MDM_COMPANY_SCOPE_FIELD_REQUIRED);

        verifyNoInteractions(userScopeMapper, enterpriseService, adminUserApi);
    }

    @Test
    void validateUserCompanyAccessBatchFailsClosedForAnyMissingDisabledOrAmbiguousMapping() {
        List<Long> companyIds = List.of(301L, 302L);
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));

        when(userScopeMapper.selectByTenantUserAndCompanyIds(TENANT_ID, USER_ID, companyIds))
                .thenReturn(List.of(userScope(1L, TENANT_ID, USER_ID, 301L,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, companyIds),
                MDM_USER_COMPANY_SCOPE_DENIED);

        when(userScopeMapper.selectByTenantUserAndCompanyIds(TENANT_ID, USER_ID, companyIds))
                .thenReturn(List.of(
                        userScope(1L, TENANT_ID, USER_ID, 301L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                        userScope(2L, TENANT_ID, USER_ID, 302L,
                                MdmEnterpriseStatusEnum.DISABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, companyIds),
                MDM_USER_COMPANY_SCOPE_DISABLED);

        when(userScopeMapper.selectByTenantUserAndCompanyIds(TENANT_ID, USER_ID, companyIds))
                .thenReturn(List.of(
                        userScope(1L, TENANT_ID, USER_ID, 301L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                        userScope(2L, TENANT_ID, USER_ID, 301L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                        userScope(3L, TENANT_ID, USER_ID, 302L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, companyIds),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);

        when(userScopeMapper.selectByTenantUserAndCompanyIds(TENANT_ID, USER_ID, companyIds))
                .thenReturn(List.of(
                        userScope(1L, 99L, USER_ID, 301L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                        userScope(2L, TENANT_ID, USER_ID, 302L,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.validateUserCompanyAccessBatch(USER_ID, companyIds),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);

        verifyNoInteractions(enterpriseService);
        verify(userScopeMapper, never()).selectByTenantUserAndCompany(any(), any(), any());
    }

    @Test
    void getEnabledCompanyIdsForUserReturnsStableValidatedFormalCompanyIds() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUser(TENANT_ID, USER_ID)).thenReturn(List.of(
                userScope(1L, TENANT_ID, USER_ID, 302L,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                userScope(2L, TENANT_ID, USER_ID, 301L,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                userScope(3L, TENANT_ID, USER_ID, 303L,
                        MdmEnterpriseStatusEnum.DISABLE.getStatus(), 1, false),
                userScope(4L, TENANT_ID, USER_ID, 304L,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, true)));
        when(enterpriseService.getEnabledEnterprises(List.of(301L, 302L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())))
                .thenReturn(List.of(enabledCompany(301L), enabledCompany(302L)));

        Set<Long> companyIds = service.getEnabledCompanyIdsForUser(USER_ID);

        assertEquals(List.of(301L, 302L), List.copyOf(companyIds));
    }

    @Test
    void getEnabledCompanyIdsForUserRejectsMissingOrOnlyDisabledMappings() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUser(TENANT_ID, USER_ID)).thenReturn(List.of());
        assertServiceException(() -> service.getEnabledCompanyIdsForUser(USER_ID), MDM_USER_COMPANY_SCOPE_DENIED);

        when(userScopeMapper.selectByTenantUser(TENANT_ID, USER_ID)).thenReturn(List.of(
                userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.DISABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.getEnabledCompanyIdsForUser(USER_ID), MDM_USER_COMPANY_SCOPE_DENIED);
    }

    @Test
    void getEnabledCompanyIdsForUserRejectsAmbiguousOrCrossTenantRawEvidence() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUser(TENANT_ID, USER_ID)).thenReturn(List.of(
                userScope(1L, 99L, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.getEnabledCompanyIdsForUser(USER_ID),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);

        when(userScopeMapper.selectByTenantUser(TENANT_ID, USER_ID)).thenReturn(List.of(
                userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                userScope(2L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.getEnabledCompanyIdsForUser(USER_ID),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);
    }

    @Test
    void validateUserCompanyAccessDeniesMissingMappingWithoutFallback() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID)).thenReturn(List.of());

        assertServiceException(() -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID),
                MDM_USER_COMPANY_SCOPE_DENIED);

        verifyNoInteractions(roleScopeMapper, roleApi, recipientResolver);
    }

    @Test
    void validateUserCompanyAccessRejectsDisabledDeletedAndAmbiguousMappings() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.DISABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID),
                MDM_USER_COMPANY_SCOPE_DISABLED);

        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, true)));
        assertServiceException(() -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID),
                MDM_USER_COMPANY_SCOPE_DENIED);

        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of(
                        userScope(1L, TENANT_ID, USER_ID, COMPANY_ID,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                        userScope(2L, TENANT_ID, USER_ID, COMPANY_ID,
                                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));
        assertServiceException(() -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID),
                MDM_COMPANY_SCOPE_CONFIG_INVALID);
    }

    @Test
    void validateUserCompanyAccessRejectsRawCrossTenantEvidenceWithoutNameLeakage() {
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.selectByTenantUserAndCompany(TENANT_ID, USER_ID, COMPANY_ID))
                .thenReturn(List.of(userScope(1L, 99L, USER_ID, COMPANY_ID,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));

        ServiceException error = assertServiceException(
                () -> service.validateUserCompanyAccess(USER_ID, COMPANY_ID), MDM_COMPANY_SCOPE_CONFIG_INVALID);

        assertEquals(-1, error.getMessage().indexOf("Owned company"));
    }

    @Test
    void createUserCompanyScopePersistsValidatedTenantOwnedMapping() {
        MdmUserCompanyScopeCreateReqDTO request = userCreateRequest();
        stubEnabledCompany();
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        doAnswer(invocation -> {
            invocation.<MdmUserCompanyScopeDO>getArgument(0).setId(501L);
            return 1;
        }).when(userScopeMapper).insert(any(MdmUserCompanyScopeDO.class));

        Long id = service.createUserCompanyScope(request);

        assertEquals(501L, id);
        ArgumentCaptor<MdmUserCompanyScopeDO> captor = ArgumentCaptor.forClass(MdmUserCompanyScopeDO.class);
        verify(userScopeMapper).insert(captor.capture());
        assertEquals(TENANT_ID, captor.getValue().getTenantId());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(COMPANY_ID, captor.getValue().getCompanyId());
        assertEquals(MdmEnterpriseStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getRevision());
    }

    @Test
    void createRoleCompanyScopePersistsValidatedTenantOwnedMapping() {
        MdmRoleCompanyScopeCreateReqDTO request = roleCreateRequest();
        stubEnabledCompany();
        when(roleApi.getRoleList(List.of(ROLE_ID))).thenReturn(List.of(enabledRole(ROLE_ID)));
        doAnswer(invocation -> {
            invocation.<MdmRoleCompanyScopeDO>getArgument(0).setId(601L);
            return 1;
        }).when(roleScopeMapper).insert(any(MdmRoleCompanyScopeDO.class));

        Long id = service.createRoleCompanyScope(request);

        assertEquals(601L, id);
        ArgumentCaptor<MdmRoleCompanyScopeDO> captor = ArgumentCaptor.forClass(MdmRoleCompanyScopeDO.class);
        verify(roleScopeMapper).insert(captor.capture());
        assertEquals(TENANT_ID, captor.getValue().getTenantId());
        assertEquals(ROLE_ID, captor.getValue().getRoleId());
        assertEquals(COMPANY_ID, captor.getValue().getCompanyId());
        assertEquals(1, captor.getValue().getRevision());
    }

    @Test
    void createMappingsTranslateOnlyTheirExactNamedUniqueConstraint() {
        stubEnabledCompany();
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        doThrow(new DuplicateKeyException("outer", new IllegalStateException(
                "Duplicate entry for key 'UK_MDM_USER_COMPANY_SCOPE_TENANT_USER_COMPANY'")))
                .when(userScopeMapper).insert(any(MdmUserCompanyScopeDO.class));
        assertServiceException(() -> service.createUserCompanyScope(userCreateRequest()),
                MDM_USER_COMPANY_SCOPE_DUPLICATE);

        stubEnabledCompany();
        when(roleApi.getRoleList(List.of(ROLE_ID))).thenReturn(List.of(enabledRole(ROLE_ID)));
        doThrow(new DuplicateKeyException("outer", new IllegalStateException(
                "Duplicate entry for key 'uk_mdm_role_company_scope_tenant_role_company'")))
                .when(roleScopeMapper).insert(any(MdmRoleCompanyScopeDO.class));
        assertServiceException(() -> service.createRoleCompanyScope(roleCreateRequest()),
                MDM_ROLE_COMPANY_SCOPE_DUPLICATE);
    }

    @Test
    void createMappingsRethrowSameUnrelatedDuplicateKeyException() {
        stubEnabledCompany();
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        DuplicateKeyException unrelated = new DuplicateKeyException("Duplicate entry for key 'uk_other_table'");
        doThrow(unrelated).when(userScopeMapper).insert(any(MdmUserCompanyScopeDO.class));

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class,
                () -> service.createUserCompanyScope(userCreateRequest()));

        assertSame(unrelated, thrown);
        verify(userScopeMapper, never()).updateById(any(MdmUserCompanyScopeDO.class));
    }

    @Test
    void createMappingsRejectInvalidInputAndWriteResult() {
        assertServiceException(() -> service.createUserCompanyScope(null), MDM_COMPANY_SCOPE_FIELD_REQUIRED);
        verifyNoInteractions(enterpriseService, adminUserApi, userScopeMapper);

        MdmUserCompanyScopeCreateReqDTO request = userCreateRequest();
        stubEnabledCompany();
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        doAnswer(invocation -> {
            invocation.<MdmUserCompanyScopeDO>getArgument(0).setId(501L);
            return 0;
        }).when(userScopeMapper).insert(any(MdmUserCompanyScopeDO.class));
        assertServiceException(() -> service.createUserCompanyScope(request), MDM_COMPANY_SCOPE_WRITE_RESULT_INVALID);
    }

    @Test
    void createCompanyScopePersistsSelectedUserAndRoleAuthorization() {
        MdmCompanyScopeSaveReqVO userRequest = saveRequest(null, "USER", USER_ID, COMPANY_ID,
                MdmEnterpriseStatusEnum.ENABLE.getStatus());
        stubEnabledCompany();
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        doAnswer(invocation -> {
            invocation.<MdmUserCompanyScopeDO>getArgument(0).setId(701L);
            return 1;
        }).when(userScopeMapper).insert(any(MdmUserCompanyScopeDO.class));

        assertEquals(701L, service.createCompanyScope(userRequest));

        MdmCompanyScopeSaveReqVO roleRequest = saveRequest(null, "ROLE", ROLE_ID, COMPANY_ID,
                MdmEnterpriseStatusEnum.ENABLE.getStatus());
        stubEnabledCompany();
        when(roleApi.getRoleList(List.of(ROLE_ID))).thenReturn(List.of(enabledRole(ROLE_ID)));
        doAnswer(invocation -> {
            invocation.<MdmRoleCompanyScopeDO>getArgument(0).setId(702L);
            return 1;
        }).when(roleScopeMapper).insert(any(MdmRoleCompanyScopeDO.class));

        assertEquals(702L, service.createCompanyScope(roleRequest));
    }

    @Test
    void updateCompanyScopeUpdatesValidatedUserMappingById() {
        MdmUserCompanyScopeDO existing = userScope(801L, TENANT_ID, USER_ID, COMPANY_ID,
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 3, false);
        when(userScopeMapper.selectById(801L)).thenReturn(existing);
        stubEnabledCompany();
        when(adminUserApi.getUserList(List.of(USER_ID))).thenReturn(List.of(enabledUser(USER_ID)));
        when(userScopeMapper.updateById(any(MdmUserCompanyScopeDO.class))).thenReturn(1);

        service.updateCompanyScope(saveRequest(801L, "USER", USER_ID, COMPANY_ID,
                MdmEnterpriseStatusEnum.DISABLE.getStatus()));

        ArgumentCaptor<MdmUserCompanyScopeDO> captor = ArgumentCaptor.forClass(MdmUserCompanyScopeDO.class);
        verify(userScopeMapper).updateById(captor.capture());
        assertEquals(801L, captor.getValue().getId());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(COMPANY_ID, captor.getValue().getCompanyId());
        assertEquals(MdmEnterpriseStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(4, captor.getValue().getRevision());
    }

    @Test
    void deleteCompanyScopeDeletesValidatedUserMappingById() {
        MdmUserCompanyScopeDO existing = userScope(901L, TENANT_ID, USER_ID, COMPANY_ID,
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 2, false);
        when(userScopeMapper.selectById(901L)).thenReturn(existing);
        when(userScopeMapper.deleteById(901L)).thenReturn(1);

        service.deleteCompanyScope("USER", 901L);

        verify(userScopeMapper).deleteById(901L);
    }

    @Test
    void resolveRecipientUserIdsDelegatesWithoutAlteringResult() {
        Set<Long> recipients = Set.of(1001L, 1002L);
        when(recipientResolver.resolve(COMPANY_ID, List.of(ROLE_ID), "dcc:registration-certificate:notify"))
                .thenReturn(recipients);

        assertSame(recipients, service.resolveRecipientUserIds(COMPANY_ID, List.of(ROLE_ID),
                "dcc:registration-certificate:notify"));
    }

    private void stubEnabledCompany() {
        when(enterpriseService.getEnabledEnterprises(List.of(COMPANY_ID),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())))
                .thenReturn(List.of(enabledCompany()));
    }

    private MdmEnterpriseDO enabledCompany() {
        return enabledCompany(COMPANY_ID);
    }

    private MdmEnterpriseDO enabledCompany(Long companyId) {
        MdmEnterpriseDO company = MdmEnterpriseDO.builder()
                .id(companyId).enterpriseCode("COMP-" + companyId).name("Owned company " + companyId)
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status(MdmEnterpriseStatusEnum.ENABLE.getStatus()).revision(1).build();
        company.setTenantId(TENANT_ID);
        company.setDeleted(false);
        return company;
    }

    private AdminUserRespDTO enabledUser(Long id) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return user;
    }

    private RoleRespDTO enabledRole(Long id) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(id);
        role.setCode("role-" + id);
        role.setName("Role " + id);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return role;
    }

    private MdmUserCompanyScopeDO userScope(Long id, Long tenantId, Long userId, Long companyId,
                                             String status, Integer revision, Boolean deleted) {
        MdmUserCompanyScopeDO scope = MdmUserCompanyScopeDO.builder()
                .id(id).userId(userId).companyId(companyId).status(status).revision(revision).build();
        scope.setTenantId(tenantId);
        scope.setDeleted(deleted);
        return scope;
    }

    private MdmUserCompanyScopeCreateReqDTO userCreateRequest() {
        MdmUserCompanyScopeCreateReqDTO request = new MdmUserCompanyScopeCreateReqDTO();
        request.setUserId(USER_ID);
        request.setCompanyId(COMPANY_ID);
        request.setStatus(MdmEnterpriseStatusEnum.ENABLE.getStatus());
        return request;
    }

    private MdmRoleCompanyScopeCreateReqDTO roleCreateRequest() {
        MdmRoleCompanyScopeCreateReqDTO request = new MdmRoleCompanyScopeCreateReqDTO();
        request.setRoleId(ROLE_ID);
        request.setCompanyId(COMPANY_ID);
        request.setStatus(MdmEnterpriseStatusEnum.ENABLE.getStatus());
        return request;
    }

    private MdmCompanyScopeSaveReqVO saveRequest(Long id, String scopeType, Long principalId, Long companyId,
                                                 String status) {
        MdmCompanyScopeSaveReqVO request = new MdmCompanyScopeSaveReqVO();
        request.setId(id);
        request.setScopeType(scopeType);
        request.setPrincipalId(principalId);
        request.setCompanyId(companyId);
        request.setStatus(status);
        return request;
    }

    private ServiceException assertServiceException(Runnable invocation, ErrorCode expected) {
        ServiceException exception = assertThrows(ServiceException.class, invocation::run);
        assertEquals(expected.getCode(), exception.getCode());
        return exception;
    }

}
