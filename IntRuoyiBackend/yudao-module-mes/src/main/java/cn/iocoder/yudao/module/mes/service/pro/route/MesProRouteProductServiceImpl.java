package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductBindFromWorkOrdersRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 工艺路线产品 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProRouteProductServiceImpl implements MesProRouteProductService {

    private static final String PRODUCTS_CONFIG_KEY = "products";

    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;

    @Resource
    @Lazy
    private MesProRouteService routeService;
    @Resource
    @Lazy
    private MesProRouteProductBomService routeProductBomService;
    @Resource
    private MesProRouteCandidateConfigService routeCandidateConfigService;

    @Override
    public Long createRouteProduct(MesProRouteProductSaveReqVO createReqVO) {
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(
                createReqVO.getRouteVersionId(), createReqVO.getRouteId());
        if (candidateVersion != null) {
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), PRODUCTS_CONFIG_KEY,
                    buildRouteProductsSnapshotWith(candidateVersion, createReqVO.getRouteId(), buildProductSnapshot(createReqVO)));
            return candidateVersion.getId();
        }
        // 1.0 已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(createReqVO.getRouteId());
        // 1.1 校验产品唯一性（一个产品只能关联一条工艺路线）
        validateItemUnique(null, createReqVO.getItemId());

        // 2. 插入
        MesProRouteProductDO routeProduct = BeanUtils.toBean(createReqVO, MesProRouteProductDO.class);
        routeProductMapper.insert(routeProduct);
        return routeProduct.getId();
    }

    private MesProRouteVersionDO requireDraftCandidateVersion(Long routeVersionId, Long routeId) {
        if (routeVersionId == null) {
            return null;
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(routeVersionId);
        if (routeVersion == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId);
        }
        if (Objects.equals(routeVersion.getRouteId(), routeId)
                && Boolean.FALSE.equals(routeVersion.getActive())
                && MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(routeVersion.getLifecycleStatus())) {
            return routeVersion;
        }
        throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                routeVersion.getId(), routeVersion.getLifecycleStatus());
    }

    private Map<String, Object> buildProductSnapshot(MesProRouteProductSaveReqVO reqVO) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("routeId", reqVO.getRouteId());
        product.put("itemId", reqVO.getItemId());
        product.put("quantity", reqVO.getQuantity());
        product.put("productionTime", reqVO.getProductionTime());
        product.put("timeUnitType", reqVO.getTimeUnitType());
        product.put("remark", reqVO.getRemark());
        return product;
    }

    private Map<String, Object> buildProductSnapshot(MesProRouteProductDO productDO) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("routeId", productDO.getRouteId());
        product.put("itemId", productDO.getItemId());
        product.put("quantity", productDO.getQuantity());
        product.put("productionTime", productDO.getProductionTime());
        product.put("timeUnitType", productDO.getTimeUnitType());
        product.put("remark", productDO.getRemark());
        return product;
    }

    private Map<String, Object> buildRouteProductsSnapshotWith(MesProRouteVersionDO candidateVersion, Long routeId,
                                                               Map<String, Object> product) {
        Map<String, Object> products = resolveRouteProductsSnapshot(candidateVersion, routeId);
        products.put(String.valueOf(product.get("itemId")), product);
        return products;
    }

    private Map<String, Object> resolveRouteProductsSnapshot(MesProRouteVersionDO candidateVersion, Long routeId) {
        Map<String, Object> products = new LinkedHashMap<>();
        Object snapshot = resolveConfigSnapshot(candidateVersion, PRODUCTS_CONFIG_KEY);
        if (snapshot instanceof JSONObject productsByKey) {
            for (Map.Entry<String, Object> entry : productsByKey.entrySet()) {
                products.put(entry.getKey(), normalizeProductSnapshot(entry.getValue()));
            }
        } else if (snapshot instanceof JSONArray productList) {
            for (Object value : productList) {
                Map<String, Object> product = normalizeProductSnapshot(value);
                products.put(String.valueOf(product.get("itemId")), product);
            }
        }
        if (!products.isEmpty()) {
            return products;
        }
        for (MesProRouteProductDO product : routeProductMapper.selectListByRouteId(routeId)) {
            products.put(String.valueOf(product.getItemId()), buildProductSnapshot(product));
        }
        return products;
    }

    private Object resolveConfigSnapshot(MesProRouteVersionDO candidateVersion, String configKey) {
        if (candidateVersion == null || StrUtil.isBlank(candidateVersion.getRouteSnapshotJson())) {
            return null;
        }
        JSONObject snapshot = JSON.parseObject(candidateVersion.getRouteSnapshotJson());
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
        return configSnapshots == null ? null : configSnapshots.get(configKey);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeProductSnapshot(Object value) {
        if (value instanceof JSONObject product) {
            return new LinkedHashMap<>(product);
        }
        return JSON.parseObject(JSON.toJSONString(value), LinkedHashMap.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyRouteProduct(MesProRouteProductCopyReqVO copyReqVO) {
        MesProRouteProductDO sourceProduct = routeProductMapper.selectById(copyReqVO.getSourceRouteProductId());
        validateRouteProductExists(sourceProduct);
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(
                copyReqVO.getRouteVersionId(), sourceProduct.getRouteId());
        if (candidateVersion != null) {
            MesProRouteProductDO targetProduct = buildCopiedProduct(sourceProduct, copyReqVO);
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), "products",
                    buildRouteProductsSnapshotWith(candidateVersion, sourceProduct.getRouteId(), buildProductSnapshot(targetProduct)));
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), "productBoms",
                    buildRouteProductBomsSnapshotForProductCopy(candidateVersion, sourceProduct.getRouteId(),
                            sourceProduct.getItemId(), copyReqVO.getTargetItemId()));
            return candidateVersion.getId();
        }
        routeService.validateRouteNotEnable(sourceProduct.getRouteId());
        validateItemUnique(null, copyReqVO.getTargetItemId());

        MesProRouteProductDO targetProduct = buildCopiedProduct(sourceProduct, copyReqVO);
        routeProductMapper.insert(targetProduct);

        for (MesProRouteProductBomDO sourceBom : routeProductBomMapper.selectListByRouteIdAndProductId(
                sourceProduct.getRouteId(), sourceProduct.getItemId())) {
            MesProRouteProductBomDO targetBom = BeanUtils.toBean(sourceBom, MesProRouteProductBomDO.class);
            targetBom.setId(null);
            targetBom.setRouteId(sourceProduct.getRouteId());
            targetBom.setProductId(copyReqVO.getTargetItemId());
            routeProductBomMapper.insert(targetBom);
        }
        return targetProduct.getId();
    }

    private MesProRouteProductDO buildCopiedProduct(MesProRouteProductDO sourceProduct,
                                                    MesProRouteProductCopyReqVO copyReqVO) {
        MesProRouteProductDO targetProduct = BeanUtils.toBean(sourceProduct, MesProRouteProductDO.class);
        targetProduct.setId(null);
        targetProduct.setRouteId(sourceProduct.getRouteId());
        targetProduct.setItemId(copyReqVO.getTargetItemId());
        targetProduct.setQuantity(copyReqVO.getQuantity() != null ? copyReqVO.getQuantity() : sourceProduct.getQuantity());
        targetProduct.setProductionTime(copyReqVO.getProductionTime() != null
                ? copyReqVO.getProductionTime() : sourceProduct.getProductionTime());
        targetProduct.setTimeUnitType(copyReqVO.getTimeUnitType() != null
                ? copyReqVO.getTimeUnitType() : sourceProduct.getTimeUnitType());
        targetProduct.setRemark(copyReqVO.getRemark() != null ? copyReqVO.getRemark() : sourceProduct.getRemark());
        return targetProduct;
    }

    @Override
    @Transactional(readOnly = true)
    public MesProRouteProductBindFromWorkOrdersRespVO previewBindFromWorkOrders(Long routeId, Long routeVersionId) {
        return buildBindFromWorkOrdersResult(routeId, routeVersionId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteProductBindFromWorkOrdersRespVO bindFromWorkOrders(Long routeId, Long routeVersionId) {
        return buildBindFromWorkOrdersResult(routeId, routeVersionId, true);
    }

    private MesProRouteProductBindFromWorkOrdersRespVO buildBindFromWorkOrdersResult(Long routeId,
                                                                                     Long routeVersionId,
                                                                                      boolean createMissing) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(routeVersionId, routeId);
        String routeName = StrUtil.trim(route.getName());
        if (StrUtil.isBlank(routeName)) {
            throw exception(PRO_ROUTE_PRODUCT_ROUTE_NAME_EMPTY);
        }

        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectListAll();
        if (CollUtil.isEmpty(workOrders)) {
            throw exception(PRO_ROUTE_PRODUCT_WORK_ORDER_MATCH_EMPTY, routeName);
        }
        List<Long> workOrderProductIds = workOrders.stream()
                .map(MesProWorkOrderDO::getProductId)
                .filter(ObjUtil::isNotNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(workOrderProductIds)) {
            throw exception(PRO_ROUTE_PRODUCT_WORK_ORDER_MATCH_EMPTY, routeName);
        }
        List<MesMdItemDO> workOrderProducts = itemMapper.selectListByIds(workOrderProductIds);
        Map<Long, MesMdItemDO> matchedItemMap = workOrderProducts.stream()
                .filter(item -> StrUtil.equals(StrUtil.trim(item.getName()), routeName))
                .collect(Collectors.toMap(MesMdItemDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        if (matchedItemMap.isEmpty()) {
            throw exception(PRO_ROUTE_PRODUCT_WORK_ORDER_MATCH_EMPTY, routeName);
        }

        List<MesProRouteProductDO> existingBindings =
                routeProductMapper.selectListByItemIds(matchedItemMap.keySet());
        Set<Long> existingCurrentRouteItemIds = candidateVersion == null
                ? existingBindings.stream()
                .filter(binding -> ObjUtil.equal(binding.getRouteId(), routeId))
                .map(MesProRouteProductDO::getItemId)
                .collect(Collectors.toSet())
                : resolveRouteProductsSnapshot(candidateVersion, routeId).values().stream()
                .map(this::normalizeProductSnapshot)
                .map(product -> product.get("itemId"))
                .filter(Objects::nonNull)
                .map(value -> Long.valueOf(String.valueOf(value)))
                .collect(Collectors.toSet());
        Set<Long> conflictItemIds = existingBindings.stream()
                .filter(binding -> ObjUtil.notEqual(binding.getRouteId(), routeId))
                .map(MesProRouteProductDO::getItemId)
                .distinct()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<MesMdItemDO> creatableItems = matchedItemMap.values().stream()
                .filter(item -> !existingCurrentRouteItemIds.contains(item.getId()))
                .filter(item -> !conflictItemIds.contains(item.getId()))
                .toList();
        if (createMissing) {
            if (candidateVersion != null) {
                Map<String, Object> products = resolveRouteProductsSnapshot(candidateVersion, routeId);
                for (MesMdItemDO item : creatableItems) {
                    products.put(String.valueOf(item.getId()), buildProductSnapshot(buildAutoRouteProduct(routeId, item)));
                }
                routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), PRODUCTS_CONFIG_KEY, products);
            } else {
                for (MesMdItemDO item : creatableItems) {
                    routeProductMapper.insert(buildAutoRouteProduct(routeId, item));
                }
            }
        }

        List<String> existingItemCodes = matchedItemMap.values().stream()
                .filter(item -> existingCurrentRouteItemIds.contains(item.getId()))
                .map(MesMdItemDO::getCode)
                .toList();
        List<String> conflictItemCodes = matchedItemMap.values().stream()
                .filter(item -> conflictItemIds.contains(item.getId()))
                .map(MesMdItemDO::getCode)
                .toList();

        return MesProRouteProductBindFromWorkOrdersRespVO.builder()
                .routeId(routeId)
                .routeName(routeName)
                .matchedCount(matchedItemMap.size())
                .existingCount(existingItemCodes.size())
                .createdCount(creatableItems.size())
                .conflictCount(conflictItemCodes.size())
                .itemCodes(matchedItemMap.values().stream().map(MesMdItemDO::getCode).toList())
                .creatableItemCodes(creatableItems.stream().map(MesMdItemDO::getCode).toList())
                .existingItemCodes(existingItemCodes)
                .conflictItemCodes(conflictItemCodes)
                .build();
    }

    private MesProRouteProductDO buildAutoRouteProduct(Long routeId, MesMdItemDO item) {
        return MesProRouteProductDO.builder()
                .routeId(routeId)
                .itemId(item.getId())
                .quantity(1)
                .productionTime(BigDecimal.ONE)
                .timeUnitType("MINUTE")
                .remark("生产订单自动补齐")
                .build();
    }

    @Override
    public void updateRouteProduct(MesProRouteProductSaveReqVO updateReqVO) {
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(
                updateReqVO.getRouteVersionId(), updateReqVO.getRouteId());
        if (candidateVersion != null) {
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), PRODUCTS_CONFIG_KEY,
                    buildRouteProductsSnapshotWith(candidateVersion, updateReqVO.getRouteId(), buildProductSnapshot(updateReqVO)));
            return;
        }
        // 1.0 已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(updateReqVO.getRouteId());
        // 1.1 校验存在
        validateRouteProductExists(updateReqVO.getId());
        // 1.2 校验产品唯一性
        validateItemUnique(updateReqVO.getId(), updateReqVO.getItemId());

        // 2. 更新
        MesProRouteProductDO updateObj = BeanUtils.toBean(updateReqVO, MesProRouteProductDO.class);
        routeProductMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRouteProduct(Long id, Long routeVersionId) {
        // 1.1 校验存在
        MesProRouteProductDO routeProduct = routeProductMapper.selectById(id);
        validateRouteProductExists(routeProduct);
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(routeVersionId, routeProduct.getRouteId());
        if (candidateVersion != null) {
            Map<String, Object> products = resolveRouteProductsSnapshot(candidateVersion, routeProduct.getRouteId());
            products.remove(String.valueOf(routeProduct.getItemId()));
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), PRODUCTS_CONFIG_KEY, products);
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), "productBoms",
                    buildRouteProductBomsSnapshotWithoutProduct(candidateVersion, routeProduct.getRouteId(),
                            routeProduct.getItemId()));
            return;
        }
        // 1.2 已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(routeProduct.getRouteId());

        // 2.1 级联删除关联的 BOM
        routeProductBomService.deleteRouteProductBomByRouteIdAndProductId(routeProduct.getRouteId(), routeProduct.getItemId());
        // 2.2 删除产品关联
        routeProductMapper.deleteById(id);
    }

    private void validateRouteProductExists(Long id) {
        if (routeProductMapper.selectById(id) == null) {
            throw exception(PRO_ROUTE_PRODUCT_NOT_EXISTS);
        }
    }

    private void validateRouteProductExists(MesProRouteProductDO routeProduct) {
        if (routeProduct == null) {
            throw exception(PRO_ROUTE_PRODUCT_NOT_EXISTS);
        }
    }

    private void validateItemUnique(Long id, Long itemId) {
        MesProRouteProductDO existing = routeProductMapper.selectByItemId(itemId);
        if (existing == null) {
            return;
        }
        if (ObjUtil.notEqual(existing.getId(), id)) {
            throw exception(PRO_ROUTE_PRODUCT_ITEM_DUPLICATE);
        }
    }

    private Map<String, Object> buildRouteProductBomsSnapshotForProductCopy(MesProRouteVersionDO candidateVersion,
                                                                            Long routeId,
                                                                            Long sourceProductId,
                                                                            Long targetProductId) {
        Map<String, Object> productBoms = resolveRouteProductBomsSnapshot(candidateVersion, routeId);
        boolean copiedFromSnapshot = false;
        for (Object value : List.copyOf(productBoms.values())) {
            Map<String, Object> bom = normalizeProductBomSnapshot(value);
            if (Objects.equals(String.valueOf(bom.get("productId")), String.valueOf(sourceProductId))) {
                putCopiedProductBomSnapshot(productBoms, bom, targetProductId);
                copiedFromSnapshot = true;
            }
        }
        if (copiedFromSnapshot) {
            return productBoms;
        }
        for (MesProRouteProductBomDO sourceBom : routeProductBomMapper.selectListByRouteIdAndProductId(
                routeId, sourceProductId)) {
            putCopiedProductBomSnapshot(productBoms, buildProductBomSnapshot(sourceBom), targetProductId);
        }
        return productBoms;
    }

    private Map<String, Object> buildRouteProductBomsSnapshotWithoutProduct(MesProRouteVersionDO candidateVersion,
                                                                            Long routeId,
                                                                            Long productId) {
        Map<String, Object> productBoms = resolveRouteProductBomsSnapshot(candidateVersion, routeId);
        productBoms.entrySet().removeIf(entry -> Objects.equals(
                String.valueOf(normalizeProductBomSnapshot(entry.getValue()).get("productId")),
                String.valueOf(productId)));
        return productBoms;
    }

    private void putCopiedProductBomSnapshot(Map<String, Object> productBoms, Map<String, Object> sourceBom,
                                             Long targetProductId) {
        Map<String, Object> targetBom = new LinkedHashMap<>(sourceBom);
        targetBom.put("id", null);
        targetBom.put("productId", targetProductId);
        productBoms.put(buildProductBomSnapshotKey(targetBom.get("processId"), targetBom.get("productId"),
                targetBom.get("itemId")), targetBom);
    }

    private Map<String, Object> buildProductBomSnapshot(MesProRouteProductBomDO bomDO) {
        Map<String, Object> bom = new LinkedHashMap<>();
        bom.put("id", bomDO.getId());
        bom.put("routeId", bomDO.getRouteId());
        bom.put("processId", bomDO.getProcessId());
        bom.put("productId", bomDO.getProductId());
        bom.put("itemId", bomDO.getItemId());
        bom.put("quantity", bomDO.getQuantity());
        bom.put("remark", bomDO.getRemark());
        return bom;
    }

    private Map<String, Object> resolveRouteProductBomsSnapshot(MesProRouteVersionDO candidateVersion, Long routeId) {
        Map<String, Object> productBoms = new LinkedHashMap<>();
        Object snapshot = resolveConfigSnapshot(candidateVersion, "productBoms");
        if (snapshot instanceof JSONObject bomsByKey) {
            for (Map.Entry<String, Object> entry : bomsByKey.entrySet()) {
                productBoms.put(entry.getKey(), normalizeProductBomSnapshot(entry.getValue()));
            }
        } else if (snapshot instanceof JSONArray bomList) {
            for (Object value : bomList) {
                Map<String, Object> bom = normalizeProductBomSnapshot(value);
                productBoms.put(buildProductBomSnapshotKey(bom.get("processId"), bom.get("productId"),
                        bom.get("itemId")), bom);
            }
        }
        if (!productBoms.isEmpty()) {
            return productBoms;
        }
        for (MesProRouteProductBomDO bom : routeProductBomMapper.selectList(routeId, null, null)) {
            productBoms.put(buildProductBomSnapshotKey(bom.getProcessId(), bom.getProductId(), bom.getItemId()),
                    buildProductBomSnapshot(bom));
        }
        return productBoms;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeProductBomSnapshot(Object value) {
        if (value instanceof JSONObject bom) {
            return new LinkedHashMap<>(bom);
        }
        return JSON.parseObject(JSON.toJSONString(value), LinkedHashMap.class);
    }

    private String buildProductBomSnapshotKey(Object processId, Object productId, Object itemId) {
        return String.valueOf(processId) + ":" + productId + ":" + itemId;
    }

    @Override
    public MesProRouteProductDO getRouteProduct(Long id) {
        return routeProductMapper.selectById(id);
    }

    @Override
    public MesProRouteProductDO getRouteProductByItemId(Long itemId) {
        return routeProductMapper.selectByItemId(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRouteProductByItem(Long itemId, Long routeId) {
        MesProRouteProductDO existing = routeProductMapper.selectByItemId(itemId);
        if (routeId == null) {
            if (existing == null) {
                return null;
            }
            routeService.validateRouteNotEnable(existing.getRouteId());
            routeProductBomService.deleteRouteProductBomByRouteIdAndProductId(existing.getRouteId(), itemId);
            routeProductMapper.deleteById(existing.getId());
            return null;
        }
        if (existing == null) {
            routeService.validateRouteNotEnable(routeId);
            MesProRouteProductDO routeProduct = MesProRouteProductDO.builder()
                    .routeId(routeId)
                    .itemId(itemId)
                    .quantity(1)
                    .productionTime(BigDecimal.ONE)
                    .timeUnitType("MINUTE")
                    .build();
            routeProductMapper.insert(routeProduct);
            return routeProduct.getId();
        }
        if (Objects.equals(existing.getRouteId(), routeId)) {
            return existing.getId();
        }
        routeService.validateRouteNotEnable(existing.getRouteId());
        routeService.validateRouteNotEnable(routeId);
        routeProductBomService.deleteRouteProductBomByRouteIdAndProductId(existing.getRouteId(), itemId);
        MesProRouteProductDO updateObj = BeanUtils.toBean(existing, MesProRouteProductDO.class);
        updateObj.setRouteId(routeId);
        routeProductMapper.updateById(updateObj);
        return updateObj.getId();
    }

    @Override
    public List<MesProRouteProductDO> getRouteProductListByRouteId(Long routeId) {
        return routeProductMapper.selectListByRouteId(routeId);
    }

    @Override
    public void deleteRouteProductByRouteId(Long routeId) {
        routeProductMapper.deleteByRouteId(routeId);
    }

}
