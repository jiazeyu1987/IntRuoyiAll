package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccStorageMappingMutationGuard;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.annotation.Resource;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.*;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import({DccFileCategoryAdminServiceImpl.class, DccFileTypeTaxonomyAdminServiceImpl.class,
        DccStorageMappingMutationGuard.class, DccFileCategoryAtomicSaveTest.TenantFixture.class})
class DccFileCategoryAtomicSaveTest extends BaseDbUnitTest {
    @TestConfiguration
    static class TenantFixture {
        @Bean TenantLineInnerInterceptor atomicTenantInterceptor(MybatisPlusInterceptor interceptor) {
            var tenant = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
            interceptor.addInnerInterceptor(tenant);
            return tenant;
        }
    }
    @Resource DccFileCategoryAdminServiceImpl categories;
    @Resource DccFileTypeTaxonomyAdminServiceImpl taxonomies;
    @Resource DccFileCategoryMapper categoryMapper;
    @MockitoSpyBean DccCategoryDirectoryBindingMapper bindingMapper;
    @Resource DccFileDirectoryMapper directoryMapper;
    @Resource DccControlledFileMasterMapper masterMapper;
    @Resource DataSource source;
    @Resource org.mybatis.spring.SqlSessionTemplate sqlSession;
    @Resource TenantLineInnerInterceptor atomicTenantInterceptor;
    @MockitoBean DccIntAuthFileCategoryClient intAuth;
    Long type;
    Long directory;
    JdbcTemplate jdbc;

