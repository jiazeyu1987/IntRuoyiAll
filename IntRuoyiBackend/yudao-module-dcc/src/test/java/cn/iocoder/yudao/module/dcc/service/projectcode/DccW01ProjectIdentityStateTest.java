package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccApprovedProductIdentityMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductIdentityResolver;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccW01ProjectIdentityStateTest {
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void disabledOrMissingStateRejectsBeforeAnyProductLookupIncludingMdm() {
        for (String state : new String[] {"DISABLE", null, ""}) {
            for (Long masterId : new Long[] {null, 55L}) {
                var project = DccProjectCodeDO.builder().id(7L)
                        .projectCode("P-7").projectName("项目七").status(state).productMasterId(masterId).build();
                project.setTenantId(1L);
                var mdm = mock(MdmProductApi.class);
                var approved = mock(DccApprovedProductIdentityMapper.class);
                assertThrows(RuntimeException.class, () -> DccProjectProductIdentityResolver.resolve(project, mdm, approved));
                verifyNoInteractions(mdm, approved);
            }
        }
    }
    @Test void enabledLegacyProjectWithoutDeclaredEvidenceRemainsExplicitlyUnbound() {
        var project = DccProjectCodeDO.builder().id(7L)
                .projectCode("P-7").projectName("项目七").status("ENABLE").build();
        project.setTenantId(1L);
        var approved = mock(DccApprovedProductIdentityMapper.class);
        when(approved.selectApprovedProduct(1L, 7L)).thenReturn(List.of());
        assertEquals("UNBOUND", DccProjectProductIdentityResolver.resolve(project, null, approved).source());
        verify(approved).countDeclaredProductEvidence(1L, 7L);
    }
}
