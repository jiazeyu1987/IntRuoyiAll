package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper;
import org.junit.jupiter.api.Test;
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

    @InjectMocks
    private DccControlledFileNameClaimService service;

    @Mock
    private DccControlledFileNameClaimMapper claimMapper;

    @Test
    void claimRejectsDifferentMasterInSameTenant() {
        when(claimMapper.selectActiveByName(1L, "检验规程"))
                .thenReturn(DccControlledFileNameClaimDO.builder()
                        .id(10L).tenantId(1L).normalizedName("检验规程").masterId(100L).build());

        assertServiceException(() -> service.claim(1L, " 检验规程 ", 200L),
                CONTROLLED_FILE_NAME_EXISTS);

        verify(claimMapper, never()).insert(any(DccControlledFileNameClaimDO.class));
    }

    @Test
    void claimIsIdempotentForSameMasterAndNormalizesName() {
        when(claimMapper.selectActiveByName(1L, "inspection procedure"))
                .thenReturn(DccControlledFileNameClaimDO.builder()
                        .id(10L).tenantId(1L).normalizedName("inspection procedure").masterId(100L).build());

        service.claim(1L, " Inspection Procedure ", 100L);

        verify(claimMapper, never()).insert(any(DccControlledFileNameClaimDO.class));
    }

    @Test
    void claimPersistsNormalizedNameForNewMaster() {
        when(claimMapper.selectActiveByName(1L, "检验规程")).thenReturn(null);
        when(claimMapper.insert(any(DccControlledFileNameClaimDO.class))).thenReturn(1);

        service.claim(1L, " 检验规程 ", 200L);

        ArgumentCaptor<DccControlledFileNameClaimDO> captor =
                ArgumentCaptor.forClass(DccControlledFileNameClaimDO.class);
        verify(claimMapper).insert(captor.capture());
        assertEquals("检验规程", captor.getValue().getNormalizedName());
        assertEquals(200L, captor.getValue().getMasterId());
        assertEquals(1L, captor.getValue().getTenantId());
    }

    @Test
    void releaseMarksActiveClaimDeleted() {
        service.release(1L, 200L);

        verify(claimMapper).releaseByMasterId(1L, 200L);
    }
}
