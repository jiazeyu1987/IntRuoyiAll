package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import({DccFileTypeConfigurationService.class, DccFileTypeTaxonomyAdminServiceImpl.class,
        DccFileCategoryAdminServiceImpl.class})
class DccFileTypeConfigurationTransactionTest extends BaseDbUnitTest {
    @Resource private DccFileTypeConfigurationService service;
    @MockitoSpyBean private DccFileTypeTaxonomyAdminServiceImpl taxonomyService;
    @MockitoSpyBean private DccFileCategoryMapper categoryMapper;
    @Resource private DccFileTypeTaxonomyMapper taxonomyMapper;
    @Resource private DccCategoryApprovalRouteMapper routeMapper;
    @Resource private DccCategoryApprovalRouteNodeMapper nodeMapper;
    @Resource private DataSource dataSource;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper directoryMapper;
    @MockitoSpyBean private DccCategoryDirectoryBindingMapper bindingMapper;
    @MockitoBean private DccCategoryApprovalMatrixAdminService matrixService;
    @MockitoBean private DccIntAuthFileCategoryClient intAuthFileCategoryClient;
    @MockitoBean private cn.iocoder.yudao.module.dcc.service.file.DccStorageMappingMutationGuard storageMappingGuard;
    private Long parent;
    private Long directoryId;

