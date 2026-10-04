package cn.iocoder.yudao.module.dcc.service.category;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFileTemplateItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFileTemplateItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_DELETE_CHILD_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_DELETE_REFERENCED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_INACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_LEVEL_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_PARENT_CHANGE_FORBIDDEN;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_TYPE_TAXONOMY_PARENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyErrors.WRITE_INCOMPLETE;

@Service
@Validated
public class DccFileTypeTaxonomyAdminServiceImpl implements DccFileTypeTaxonomyAdminService {

    private static final Long ROOT_PARENT_ID = 0L;
    private static final int MAX_LEVEL_NO = 5;

    @Resource
    private DccFileTypeTaxonomyMapper taxonomyMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccProjectFileTemplateItemMapper projectFileTemplateItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTaxonomy(DccFileTypeTaxonomySaveReqVO reqVO) {
        DccFileTypeTaxonomyDO taxonomy = BeanUtils.toBean(reqVO, DccFileTypeTaxonomyDO.class);
        normalizeForCreate(taxonomy);
        validateDuplicateSibling(taxonomy);
        if (taxonomyMapper.insert(taxonomy) != 1 || taxonomy.getId() == null) {
            throw exception(WRITE_INCOMPLETE, "新增分类");
        }
        return taxonomy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaxonomy(DccFileTypeTaxonomySaveReqVO reqVO) {
        DccFileTypeTaxonomyDO existing = validateExists(reqVO.getId());
        Long normalizedParentId = normalizeParentId(reqVO.getParentId());
        if (!Objects.equals(existing.getParentId(), normalizedParentId)) {
            throw exception(FILE_TYPE_TAXONOMY_PARENT_CHANGE_FORBIDDEN);
        }
        // 停用保留正式类型 ID、模板映射与历史；新申请解析会明确拒绝失效路径。
        DccFileTypeTaxonomyDO taxonomy = BeanUtils.toBean(reqVO, DccFileTypeTaxonomyDO.class);
        taxonomy.setParentId(existing.getParentId());
        taxonomy.setLevelNo(existing.getLevelNo());
        validateDuplicateSibling(taxonomy);
        if (taxonomyMapper.updateById(taxonomy) != 1) {
            throw exception(WRITE_INCOMPLETE, "修改分类");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaxonomy(Long id) {
        validateExists(id);
        if (taxonomyMapper.selectCount(DccFileTypeTaxonomyDO::getParentId, id) > 0) {
            throw exception(FILE_TYPE_TAXONOMY_DELETE_CHILD_EXISTS);
        }
        if (categoryMapper.selectCount(DccFileCategoryDO::getFileTypeTaxonomyId, id) > 0
                || controlledFileMapper.selectCount(DccControlledFileDO::getFileTypeTaxonomyId, id) > 0
                || projectFileTemplateItemMapper.selectCount(
                DccProjectFileTemplateItemDO::getFileTypeTaxonomyId, id) > 0) {
            throw exception(FILE_TYPE_TAXONOMY_DELETE_REFERENCED);
        }
        if (taxonomyMapper.deleteById(id) != 1) {
            throw exception(WRITE_INCOMPLETE, "删除分类");
        }
    }

    @Override
    public List<DccFileTypeTaxonomyDO> getTaxonomyList() {
        return sortTaxonomies(taxonomyMapper.selectList());
    }

    @Override
    public DccFileTypeTaxonomyPath resolveActivePath(Long id) {
        DccFileTypeTaxonomyDO current = validateExists(id);
        Map<Long, DccFileTypeTaxonomyDO> byId = buildTaxonomyMap();
        return buildActivePath(current, byId);
    }

    /** 文件夹身份不参与类型或矩阵推断；只接受唯一启用分类映射。 */
    @Override
    public Long resolveActiveCategoryId(Long fileTypeTaxonomyId) {
        DccFileTypeTaxonomyPath path = resolveActivePath(fileTypeTaxonomyId);
        if (StrUtil.isBlank(path.level3())) throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        List<DccFileCategoryDO> categories = categoryMapper.selectList().stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive())
                        && Objects.equals(category.getFileTypeTaxonomyId(), fileTypeTaxonomyId))
                .toList();
        if (categories.size() != 1) {
            throw exception(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_CATEGORY_INVALID);
        }
        return categories.get(0).getId();
    }

    @Override
    public List<Long> listActiveDescendantIds(Long id) {
        resolveActivePath(id);
        List<DccFileTypeTaxonomyDO> activeRows = sortTaxonomies(taxonomyMapper.selectList()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .toList();
        Map<Long, List<DccFileTypeTaxonomyDO>> childrenByParentId = new LinkedHashMap<>();
        for (DccFileTypeTaxonomyDO row : activeRows) {
            childrenByParentId.computeIfAbsent(row.getParentId(), key -> new ArrayList<>()).add(row);
        }
        List<Long> descendantIds = new ArrayList<>();
        collectActiveDescendantIds(id, childrenByParentId, new HashSet<>(), descendantIds);
        return descendantIds;
    }

    @Override
    public List<DccFileTypeTaxonomyPath> listActiveDescendantPaths(Long id) {
        List<Long> descendantIds = listActiveDescendantIds(id);
        Map<Long, DccFileTypeTaxonomyDO> byId = buildTaxonomyMap();
        return descendantIds.stream()
                .map(descendantId -> {
                    DccFileTypeTaxonomyDO row = byId.get(descendantId);
                    if (row == null) {
                        throw exception(FILE_TYPE_TAXONOMY_NOT_EXISTS);
                    }
                    return buildActivePath(row, byId);
                })
                .toList();
    }

    @Override
    public Long resolveActiveIdByPath(String level1, String level2, String level3, String level4, String level5) {
        DccFileTypeTaxonomyPath expected = new DccFileTypeTaxonomyPath(null,
                normalizeName(level1), normalizeName(level2), normalizeName(level3),
                normalizeName(level4), normalizeName(level5));
        if (StrUtil.isBlank(expected.level1())
                || StrUtil.isBlank(expected.level2())
                || StrUtil.isBlank(expected.level3())) {
            return null;
        }
        Map<Long, DccFileTypeTaxonomyDO> byId = buildTaxonomyMap();
        List<Long> matches = sortTaxonomies(List.copyOf(byId.values())).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .map(item -> buildActivePath(item, byId))
                .filter(path -> Objects.equals(normalizeName(path.level1()), expected.level1())
                        && Objects.equals(normalizeName(path.level2()), expected.level2())
                        && Objects.equals(normalizeName(path.level3()), expected.level3())
                        && Objects.equals(normalizeName(path.level4()), expected.level4())
                        && Objects.equals(normalizeName(path.level5()), expected.level5()))
                .map(DccFileTypeTaxonomyPath::id)
                .toList();
        if (matches.size() > 1) throw exception(FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private DccFileTypeTaxonomyPath buildActivePath(DccFileTypeTaxonomyDO current,
                                                    Map<Long, DccFileTypeTaxonomyDO> byId) {
        Long originalId = current == null ? null : current.getId();
        List<String> names = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        while (current != null && !ROOT_PARENT_ID.equals(current.getParentId())) {
            if (!visited.add(current.getId())) {
                throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
            }
            if (!Boolean.TRUE.equals(current.getActive())) {
                throw exception(FILE_TYPE_TAXONOMY_INACTIVE);
            }
            names.add(0, current.getName());
            current = byId.get(current.getParentId());
            if (current == null) {
                throw exception(FILE_TYPE_TAXONOMY_PARENT_NOT_EXISTS);
            }
        }
        if (current != null) {
            if (!visited.add(current.getId())) {
                throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
            }
            if (!Boolean.TRUE.equals(current.getActive())) {
                throw exception(FILE_TYPE_TAXONOMY_INACTIVE);
            }
            names.add(0, current.getName());
        }
        if (names.isEmpty() || names.size() > MAX_LEVEL_NO) {
            throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        }
        return new DccFileTypeTaxonomyPath(originalId,
                level(names, 0), level(names, 1), level(names, 2),
                level(names, 3), level(names, 4));
    }

    private void collectActiveDescendantIds(Long id,
                                            Map<Long, List<DccFileTypeTaxonomyDO>> childrenByParentId,
                                            Set<Long> visited,
                                            List<Long> descendantIds) {
        if (id == null || !visited.add(id)) {
            throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        }
        descendantIds.add(id);
        for (DccFileTypeTaxonomyDO child : childrenByParentId.getOrDefault(id, List.of())) {
            collectActiveDescendantIds(child.getId(), childrenByParentId, visited, descendantIds);
        }
    }

    private void normalizeForCreate(DccFileTypeTaxonomyDO taxonomy) {
        Long parentId = normalizeParentId(taxonomy.getParentId());
        taxonomy.setParentId(parentId);
        if (ROOT_PARENT_ID.equals(parentId)) {
            taxonomy.setLevelNo(1);
            return;
        }
        DccFileTypeTaxonomyDO parent = taxonomyMapper.selectById(parentId);
        if (parent == null) {
            throw exception(FILE_TYPE_TAXONOMY_PARENT_NOT_EXISTS);
        }
        int levelNo = parent.getLevelNo() + 1;
        if (levelNo > MAX_LEVEL_NO) {
            throw exception(FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        }
        taxonomy.setLevelNo(levelNo);
    }

    private DccFileTypeTaxonomyDO validateExists(Long id) {
        DccFileTypeTaxonomyDO taxonomy = id == null ? null : taxonomyMapper.selectById(id);
        if (taxonomy == null) {
            throw exception(FILE_TYPE_TAXONOMY_NOT_EXISTS);
        }
        return taxonomy;
    }

    private Map<Long, DccFileTypeTaxonomyDO> buildTaxonomyMap() {
        Map<Long, DccFileTypeTaxonomyDO> byId = new LinkedHashMap<>();
        taxonomyMapper.selectList().forEach(item -> byId.put(item.getId(), item));
        return byId;
    }

    private void validateDuplicateSibling(DccFileTypeTaxonomyDO taxonomy) {
        Long parentId = normalizeParentId(taxonomy.getParentId());
        String code = StrUtil.trim(taxonomy.getCode());
        String name = StrUtil.trim(taxonomy.getName());
        taxonomy.setCode(code);
        taxonomy.setName(name);
        taxonomy.setParentId(parentId);
        for (DccFileTypeTaxonomyDO existing : taxonomyMapper.selectList()) {
            if (Objects.equals(existing.getId(), taxonomy.getId())) {
                continue;
            }
            if (Objects.equals(existing.getParentId(), parentId) && Objects.equals(existing.getName(), name)) {
                throw exception(FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING);
            }
            if (Objects.equals(existing.getCode(), code)) {
                throw exception(FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING);
            }
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private String level(List<String> names, int index) {
        return names.size() > index ? names.get(index) : null;
    }

    private String normalizeName(String name) {
        return StrUtil.trimToNull(name);
    }

    private List<DccFileTypeTaxonomyDO> sortTaxonomies(List<DccFileTypeTaxonomyDO> rows) {
        return rows.stream()
                .sorted(Comparator
                        .comparing(DccFileTypeTaxonomyDO::getLevelNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccFileTypeTaxonomyDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccFileTypeTaxonomyDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }
}
