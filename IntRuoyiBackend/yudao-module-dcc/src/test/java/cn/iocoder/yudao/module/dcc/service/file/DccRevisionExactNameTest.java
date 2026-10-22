package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccRevisionExactNameTest extends BaseMockitoUnitTest {
    @org.mockito.Mock private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper reservationMapper;

    @InjectMocks private DccControlledFileNameClaimService service;
    @Mock private DccControlledFileNameClaimMapper mapper;
    @Mock private DccControlledFileMasterMapper masterMapper;

    @BeforeEach void tenant() {
        TenantContextHolder.setTenantId(1L);
        org.mockito.Mockito.lenient().when(reservationMapper.insertModern(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyLong())).thenReturn(1);

        when(masterMapper.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L).dccProjectCodeId(5L).fileTypeTaxonomyLeafId(6L).normalizedFileNumber("SOP-001").build());
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }
    @ParameterizedTest
    @ValueSource(strings={"SOP.pdf", "sop.pdf", "SOP.PDF", "SOP.docx", " SOP.pdf "})
    void reservesExactFullSourceName(String name) {
        when(mapper.insert(any(DccControlledFileNameClaimDO.class))).thenAnswer(i -> {var row=i.<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO>getArgument(0);row.setId(999L);return 1;});
        service.claimIdentity(1L, name, 5L, 6L, "SOP-001", 10L);
        var captor = ArgumentCaptor.forClass(DccControlledFileNameClaimDO.class);
        verify(mapper).insert(captor.capture());
        assertEquals(name, captor.getValue().getNormalizedName());
    }
}
