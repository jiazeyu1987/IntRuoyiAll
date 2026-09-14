package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_BINDING_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;

@Service
@Validated
public class MesRouteDccProjectBindingServiceImpl implements MesRouteDccProjectBindingService {

    @Resource
    private MesRouteDccProjectBindingMapper bindingMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesMdItemMapper itemMapper;

    @Override
    @Transactional(readOnly = true)
    public MesRouteDccProjectBindingRespVO getBinding(Long routeId) {
        requireRoute(routeId);
        MesRouteDccProjectBindingDO current = bindingMapper.selectCurrentByRouteId(routeId);
        Long latestVersion = current != null ? current.getVersion() : currentVersion(routeId);
        return toResp(routeId, current, latestVersion);
    }

    @Override
    @Transactional
    public MesRouteDccProjectBindingRespVO saveBinding(MesRouteDccProjectBindingSaveReqVO reqVO) {
        requireRouteForUpdate(reqVO.getRouteId());
        MesRouteDccProjectBindingDO current =
                bindingMapper.selectCurrentByRouteIdForUpdate(reqVO.getRouteId());
        Long currentVersion = current != null ? current.getVersion() : currentVersion(reqVO.getRouteId());
        requireExpectedVersion(reqVO.getRouteId(), reqVO.getExpectedVersion(), currentVersion);
        DccProjectCodeDO projectCode = requireEnabledDccProjectCode(reqVO.getDccProjectCodeId());
        requireMatchingProductMaster(reqVO.getRouteId(), projectCode);
        if (current != null && Objects.equals(current.getDccProjectCodeId(), reqVO.getDccProjectCodeId())) {
            return toResp(reqVO.getRouteId(), current, currentVersion);
        }
        if (current != null) {
            bindingMapper.markDeletedById(current.getId());
        }
        MesRouteDccProjectBindingDO next = MesRouteDccProjectBindingDO.builder()
                .routeId(reqVO.getRouteId())
                .dccProjectCodeId(reqVO.getDccProjectCodeId())
                .version(currentVersion + 1)
                .build();
        next.setDeleted(false);
        bindingMapper.insert(next);
        return toResp(reqVO.getRouteId(), next, next.getVersion());
    }

    @Override
    @Transactional
    public MesRouteDccProjectBindingRespVO deleteBinding(Long routeId, Long expectedVersion) {
        requireRouteForUpdate(routeId);
        MesRouteDccProjectBindingDO current = bindingMapper.selectCurrentByRouteIdForUpdate(routeId);
        Long currentVersion = current != null ? current.getVersion() : currentVersion(routeId);
        requireExpectedVersion(routeId, expectedVersion, currentVersion);
        if (current == null) {
            return toResp(routeId, null, currentVersion);
        }
        bindingMapper.markDeletedById(current.getId());
        MesRouteDccProjectBindingDO tombstone = MesRouteDccProjectBindingDO.builder()
                .routeId(routeId)
                .dccProjectCodeId(current.getDccProjectCodeId())
                .version(currentVersion + 1)
                .build();
        tombstone.setDeleted(true);
        bindingMapper.insert(tombstone);
        return toResp(routeId, null, tombstone.getVersion());
    }

    private MesProRouteDO requireRouteForUpdate(Long routeId) {
        MesProRouteDO route = routeMapper.selectByIdForUpdate(routeId);
        if (route == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
        return route;
    }

    private MesProRouteDO requireRoute(Long routeId) {
        MesProRouteDO route = routeMapper.selectById(routeId);
        if (route == null) {
            throw exception(PRO_ROUTE_NOT_EXISTS);
        }
        return route;
    }

    private DccProjectCodeDO requireEnabledDccProjectCode(Long dccProjectCodeId) {
        DccProjectCodeDO projectCode = dccProjectCodeMapper.selectById(dccProjectCodeId);
        if (projectCode == null || !DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw exception(PRO_ROUTE_DCC_PROJECT_INVALID, dccProjectCodeId);
        }
        return projectCode;
    }

    private void requireMatchingProductMaster(Long routeId, DccProjectCodeDO projectCode) {
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteId(routeId);
        List<Long> itemIds = routeProducts == null ? List.of() : routeProducts.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesMdItemDO> itemsById = itemIds.isEmpty() ? Map.of() : itemMapper.selectListByIds(itemIds).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(MesMdItemDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        MesRouteDccProductMasterInvariant.requireMatching(routeId, routeProducts, itemsById, projectCode);
    }

    private Long currentVersion(Long routeId) {
        Long maxVersion = bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(routeId);
        return maxVersion == null ? 0L : maxVersion;
    }

    private void requireExpectedVersion(Long routeId, Long expectedVersion, Long currentVersion) {
        if (!Objects.equals(expectedVersion, currentVersion)) {
            throw exception(PRO_ROUTE_DCC_BINDING_VERSION_CONFLICT, routeId, expectedVersion, currentVersion);
        }
    }

    private MesRouteDccProjectBindingRespVO toResp(Long routeId, MesRouteDccProjectBindingDO current,
                                                   Long version) {
        return new MesRouteDccProjectBindingRespVO()
                .setRouteId(routeId)
                .setDccProjectCodeId(current == null ? null : current.getDccProjectCodeId())
                .setVersion(version == null ? 0L : version)
                .setBound(current != null);
    }
}
