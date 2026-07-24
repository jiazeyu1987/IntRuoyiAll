package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_DELETE_CHILD_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_INACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(DccFileTypeTaxonomyAdminServiceImpl.class)
class DccFileTypeTaxonomyAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccFileTypeTaxonomyAdminServiceImpl taxonomyAdminService;
    @Resource
    private DccFileTypeTaxonomyMapper taxonomyMapper;

    @Test
    void createFiveLevelPath_shouldPersistComputedLevelsAndResolvePath() {
        Long level1 = createTaxonomy(null, "TECH", "技术文档", 1);
        Long level2 = createTaxonomy(level1, "PLAN", "设计和开发策划阶段", 2);
        Long level3 = createTaxonomy(level2, "PLAN-BOOK", "项目策划书", 3);
        Long level4 = createTaxonomy(level3, "PLAN-BOOK-DRAFT", "草案", 4);
        Long level5 = createTaxonomy(level4, "PLAN-BOOK-ARCHIVE", "归档件", 5);

        List<DccFileTypeTaxonomyDO> rows = taxonomyAdminService.getTaxonomyList();
        assertEquals(5, rows.size());
        assertEquals(0L, taxonomyMapper.selectById(level1).getParentId());
        assertEquals(1, taxonomyMapper.selectById(level1).getLevelNo());
        assertEquals(5, taxonomyMapper.selectById(level5).getLevelNo());

        DccFileTypeTaxonomyPath path = taxonomyAdminService.resolveActivePath(level5);
        assertEquals("技术文档", path.level1());
        assertEquals("设计和开发策划阶段", path.level2());
        assertEquals("项目策划书", path.level3());
        assertEquals("草案", path.level4());
        assertEquals("归档件", path.level5());
    }

    @Test
    void createSixthLevel_shouldFailFast() {
        Long parent = null;
        for (int level = 1; level <= 5; level++) {
            parent = createTaxonomy(parent, "L" + level, "第" + level + "级", level);
        }

        DccFileTypeTaxonomySaveReqVO reqVO = req(parent, "L6", "第六级", 6);

        assertServiceException(() -> taxonomyAdminService.createTaxonomy(reqVO),
                FILE_TYPE_TAXONOMY_LEVEL_INVALID);
    }

    @Test
    void createDuplicateSiblingName_shouldFailFast() {
        createTaxonomy(null, "TECH-DUP", "技术文档-重复", 1);
        DccFileTypeTaxonomySaveReqVO reqVO = req(null, "TECH-DUP-2", "技术文档-重复", 2);

        assertServiceException(() -> taxonomyAdminService.createTaxonomy(reqVO),
                FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING);
    }

    @Test
    void deleteParentWithChildren_shouldFailFast() {
        Long parent = createTaxonomy(null, "TECH-DELETE", "技术文档-删除保护", 1);
        createTaxonomy(parent, "PLAN-DELETE", "设计和开发策划阶段-删除保护", 2);

        assertServiceException(() -> taxonomyAdminService.deleteTaxonomy(parent),
                FILE_TYPE_TAXONOMY_DELETE_CHILD_EXISTS);
        assertTrue(taxonomyMapper.selectById(parent) != null);
    }

    @Test
    void resolveInactivePath_shouldFailFast() {
        Long taxonomyId = createTaxonomy(null, "TECH-INACTIVE", "技术文档-停用", 1);
        DccFileTypeTaxonomyDO taxonomy = taxonomyMapper.selectById(taxonomyId);
        taxonomy.setActive(Boolean.FALSE);
        taxonomyMapper.updateById(taxonomy);

        assertServiceException(() -> taxonomyAdminService.resolveActivePath(taxonomyId),
                FILE_TYPE_TAXONOMY_INACTIVE);
    }

    @Test
    void listActiveDescendantIds_shouldIncludeSelectedNodeAndActiveChildrenOnly() {
        Long level1 = createTaxonomy(null, "DESC-TECH", "技术文档-范围", 1);
        Long level2 = createTaxonomy(level1, "DESC-PLAN", "策划文件-范围", 2);
        Long level3 = createTaxonomy(level2, "DESC-BOOK", "项目策划书-范围", 3);
        Long level4Active = createTaxonomy(level3, "DESC-DRAFT", "草案-范围", 4);
        Long level4Inactive = createTaxonomy(level3, "DESC-OLD", "停用-范围", 5);
        DccFileTypeTaxonomyDO inactive = taxonomyMapper.selectById(level4Inactive);
        inactive.setActive(Boolean.FALSE);
        taxonomyMapper.updateById(inactive);

        List<Long> descendantIds = taxonomyAdminService.listActiveDescendantIds(level3);

        assertEquals(List.of(level3, level4Active), descendantIds);
    }

    @Test
    void listActiveDescendantPathsAndResolveByPath_shouldSupportLegacyControlledFiles() {
        Long level1 = createTaxonomy(null, "PATH-TECH", "技术文档-路径", 1);
        Long level2 = createTaxonomy(level1, "PATH-PLAN", "策划文件-路径", 2);
        Long level3 = createTaxonomy(level2, "PATH-BOOK", "项目策划书-路径", 3);
        Long level4 = createTaxonomy(level3, "PATH-DRAFT", "草案-路径", 4);

        List<DccFileTypeTaxonomyPath> paths = taxonomyAdminService.listActiveDescendantPaths(level3);

        assertEquals(List.of(level3, level4), paths.stream().map(DccFileTypeTaxonomyPath::id).toList());
        assertEquals(level3, taxonomyAdminService.resolveActiveIdByPath(
                "技术文档-路径", "策划文件-路径", "项目策划书-路径", null, null));
        assertEquals(level4, taxonomyAdminService.resolveActiveIdByPath(
                "技术文档-路径", "策划文件-路径", "项目策划书-路径", "草案-路径", null));
        assertNull(taxonomyAdminService.resolveActiveIdByPath(
                "技术文档-路径", "策划文件-路径", "不存在", null, null));
    }

    private Long createTaxonomy(Long parentId, String code, String name, int sort) {
        return taxonomyAdminService.createTaxonomy(req(parentId, code, name, sort));
    }

    private DccFileTypeTaxonomySaveReqVO req(Long parentId, String code, String name, int sort) {
        DccFileTypeTaxonomySaveReqVO reqVO = new DccFileTypeTaxonomySaveReqVO();
        reqVO.setParentId(parentId);
        reqVO.setCode(code);
        reqVO.setName(name);
        reqVO.setActive(Boolean.TRUE);
        reqVO.setSort(sort);
        reqVO.setRemark("seed");
        return reqVO;
    }
}