    @BeforeEach void parents() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        var directory = cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO.builder()
                .tenantId(1L).name("W03 task storage").code("W03-STORAGE").active(true).sort(0).build();
        directoryMapper.insert(directory); directoryId = directory.getId();
        // H2 excludes the production tenant interceptor: fill its tenant column explicitly in this fixture.
        doAnswer(call -> {
            Long id = (Long) call.callRealMethod();
            new JdbcTemplate(dataSource).update("UPDATE dcc_file_type_taxonomy SET tenant_id=1 WHERE id=?", id);
            return id;
        }).when(taxonomyService).createTaxonomy(any());
        Long root = taxonomyService.createTaxonomy(request(null, "W03-TX-ROOT"));
        var stage = request(root, "W03-TX-STAGE"); stage.setName("策划文件");
        parent = taxonomyService.createTaxonomy(stage);
    }
    @AfterEach void clearTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }

    @Test void applicableTypeRequiresValidStorageBeforeAnyTypeWrite() {
        var req = publicRequest(); req.setDirectoryBinding(null);
        assertThrows(RuntimeException.class, () -> service.create(req));
        assertNoOrphans();
        req.setDirectoryBinding(binding()); req.getDirectoryBinding().setDirectoryId(-1L);
        assertThrows(RuntimeException.class, () -> service.create(req));
        assertNoOrphans();
    }

    @Test void publicTypeSavesItsExplicitDirectoryBinding() {
        Long type = service.create(publicRequest());
        var category = categoryMapper.selectOne(DccFileCategoryDO::getFileTypeTaxonomyId, type);
        assertNotNull(category);
        assertEquals("PUBLIC_MATRIX", category.getApprovalSourceMode());
        var saved = bindingMapper.selectActiveByCategoryId(category.getId());
        assertNotNull(saved); assertEquals(directoryId, saved.getDirectoryId());
        verify(matrixService).saveApprovalMatrix(eq(category.getId()), any());
    }

    @Test void customOtherTypeSavesDirectoryWithoutCreatingPublicMatrix() {
        var req = request(parent,"W03-CUSTOM"); req.setName("其他");
        req.setApprovalSourceMode("APPLICATION_CUSTOM"); req.setDirectoryBinding(binding());
        Long type = service.create(req);
        var category = categoryMapper.selectOne(DccFileCategoryDO::getFileTypeTaxonomyId, type);
        assertEquals("APPLICATION_CUSTOM",category.getApprovalSourceMode());
        assertEquals(directoryId,bindingMapper.selectActiveByCategoryId(category.getId()).getDirectoryId());
        verifyNoInteractions(matrixService);
    }

    @Test void otherPublicMatrixIsRejectedAndNewTypeTransactionLeavesNoOrphans() {
        var req = publicRequest(); req.setName("其他"); req.setApprovalMatrix(validMatrix());
        var error = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.create(req));
        assertEquals(DccCategoryApprovalMatrixAdminErrors.CUSTOM_MATRIX_PUBLIC_FORBIDDEN.getCode(), error.getCode());
        assertNoOrphans();
        verifyNoInteractions(matrixService);
    }

    @Test void existingCustomOtherCannotBeConvertedToPublicAndKeepsSavedConfiguration() {
        var req = request(parent, "W03-CUSTOM-KEEP"); req.setName("其他");
        req.setApprovalSourceMode("APPLICATION_CUSTOM"); req.setDirectoryBinding(binding());
        Long typeId = service.create(req);
        var before = categoryMapper.selectOne(DccFileCategoryDO::getFileTypeTaxonomyId, typeId);
        var update = new DccFileCategorySaveReqVO(); update.setId(before.getId());
        update.setFileTypeTaxonomyId(typeId); update.setCode(before.getCode()); update.setName(before.getName());
        update.setActive(true); update.setSort(1); update.setDirectoryBinding(binding());
        update.setApprovalSourceMode("PUBLIC_MATRIX"); update.setApprovalMatrix(validMatrix());
        var error = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.updateCategory(update));
        assertEquals(DccCategoryApprovalMatrixAdminErrors.CUSTOM_MATRIX_PUBLIC_FORBIDDEN.getCode(), error.getCode());
        var after = categoryMapper.selectById(before.getId());
        assertEquals("APPLICATION_CUSTOM", after.getApprovalSourceMode());
        assertEquals(before.getFileTypeTaxonomyId(), after.getFileTypeTaxonomyId());
        assertEquals(before.getName(), after.getName());
        assertEquals(3, taxonomyMapper.selectList().size());
        assertEquals(1, categoryMapper.selectList().size());
        assertEquals(1, bindingMapper.selectList().size());
        assertEquals(directoryId, bindingMapper.selectActiveByCategoryId(before.getId()).getDirectoryId());
        assertTrue(routeMapper.selectList().isEmpty()); assertTrue(nodeMapper.selectList().isEmpty());
        verifyNoInteractions(matrixService);
    }

    @Test void pathOnlyNodeRequiresNoStorageAndCreatesNoCategory() {
        var req = request(parent,"W03-PATH"); req.setPathNodeOnly(true);
        assertNotNull(service.create(req));
        assertTrue(categoryMapper.selectList().isEmpty());
        assertTrue(bindingMapper.selectList().isEmpty());
        verifyNoInteractions(matrixService);
    }

    @Test void directoryBindingWriteFailureRollsBackTypeAndCategory() {
        doReturn(0).when(bindingMapper).insert(any(DccCategoryDirectoryBindingDO.class));
        assertThrows(RuntimeException.class, () -> service.create(publicRequest()));
        assertNoOrphans();
    }

    @Test void missingMatrixRollsBackTheNewTaxonomy() {
        assertThrows(RuntimeException.class, () -> service.create(request(parent, "W03-TX-LEAF")));
        assertNoOrphans();
    }

    @Test void categoryWriteFailureRollsBackTheNewTaxonomy() {
        doReturn(0).when(categoryMapper).insert(any(DccFileCategoryDO.class));
        assertThrows(RuntimeException.class, () -> service.create(publicRequest()));
        assertNoOrphans();
    }

    @Test void partialMatrixFailureRollsBackTaxonomyCategoryRouteAndNodes() {
        when(matrixService.saveApprovalMatrix(anyLong(), any())).thenAnswer(call -> {
            Long categoryId = call.getArgument(0);
            var route = DccCategoryApprovalRouteDO.builder().categoryId(categoryId).actionType("LEGACY")
                    .versionNo(1).active(true).effectiveTime(LocalDateTime.now()).build();
            routeMapper.insert(route);
            nodeMapper.insert(DccCategoryApprovalRouteNodeDO.builder().routeId(route.getId())
                    .stageNo(1).stageName("文控审核").candidateSourceType("USER").candidateSourceId(1L)
                    .approveMethod("ANY").sort(0).required(true).build());
            throw new IllegalStateException("injected matrix node failure");
        });
        assertThrows(IllegalStateException.class, () -> service.create(publicRequest()));
        verify(matrixService).saveApprovalMatrix(anyLong(), any());
        assertNoOrphans();
    }

    private void assertNoOrphans() {
        assertEquals(2, taxonomyMapper.selectList().size());
        assertTrue(categoryMapper.selectList().isEmpty());
        assertTrue(routeMapper.selectList().isEmpty());
        assertTrue(nodeMapper.selectList().isEmpty());
        assertTrue(bindingMapper.selectList().isEmpty());
    }
    private DccFileTypeTaxonomySaveReqVO publicRequest() {
        var req = request(parent, "W03-TX-LEAF"); req.setApprovalSourceMode("PUBLIC_MATRIX");
        req.setDirectoryBinding(binding());
        req.setApprovalMatrix(new DccCategoryApprovalMatrixSaveReqVO()
                .setEffectiveTime(LocalDateTime.now().minusMinutes(1)).setRules(List.of()));
        return req;
    }
    private DccCategoryDirectoryBindingSaveReqVO binding() {
        var value = new DccCategoryDirectoryBindingSaveReqVO(); value.setDirectoryId(directoryId); value.setActive(true); return value;
    }
    private DccCategoryApprovalMatrixSaveReqVO validMatrix() {
        return new DccCategoryApprovalMatrixSaveReqVO().setEffectiveTime(LocalDateTime.now().minusMinutes(1))
                .setRules(List.of(
                        new DccCategoryApprovalMatrixSaveReqVO.Rule().setStageType("SIGNOFF").setActive(true)
                                .setSubjectType("DEPT").setSubjectId(910380L).setSubjectLabel("正式部门").setMarker("●"),
                        new DccCategoryApprovalMatrixSaveReqVO.Rule().setStageType("APPROVAL").setActive(true)
                                .setSubjectType("USER").setSubjectId(1L).setSubjectLabel("批准人").setMarker("●")));
    }
    private DccFileTypeTaxonomySaveReqVO request(Long parentId, String code) {
        var req = new DccFileTypeTaxonomySaveReqVO(); req.setParentId(parentId); req.setCode(code);
        req.setName(code); req.setActive(true); req.setSort(1); return req;
    }
}
