package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NAME_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileNameClaimServiceTest extends BaseMockitoUnitTest {
    @org.mockito.Mock private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper reservationMapper;


    @InjectMocks
    private DccControlledFileNameClaimService service;

    @Mock
    private DccControlledFileNameClaimMapper claimMapper;
    @Mock private DccControlledFileMasterMapper masterMapper;

    @BeforeEach void tenant() {
        TenantContextHolder.setTenantId(1L);
        org.mockito.Mockito.lenient().when(reservationMapper.insertModern(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyLong())).thenReturn(1);

        org.mockito.Mockito.lenient().when(masterMapper.selectByIdForUpdate(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(i -> DccControlledFileMasterDO.builder().id(i.getArgument(0)).tenantId(1L).dccProjectCodeId(5L).fileTypeTaxonomyLeafId(6L).normalizedFileNumber("SOP-001").build());
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void claimRejectsDifferentMasterInSameTenant() {
        when(claimMapper.selectActiveByName(1L, "检验规程"))
                .thenReturn(DccControlledFileNameClaimDO.builder()
                        .id(10L).tenantId(1L).normalizedName("检验规程").masterId(100L).build());

        assertServiceException(() -> service.claimIdentity(1L, "检验规程", 5L, 6L, "SOP-001", 200L),
                CONTROLLED_FILE_NAME_EXISTS);

        verify(claimMapper, never()).insert(any(DccControlledFileNameClaimDO.class));
    }

    @Test
    void claimIsIdempotentForSameMasterAndNormalizesName() {
        when(claimMapper.selectActiveByName(1L, "Inspection Procedure.pdf"))
                .thenReturn(DccControlledFileNameClaimDO.builder()
                        .id(10L).tenantId(1L).normalizedName("Inspection Procedure.pdf").masterId(100L).dccProjectCodeId(5L).fileTypeTaxonomyLeafId(6L).normalizedFileNumber("SOP-001").build());

        assertServiceException(() -> service.claimIdentity(1L, "Inspection Procedure.pdf", 5L, 6L, "SOP-001", 100L),CONTROLLED_FILE_NAME_EXISTS);

        verify(claimMapper, never()).insert(any(DccControlledFileNameClaimDO.class));
    }

    @Test
    void claimPersistsNormalizedNameForNewMaster() {
        when(claimMapper.selectActiveByName(1L, "检验规程")).thenReturn(null);
        when(claimMapper.insert(any(DccControlledFileNameClaimDO.class))).thenAnswer(i -> {var row=i.<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO>getArgument(0);row.setId(999L);return 1;});

        service.claimIdentity(1L, "检验规程", 5L, 6L, "SOP-001", 200L);

        ArgumentCaptor<DccControlledFileNameClaimDO> captor =
                ArgumentCaptor.forClass(DccControlledFileNameClaimDO.class);
        verify(claimMapper).insert(captor.capture());
        assertEquals("检验规程", captor.getValue().getNormalizedName());
        assertEquals(200L, captor.getValue().getMasterId());
        assertEquals(1L, captor.getValue().getTenantId());
    }

    @Test
    void immediateReleaseIsExplicitlyForbidden() {
        assertThrows(IllegalStateException.class, () -> service.release(1L, 200L));
        verify(claimMapper, never()).releaseByMasterId(1L, 200L);
    }
    @Test void legacyNameOnlyEntryCannotCreateIncompleteNumberReservation() {
        org.mockito.Mockito.lenient().when(claimMapper.insert(any(DccControlledFileNameClaimDO.class))).thenAnswer(i -> {var row=i.<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO>getArgument(0);row.setId(999L);return 1;});
        assertThrows(IllegalStateException.class, () -> service.claim(1L,"SOP.pdf",200L));
        verify(claimMapper,never()).insert(any(DccControlledFileNameClaimDO.class));
    }
    @Test void explicitIdentityRequiresAllFormalNumberFields() {
        org.mockito.Mockito.lenient().when(claimMapper.insert(any(DccControlledFileNameClaimDO.class))).thenAnswer(i -> {var row=i.<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO>getArgument(0);row.setId(999L);return 1;});
        assertThrows(IllegalArgumentException.class, () -> service.claimIdentity(1L,"SOP.pdf",null,null,null,200L));
        verify(claimMapper,never()).insert(any(DccControlledFileNameClaimDO.class));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"99,6,SOP-001", "5,99,SOP-001", "5,6,OTHER"})
    void claimedNumberMustMatchTheLockedMastersFormalIdentity(Long project,Long leaf,String number) {
        when(masterMapper.selectByIdForUpdate(200L)).thenReturn(DccControlledFileMasterDO.builder().id(200L).tenantId(1L)
                .dccProjectCodeId(5L).fileTypeTaxonomyLeafId(6L).normalizedFileNumber("SOP-001").build());
        org.mockito.Mockito.lenient().when(claimMapper.insert(any(DccControlledFileNameClaimDO.class))).thenAnswer(i -> {var row=i.<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO>getArgument(0);row.setId(999L);return 1;});
        assertThrows(IllegalArgumentException.class,() -> service.claimIdentity(1L,"SOP.pdf",project,leaf,number,200L));
        verify(claimMapper,never()).insert(any(DccControlledFileNameClaimDO.class));
    }
}
