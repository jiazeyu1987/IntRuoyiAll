package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_BINDING_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PRODUCT_CLEAR_CONFIRM_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;

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
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    @Lazy
    private MesProRouteProductService routeProductService;
    @Resource
    private MesProRouteCandidateConfigService candidateConfigService;

    @Override
    @Transactional(readOnly = true)
    public MesRouteDccProjectBindingRespVO getBinding(Long routeId) {
        requireRoute(routeId);
        MesRouteDccProjectBindingDO current = bindingMapper.selectCurrentByRouteId(routeId);
        Long latestVersion = current != null ? current.getVersion() : currentVersion(routeId);
        return toResp(routeId, current, latestVersion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesRouteDccProjectBindingRespVO saveBinding(MesRouteDccProjectBindingSaveReqVO reqVO) {
        requireRouteForUpdate(reqVO.getRouteId());
        MesRouteDccProjectBindingDO current =
                bindingMapper.selectCurrentByRouteIdForUpdate(reqVO.getRouteId());
        Long currentVersion = current != null ? current.getVersion() : currentVersion(reqVO.getRouteId());
        requireExpectedVersion(reqVO.getRouteId(), reqVO.getExpectedVersion(), currentVersion);
        requireEnabledDccProjectCode(reqVO.getDccProjectCodeId());
        clearConfirmedRouteProducts(reqVO);
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

    private void clearConfirmedRouteProducts(MesRouteDccProjectBindingSaveReqVO reqVO) {
        List<MesProRouteProductDO> products = routeProductMapper.selectListByRouteId(reqVO.getRouteId());
        MesProRouteVersionDO draft = null;
        List<MesProRouteProductDO> allProducts = new ArrayList<>(products);
        if (reqVO.getRouteVersionId() != null) {
            draft = routeVersionMapper.selectById(reqVO.getRouteVersionId());
            if (draft == null) {
                throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, reqVO.getRouteVersionId());
            }
            if (!Objects.equals(draft.getRouteId(), reqVO.getRouteId())
                    || !Boolean.FALSE.equals(draft.getActive())
                    || !MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(draft.getLifecycleStatus())) {
                throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                        draft.getId(), draft.getLifecycleStatus());
            }
            allProducts.addAll(routeProductService.getRouteProductListByRouteId(reqVO.getRouteId(), draft.getId()));
        }
        List<Long> itemIds = allProducts.stream().map(MesProRouteProductDO::getItemId).distinct().toList();
        List<Long> confirmed = reqVO.getConfirmedProductItemIds();
        if (confirmed == null || itemIds.stream().anyMatch(Objects::isNull)
                || confirmed.stream().anyMatch(Objects::isNull)
                || confirmed.size() != itemIds.size()
                || !new HashSet<>(itemIds).equals(new HashSet<>(confirmed))) {
            throw exception(PRO_ROUTE_DCC_PRODUCT_CLEAR_CONFIRM_REQUIRED, reqVO.getRouteId());
        }
        if (draft != null) {
            candidateConfigService.saveConfigSnapshots(draft.getId(), draft.getRouteSnapshotSha256(),
                    Map.of("products", Map.of(), "productBoms", Map.of()));
        }
        if (!products.isEmpty()) {
            routeProductBomMapper.deleteByRouteId(reqVO.getRouteId());
            routeProductMapper.deleteByRouteId(reqVO.getRouteId());
        }
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
