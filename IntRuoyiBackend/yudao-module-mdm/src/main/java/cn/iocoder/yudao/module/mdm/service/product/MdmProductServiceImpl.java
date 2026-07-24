package cn.iocoder.yudao.module.mdm.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportExcelVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportPreviewRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportRowRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductPageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductReferenceRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductImportBatchDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductImportRowDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductImportBatchMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductImportRowMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductReferenceMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmProductImportActionConstants;
import cn.iocoder.yudao.module.mdm.enums.MdmProductImportStatusConstants;
import cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class MdmProductServiceImpl implements MdmProductService {

    @Resource
    private MdmProductMapper productMapper;
    @Resource
    private MdmProductImportBatchMapper importBatchMapper;
    @Resource
    private MdmProductImportRowMapper importRowMapper;
    @Resource
    private MdmProductReferenceMapper referenceMapper;

    @Override
    public Long createProduct(MdmProductSaveReqVO reqVO) {
        NormalizedProduct normalized = validateAndNormalize(reqVO, null);
        MdmProductDO product = MdmProductDO.builder()
                .productCode(normalized.productCode())
                .dccProductCode(normalized.dccProductCode())
                .nameCn(normalized.nameCn())
                .nameEn(normalized.nameEn())
                .modelSpecification(normalized.modelSpecification())
                .category(normalized.category())
                .status(normalized.status())
                .build();
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public void updateProduct(MdmProductSaveReqVO reqVO) {
        MdmProductDO existing = requireProduct(reqVO.getId());
        NormalizedProduct normalized = validateAndNormalize(reqVO, existing);
        MdmProductDO update = MdmProductDO.builder()
                .id(existing.getId())
                .productCode(normalized.productCode())
                .dccProductCode(normalized.dccProductCode())
                .nameCn(normalized.nameCn())
                .nameEn(normalized.nameEn())
                .modelSpecification(normalized.modelSpecification())
                .category(normalized.category())
                .status(normalized.status())
                .build();
        productMapper.updateById(update);
    }

    @Override
    public void updateProductStatus(Long id, String status) {
        MdmProductDO existing = requireProduct(id);
        String normalizedStatus = normalizeStatus(status);
        productMapper.updateById(MdmProductDO.builder()
                .id(existing.getId())
                .status(normalizedStatus)
                .build());
    }

    @Override
    public MdmProductDO getProduct(Long id) {
        return id == null ? null : productMapper.selectById(id);
    }

    @Override
    public MdmProductDO getEnabledDccProduct(Long id) {
        MdmProductDO product = requireProduct(id);
        validateEnabledDccProduct(product);
        return product;
    }

    @Override
    public MdmProductDO getEnabledDccProductByDccProductCode(String dccProductCode) {
        String normalizedDccProductCode = MdmProductCodePolicy.normalize(dccProductCode);
        if (!MdmProductCodePolicy.isValidDccProductCode(normalizedDccProductCode)) {
            throw new IllegalStateException("MDM_PRODUCT_DCC_CODE_INVALID: 产品主数据缺少合法 14 位 DCC 产品编号");
        }
        MdmProductDO product = productMapper.selectByDccProductCode(normalizedDccProductCode);
        if (product == null) {
            throw new IllegalStateException("MDM_PRODUCT_NOT_FOUND: dccProductCode=" + normalizedDccProductCode);
        }
        validateEnabledDccProduct(product);
        return product;
    }

    private void validateEnabledDccProduct(MdmProductDO product) {
        if (!MdmProductStatusConstants.ENABLE.equals(product.getStatus())) {
            throw new IllegalStateException("MDM_PRODUCT_DISABLED: 产品主数据已停用");
        }
        if (!MdmProductCodePolicy.isValidDccProductCode(product.getDccProductCode())) {
            throw new IllegalStateException("MDM_PRODUCT_DCC_CODE_INVALID: 产品主数据缺少合法 14 位 DCC 产品编号");
        }
    }

    @Override
    public PageResult<MdmProductDO> getProductPage(MdmProductPageReqVO reqVO) {
        return productMapper.selectPage(reqVO);
    }

    @Override
    public List<MdmProductDO> listSimpleProducts(String status, Boolean requireDccProductCode, String keyword) {
        String normalizedStatus = StrUtil.trimToNull(status);
        if (normalizedStatus != null && !MdmProductStatusConstants.isValid(normalizedStatus)) {
            throw new IllegalArgumentException("MDM_PRODUCT_STATUS_INVALID: status=" + status);
        }
        return productMapper.selectSimpleList(normalizedStatus, requireDccProductCode, keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MdmProductImportPreviewRespVO previewImport(List<MdmProductImportExcelVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            throw new IllegalStateException("MDM_PRODUCT_IMPORT_EMPTY: 产品主数据导入文件不能为空");
        }

        List<MdmProductDO> existingProducts = productMapper.selectList();
        Map<String, MdmProductDO> existingByCode = existingProducts.stream()
                .collect(Collectors.toMap(MdmProductDO::getProductCode, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, MdmProductDO> existingByDccCode = existingProducts.stream()
                .filter(product -> StrUtil.isNotBlank(product.getDccProductCode()))
                .collect(Collectors.toMap(MdmProductDO::getDccProductCode, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));

        Set<String> seenProductCodes = new LinkedHashSet<>();
        Set<String> seenDccProductCodes = new LinkedHashSet<>();
        List<MdmProductImportRowDO> importRows = new ArrayList<>();
        Set<String> importedProductCodes = new LinkedHashSet<>();

        for (int index = 0; index < rows.size(); index++) {
            int rowNo = index + 2;
            MdmProductImportExcelVO row = rows.get(index);
            ImportRowDraft draft = normalizeImportRow(row, rowNo);
            String failure = validateImportRow(draft, seenProductCodes, seenDccProductCodes, existingByDccCode);
            MdmProductDO existing = draft.productCode() == null ? null : existingByCode.get(draft.productCode());
            String action = resolveImportAction(draft, existing, failure);
            if (failure == null && draft.productCode() != null) {
                importedProductCodes.add(draft.productCode());
            }
            importRows.add(toImportRow(rowNo, draft, existing, action, failure));
        }

        for (MdmProductDO existing : existingProducts) {
            if (!importedProductCodes.contains(existing.getProductCode())
                    && MdmProductStatusConstants.ENABLE.equals(existing.getStatus())) {
                importRows.add(MdmProductImportRowDO.builder()
                        .rowNo(rows.size() + importRows.size() + 2)
                        .productCode(existing.getProductCode())
                        .dccProductCode(existing.getDccProductCode())
                        .nameCn(existing.getNameCn())
                        .nameEn(existing.getNameEn())
                        .modelSpecification(existing.getModelSpecification())
                        .category(existing.getCategory())
                        .currentStatus(existing.getStatus())
                        .importAction(MdmProductImportActionConstants.DISABLE)
                        .build());
            }
        }

        ImportSummary summary = summarize(importRows);
        MdmProductImportBatchDO batch = MdmProductImportBatchDO.builder()
                .status(summary.failureCount() > 0
                        ? MdmProductImportStatusConstants.FAILED
                        : MdmProductImportStatusConstants.PREVIEWED)
                .totalCount(summary.totalCount())
                .createCount(summary.createCount())
                .updateCount(summary.updateCount())
                .disableCount(summary.disableCount())
                .unchangedCount(summary.unchangedCount())
                .failureCount(summary.failureCount())
                .build();
        importBatchMapper.insert(batch);
        importRows.forEach(row -> row.setBatchId(batch.getId()));
        importRowMapper.insertBatch(importRows);
        return toPreviewResp(batch, importRows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MdmProductImportPreviewRespVO confirmImport(Long batchId) {
        MdmProductImportBatchDO batch = importBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalStateException("MDM_PRODUCT_IMPORT_BATCH_NOT_FOUND: batchId=" + batchId);
        }
        if (!MdmProductImportStatusConstants.PREVIEWED.equals(batch.getStatus())) {
            throw new IllegalStateException("MDM_PRODUCT_IMPORT_BATCH_NOT_CONFIRMABLE: status=" + batch.getStatus());
        }
        List<MdmProductImportRowDO> rows = importRowMapper.selectListByBatchId(batchId);
        if (rows.stream().anyMatch(row -> row.getFailureReason() != null)) {
            throw new IllegalStateException("MDM_PRODUCT_IMPORT_HAS_FAILURES: 请重新预览并修正失败行");
        }
        for (MdmProductImportRowDO row : rows) {
            applyImportRow(row);
        }
        batch.setStatus(MdmProductImportStatusConstants.CONFIRMED);
        batch.setConfirmedAt(LocalDateTime.now());
        importBatchMapper.updateById(batch);
        return toPreviewResp(batch, rows);
    }

    @Override
    public List<MdmProductImportExcelVO> exportForShowroomWorkbook(Collection<String> productCodes) {
        if (CollUtil.isEmpty(productCodes)) {
            return List.of();
        }
        List<String> normalizedCodes = productCodes.stream()
                .map(MdmProductCodePolicy::normalize)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return List.of();
        }
        Map<String, MdmProductDO> productsByCode = productMapper.selectListByProductCodes(normalizedCodes).stream()
                .collect(Collectors.toMap(MdmProductDO::getProductCode, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<MdmProductImportExcelVO> rows = new ArrayList<>();
        for (String productCode : normalizedCodes) {
            MdmProductDO product = productsByCode.get(productCode);
            if (product == null) {
                throw new IllegalStateException("MDM_PRODUCT_SHOWROOM_WORKBOOK_EXPORT_MISSING: 产品主数据不存在："
                        + productCode);
            }
            rows.add(MdmProductImportExcelVO.builder()
                    .productCode(product.getProductCode())
                    .dccProductCode(product.getDccProductCode())
                    .nameCn(product.getNameCn())
                    .nameEn(product.getNameEn())
                    .modelSpecification(product.getModelSpecification())
                    .category(product.getCategory())
                    .build());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> importFromShowroomWorkbook(List<MdmProductImportExcelVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            throw new IllegalStateException("MDM_PRODUCT_IMPORT_EMPTY: 产品主数据导入文件不能为空");
        }
        List<MdmProductDO> existingProducts = productMapper.selectList();
        Map<String, MdmProductDO> existingByCode = existingProducts.stream()
                .collect(Collectors.toMap(MdmProductDO::getProductCode, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, MdmProductDO> existingByDccCode = existingProducts.stream()
                .filter(product -> StrUtil.isNotBlank(product.getDccProductCode()))
                .collect(Collectors.toMap(MdmProductDO::getDccProductCode, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));

        Set<String> seenProductCodes = new LinkedHashSet<>();
        Set<String> seenDccProductCodes = new LinkedHashSet<>();
        Map<String, Long> productMasterIdsByCode = new LinkedHashMap<>();

        for (int index = 0; index < rows.size(); index++) {
            int rowNo = index + 2;
            ImportRowDraft draft = normalizeImportRow(rows.get(index), rowNo);
            String failure = validateImportRow(draft, seenProductCodes, seenDccProductCodes, existingByDccCode);
            if (failure != null) {
                throw new IllegalStateException("MDM_PRODUCT_SHOWROOM_WORKBOOK_IMPORT_ROW_INVALID: 第 "
                        + rowNo + " 行" + failure);
            }
            MdmProductDO existing = existingByCode.get(draft.productCode());
            if (existing == null) {
                MdmProductDO product = MdmProductDO.builder()
                        .productCode(draft.productCode())
                        .dccProductCode(draft.dccProductCode())
                        .nameCn(draft.nameCn())
                        .nameEn(draft.nameEn())
                        .modelSpecification(draft.modelSpecification())
                        .category(draft.category())
                        .status(MdmProductStatusConstants.ENABLE)
                        .build();
                productMapper.insert(product);
                if (product.getId() == null) {
                    throw new IllegalStateException("MDM_PRODUCT_SHOWROOM_WORKBOOK_IMPORT_MISSING: 产品主数据导入后未返回 ID："
                            + draft.productCode());
                }
                productMasterIdsByCode.put(draft.productCode(), product.getId());
                existingByCode.put(draft.productCode(), product);
                if (StrUtil.isNotBlank(draft.dccProductCode())) {
                    existingByDccCode.put(draft.dccProductCode(), product);
                }
                continue;
            }
            String action = resolveImportAction(draft, existing, null);
            if (MdmProductImportActionConstants.UPDATE.equals(action)) {
                productMapper.updateById(MdmProductDO.builder()
                        .id(existing.getId())
                        .dccProductCode(draft.dccProductCode())
                        .nameCn(draft.nameCn())
                        .nameEn(draft.nameEn())
                        .modelSpecification(draft.modelSpecification())
                        .category(draft.category())
                        .status(MdmProductStatusConstants.ENABLE)
                        .build());
            }
            productMasterIdsByCode.put(draft.productCode(), existing.getId());
        }
        return productMasterIdsByCode;
    }

    @Override
    public MdmProductReferenceRespVO getReferences(Long productId) {
        requireProduct(productId);
        return MdmProductReferenceRespVO.builder()
                .productId(productId)
                .dccReferenceCount(referenceMapper.countDccReferences(productId))
                .showroomReferenceCount(referenceMapper.countShowroomReferences(productId))
                .build();
    }

    private void applyImportRow(MdmProductImportRowDO row) {
        switch (row.getImportAction()) {
            case MdmProductImportActionConstants.CREATE -> productMapper.insert(MdmProductDO.builder()
                    .productCode(row.getProductCode())
                    .dccProductCode(row.getDccProductCode())
                    .nameCn(row.getNameCn())
                    .nameEn(row.getNameEn())
                    .modelSpecification(row.getModelSpecification())
                    .category(row.getCategory())
                    .status(MdmProductStatusConstants.ENABLE)
                    .build());
            case MdmProductImportActionConstants.UPDATE -> {
                MdmProductDO existing = requireProductByCode(row.getProductCode());
                productMapper.updateById(MdmProductDO.builder()
                        .id(existing.getId())
                        .dccProductCode(row.getDccProductCode())
                        .nameCn(row.getNameCn())
                        .nameEn(row.getNameEn())
                        .modelSpecification(row.getModelSpecification())
                        .category(row.getCategory())
                        .status(MdmProductStatusConstants.ENABLE)
                        .build());
            }
            case MdmProductImportActionConstants.DISABLE -> {
                MdmProductDO existing = requireProductByCode(row.getProductCode());
                productMapper.updateById(MdmProductDO.builder()
                        .id(existing.getId())
                        .status(MdmProductStatusConstants.DISABLE)
                        .build());
            }
            case MdmProductImportActionConstants.UNCHANGED -> {
            }
            default -> throw new IllegalStateException("MDM_PRODUCT_IMPORT_ACTION_INVALID: " + row.getImportAction());
        }
    }

    private NormalizedProduct validateAndNormalize(MdmProductSaveReqVO reqVO, MdmProductDO existing) {
        String productCode = requireText(reqVO.getProductCode(), "MDM_PRODUCT_CODE_REQUIRED: 产品编码不能为空");
        String dccProductCode = MdmProductCodePolicy.normalize(reqVO.getDccProductCode());
        if (dccProductCode != null && !MdmProductCodePolicy.isValidDccProductCode(dccProductCode)) {
            throw new IllegalStateException("MDM_PRODUCT_DCC_CODE_INVALID: DCC 产品编号必须为 14 位字母或数字");
        }
        String nameCn = requireText(reqVO.getNameCn(), "MDM_PRODUCT_NAME_CN_REQUIRED: 中文名称不能为空");
        String status = normalizeStatus(reqVO.getStatus());

        validateProductCodeUnique(existing == null ? null : existing.getId(), productCode);
        validateDccProductCodeUnique(existing == null ? null : existing.getId(), dccProductCode);
        return new NormalizedProduct(productCode, dccProductCode, nameCn,
                StrUtil.trimToNull(reqVO.getNameEn()),
                StrUtil.trimToNull(reqVO.getModelSpecification()),
                StrUtil.trimToNull(reqVO.getCategory()),
                status);
    }

    private String validateImportRow(ImportRowDraft draft, Set<String> seenProductCodes,
                                     Set<String> seenDccProductCodes,
                                     Map<String, MdmProductDO> existingByDccCode) {
        if (draft.productCode() == null) {
            return "产品编码不能为空";
        }
        if (!seenProductCodes.add(draft.productCode())) {
            return "产品编码在 Excel 中重复";
        }
        if (draft.nameCn() == null) {
            return "中文名称不能为空";
        }
        if (draft.dccProductCode() != null) {
            if (!MdmProductCodePolicy.isValidDccProductCode(draft.dccProductCode())) {
                return "DCC 产品编号必须为 14 位字母或数字";
            }
            if (!seenDccProductCodes.add(draft.dccProductCode())) {
                return "DCC 产品编号在 Excel 中重复";
            }
            MdmProductDO existingByDcc = existingByDccCode.get(draft.dccProductCode());
            if (existingByDcc != null && !Objects.equals(existingByDcc.getProductCode(), draft.productCode())) {
                return "DCC 产品编号已绑定其他产品编码：" + existingByDcc.getProductCode();
            }
        }
        return null;
    }

    private String resolveImportAction(ImportRowDraft draft, MdmProductDO existing, String failure) {
        if (failure != null) {
            return MdmProductImportActionConstants.INVALID;
        }
        if (existing == null) {
            return MdmProductImportActionConstants.CREATE;
        }
        if (Objects.equals(existing.getDccProductCode(), draft.dccProductCode())
                && Objects.equals(existing.getNameCn(), draft.nameCn())
                && Objects.equals(existing.getNameEn(), draft.nameEn())
                && Objects.equals(existing.getModelSpecification(), draft.modelSpecification())
                && Objects.equals(existing.getCategory(), draft.category())
                && MdmProductStatusConstants.ENABLE.equals(existing.getStatus())) {
            return MdmProductImportActionConstants.UNCHANGED;
        }
        return MdmProductImportActionConstants.UPDATE;
    }

    private MdmProductImportRowDO toImportRow(int rowNo, ImportRowDraft draft, MdmProductDO existing,
                                             String action, String failure) {
        return MdmProductImportRowDO.builder()
                .rowNo(rowNo)
                .productCode(draft.productCode())
                .dccProductCode(draft.dccProductCode())
                .nameCn(draft.nameCn())
                .nameEn(draft.nameEn())
                .modelSpecification(draft.modelSpecification())
                .category(draft.category())
                .currentStatus(existing == null ? null : existing.getStatus())
                .importAction(action)
                .failureReason(failure)
                .build();
    }

    private ImportRowDraft normalizeImportRow(MdmProductImportExcelVO row, int rowNo) {
        if (row == null) {
            return new ImportRowDraft(rowNo, null, null, null, null, null, null);
        }
        return new ImportRowDraft(rowNo,
                MdmProductCodePolicy.normalize(row.getProductCode()),
                MdmProductCodePolicy.normalize(row.getDccProductCode()),
                MdmProductCodePolicy.normalize(row.getNameCn()),
                MdmProductCodePolicy.normalize(row.getNameEn()),
                MdmProductCodePolicy.normalize(row.getModelSpecification()),
                MdmProductCodePolicy.normalize(row.getCategory()));
    }

    private ImportSummary summarize(List<MdmProductImportRowDO> rows) {
        int createCount = countAction(rows, MdmProductImportActionConstants.CREATE);
        int updateCount = countAction(rows, MdmProductImportActionConstants.UPDATE);
        int disableCount = countAction(rows, MdmProductImportActionConstants.DISABLE);
        int unchangedCount = countAction(rows, MdmProductImportActionConstants.UNCHANGED);
        int failureCount = countAction(rows, MdmProductImportActionConstants.INVALID);
        return new ImportSummary(rows.size(), createCount, updateCount, disableCount, unchangedCount, failureCount);
    }

    private int countAction(List<MdmProductImportRowDO> rows, String action) {
        return (int) rows.stream().filter(row -> action.equals(row.getImportAction())).count();
    }

    private MdmProductImportPreviewRespVO toPreviewResp(MdmProductImportBatchDO batch,
                                                       List<MdmProductImportRowDO> rows) {
        return MdmProductImportPreviewRespVO.builder()
                .batchId(batch.getId())
                .status(batch.getStatus())
                .totalCount(batch.getTotalCount())
                .createCount(batch.getCreateCount())
                .updateCount(batch.getUpdateCount())
                .disableCount(batch.getDisableCount())
                .unchangedCount(batch.getUnchangedCount())
                .failureCount(batch.getFailureCount())
                .rows(rows.stream().map(this::toImportRowResp).toList())
                .build();
    }

    private MdmProductImportRowRespVO toImportRowResp(MdmProductImportRowDO row) {
        return MdmProductImportRowRespVO.builder()
                .rowNo(row.getRowNo())
                .productCode(row.getProductCode())
                .dccProductCode(row.getDccProductCode())
                .nameCn(row.getNameCn())
                .nameEn(row.getNameEn())
                .modelSpecification(row.getModelSpecification())
                .category(row.getCategory())
                .currentStatus(row.getCurrentStatus())
                .importAction(row.getImportAction())
                .failureReason(row.getFailureReason())
                .build();
    }

    private MdmProductDO requireProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("MDM_PRODUCT_ID_REQUIRED: 产品主数据 ID 不能为空");
        }
        MdmProductDO product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalStateException("MDM_PRODUCT_NOT_FOUND: productId=" + id);
        }
        return product;
    }

    private MdmProductDO requireProductByCode(String productCode) {
        MdmProductDO product = productMapper.selectByProductCode(productCode);
        if (product == null) {
            throw new IllegalStateException("MDM_PRODUCT_NOT_FOUND: productCode=" + productCode);
        }
        return product;
    }

    private String requireText(String value, String message) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            throw new IllegalStateException(message);
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = StrUtil.blankToDefault(status, MdmProductStatusConstants.ENABLE);
        if (!MdmProductStatusConstants.isValid(normalized)) {
            throw new IllegalStateException("MDM_PRODUCT_STATUS_INVALID: status=" + status);
        }
        return normalized;
    }

    private void validateProductCodeUnique(Long id, String productCode) {
        MdmProductDO existing = productMapper.selectByProductCode(productCode);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw new IllegalStateException("MDM_PRODUCT_CODE_DUPLICATE: 产品编码已存在");
        }
    }

    private void validateDccProductCodeUnique(Long id, String dccProductCode) {
        if (dccProductCode == null) {
            return;
        }
        MdmProductDO existing = productMapper.selectByDccProductCode(dccProductCode);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw new IllegalStateException("MDM_PRODUCT_DCC_CODE_DUPLICATE: DCC 产品编号已存在");
        }
    }

    private record NormalizedProduct(String productCode, String dccProductCode, String nameCn, String nameEn,
                                     String modelSpecification, String category, String status) {
    }

    private record ImportRowDraft(int rowNo, String productCode, String dccProductCode, String nameCn, String nameEn,
                                  String modelSpecification, String category) {
    }

    private record ImportSummary(int totalCount, int createCount, int updateCount, int disableCount,
                                 int unchangedCount, int failureCount) {
    }

}
