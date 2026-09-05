package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGlobalClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGlobalClaimMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSourceGlobalClaimServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileSourceGlobalClaimMapper claimMapper;
    @InjectMocks
    private DccControlledFileSourceGlobalClaimService service;

    @Test
    void claim_sameOwnerAndHashIsIdempotent() {
        when(claimMapper.selectBySourceFileId(700L)).thenReturn(claim(31L, 700L, 901L, "sha"));

        service.claim(31L, 700L, 901L, "sha", 120L, null, null);

        verify(claimMapper, never()).insert(any(DccControlledFileSourceGlobalClaimDO.class));
    }

    @Test
    void claim_crossTenantOwnerFailsBeforeInsert() {
        when(claimMapper.selectBySourceFileId(700L)).thenReturn(claim(32L, 700L, 902L, "sha"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.claim(31L, 700L, 901L, "sha", 120L, null, null));

        assertEquals(CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT.getCode(), ex.getCode());
        verify(claimMapper, never()).insert(any(DccControlledFileSourceGlobalClaimDO.class));
    }

    @Test
    void claim_newSourcePersistsGlobalIdentity() {
        when(claimMapper.selectBySourceFileId(700L)).thenReturn(null);

        service.claim(31L, 700L, 901L, "sha", 120L, 55L, 66L);

        verify(claimMapper).insert(org.mockito.ArgumentMatchers.argThat((DccControlledFileSourceGlobalClaimDO value) ->
                value.getTenantId().equals(31L)
                        && value.getSourceFileId().equals(700L)
                        && value.getControlledFileId().equals(901L)
                        && value.getSourceSha256().equals("sha")
                        && value.getGovernanceBatchId().equals(55L)
                        && value.getGovernanceItemId().equals(66L)));
    }

    private DccControlledFileSourceGlobalClaimDO claim(Long tenantId, Long sourceFileId,
                                                        Long controlledFileId, String hash) {
        return DccControlledFileSourceGlobalClaimDO.builder()
                .tenantId(tenantId).sourceFileId(sourceFileId).controlledFileId(controlledFileId)
                .sourceSha256(hash).claimStatus("ACTIVE").build();
    }
}
