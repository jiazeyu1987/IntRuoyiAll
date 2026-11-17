package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomyRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateItemRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateItemSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFileTemplateItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFileTemplateItemMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_CATEGORY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_DUPLICATE_ITEM;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_SELECTION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID;

@Service
@Validated
public class DccProjectFileTemplateServiceImpl implements DccProjectFileTemplateService {

    @Resource
    private DccProjectFileTemplateItemMapper templateItemMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileTypeTaxonomyAdminService taxonomyAdminService;

    @Override
    public DccProjectFileTemplateRespVO getProjectTemplate(Long projectCodeId) {
        requireProjectCode(projectCodeMapper.selectById(projectCodeId));
        List<DccFileTypeTaxonomyDO> taxonomyRows = taxonomyAdminService.getTaxonomyList();
        return buildResponse(projectCodeId, taxonomyRows,
                templateItemMapper.selectListByProjectCodeId(projectCodeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProjectFileTemplateRespVO replaceProjectTemplate(Long projectCodeId,
                                                               DccProjectFileTemplateSaveReqVO reqVO) {
        requireProjectCode(projectCodeMapper.selectByIdForUpdate(projectCodeId));
        List<DccProjectFileTemplateItemSaveReqVO> requestedItems = reqVO == null ? null : reqVO.getItems();
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw exception(PROJECT_FILE_TEMPLATE_NOT_CONFIGURED);
        }

        List<DccFileTypeTaxonomyDO> taxonomyRows = taxonomyAdminService.getTaxonomyList();
        Map<Long, DccFileTypeTaxonomyDO> taxonomyById = taxonomyMap(taxonomyRows);
        Set<String> uniqueItems = new HashSet<>();
        List<DccProjectFileTemplateItemDO> normalizedItems = new ArrayList<>();
        for (DccProjectFileTemplateItemSaveReqVO requestedItem : requestedItems) {
            normalizedItems.add(normalizeAndValidate(projectCodeId, requestedItem, taxonomyById,
                    uniqueItems));
        }

        templateItemMapper.deleteByProjectCodeId(projectCodeId);
        normalizedItems.forEach(templateItemMapper::insert);
        return buildResponse(projectCodeId, taxonomyRows, normalizedItems);
    }

    @Override
    public void validateUploadSelection(Long projectCodeId, Long fileTypeTaxonomyId, String fileName) {
        List<DccProjectFileTemplateItemDO> configuredItems =
                templateItemMapper.selectListByProjectCodeId(projectCodeId);
        if (configuredItems.isEmpty()) {
            throw exception(PROJECT_FILE_TEMPLATE_NOT_CONFIGURED);
        }
        String normalizedFileName = StrUtil.trim(fileName);
        boolean matched = configuredItems.stream().anyMatch(item ->
                Objects.equals(item.getFileTypeTaxonomyId(), fileTypeTaxonomyId)
                        && Objects.equals(item.getFileName(), normalizedFileName));
        if (!matched) {
            throw exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        }
        List<DccFileTypeTaxonomyDO> rows = taxonomyAdminService.getTaxonomyList();
        validateTemplateTaxonomy(fileTypeTaxonomyId, taxonomyMap(rows), activeCategoryCounts());
    }

    @Override
    public void validateUploadLocation(Long projectCodeId, Long fileTypeTaxonomyId) {
        List<DccProjectFileTemplateItemDO> configuredItems =
                templateItemMapper.selectListByProjectCodeId(projectCodeId);
        if (configuredItems.isEmpty()) {
            throw exception(PROJECT_FILE_TEMPLATE_NOT_CONFIGURED);
        }
        boolean taxonomyConfigured = configuredItems.stream()
                .anyMatch(item -> Objects.equals(item.getFileTypeTaxonomyId(), fileTypeTaxonomyId));
        if (!taxonomyConfigured) {
            throw exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        }
        List<DccFileTypeTaxonomyDO> rows = taxonomyAdminService.getTaxonomyList();
        validateTemplateTaxonomy(fileTypeTaxonomyId, taxonomyMap(rows), activeCategoryCounts());
    }

    private DccProjectFileTemplateItemDO normalizeAndValidate(
            Long projectCodeId,
            DccProjectFileTemplateItemSaveReqVO requestedItem,
            Map<Long, DccFileTypeTaxonomyDO> taxonomyById,
            Set<String> uniqueItems) {
        if (requestedItem == null || StrUtil.isBlank(requestedItem.getFileName()) || requestedItem.getSortOrder() == null
                || requestedItem.getSortOrder() < 0) {
            throw exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        }
        Long taxonomyId = requestedItem.getFileTypeTaxonomyId();
        validateOptionalTaxonomy(taxonomyId, taxonomyById);
        String fileName = StrUtil.trim(requestedItem.getFileName());
        String uniqueKey = taxonomyId + "\u0000" + fileName.toLowerCase(Locale.ROOT);
        if (!uniqueItems.add(uniqueKey)) {
            throw exception(PROJECT_FILE_TEMPLATE_DUPLICATE_ITEM);
        }
        return DccProjectFileTemplateItemDO.builder()
                .projectCodeId(projectCodeId)
                .fileTypeTaxonomyId(taxonomyId)
                .fileName(fileName)
                .sortOrder(requestedItem.getSortOrder())
                .build();
    }

    private DccProjectFileTemplateRespVO buildResponse(Long projectCodeId,
                                                       List<DccFileTypeTaxonomyDO> taxonomyRows,
                                                       List<DccProjectFileTemplateItemDO> items) {
        Map<Long, DccFileTypeTaxonomyDO> taxonomyById = taxonomyMap(taxonomyRows);
        DccProjectFileTemplateRespVO response = new DccProjectFileTemplateRespVO();
        response.setProjectCodeId(projectCodeId);
        response.setTaxonomyOptions(listTemplateTaxonomyOptions(
                taxonomyRows).stream()
                .map(item -> BeanUtils.toBean(item, DccFileTypeTaxonomyRespVO.class))
                .toList());
        response.setItems(items.stream()
                .map(item -> toResponseItem(item, taxonomyById))
                .toList());
        return response;
    }

    private List<DccFileTypeTaxonomyDO> listTemplateTaxonomyOptions(
            List<DccFileTypeTaxonomyDO> taxonomyRows) {
        return taxonomyRows.stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .toList();
    }

    private DccProjectFileTemplateItemRespVO toResponseItem(
            DccProjectFileTemplateItemDO item,
            Map<Long, DccFileTypeTaxonomyDO> taxonomyById) {
        DccProjectFileTemplateItemRespVO response = new DccProjectFileTemplateItemRespVO();
        response.setId(item.getId());
        response.setProjectCodeId(item.getProjectCodeId());
        response.setFileTypeTaxonomyId(item.getFileTypeTaxonomyId());
        response.setFileName(item.getFileName());
        response.setSortOrder(item.getSortOrder());
        try {
            if (item.getFileTypeTaxonomyId() != null) {
                List<DccFileTypeTaxonomyDO> lineage = validateOptionalTaxonomy(item.getFileTypeTaxonomyId(), taxonomyById);
                if (lineage.isEmpty() || lineage.stream().anyMatch(node -> !Boolean.TRUE.equals(node.getActive()))) {
                    throw exception(PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID);
                }
                if (lineage.size() >= 2) {
                    response.setStageTaxonomyId(lineage.get(1).getId());
                    response.setStageName(lineage.get(1).getName());
                }
                if (lineage.size() >= 3) {
                    response.setFileTypeNodeId(lineage.get(2).getId());
                    response.setFileTypeName(lineage.get(2).getName());
                }
                response.setTaxonomyPath(lineage.stream().map(DccFileTypeTaxonomyDO::getName)
                        .collect(java.util.stream.Collectors.joining(" / ")));
            }
            response.setValid(true);
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException invalidConfiguration) {
            // Preserve invalid optional associations for administrative repair.
            response.setValid(false);
            response.setValidationCode(invalidConfiguration.getCode());
            response.setValidationMessage(invalidConfiguration.getMessage());
        }
        return response;
    }

    private List<DccFileTypeTaxonomyDO> validateOptionalTaxonomy(Long taxonomyId,
            Map<Long, DccFileTypeTaxonomyDO> taxonomyById) {
        if (taxonomyId == null) return List.of();
        List<DccFileTypeTaxonomyDO> lineage = resolveLineage(taxonomyId, taxonomyById);
        if (lineage.isEmpty() || lineage.get(0).getParentId() == null
                || lineage.get(0).getParentId() != 0L
                || lineage.stream().anyMatch(node -> !Boolean.TRUE.equals(node.getActive()))) {
            throw exception(PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID);
        }
        return lineage;
    }

    private DccFileTypeTaxonomyPath validateTemplateTaxonomy(Long taxonomyId,
            Map<Long, DccFileTypeTaxonomyDO> taxonomyById, Map<Long, Long> activeCategoryCounts) {
        DccFileTypeTaxonomyDO taxonomy = taxonomyById.get(taxonomyId);
        if (taxonomy == null || !Boolean.TRUE.equals(taxonomy.getActive())) {
            throw exception(PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID);
        }
        DccFileTypeTaxonomyPath path = taxonomyAdminService.resolveActivePath(taxonomyId);
        if (path == null || StrUtil.isBlank(path.level3()) || hasActiveChildTaxonomy(taxonomyId, taxonomyById)) {
            throw exception(PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID);
        }
        if (!Objects.equals(activeCategoryCounts.get(taxonomyId), 1L)) {
            throw exception(PROJECT_FILE_TEMPLATE_CATEGORY_INVALID);
        }
        return path;
    }

    private List<DccFileTypeTaxonomyDO> resolveLineage(
            Long taxonomyId, Map<Long, DccFileTypeTaxonomyDO> taxonomyById) {
        List<DccFileTypeTaxonomyDO> lineage = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        DccFileTypeTaxonomyDO current = taxonomyById.get(taxonomyId);
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw exception(PROJECT_FILE_TEMPLATE_TAXONOMY_INVALID);
            }
            lineage.add(current);
            if (current.getParentId() == null || current.getParentId() == 0L) {
                break;
            }
            current = taxonomyById.get(current.getParentId());
        }
        Collections.reverse(lineage);
        return lineage;
    }

    private Map<Long, DccFileTypeTaxonomyDO> taxonomyMap(List<DccFileTypeTaxonomyDO> rows) {
        Map<Long, DccFileTypeTaxonomyDO> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(row.getId(), row));
        return result;
    }

    private Map<Long, Long> activeCategoryCounts() {
        Map<Long, Long> result = new LinkedHashMap<>();
        categoryMapper.selectList().stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .map(DccFileCategoryDO::getFileTypeTaxonomyId)
                .filter(Objects::nonNull)
                .forEach(taxonomyId -> result.merge(taxonomyId, 1L, Long::sum));
        return result;
    }

    private boolean hasActiveChildTaxonomy(Long taxonomyId, Map<Long, DccFileTypeTaxonomyDO> taxonomyById) {
        return taxonomyById.values().stream()
                .anyMatch(item -> Objects.equals(item.getParentId(), taxonomyId)
                        && Boolean.TRUE.equals(item.getActive()));
    }

    private DccProjectCodeDO requireProjectCode(DccProjectCodeDO projectCode) {
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        return projectCode;
    }
}
