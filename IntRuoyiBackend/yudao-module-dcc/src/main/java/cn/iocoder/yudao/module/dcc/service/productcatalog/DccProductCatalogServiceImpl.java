package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogTreeNodeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogTreeReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.relation.DccDataRelationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PRODUCT_CATALOG_DATA_SOURCE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PRODUCT_CATALOG_ROW_KEY_INVALID;

@Service
@Validated
public class DccProductCatalogServiceImpl implements DccProductCatalogService {

    private static final String STATUS_MATCH = "MATCH";
    private static final String STATUS_MISMATCH = "MISMATCH";
    private static final String STATUS_FETCH_FAILED = "FETCH_FAILED";
    private static final String STATUS_NO_LINK = "NO_LINK";
    private static final String STATUS_UNSUPPORTED = "UNSUPPORTED";
    private static final String NODE_TYPE_CATEGORY_LEVEL1 = "categoryLevel1";
    private static final String NODE_TYPE_CATEGORY_LEVEL2 = "categoryLevel2";
    private static final String NODE_TYPE_PRODUCT = "product";
    private static final String NODE_TYPE_DETAIL = "detail";
    private static final Set<String> SUPPORTED_DATA_SOURCES = Set.of("瑛泰产品");
    private static final Pattern EXPIRY_DATE_PATTERN = Pattern.compile(
            "(?:有效期至|有效期)\\s*[:：]?\\s*([0-9]{4})\\s*[./\\-/年]\\s*([0-9]{1,2})\\s*[./\\-/月]\\s*([0-9]{1,2})\\s*(?:日)?");
    private static final DateTimeFormatter NORMALIZED_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Resource
    private DccProductCatalogMapper productCatalogMapper;

    @Resource
    private DccProjectCodeMapper projectCodeMapper;

    @Resource
    private DccDataRelationMapper relationMapper;

    @Resource
    private DccRegistrationExpiryExternalPageClient externalPageClient;

    @Override
    public PageResult<DccProductCatalogRespVO> getProductCatalogPage(DccProductCatalogPageReqVO reqVO) {
        PageResult<DccProductCatalogRespVO> page =
                BeanUtils.toBean(productCatalogMapper.selectPage(reqVO), DccProductCatalogRespVO.class);
        enrichBoundRelationIds(page.getList());
        enrichBatchRecordTotalRecognitionJson(page.getList());
        return page;
    }

