package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

@Service
public class MesProRouteVersionLifecycleServiceImpl implements MesProRouteVersionLifecycleService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_READY_TO_PUBLISH = "READY_TO_PUBLISH";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUPERSEDED = "SUPERSEDED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final Set<String> PUBLISHABLE_STATUSES = Set.of(STATUS_READY_TO_PUBLISH);

    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteVersionPublishProjectionServiceImpl publishProjectionService;
    @Resource
    private MesProRouteControlledContentAdapter platformAdapter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO publishCandidate(Long candidateRouteVersionId) {
        return publishCandidate(candidateRouteVersionId, requireLoginUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProRouteVersionDO publishCandidate(Long candidateRouteVersionId, Long publisherUserId) {
        Long publisherId = requirePublisherUserId(publisherUserId);
        MesProRouteVersionDO candidate = routeVersionMapper.selectById(candidateRouteVersionId);
        if (candidate == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, candidateRouteVersionId);
        }
        if (isAlreadyActive(candidate)) {
            return candidate;
        }
        if (Boolean.TRUE.equals(candidate.getActive())
                || !PUBLISHABLE_STATUSES.contains(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        if (StrUtil.isBlank(candidate.getRouteSnapshotJson())
                || !MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(candidate.getRouteSnapshotJson())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }
        MesProRouteVersionDO active = routeVersionMapper.selectActiveByRouteId(candidate.getRouteId());
        if (active == null) {
            throw exception(PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS, candidate.getRouteId());
        }
        if (!Objects.equals(candidate.getSourceRouteVersionId(), active.getId())) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getSourceRouteVersionId(), active.getId());
        }

        publishProjectionService.projectCandidate(candidate);

        MesProRouteVersionDO superseded = new MesProRouteVersionDO();
        superseded.setId(active.getId());
        superseded.setActive(Boolean.FALSE);
        superseded.setLifecycleStatus(STATUS_SUPERSEDED);
        routeVersionMapper.updateById(superseded);

        LocalDateTime publishedTime = LocalDateTime.now();
        MesProRouteVersionDO published = new MesProRouteVersionDO();
        published.setId(candidate.getId());
        published.setActive(Boolean.TRUE);
        published.setLifecycleStatus(STATUS_ACTIVE);
        published.setPublishedBy(publisherId);
        published.setPublishedTime(publishedTime);
        routeVersionMapper.updateById(published);

        platformAdapter.recordPublished(active, candidate, publisherId);

        candidate.setActive(Boolean.TRUE);
        candidate.setLifecycleStatus(STATUS_ACTIVE);
        candidate.setPublishedBy(publisherId);
        candidate.setPublishedTime(publishedTime);
        return candidate;
    }

    private Long requireLoginUserId() {
        return requirePublisherUserId(SecurityFrameworkUtils.getLoginUserId());
    }

    private Long requirePublisherUserId(Long publisherUserId) {
        if (publisherUserId == null) {
            throw new IllegalStateException("route version publisher is required");
        }
        return publisherUserId;
    }

    private boolean isAlreadyActive(MesProRouteVersionDO candidate) {
        return Boolean.TRUE.equals(candidate.getActive())
                && STATUS_ACTIVE.equals(candidate.getLifecycleStatus());
    }

}
