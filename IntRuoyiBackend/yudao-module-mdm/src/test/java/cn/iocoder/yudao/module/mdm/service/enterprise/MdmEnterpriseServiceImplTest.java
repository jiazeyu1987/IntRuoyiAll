package cn.iocoder.yudao.module.mdm.service.enterprise;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterprisePageReqVO;
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
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_BATCH_RESULT_INVALID;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DELETED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_DISABLED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_NOT_FOUND;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TYPE_INVALID;
import static cn.iocoder.yudao.module.mdm.enums.ErrorCodeConstants.MDM_ENTERPRISE_TYPE_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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
    void listEnabledEnterprisesReturnsEnabledRowsByTypeAndKeyword() {
        MdmEnterpriseDO entrustedParty = enterprise(202L, TENANT_ID, "TRUST-002", "受托企业：上海受托制造有限公司",
                ENTRUSTED_PARTY, MdmEnterpriseStatusEnum.ENABLE.getStatus(), 7, false);
        when(enterpriseMapper.selectEnabledByTypes(TENANT_ID, Set.of(ENTRUSTED_PARTY), "上海受托", 20))
                .thenReturn(List.of(entrustedParty));

        List<MdmEnterpriseDO> result = enterpriseService.listEnabledEnterprises(
                Set.of(ENTRUSTED_PARTY), " 上海受托 ", 20);

        assertEquals(List.of(202L), result.stream().map(MdmEnterpriseDO::getId).toList());
        assertEquals("受托企业：上海受托制造有限公司", result.get(0).getName());
        assertEquals(ENTRUSTED_PARTY, result.get(0).getType());
        assertEquals(List.of("selectEnabledByTypes"), mapperInvocationNames());
    }

    @Test
    void listEnabledEnterprisesRejectsInvalidLimitBeforeQuery() {
        assertServiceException(() -> enterpriseService.listEnabledEnterprises(Set.of(ENTRUSTED_PARTY), null, 0),
                MDM_ENTERPRISE_FIELD_REQUIRED);

        verifyNoInteractions(enterpriseMapper);
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
    void getEnabledEnterprisesRequiresTenantBeforeRawClassificationQuery() {
        TenantContextHolder.clear();

        assertThrows(NullPointerException.class, () -> enterpriseService.getEnabledEnterprises(List.of(101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())));

        verifyNoInteractions(enterpriseMapper);
    }

    @Test
    void getEnabledEnterprisesRejectsIncompleteRawEvidenceWithoutReturningAnyRows() {
        List<MdmEnterpriseDO> invalidRows = List.of(
                enterprise(null, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(0L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, null, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, null, "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, " ", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", null, MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", " ", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", null,
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", " ",
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", "UNKNOWN_TYPE",
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        null, 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        " ", 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        "UNKNOWN_STATUS", 1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), null, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 0, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), -1, false),
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, null));

        for (MdmEnterpriseDO invalidRow : invalidRows) {
            reset(enterpriseMapper);
            when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(invalidRow));

            assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L),
                    Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_BATCH_RESULT_INVALID);
            assertEquals(List.of("selectClassificationByIds"), mapperInvocationNames());
        }
    }

    @Test
    void getEnabledEnterprisesRejectsMapperRowsOutsideRequestedIdSet() {
        when(enterpriseMapper.selectClassificationByIds(any())).thenReturn(List.of(
                enterprise(101L, TENANT_ID, "COMP-001", "Owned company",
                        MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false),
                enterprise(202L, TENANT_ID, "COMP-002", "Other company",
                        MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                        MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false)));

        assertServiceException(() -> enterpriseService.getEnabledEnterprises(List.of(101L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())), MDM_ENTERPRISE_BATCH_RESULT_INVALID);
        assertEquals(List.of("selectClassificationByIds"), mapperInvocationNames());
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
    void createEnterpriseMapsOnlyNamedTenantCodeDuplicateCauseToBusinessConflict() {
        MdmEnterpriseSaveReqVO request = new MdmEnterpriseSaveReqVO();
        request.setEnterpriseCode("COMP-001");
        request.setName("Owned company");
        request.setType(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
        request.setStatus(MdmEnterpriseStatusEnum.ENABLE.getStatus());
        doThrow(new DuplicateKeyException("outer duplicate", new IllegalStateException(
                "Duplicate entry for key 'UK_MDM_ENTERPRISE_TENANT_CODE'")))
                .when(enterpriseMapper).insert(any(MdmEnterpriseDO.class));

        assertServiceException(() -> enterpriseService.createEnterprise(request), MDM_ENTERPRISE_CODE_DUPLICATE);

        verify(enterpriseMapper).insert(any(MdmEnterpriseDO.class));
        verify(enterpriseMapper, never()).updateById(any(MdmEnterpriseDO.class));
    }

    @Test
    void createEnterpriseRethrowsSameUnrelatedDuplicateKeyException() {
        MdmEnterpriseSaveReqVO request = validCreateRequest();
        DuplicateKeyException unrelated = new DuplicateKeyException("Duplicate entry for key 'uk_other_table'");
        doThrow(unrelated).when(enterpriseMapper).insert(any(MdmEnterpriseDO.class));

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class,
                () -> enterpriseService.createEnterprise(request));

        assertSame(unrelated, thrown);
    }

    @Test
    void createEnterpriseRejectsNonExactInsertCountAndInvalidGeneratedId() {
        List<Integer> invalidCounts = List.of(0, 2, -1);
        for (Integer invalidCount : invalidCounts) {
            reset(enterpriseMapper);
            doAnswer(invocation -> {
                invocation.<MdmEnterpriseDO>getArgument(0).setId(301L);
                return invalidCount;
            }).when(enterpriseMapper).insert(any(MdmEnterpriseDO.class));
            assertServiceException(() -> enterpriseService.createEnterprise(validCreateRequest()),
                    MDM_ENTERPRISE_BATCH_RESULT_INVALID);
            assertEquals(List.of("insert"), mapperInvocationNames());
        }
        for (Long invalidId : java.util.Arrays.asList(null, 0L, -1L)) {
            reset(enterpriseMapper);
            doAnswer(invocation -> {
                invocation.<MdmEnterpriseDO>getArgument(0).setId(invalidId);
                return 1;
            }).when(enterpriseMapper).insert(any(MdmEnterpriseDO.class));
            assertServiceException(() -> enterpriseService.createEnterprise(validCreateRequest()),
                    MDM_ENTERPRISE_BATCH_RESULT_INVALID);
            assertEquals(List.of("insert"), mapperInvocationNames());
        }
    }

    @Test
    void getEnterprisePageDelegatesToMapperWithFormalFilters() {
        MdmEnterprisePageReqVO reqVO = new MdmEnterprisePageReqVO();
        reqVO.setKeyword("七木");
        reqVO.setType(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
        MdmEnterpriseDO enterprise = enterprise(301L, TENANT_ID, "COMP-001", "上海七木医疗器械有限公司",
                MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), MdmEnterpriseStatusEnum.ENABLE.getStatus(), 2, false);
        when(enterpriseMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(enterprise), 1L));

        PageResult<MdmEnterpriseDO> result = enterpriseService.getEnterprisePage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("COMP-001", result.getList().get(0).getEnterpriseCode());
        verify(enterpriseMapper).selectPage(reqVO);
    }

    @Test
    void updateEnterprisePreservesIdentityAndIncrementsRevision() {
        MdmEnterpriseDO existing = enterprise(301L, TENANT_ID, "COMP-001", "上海七木医疗器械有限公司",
                MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), MdmEnterpriseStatusEnum.ENABLE.getStatus(), 2, false);
        when(enterpriseMapper.selectById(301L)).thenReturn(existing);
        when(enterpriseMapper.updateById(any(MdmEnterpriseDO.class))).thenReturn(1);
        MdmEnterpriseSaveReqVO request = validCreateRequest();
        request.setId(301L);
        request.setEnterpriseCode(" COMP-002 ");
        request.setName(" 上海七木医疗科技有限公司 ");
        request.setType(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
        request.setStatus(MdmEnterpriseStatusEnum.DISABLE.getStatus());

        enterpriseService.updateEnterprise(request);

        ArgumentCaptor<MdmEnterpriseDO> captor = ArgumentCaptor.forClass(MdmEnterpriseDO.class);
        verify(enterpriseMapper).updateById(captor.capture());
        assertEquals(301L, captor.getValue().getId());
        assertEquals("COMP-002", captor.getValue().getEnterpriseCode());
        assertEquals("上海七木医疗科技有限公司", captor.getValue().getName());
        assertEquals(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), captor.getValue().getType());
        assertEquals(MdmEnterpriseStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getRevision());
    }

    @Test
    void updateEnterpriseRejectsMissingRowBeforeMutation() {
        MdmEnterpriseSaveReqVO request = validCreateRequest();
        request.setId(404L);
        when(enterpriseMapper.selectById(404L)).thenReturn(null);

        assertServiceException(() -> enterpriseService.updateEnterprise(request), MDM_ENTERPRISE_NOT_FOUND);

        verify(enterpriseMapper, never()).updateById(any(MdmEnterpriseDO.class));
    }

    @Test
    void updateEnterpriseMapsOnlyNamedTenantCodeDuplicateCauseToBusinessConflict() {
        MdmEnterpriseSaveReqVO request = validCreateRequest();
        request.setId(301L);
        when(enterpriseMapper.selectById(301L)).thenReturn(enterprise(301L, TENANT_ID, "COMP-001",
                "上海七木医疗器械有限公司", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false));
        doThrow(new DuplicateKeyException("outer duplicate", new IllegalStateException(
                "Duplicate entry for key 'UK_MDM_ENTERPRISE_TENANT_CODE'")))
                .when(enterpriseMapper).updateById(any(MdmEnterpriseDO.class));

        assertServiceException(() -> enterpriseService.updateEnterprise(request), MDM_ENTERPRISE_CODE_DUPLICATE);
    }

    @Test
    void updateEnterpriseStatusIncrementsRevision() {
        when(enterpriseMapper.selectById(301L)).thenReturn(enterprise(301L, TENANT_ID, "COMP-001",
                "上海七木医疗器械有限公司", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 4, false));
        when(enterpriseMapper.updateById(any(MdmEnterpriseDO.class))).thenReturn(1);

        enterpriseService.updateEnterpriseStatus(301L, MdmEnterpriseStatusEnum.DISABLE.getStatus());

        ArgumentCaptor<MdmEnterpriseDO> captor = ArgumentCaptor.forClass(MdmEnterpriseDO.class);
        verify(enterpriseMapper).updateById(captor.capture());
        assertEquals(301L, captor.getValue().getId());
        assertEquals(MdmEnterpriseStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(5, captor.getValue().getRevision());
    }

    @Test
    void deleteEnterpriseRequiresExistingRowThenSoftDeletes() {
        when(enterpriseMapper.selectById(301L)).thenReturn(enterprise(301L, TENANT_ID, "COMP-001",
                "上海七木医疗器械有限公司", MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), 1, false));
        when(enterpriseMapper.deleteById(301L)).thenReturn(1);

        enterpriseService.deleteEnterprise(301L);

        verify(enterpriseMapper).deleteById(301L);
    }

    @Test
    void listSimpleEnterprisesRejectsInvalidStatusAndTypeBeforeQuery() {
        assertServiceException(() -> enterpriseService.listSimpleEnterprises("BAD_TYPE",
                MdmEnterpriseStatusEnum.ENABLE.getStatus(), null), MDM_ENTERPRISE_TYPE_INVALID);
        assertServiceException(() -> enterpriseService.listSimpleEnterprises(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(),
                "BAD_STATUS", null), MDM_ENTERPRISE_STATUS_INVALID);

        verifyNoInteractions(enterpriseMapper);
    }

    private MdmEnterpriseDO enterprise(Long id, Long tenantId, String enterpriseCode, String name, String type,
                                       String status, Integer revision, Boolean deleted) {
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

    private MdmEnterpriseSaveReqVO validCreateRequest() {
        MdmEnterpriseSaveReqVO request = new MdmEnterpriseSaveReqVO();
        request.setEnterpriseCode("COMP-001");
        request.setName("Owned company");
        request.setType(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
        request.setStatus(MdmEnterpriseStatusEnum.ENABLE.getStatus());
        return request;
    }

    private List<String> mapperInvocationNames() {
        return mockingDetails(enterpriseMapper).getInvocations().stream()
                .map(invocation -> invocation.getMethod().getName())
                .toList();
    }

}