    @Override
    public List<DccProductCatalogTreeNodeRespVO> getProductCatalogTree(DccProductCatalogTreeReqVO reqVO) {
        List<DccProductCatalogDO> rows = productCatalogMapper.selectTreeRows(reqVO);
        Map<String, DccProductCatalogTreeNodeRespVO> categoryLevel1Nodes = new LinkedHashMap<>();
        for (DccProductCatalogDO row : rows) {
            String dataSource = StrUtil.blankToDefault(row.getDataSource(), "");
            String categoryLevel1 = StrUtil.blankToDefault(row.getCategoryLevel1(), "未分类");
            String categoryLevel2 = StrUtil.blankToDefault(row.getCategoryLevel2(), "未分类");
            String product = StrUtil.blankToDefault(row.getProduct(), "未命名产品");

            String categoryLevel1Key = dataSource + "|" + categoryLevel1;
            DccProductCatalogTreeNodeRespVO categoryLevel1Node = categoryLevel1Nodes.computeIfAbsent(
                    categoryLevel1Key,
                    key -> branchNode(key, NODE_TYPE_CATEGORY_LEVEL1, 1, categoryLevel1,
                            dataSource, categoryLevel1, null, null));

            String categoryLevel2Key = categoryLevel1Key + "|" + categoryLevel2;
            DccProductCatalogTreeNodeRespVO categoryLevel2Node =
                    childById(categoryLevel1Node, NODE_TYPE_CATEGORY_LEVEL2, categoryLevel2Key);
            if (categoryLevel2Node == null) {
                categoryLevel2Node = branchNode(categoryLevel2Key, NODE_TYPE_CATEGORY_LEVEL2, 2, categoryLevel2,
                        dataSource, categoryLevel1, categoryLevel2, null);
                categoryLevel1Node.getChildren().add(categoryLevel2Node);
            }

            String productKey = categoryLevel2Key + "|" + product;
            DccProductCatalogTreeNodeRespVO productNode =
                    childById(categoryLevel2Node, NODE_TYPE_PRODUCT, productKey);
            if (productNode == null) {
                productNode = branchNode(productKey, NODE_TYPE_PRODUCT, 3, product,
                        dataSource, categoryLevel1, categoryLevel2, product);
                productNode.setProductSequence(row.getProductSequence());
                categoryLevel2Node.getChildren().add(productNode);
            }
            productNode.getChildren().add(detailNode(row, productKey + "|" + row.getOriginalRowNo()));
        }
        return new ArrayList<>(categoryLevel1Nodes.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProductCatalogRespVO createProductCatalog(DccProductCatalogSaveReqVO reqVO) {
        String dataSource = validateDataSource(reqVO.getDataSource());
        Integer maxOriginalRowNo = productCatalogMapper.selectMaxOriginalRowNo(dataSource);
        DccProductCatalogDO row = buildProductCatalogDO(reqVO);
        row.setDataSource(dataSource);
        row.setOriginalRowNo((maxOriginalRowNo == null ? 1 : maxOriginalRowNo) + 1);
        productCatalogMapper.insert(row);
        return BeanUtils.toBean(row, DccProductCatalogRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductCatalog(DccProductCatalogUpdateReqVO reqVO) {
        String dataSource = validateDataSource(reqVO.getDataSource());
        DccProductCatalogDO existing = requireRow(dataSource, reqVO.getOriginalRowNo());
        DccProductCatalogDO update = buildProductCatalogDO(reqVO);
        update.setId(existing.getId());
        update.setDataSource(dataSource);
        update.setOriginalRowNo(existing.getOriginalRowNo());
        productCatalogMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductCatalog(String dataSource, Integer originalRowNo) {
        DccProductCatalogDO existing = requireRow(validateDataSource(dataSource), originalRowNo);
        productCatalogMapper.deleteById(existing.getId());
    }

    @Override
    public List<DccProductCatalogRegistrationExpiryCompareRespVO> compareRegistrationExpiry(
            DccProductCatalogRegistrationExpiryCompareReqVO reqVO) {
        Map<RowKey, DccProductCatalogRespVO> rowMap = buildRowMap(
                BeanUtils.toBean(productCatalogMapper.selectAllInDisplayOrder(), DccProductCatalogRespVO.class));
        return reqVO.getRows().stream()
                .map(rowKey -> compareRow(rowKey, rowMap))
                .toList();
    }

    private DccProductCatalogDO requireRow(String dataSource, Integer originalRowNo) {
        DccProductCatalogDO row = productCatalogMapper.selectByRowKey(dataSource, originalRowNo);
        if (row == null) {
            throw exception(DCC_PRODUCT_CATALOG_ROW_KEY_INVALID, rowKeyMessage(dataSource, originalRowNo));
        }
        return row;
    }

    private String validateDataSource(String dataSource) {
        String normalized = StrUtil.trimToEmpty(dataSource);
        if (!SUPPORTED_DATA_SOURCES.contains(normalized)) {
            throw exception(DCC_PRODUCT_CATALOG_DATA_SOURCE_INVALID, dataSource);
        }
        return normalized;
    }

    private DccProductCatalogDO buildProductCatalogDO(DccProductCatalogSaveReqVO reqVO) {
        DccProductCatalogDO row = BeanUtils.toBean(reqVO, DccProductCatalogDO.class);
        row.setDataSource(StrUtil.trim(reqVO.getDataSource()));
        return row;
    }

    private void enrichBatchRecordTotalRecognitionJson(List<DccProductCatalogRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, String> recognitionJsonByProjectCode = new HashMap<>();
        for (DccProjectCodeDO projectCode : projectCodeMapper.selectEnabledList()) {
            String code = StrUtil.trimToNull(projectCode.getProjectCode());
            if (code == null || StrUtil.isBlank(projectCode.getBatchRecordTotalRecognitionJson())) {
                continue;
            }
            recognitionJsonByProjectCode.putIfAbsent(code, projectCode.getBatchRecordTotalRecognitionJson());
        }
        for (DccProductCatalogRespVO row : rows) {
            String projectCode = StrUtil.trimToNull(row.getProjectCode());
            if (projectCode != null) {
                row.setBatchRecordTotalRecognitionJson(recognitionJsonByProjectCode.get(projectCode));
            }
        }
    }

    private void enrichBoundRelationIds(List<DccProductCatalogRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> productCatalogIds = rows.stream()
                .map(DccProductCatalogRespVO::getId)
                .filter(id -> id != null)
                .toList();
        if (productCatalogIds.isEmpty()) {
            return;
        }
        Map<Long, DccDataRelationDO> latestRelationByCatalogId = new HashMap<>();
        for (DccDataRelationDO relation : relationMapper.selectByProductCatalogIds(productCatalogIds)) {
            if (relation.getProductCatalogId() != null) {
                latestRelationByCatalogId.putIfAbsent(relation.getProductCatalogId(), relation);
            }
        }
        for (DccProductCatalogRespVO row : rows) {
            DccDataRelationDO relation = latestRelationByCatalogId.get(row.getId());
            if (relation == null) {
                continue;
            }
            row.setProjectCodeId(relation.getProjectCodeId());
            row.setRegistrationCertificateId(relation.getRegistrationCertificateId());
        }
    }

    private DccProductCatalogTreeNodeRespVO branchNode(String treeNodeId, String nodeType, int treeLevel,
            String treeLabel, String dataSource, String categoryLevel1, String categoryLevel2, String product) {
        DccProductCatalogTreeNodeRespVO node = new DccProductCatalogTreeNodeRespVO();
        node.setTreeNodeId(nodeType + ":" + treeNodeId);
        node.setNodeType(nodeType);
        node.setTreeLevel(treeLevel);
        node.setTreeLabel(treeLabel);
        node.setDataSource(dataSource);
        node.setCategoryLevel1(categoryLevel1);
        node.setCategoryLevel2(categoryLevel2);
        node.setProduct(product);
        return node;
    }

    private DccProductCatalogTreeNodeRespVO childById(DccProductCatalogTreeNodeRespVO parent, String nodeType,
            String treeNodeId) {
        String normalizedTreeNodeId = nodeType + ":" + treeNodeId;
        return parent.getChildren().stream()
                .filter(child -> normalizedTreeNodeId.equals(child.getTreeNodeId()))
                .findFirst()
                .orElse(null);
    }

    private DccProductCatalogTreeNodeRespVO detailNode(DccProductCatalogDO row, String treeNodeId) {
        DccProductCatalogTreeNodeRespVO node = BeanUtils.toBean(row, DccProductCatalogTreeNodeRespVO.class);
        node.setTreeNodeId(NODE_TYPE_DETAIL + ":" + treeNodeId);
        node.setNodeType(NODE_TYPE_DETAIL);
        node.setTreeLevel(4);
        node.setTreeLabel(StrUtil.blankToDefault(row.getRegistrationCertificateName(), row.getProductCode()));
        node.setChildren(new ArrayList<>());
        return node;
    }

    private DccProductCatalogRegistrationExpiryCompareRespVO compareRow(
            DccProductCatalogRegistrationExpiryCompareReqVO.RowKey rowKey,
            Map<RowKey, DccProductCatalogRespVO> rowMap) {
        RowKey key = new RowKey(rowKey.getDataSource(), rowKey.getOriginalRowNo());
        DccProductCatalogRespVO row = rowMap.get(key);
        if (row == null) {
            throw exception(DCC_PRODUCT_CATALOG_ROW_KEY_INVALID, key.toMessage());
        }

        DccProductCatalogRegistrationExpiryCompareRespVO result = baseCompareResult(row);
        LocalDate localDate = parseDate(row.getExpiryDate());
        result.setLocalExpiryDate(formatDate(localDate));
        if (StrUtil.isBlank(row.getRegistrationInfoLink())) {
            result.setStatus(STATUS_NO_LINK);
            result.setMessage("注册证信息链接为空");
            return result;
        }

        String remotePage;
        try {
            remotePage = externalPageClient.fetch(row.getRegistrationInfoLink());
        } catch (DccRegistrationExpiryExternalPageFetchException ex) {
            result.setStatus(STATUS_FETCH_FAILED);
            result.setMessage(StrUtil.blankToDefault(ex.getMessage(), "注册证信息链接访问失败"));
            return result;
        }

        LocalDate remoteDate = parseRemoteExpiryDate(remotePage);
        result.setRemoteExpiryDate(formatDate(remoteDate));
        if (remoteDate == null || localDate == null) {
            result.setStatus(STATUS_UNSUPPORTED);
            result.setMessage("页面没有可解析的有效期至");
            return result;
        }
        if (localDate.equals(remoteDate)) {
            result.setStatus(STATUS_MATCH);
            result.setMessage("有效期一致");
        } else {
            result.setStatus(STATUS_MISMATCH);
            result.setMessage("有效期不一致");
        }
        return result;
    }

    private DccProductCatalogRegistrationExpiryCompareRespVO baseCompareResult(DccProductCatalogRespVO row) {
        DccProductCatalogRegistrationExpiryCompareRespVO result =
                new DccProductCatalogRegistrationExpiryCompareRespVO();
        result.setDataSource(row.getDataSource());
        result.setOriginalRowNo(row.getOriginalRowNo());
        return result;
    }

    private Map<RowKey, DccProductCatalogRespVO> buildRowMap(List<DccProductCatalogRespVO> rows) {
        Map<RowKey, DccProductCatalogRespVO> rowMap = new HashMap<>();
        for (DccProductCatalogRespVO row : rows) {
            RowKey key = new RowKey(row.getDataSource(), row.getOriginalRowNo());
            DccProductCatalogRespVO previous = rowMap.putIfAbsent(key, row);
            if (previous != null) {
                throw exception(DCC_PRODUCT_CATALOG_ROW_KEY_INVALID, key.toMessage());
            }
        }
        return rowMap;
    }

    private LocalDate parseRemoteExpiryDate(String pageText) {
        if (StrUtil.isBlank(pageText)) {
            return null;
        }
        String normalizedText = pageText
                .replace('\u00A0', ' ')
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .replaceAll("<[^>]+>", " ");
        Matcher matcher = EXPIRY_DATE_PATTERN.matcher(normalizedText);
        if (!matcher.find()) {
            return null;
        }
        return toDate(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private LocalDate parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        Matcher matcher = Pattern.compile(
                        "^\\s*([0-9]{4})\\s*[./\\-/年]\\s*([0-9]{1,2})\\s*[./\\-/月]\\s*([0-9]{1,2})\\s*(?:日)?\\s*$")
                .matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        return toDate(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private LocalDate toDate(String year, String month, String day) {
        return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : NORMALIZED_DATE_FORMATTER.format(date);
    }

    private String rowKeyMessage(String dataSource, Integer originalRowNo) {
        return StrUtil.trimToEmpty(dataSource).toLowerCase(Locale.ROOT) + "#" + originalRowNo;
    }

    private record RowKey(String dataSource, Integer originalRowNo) {

        private RowKey {
            dataSource = StrUtil.trimToEmpty(dataSource).toLowerCase(Locale.ROOT);
        }

        private String toMessage() {
            return dataSource + "#" + originalRowNo;
        }
    }
}
