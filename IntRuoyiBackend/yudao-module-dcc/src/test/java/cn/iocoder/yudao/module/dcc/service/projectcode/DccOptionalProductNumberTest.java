package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccApprovedProductIdentityMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImpl;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductIdentityResolver;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccOptionalProductNumberTest {
    private final MdmProductApi mdm = mock(MdmProductApi.class);
    private final DccApprovedProductIdentityMapper catalog = mock(DccApprovedProductIdentityMapper.class);
    private final DccProjectCodeDO project = DccProjectCodeDO.builder().id(10L)
            .projectCode("AC").projectName("造影导管").status("ENABLE").productMasterId(20L)
            .defaultAttributesJson("{\"targetMarkets\":[\"NA\"],\"licenseHolder\":\"NA\",\"actualManufacturer\":\"NA\",\"documentTransfer\":\"N\"}").build();

    @BeforeEach void tenant() {
        TenantContextHolder.setTenantId(1L);
        project.setTenantId(1L);
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"   ", "A1234567890123"})
    void projectListAndActualProductPreviewAllowMissingNumberWithoutLosingBinding(String code) {
        when(mdm.getProduct(20L)).thenReturn(MdmProductRespDTO.builder().id(20L)
                .productCode("INT-27").dccProductCode(code).nameCn("造影导管").status("ENABLE").build());
        var workflow = new DccControlledFileWorkflowServiceImpl();
        var mapper = mock(DccProjectCodeMapper.class);
        var access = mock(DccProjectAccessService.class);
        when(mapper.selectById(10L)).thenReturn(project);
        ReflectionTestUtils.setField(workflow,"projectCodeMapper",mapper);
        ReflectionTestUtils.setField(workflow,"projectAccessService",access);
        ReflectionTestUtils.setField(workflow,"mdmProductApi",mdm);
        ReflectionTestUtils.setField(workflow,"approvedProductIdentityMapper",catalog);
        var preview = workflow.previewProjectProduct(99L,10L);
        assertEquals(20L,preview.getProductMasterId());
        assertEquals("MDM_MASTER",preview.getSource());
        assertEquals("造影导管",preview.getProductName());
        assertEquals(cn.hutool.core.util.StrUtil.trimToNull(code),preview.getProductCode());
        var projects = mock(DccProjectCodeService.class);
        when(projects.getReadableProjectCodePage(eq(99L),any())).thenReturn(new PageResult<>(List.of(project),1L));
        var options = new DccProjectUploadOptionsService();
        ReflectionTestUtils.setField(options,"projectCodeService",projects);
        ReflectionTestUtils.setField(options,"workflowService",workflow);
        assertTrue(options.list(99L,null).get(0).uploadSelectable());
        verify(access,times(2)).assertProjectEditorOrOwner(99L,10L);
        verifyNoInteractions(catalog);
        verify(mdm,never()).getEnabledDccProduct(any());
    }

    @Test void missingDisabledForeignOrNamelessProductStillCannotBecomeAValidBinding() {
        assertThrows(RuntimeException.class,()->DccProjectProductIdentityResolver.resolve(project,mdm,catalog));
        for (var product : List.of(
                MdmProductRespDTO.builder().id(20L).nameCn("产品").status("DISABLE").build(),
                MdmProductRespDTO.builder().id(21L).nameCn("产品").status("ENABLE").build(),
                MdmProductRespDTO.builder().id(20L).status("ENABLE").build())) {
            when(mdm.getProduct(20L)).thenReturn(product);
            assertThrows(RuntimeException.class,()->DccProjectProductIdentityResolver.resolve(project,mdm,catalog));
        }
        verifyNoInteractions(catalog);
    }
}