    @BeforeEach void setupAtomicFixture() throws Exception {
        try (var connection = source.getConnection()) { assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:")); }
        jdbc = new JdbcTemplate(source);
        type = taxonomy(taxonomy(taxonomy(null, "ROOT", "技术文档"), "PLAN", "设计和开发策划阶段"), "TYPE", "测试类型");
        var folder = DccFileDirectoryDO.builder().code("ATOMIC-DIR").name("正式目录")
                .parentId(0L).sort(1).active(true).build();
        directoryMapper.insert(folder); directory = folder.getId();
    }
    @AfterEach void clearMappings() { jdbc.update("DELETE FROM dcc_project_folder_storage_mapping"); }
    Long taxonomy(Long parent, String code, String name) {
        var request = new DccFileTypeTaxonomySaveReqVO();
        request.setParentId(parent); request.setCode(code); request.setName(name); request.setActive(true); request.setSort(1);
        return taxonomies.createTaxonomy(request);
    }
    DccFileCategorySaveReqVO request(String code) {
        var req = new DccFileCategorySaveReqVO(); req.setCode(code); req.setName("原类别"); req.setActive(true);
        req.setSort(1); req.setSource("LOCAL"); req.setDescription("原说明"); req.setRemark("原备注");
        req.setFileTypeTaxonomyId(type); req.setDistributionRequired(false); req.setTrainingRequired(false);
        return req;
    }
    void binding(DccFileCategorySaveReqVO req, Long dir, boolean active) {
        var bind = new DccCategoryDirectoryBindingSaveReqVO(); bind.setDirectoryId(dir); bind.setActive(active);
        req.setDirectoryBinding(bind);
    }
    Long original() {
        Long id = categories.createCategory(request("ORIGINAL"));
        var bind = new DccCategoryDirectoryBindingSaveReqVO(); bind.setDirectoryId(directory); bind.setActive(true);
        categories.bindDirectory(id, bind); return id;
    }
    DccFileCategorySaveReqVO changed(Long id) {
        var req = request("UPDATED"); req.setId(id); req.setName("新类别"); req.setDescription("新说明");
        req.setRemark("新备注"); req.setSort(9); req.setDistributionRequired(true); req.setTrainingRequired(true);
        return req;
    }
    List<Map<String,Object>> categoryRows(Long id) { return jdbc.queryForList("SELECT * FROM dcc_file_category WHERE id=?", id); }
    List<Map<String,Object>> bindingRows(Long id) { return jdbc.queryForList("SELECT * FROM dcc_category_directory_binding WHERE category_id=? ORDER BY id", id); }

    @Test void createAndUpdateCommitBothExplicitBindingFacts() {
        var req = request("COMMIT"); binding(req, directory, false);
        Long id = categories.createCategory(req);
        var saved = bindingMapper.selectList(DccCategoryDirectoryBindingDO::getCategoryId, id);
        assertEquals(1, saved.size()); assertEquals(directory, saved.get(0).getDirectoryId()); assertFalse(saved.get(0).getActive());
        var target = DccFileDirectoryDO.builder().code("ATOMIC-TARGET").name("新正式目录").parentId(0L).sort(2).active(true).build();
        directoryMapper.insert(target);
        var update = changed(id); binding(update, target.getId(), true); categories.updateCategory(update);
        assertEquals("UPDATED", categoryMapper.selectById(id).getCode());
        assertEquals("新备注", categoryMapper.selectById(id).getRemark());
        assertTrue(bindingMapper.selectList(DccCategoryDirectoryBindingDO::getCategoryId, id).get(0).getActive());
        assertEquals(target.getId(), bindingMapper.selectList(DccCategoryDirectoryBindingDO::getCategoryId, id).get(0).getDirectoryId());
        assertNotEquals(saved.get(0).getId(), bindingMapper.selectList(DccCategoryDirectoryBindingDO::getCategoryId, id).get(0).getId());
        assertEquals(1, bindingRows(id).size(), "旧绑定替换后只有一次真实绑定");
    }
    @Test void unknownDirectoryRollsBackNewCategoryAndExistingFullRow() {
        var req = request("NO-DIR"); binding(req, Long.MAX_VALUE, true);
        assertThrows(RuntimeException.class, () -> categories.createCategory(req));
        assertEquals(0, categoryMapper.selectList().size());
        Long id = original(); var before = categoryRows(id); var bindings = bindingRows(id);
        var update = changed(id); binding(update, Long.MAX_VALUE, true);
        assertThrows(RuntimeException.class, () -> categories.updateCategory(update));
        assertEquals(before, categoryRows(id)); assertEquals(bindings, bindingRows(id));
    }
    @Test void realStorageMappingGuardRollsBackUpdate() {
        Long id = original(); var before = categoryRows(id); var bindings = bindingRows(id);
        jdbc.update("INSERT INTO dcc_project_folder_storage_mapping(tenant_id,project_code_id,project_folder_id,category_id,base_directory_id,storage_directory_id) VALUES(1,20,30,?,?,?)", id, directory, 99L);
        var update = changed(id); binding(update, directory, true);
        assertThrows(RuntimeException.class, () -> categories.updateCategory(update));
        assertEquals(before, categoryRows(id)); assertEquals(bindings, bindingRows(id));
    }
    @Test void insertAfterActualWriteFailureRollsBackCreateAndUpdate() {
        Long id = original(); var before = categoryRows(id); var bindings = bindingRows(id);
        doAnswer(invocation -> { int written = sqlSession.getMapper(DccCategoryDirectoryBindingMapper.class)
                    .insert(invocation.getArgument(0, DccCategoryDirectoryBindingDO.class));
                    assertEquals(1, written); throw new IllegalStateException("late binding insert"); })
                .when(bindingMapper).insert(any(DccCategoryDirectoryBindingDO.class));
        var update = changed(id); binding(update, directory, true);
        assertThrows(IllegalStateException.class, () -> categories.updateCategory(update));
        assertEquals(before, categoryRows(id)); assertEquals(bindings, bindingRows(id));
        var create = request("INSERT-FAIL");
        create.setFileTypeTaxonomyId(taxonomy(taxonomy(taxonomy(null, "FAIL-ROOT", "其他技术文档"), "FAIL-PLAN", "设计和开发策划阶段"), "FAIL-TYPE", "另一测试类型"));
        binding(create, directory, true);
        assertThrows(IllegalStateException.class, () -> categories.createCategory(create));
        assertEquals(1, categoryMapper.selectList().size()); assertEquals(1, bindingRows(id).size());
    }
    @Test void deleteAfterActualRemovalFailureRollsBackWholeUpdate() {
        Long id = original(); var before = categoryRows(id); var bindings = bindingRows(id);
        doAnswer(invocation -> { int removed = sqlSession.getMapper(DccCategoryDirectoryBindingMapper.class)
                    .deleteAllByCategoryIdForce(invocation.getArgument(0, Long.class));
                    assertEquals(1, removed); throw new IllegalStateException("late binding delete"); })
                .when(bindingMapper).deleteAllByCategoryIdForce(id);
        var update = changed(id); binding(update, directory, true);
        assertThrows(IllegalStateException.class, () -> categories.updateCategory(update));
        assertEquals(before, categoryRows(id)); assertEquals(bindings, bindingRows(id));
    }
    @Test void deleteStageFailureRollsBackNewCategory() {
        doAnswer(invocation -> { int removed = sqlSession.getMapper(DccCategoryDirectoryBindingMapper.class)
                    .deleteAllByCategoryIdForce(invocation.getArgument(0, Long.class));
                    assertEquals(0, removed); throw new IllegalStateException("new category binding delete failed"); })
                .when(bindingMapper).deleteAllByCategoryIdForce(anyLong());
        var create = request("DELETE-FAIL-CREATE"); binding(create, directory, true);
        assertThrows(IllegalStateException.class, () -> categories.createCategory(create));
        assertTrue(categoryMapper.selectList().isEmpty());
        assertTrue(jdbc.queryForList("SELECT * FROM dcc_category_directory_binding").isEmpty());
    }
    @Test void zeroInsertRollsBackNewCategory() {
        doReturn(0).when(bindingMapper).insert(any(DccCategoryDirectoryBindingDO.class));
        var create = request("ZERO-INSERT-CREATE"); binding(create, directory, true);
        assertThrows(RuntimeException.class, () -> categories.createCategory(create));
        assertTrue(categoryMapper.selectList().isEmpty());
        assertTrue(jdbc.queryForList("SELECT * FROM dcc_category_directory_binding").isEmpty());
    }
    @Test void zeroDeleteOrInsertDoesNotCommitCategoryChanges() {
        Long id = original(); var before = categoryRows(id); var bindings = bindingRows(id);
        doReturn(0).when(bindingMapper).deleteAllByCategoryIdForce(id);
        var update = changed(id); binding(update, directory, true);
        assertThrows(RuntimeException.class, () -> categories.updateCategory(update));
        assertEquals(before, categoryRows(id)); assertEquals(bindings, bindingRows(id));
        reset(bindingMapper);
        doReturn(0).when(bindingMapper).insert(any(DccCategoryDirectoryBindingDO.class));
        assertThrows(RuntimeException.class, () -> categories.updateCategory(update));
        assertEquals(before, categoryRows(id)); assertEquals(bindings, bindingRows(id));
    }
    @Test void noBindingObjectDoesNotChangeOrGuessBinding() {
        Long id = categories.createCategory(request("NO-BIND")); assertTrue(bindingRows(id).isEmpty());
        var bind = new DccCategoryDirectoryBindingSaveReqVO(); bind.setDirectoryId(directory); bind.setActive(true); categories.bindDirectory(id, bind);
        var bindings = bindingRows(id); categories.updateCategory(changed(id)); assertEquals(bindings, bindingRows(id));
    }
    @Test void nestedValidRequiresActualDirectoryAndActive() {
        var req = request("VALID"); binding(req, null, true);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(req).stream().anyMatch(v -> v.getPropertyPath().toString().equals("directoryBinding.directoryId")));
            binding(req, directory, true);
            req.getDirectoryBinding().setActive(null);
            assertTrue(factory.getValidator().validate(req).stream().anyMatch(v -> v.getPropertyPath().toString().equals("directoryBinding.active")));
        }
    }
    @Test void uniqueTypeAndUsedTypeGuardsStayEffectiveWithBinding() {
        Long id = original(); var before = categoryRows(id); var bindings = bindingRows(id);
        var duplicate = request("DUPLICATE"); binding(duplicate, directory, true);
        assertThrows(RuntimeException.class, () -> categories.createCategory(duplicate));
        assertEquals(1, categoryMapper.selectList().size());
        masterMapper.insert(DccControlledFileMasterDO.builder().categoryId(id).fileName("正式文件").fileNumber("ATOMIC-NO").status("ACTIVE").build());
        var update = changed(id); update.setFileTypeTaxonomyId(Long.MAX_VALUE); binding(update, directory, true);
        assertThrows(RuntimeException.class, () -> categories.updateCategory(update));
        assertEquals(before, categoryRows(id)); assertEquals(bindings, bindingRows(id));
    }
}
