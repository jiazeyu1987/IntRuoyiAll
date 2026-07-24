package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.productbom.MesProRouteProductBomSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdProductBomService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 工艺路线产品 BOM Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProRouteProductBomServiceImpl implements MesProRouteProductBomService {

    private static final String PRODUCT_BOMS_CONFIG_KEY = "productBoms";

    @Resource
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;

    @Resource
    @Lazy
    private MesProRouteProcessService routeProcessService;
    @Resource
    @Lazy
    private MesProRouteService routeService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdProductBomService productBomService;
    @Resource
    private MesProRouteCandidateConfigService routeCandidateConfigService;

    @Override
    public Long createRouteProductBom(MesProRouteProductBomSaveReqVO createReqVO) {
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(
                createReqVO.getRouteVersionId(), createReqVO.getRouteId());
        if (candidateVersion != null) {
            validateCandidateRouteProductBomSaveData(null, createReqVO, candidateVersion);
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), "productBoms",
                    buildRouteProductBomsSnapshotWith(candidateVersion, createReqVO.getRouteId(),
                            buildProductBomSnapshot(createReqVO)));
            return candidateVersion.getId();
        }
        // 1. 校验数据
        validateRouteProductBomSaveData(null, createReqVO);

        // 2. 插入
        MesProRouteProductBomDO routeProductBom = BeanUtils.toBean(createReqVO, MesProRouteProductBomDO.class);
        routeProductBomMapper.insert(routeProductBom);
        return routeProductBom.getId();
    }

    @Override
    public void updateRouteProductBom(MesProRouteProductBomSaveReqVO updateReqVO) {
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(
                updateReqVO.getRouteVersionId(), updateReqVO.getRouteId());
        if (candidateVersion != null) {
            validateRouteProductBomExists(updateReqVO.getId());
            validateCandidateRouteProductBomSaveData(updateReqVO.getId(), updateReqVO, candidateVersion);
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), "productBoms",
                    buildRouteProductBomsSnapshotWith(candidateVersion, updateReqVO.getRouteId(),
                            buildProductBomSnapshot(updateReqVO)));
            return;
        }
        // 1. 校验存在 + 校验数据
        validateRouteProductBomExists(updateReqVO.getId());
        validateRouteProductBomSaveData(updateReqVO.getId(), updateReqVO);

        // 2. 更新
        MesProRouteProductBomDO updateObj = BeanUtils.toBean(updateReqVO, MesProRouteProductBomDO.class);
        routeProductBomMapper.updateById(updateObj);
    }

    @Override
    public void deleteRouteProductBom(Long id, Long routeVersionId) {
        // 1.1 校验存在
        MesProRouteProductBomDO bom = routeProductBomMapper.selectById(id);
        if (bom == null) {
            throw exception(PRO_ROUTE_PRODUCT_BOM_NOT_EXISTS);
        }
        MesProRouteVersionDO candidateVersion = requireDraftCandidateVersion(routeVersionId, bom.getRouteId());
        if (candidateVersion != null) {
            Map<String, Object> boms = resolveRouteProductBomsSnapshot(candidateVersion, bom.getRouteId());
            boms.remove(buildProductBomSnapshotKey(bom.getProcessId(), bom.getProductId(), bom.getItemId()));
            routeCandidateConfigService.saveConfigSnapshot(candidateVersion.getId(), "productBoms", boms);
            return;
        }
        // 1.2 已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(bom.getRouteId());
        // 2. 删除
        routeProductBomMapper.deleteById(id);
    }

    private void validateRouteProductBomExists(Long id) {
        if (routeProductBomMapper.selectById(id) == null) {
            throw exception(PRO_ROUTE_PRODUCT_BOM_NOT_EXISTS);
        }
    }

    /**
     * 校验保存时的关联数据
     *
     * @param id    记录编号（新增时为 null）
     * @param reqVO 保存请求
     */
    private void validateRouteProductBomSaveData(Long id, MesProRouteProductBomSaveReqVO reqVO) {
        // 校验已启用的工艺路线，不允许操作
        routeService.validateRouteNotEnable(reqVO.getRouteId());
        // 校验唯一性
        validateBomUnique(id, reqVO.getItemId(), reqVO.getProcessId(), reqVO.getProductId());
        // 校验物料属于产品 BOM
        validateBomItemBelongsToProduct(reqVO.getProductId(), reqVO.getItemId());
    }

    private void validateCandidateRouteProductBomSaveData(Long id, MesProRouteProductBomSaveReqVO reqVO,
                                                          MesProRouteVersionDO candidateVersion) {
        validateBomUnique(id, reqVO.getItemId(), reqVO.getProcessId(), reqVO.getProductId(),
                resolveRouteProductBomsSnapshot(candidateVersion, reqVO.getRouteId()));
        validateBomItemBelongsToProduct(reqVO.getProductId(), reqVO.getItemId());
    }

    private void validateBomUnique(Long id, Long itemId, Long processId, Long productId) {
        MesProRouteProductBomDO existing = routeProductBomMapper.selectByUnique(itemId, processId, productId);
        if (existing == null) {
            return;
        }
        if (ObjUtil.notEqual(existing.getId(), id)) {
            throw exception(PRO_ROUTE_PRODUCT_BOM_DUPLICATE);
        }
    }

    private void validateBomUnique(Long id, Long itemId, Long processId, Long productId,
                                   Map<String, Object> candidateBoms) {
        String key = buildProductBomSnapshotKey(processId, productId, itemId);
        if (!candidateBoms.containsKey(key)) {
            return;
        }
        if (id == null) {
            throw exception(PRO_ROUTE_PRODUCT_BOM_DUPLICATE);
        }
        Map<String, Object> existing = normalizeProductBomSnapshot(candidateBoms.get(key));
        Object existingId = existing.get("id");
        if (existingId != null && !Objects.equals(String.valueOf(existingId), String.valueOf(id))) {
            throw exception(PRO_ROUTE_PRODUCT_BOM_DUPLICATE);
        }
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

    private Map<String, Object> buildProductBomSnapshot(MesProRouteProductBomSaveReqVO reqVO) {
        Map<String, Object> bom = new LinkedHashMap<>();
        bom.put("id", reqVO.getId());
        bom.put("routeId", reqVO.getRouteId());
        bom.put("processId", reqVO.getProcessId());
        bom.put("productId", reqVO.getProductId());
        bom.put("itemId", reqVO.getItemId());
        bom.put("quantity", reqVO.getQuantity());
        bom.put("remark", reqVO.getRemark());
        return bom;
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

    private Map<String, Object> buildRouteProductBomsSnapshotWith(MesProRouteVersionDO candidateVersion, Long routeId,
                                                                  Map<String, Object> bom) {
        Map<String, Object> boms = resolveRouteProductBomsSnapshot(candidateVersion, routeId);
        boms.put(buildProductBomSnapshotKey(bom.get("processId"), bom.get("productId"), bom.get("itemId")), bom);
        return boms;
    }

    private Map<String, Object> resolveRouteProductBomsSnapshot(MesProRouteVersionDO candidateVersion, Long routeId) {
        Map<String, Object> boms = new LinkedHashMap<>();
        Object snapshot = resolveConfigSnapshot(candidateVersion, PRODUCT_BOMS_CONFIG_KEY);
        if (snapshot instanceof JSONObject bomsByKey) {
            for (Map.Entry<String, Object> entry : bomsByKey.entrySet()) {
                boms.put(entry.getKey(), normalizeProductBomSnapshot(entry.getValue()));
            }
        } else if (snapshot instanceof JSONArray bomList) {
            for (Object value : bomList) {
                Map<String, Object> bom = normalizeProductBomSnapshot(value);
                boms.put(buildProductBomSnapshotKey(bom.get("processId"), bom.get("productId"), bom.get("itemId")), bom);
            }
        }
        if (!boms.isEmpty()) {
            return boms;
        }
        for (MesProRouteProductBomDO bom : routeProductBomMapper.selectList(routeId, null, null)) {
            boms.put(buildProductBomSnapshotKey(bom.getProcessId(), bom.getProductId(), bom.getItemId()),
                    buildProductBomSnapshot(bom));
        }
        return boms;
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
    private Map<String, Object> normalizeProductBomSnapshot(Object value) {
        if (value instanceof JSONObject bom) {
            return new LinkedHashMap<>(bom);
        }
        return JSON.parseObject(JSON.toJSONString(value), LinkedHashMap.class);
    }

    private String buildProductBomSnapshotKey(Object processId, Object productId, Object itemId) {
        return String.valueOf(processId) + ":" + productId + ":" + itemId;
    }

    private void validateBomItemBelongsToProduct(Long productId, Long itemId) {
        itemService.validateItemExists(itemId);
        if (!CollUtil.anyMatch(productBomService.getProductBomListByItemId(productId),
                productBom -> ObjUtil.equal(productBom.getBomItemId(), itemId))) {
            throw exception(MD_PRODUCT_BOM_ITEM_INVALID);
        }
    }

    @Override
    public MesProRouteProductBomDO getRouteProductBom(Long id) {
        return routeProductBomMapper.selectById(id);
    }

    @Override
    public List<MesProRouteProductBomDO> getRouteProductBomList(Long routeId, Long processId, Long productId) {
        return routeProductBomMapper.selectList(routeId, processId, productId);
    }

    @Override
    public List<MesProRouteProductBomDO> getRouteProductBomListByProcessIdentity(
            Long routeId, Long processId, Long productId) {
        return routeProductBomMapper.selectListByProcessIds(routeId,
                routeProcessService.getProcessIdentityMap(List.of(processId)).keySet(), productId);
    }

    @Override
    public void deleteRouteProductBomByRouteId(Long routeId) {
        routeProductBomMapper.deleteByRouteId(routeId);
    }

    @Override
    public void deleteRouteProductBomByRouteIdAndProductId(Long routeId, Long productId) {
        routeProductBomMapper.deleteByRouteIdAndProductId(routeId, productId);
    }

}
