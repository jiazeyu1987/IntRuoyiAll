package cn.iocoder.yudao.module.mdm.service.enterprise;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.enterprise.MdmEnterpriseMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
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

import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_BATCH_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_BATCH_EMPTY;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DELETED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DISABLED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_NOT_FOUND;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TYPE_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmEnterpriseServiceImplTest {

    private static final Long TENANT_ID = 11L;
    private static final String ENTRUSTED_PARTY = "ENTRUSTED_PARTY";

    @Mock
    private MdmEnterpriseMapper enterpriseMapper;

    private MdmEnterpriseServiceImpl enterpriseService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        enterpriseService = new MdmEnterpriseServiceImpl();
        ReflectionTestUtils.setField(enterpriseService, "enterpriseMapper", enterpriseMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getEnabledEnterprisesReturnsCompleteEvidenceInRequestedOrder() {
        MdmEnterpriseDO ownedCompany = enterprise(101L, TENANT_ID, "COMP-001", "Owned company",
                MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), MdmEnterpriseStatusEnum.ENABLE.getStatus(), 3, false);
        MdmEnterpriseDO entrustedParty = enterprise(202L, TENANT_ID, "TRUST-002", "Entrusted party",
                ENTRUSTED_PARTY, MdmEnterpriseStatusEnum.ENABLE.getStatus(), 7,
                false);
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(entrustedParty, ownedCompany));

        List<MdmEnterpriseDO> result = assertDoesNotThrow(() -> enterpriseService.getEnabledEnterprises(
                List.of(101L, 202L), Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), ENTRUSTED_PARTY)));

        assertEquals(List.of(101L, 202L), result.stream().map(MdmEnterpriseDO::getId).toList());
        assertEquals(TENANT_ID, result.get(0).getTenantId());
        assertEquals("COMP-001", result.get(0).getEnterpriseCode());
        assertEquals("Owned company", result.get(0).getName());
        assertEquals(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), result.get(0).getType());
        assertEquals(MdmEnterpriseStatusEnum.ENABLE.getStatus(), result.get(0).getStatus());
        assertEquals(3, result.get(0).getRevision());
        assertEquals(List.of("selectClassificationByIds"), mapperInvocationNames());
    }

    @Test
    void getEnabledEnterprisesRejectsEmptyBatchBeforeQuery() {
        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_BATCH_EMPTY);

        verifyNoInteractions(enterpriseMapper);
    }

    @Test
    void getEnabledEnterprisesRejectsDuplicateIdsBeforeQuery() {
        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L, 101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_BATCH_DUPLICATE);

        verifyNoInteractions(enterpriseMapper);
    }

    @Test
    void getEnabledEnterprisesRejectsNullEmptyAndIllegalAllowedTypesBeforeQuery() {
        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L), null),
                MDM_ENTERPRISE_TYPE_MISMATCH);
        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L), Set.of()),
                MDM_ENTERPRISE_TYPE_MISMATCH);
        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L),
                Set.of("EXTERNAL_ENTERPRISE")), MDM_ENTERPRISE_TYPE_MISMATCH);

        verifyNoInteractions(enterpriseMapper);
    }

    @Test
    void getEnabledEnterprisesRejectsMissingRowWithoutReturningPartialData() {
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(enterprise(101L, TENANT_ID,
                "COMP-001",
                "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));

        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L, 202L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_NOT_FOUND);
    }

    @Test
    void getEnabledEnterprisesRejectsDisabledRowWithoutReturningPartialData() {
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(enterprise(101L, TENANT_ID,
                "COMP-001",
                "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.DISABLE.getStatus(), 1, false)));

        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_DISABLED);
    }

    @Test
    void getEnabledEnterprisesRejectsDeletedRowWithoutReturningPartialData() {
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(enterprise(101L, TENANT_ID,
                "COMP-001",
                "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, true)));

        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_DELETED);
    }

    @Test
    void getEnabledEnterprisesRejectsWrongTypeWithoutReturningPartialData() {
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(enterprise(202L, TENANT_ID,
                "TRUST-002",
                "Entrusted party", ENTRUSTED_PARTY,
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));

        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(202L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_TYPE_MISMATCH);
    }

    @Test
    void getEnabledEnterprisesRejectsCrossTenantRowWithoutReturningPartialData() {
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(enterprise(101L, 99L,
                "COMP-001",
                "Foreign company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));

        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_TENANT_MISMATCH);
    }

    @Test
    void getEnabledEnterprisesValidatesWholeRawBatchBeforeReturningAnyRows() {
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company",
                        MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(202L, TENANT_ID, "TRUST-002", "Entrusted party",
                        ENTRUSTED_PARTY,
                        MdmEnterpriseStatusEnum.DISABLE.getStatus(), 1, false)));

        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L, 202L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        ENTRUSTED_PARTY)), MDM_ENTERPRISE_DISABLED);
        assertEquals(List.of("selectClassificationByIds"), mapperInvocationNames());
    }

    @Test
    void createEnterprisePersistsStableIdentityAndInitialRevision() {
        MdmEnterpriseSaveReqVO request = new MdmEnterpriseSaveReqVO();
        request.setEnterpriseCode("  COMP-001  ");
        request.setName("  Owned company  ");
        request.setType(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
        request.setStatus(MdmEnterpriseStatusEnum.ENABLE.getStatus());
        doAnswer(invocation -> {
            MdmEnterpriseDO enterprise = invocation.getArgument(0);
            enterprise.setId(301L);
            return 1;
        }).when(enterpriseMapper).insert(any(MdmEnterpriseDO.class));

        Long enterpriseId = enterpriseService.createEnterprise(request);

        assertEquals(301L, enterpriseId);
        ArgumentCaptor<MdmEnterpriseDO> captor = ArgumentCaptor.forClass(MdmEnterpriseDO.class);
        verify(enterpriseMapper).insert(captor.capture());
        assertEquals("COMP-001", captor.getValue().getEnterpriseCode());
        assertEquals("Owned company", captor.getValue().getName());
        assertEquals(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), captor.getValue().getType());
        assertEquals(MdmEnterpriseStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getRevision());
    }

    @Test
    void createEnterpriseMapsConcurrentTenantCodeDuplicateToBusinessConflict() {
        MdmEnterpriseSaveReqVO request = new MdmEnterpriseSaveReqVO();
        request.setEnterpriseCode("COMP-001");
        request.setName("Owned company");
        request.setType(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
        request.setStatus(MdmEnterpriseStatusEnum.ENABLE.getStatus());
        doThrow(new DuplicateKeyException("uk_mdm_enterprise_tenant_code"))
                .when(enterpriseMapper).insert(any(MdmEnterpriseDO.class));

        assertServiceException(() -> enterpriseService.createEnterprise(request), MDM_ENTERPRISE_CODE_DUPLICATE);

        verify(enterpriseMapper).insert(any(MdmEnterpriseDO.class));
        verify(enterpriseMapper, never()).updateById(any(MdmEnterpriseDO.class));
    }

    private MdmEnterpriseDO enterprise(Long id, Long tenantId, String enterpriseCode, String name, String type,
                                       String status, Integer revision, boolean deleted) {
        MdmEnterpriseDO enterprise = MdmEnterpriseDO.builder()
                .id(id)
                .enterpriseCode(enterpriseCode)
                .name(name)
                .type(type)
                .status(status)
                .revision(revision)
                .build();
        enterprise.setTenantId(tenantId);
        enterprise.setDeleted(deleted);
        return enterprise;
    }

    private void assertServiceException(Runnable invocation, ErrorCode expected) {
        ServiceException exception = assertThrows(ServiceException.class, invocation::run);
        assertEquals(expected.getCode(), exception.getCode());
    }

    private List<String> mapperInvocationNames() {
        return mockingDetails(enterpriseMapper).getInvocations().stream()
                .map(invocation -> invocation.getMethod().getName())
                .toList();
    }

}
