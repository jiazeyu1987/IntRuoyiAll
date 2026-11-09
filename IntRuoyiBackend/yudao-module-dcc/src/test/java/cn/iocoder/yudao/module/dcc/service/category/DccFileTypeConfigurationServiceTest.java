package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DccFileTypeConfigurationServiceTest {
    @InjectMocks private DccFileTypeConfigurationService service;
    @Mock private DccFileTypeTaxonomyAdminService taxonomyService;
    @Mock private DccFileCategoryAdminService categoryService;
    @Mock private DccCategoryApprovalMatrixAdminService matrixService;
    @Mock private DccFileTypeTaxonomyMapper taxonomyMapper;
    @Mock private DccFileCategoryMapper categoryMapper;
    @Mock private cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper directoryMapper;
    private DccFileTypeTaxonomySaveReqVO request;

    @BeforeEach void setup() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        request = new DccFileTypeTaxonomySaveReqVO();
        request.setParentId(10L);
        request.setDirectoryBinding(new DccCategoryDirectoryBindingSaveReqVO());
        request.getDirectoryBinding().setDirectoryId(700L); request.getDirectoryBinding().setActive(true);
        lenient().when(taxonomyMapper.selectById(10L)).thenReturn(DccFileTypeTaxonomyDO.builder().id(10L).levelNo(2).build());
        lenient().when(directoryMapper.selectTenantForUpdate(1L,700L)).thenReturn(
                cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO.builder().id(700L).tenantId(1L).active(true).build());
        request.setName("测试类型"); request.setActive(true); request.setSort(1);
        lenient().when(taxonomyService.createTaxonomy(request)).thenReturn(100L);
        lenient().when(taxonomyMapper.selectById(100L)).thenReturn(DccFileTypeTaxonomyDO.builder()
                .id(100L).levelNo(3).code("TEST").name("测试类型").active(true).sort(1).build());
    }
    @AfterEach void clearTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    @Test void missingStorageDirectoryIsRejectedBeforeCreatingType() {
        request.setDirectoryBinding(null);
        assertThrows(ServiceException.class, () -> service.create(request));
        verifyNoInteractions(taxonomyService, categoryService, matrixService);
    }
    @Test void inactiveOrOtherTenantStorageDirectoryIsRejectedBeforeCreatingType() {
        var directory = directoryMapper.selectTenantForUpdate(1L,700L);
        directory.setActive(false);
        assertThrows(ServiceException.class, () -> service.create(request));
        directory.setActive(true); directory.setTenantId(2L);
        assertThrows(ServiceException.class, () -> service.create(request));
        verifyNoInteractions(taxonomyService, categoryService, matrixService);
    }
    @Test void leafWithoutMatrixFailsWithoutCreatingCategory() {
        assertThrows(ServiceException.class, () -> service.create(request));
        verifyNoInteractions(categoryService, matrixService);
    }
    @Test void customApplicationCreatesNoPublicMatrix() {
        request.setApprovalSourceMode("APPLICATION_CUSTOM");
        var other = taxonomyMapper.selectById(100L); other.setName("其他");
        when(categoryService.createCategory(any())).thenReturn(200L);
        assertEquals(100L, service.create(request));
        verify(categoryService).createCategory(argThat(row -> row.getFileTypeTaxonomyId().equals(100L)
                && "APPLICATION_CUSTOM".equals(row.getApprovalSourceMode())
                && row.getDirectoryBinding().getDirectoryId().equals(700L) && row.getDirectoryBinding().getActive()));
        verifyNoInteractions(matrixService);
    }
    @Test void ordinaryTypeCannotBypassPublicMatrixUsingCustomSource() {
        request.setApprovalSourceMode("APPLICATION_CUSTOM");
        assertThrows(ServiceException.class, () -> service.create(request));
        verifyNoInteractions(categoryService, matrixService);
    }
    @Test void standaloneCategoryCannotCreatePublicTypeWithoutMatrix() {
        when(taxonomyMapper.selectById(100L)).thenReturn(DccFileTypeTaxonomyDO.builder().id(100L).name("普通类型").build());
        var category = new DccFileCategorySaveReqVO(); category.setFileTypeTaxonomyId(100L);
        category.setApprovalSourceMode("PUBLIC_MATRIX");
        assertThrows(ServiceException.class, () -> service.createCategory(category));
        verifyNoInteractions(categoryService);
    }
    @Test void categoryUpdateCannotSwitchOrdinaryTypeToCustom() {
        var category = new DccFileCategorySaveReqVO(); category.setId(200L);
        category.setFileTypeTaxonomyId(100L); category.setApprovalSourceMode("APPLICATION_CUSTOM");
        assertThrows(ServiceException.class, () -> service.updateCategory(category));
        verifyNoInteractions(categoryService, matrixService);
    }
    @Test void standaloneOtherCategoryCannotSelectPublicEvenWithValidMatrix() {
        taxonomyMapper.selectById(100L).setName("其他");
        var category = new DccFileCategorySaveReqVO();
        category.setFileTypeTaxonomyId(100L); category.setApprovalSourceMode("PUBLIC_MATRIX");
        category.setApprovalMatrix(new DccCategoryApprovalMatrixSaveReqVO()
                .setEffectiveTime(LocalDateTime.now().minusMinutes(1)).setRules(List.of(
                        new DccCategoryApprovalMatrixSaveReqVO.Rule().setStageType("SIGNOFF").setActive(true)
                                .setSubjectType("DEPT").setSubjectId(910380L).setSubjectLabel("正式部门").setMarker("●"),
                        new DccCategoryApprovalMatrixSaveReqVO.Rule().setStageType("APPROVAL").setActive(true)
                                .setSubjectType("USER").setSubjectId(1L).setSubjectLabel("批准人").setMarker("●"))));
        ServiceException error = assertThrows(ServiceException.class, () -> service.createCategory(category));
        assertEquals(DccCategoryApprovalMatrixAdminErrors.CUSTOM_MATRIX_PUBLIC_FORBIDDEN.getCode(), error.getCode());
        verifyNoInteractions(categoryService, matrixService);
    }
    @Test void pathNodeDoesNotCreateAnApplicableType() {
        request.setPathNodeOnly(true);
        request.setDirectoryBinding(null);
        assertEquals(100L, service.create(request));
        verifyNoInteractions(categoryService, matrixService);
    }
    @Test void missingSourceMatrixFailsRatherThanCreatingEmptyRules() {
        request.setApprovalSourceMode("PUBLIC_MATRIX"); request.setMatrixSourceCategoryId(300L);
        when(categoryMapper.selectById(300L)).thenReturn(DccFileCategoryDO.builder()
                .id(300L).active(true).approvalSourceMode("PUBLIC_MATRIX").build());
        when(matrixService.getApprovalMatrix(300L)).thenReturn(new DccCategoryApprovalMatrixRespVO());
        assertThrows(ServiceException.class, () -> service.create(request));
        verifyNoInteractions(categoryService);
    }
    @Test void selectedSourceCopiesRulesAndRecordsSourceVersion() {
        request.setApprovalSourceMode("PUBLIC_MATRIX"); request.setMatrixSourceCategoryId(300L);
        when(categoryMapper.selectById(300L)).thenReturn(DccFileCategoryDO.builder()
                .id(300L).active(true).approvalSourceMode("PUBLIC_MATRIX").build());
        var matrix = new DccCategoryApprovalMatrixRespVO(); matrix.setRouteId(400L); matrix.setRouteVersionNo(7); matrix.setEffectiveTime(LocalDateTime.now().minusMinutes(1));
        var rule = new DccCategoryApprovalMatrixRespVO.Rule(); rule.setStageType("SIGNOFF"); rule.setActive(true);
        matrix.setRules(List.of(rule)); when(matrixService.getApprovalMatrix(300L)).thenReturn(matrix);
        when(categoryService.createCategory(any())).thenReturn(200L);
        assertEquals(100L, service.create(request));
        verify(categoryService).createCategory(argThat(value -> value.getDirectoryBinding().getDirectoryId().equals(700L)));
        verify(matrixService).saveApprovalMatrix(eq(200L), argThat(value -> value.getRules().size() == 1
                && value.getRemark().contains("300") && value.getRemark().contains("7")));
    }
    @Test void matrixFailurePropagatesToTransactionalCaller() {
        request.setApprovalSourceMode("PUBLIC_MATRIX");
        request.setApprovalMatrix(new DccCategoryApprovalMatrixSaveReqVO()
                .setEffectiveTime(LocalDateTime.now().minusMinutes(1)).setRules(List.of()));
        when(categoryService.createCategory(any())).thenReturn(200L);
        var failure = new IllegalStateException("matrix insert failed");
        when(matrixService.saveApprovalMatrix(eq(200L), any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.create(request)));
    }
}
