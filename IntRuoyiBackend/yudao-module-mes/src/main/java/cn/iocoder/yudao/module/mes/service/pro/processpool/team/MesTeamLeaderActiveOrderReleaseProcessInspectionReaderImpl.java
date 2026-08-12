package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl
        implements MesTeamLeaderActiveOrderReleaseProcessInspectionReader {

    private final MesPqcInspectionTaskMapper taskMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesQaInspectionRegulationMapper regulationMapper;
    private final MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    private final MesQaInspectionRegulationItemMapper regulationItemMapper;
    private final MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesMdItemService itemService;
    private final DccProjectCodeMapper dccProjectCodeMapper;
    private final MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort qaProvenancePort;

    public MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl(
            MesPqcInspectionTaskMapper taskMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesQaInspectionRegulationMapper regulationMapper,
            MesQaInspectionRegulationVersionMapper regulationVersionMapper,
            MesQaInspectionRegulationItemMapper regulationItemMapper,
            MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesMdItemService itemService,
            DccProjectCodeMapper dccProjectCodeMapper,
            MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort qaProvenancePort) {
        this.taskMapper = taskMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
        this.eventMapper = eventMapper;
        this.pqcRecordMapper = pqcRecordMapper;
        this.reviewMapper = reviewMapper;
        this.regulationMapper = regulationMapper;
        this.regulationVersionMapper = regulationVersionMapper;
        this.regulationItemMapper = regulationItemMapper;
        this.regulationItemEquipmentMapper = regulationItemEquipmentMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.itemService = itemService;
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.qaProvenancePort = qaProvenancePort;
    }

    @Override
    public SourceBundle read(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        RouteDccProject routeDccProject = resolveRouteDccProject(command);
        List<MesPqcInspectionTaskDO> tasks = taskMapper.selectListByActiveOrderId(command.getActiveOrderId());
        List<MesPqcProcessInspectionAggregateDetailDO> details =
                aggregateDetailMapper.selectListByActiveOrderId(command.getActiveOrderId());
        Map<Long, List<MesPqcProcessInspectionAggregateDetailDO>> detailsByTask = details.stream()
                .filter(Objects::nonNull)
                .filter(detail -> detail.getPqcTaskId() != null)
                .collect(Collectors.groupingBy(MesPqcProcessInspectionAggregateDetailDO::getPqcTaskId));
        List<InspectionSource> sources = tasks.stream()
                .filter(Objects::nonNull)
                .map(task -> readSource(command, task, detailsByTask.getOrDefault(task.getId(), List.of()),
                        routeDccProject))
                .toList();
        return new SourceBundle().setSources(sources);
    }

    private InspectionSource readSource(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
                                        MesPqcInspectionTaskDO task,
                                        List<MesPqcProcessInspectionAggregateDetailDO> details,
                                        RouteDccProject routeDccProject) {
        DccProjectCodeDO dccProject = routeDccProject.project();
        Long eventId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getEventId);
        Long reviewId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getReviewId);
        MesProProcessPoolEventDO event = eventId == null ? null : eventMapper.selectById(eventId);
        MesProProcessPoolPqcRecordDO record = eventId == null ? null : pqcRecordMapper.selectByEventId(eventId);
        MesProcessPoolSubmissionReviewDO review = reviewId == null ? null : reviewMapper.selectById(reviewId);
        PublishedQa publishedQa = dccProject == null ? PublishedQa.EMPTY
                : selectPublishedQa(dccProject);
        MesQaInspectionRegulationDO regulation = publishedQa.regulation();
        MesQaInspectionRegulationVersionDO version = publishedQa.version();
        List<MesQaInspectionRegulationItemDO> items = version == null ? List.of()
                : regulationItemMapper.selectListByVersionId(version.getId());
        List<MesQaInspectionRegulationItemEquipmentDO> equipment = version == null ? List.of()
                : regulationItemEquipmentMapper.selectListByVersionId(version.getId());
        return new InspectionSource()
                .setTask(task)
                .setAggregateDetails(List.copyOf(details))
                .setEvent(event)
                .setPqcRecord(record)
                .setReview(review)
                .setRegulation(regulation)
                .setRegulationVersion(version)
                .setRegulationItems(items == null ? List.of() : List.copyOf(items))
                .setRegulationItemEquipment(equipment == null ? List.of() : List.copyOf(equipment))
                .setDccProject(dccProject)
                .setRouteProjectCode(routeDccProject.projectCode())
                .setQaDccProvenance(publishedQa.provenance());
    }

    private RouteDccProject resolveRouteDccProject(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        if (command == null || command.getRouteId() == null || command.getRouteVersionId() == null
                || command.getProductId() == null) {
            return RouteDccProject.EMPTY;
        }
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(command.getRouteVersionId());
        if (!isPublishedRouteVersion(command.getRouteId(), routeVersion)) {
            return RouteDccProject.EMPTY;
        }
        Set<Long> routeItemIds = parseRouteVersionProductIds(routeVersion);
        if (!routeItemIds.contains(command.getProductId())) {
            return RouteDccProject.EMPTY;
        }
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(routeItemIds);
        Set<String> routeItemCodes = (itemMap == null ? Map.<Long, MesMdItemDO>of() : itemMap).values().stream()
                .filter(Objects::nonNull)
                .map(MesMdItemDO::getCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<DccProjectCodeDO> enabledProjects = dccProjectCodeMapper.selectEnabledList();
        Map<Long, DccProjectCodeDO> matches = (enabledProjects == null
                ? List.<DccProjectCodeDO>of() : enabledProjects).stream()
                .filter(Objects::nonNull)
                .filter(project -> project.getId() != null)
                .filter(project -> routeItemCodes.contains(StrUtil.trim(project.getProjectCode())))
                .collect(Collectors.toMap(DccProjectCodeDO::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        if (matches.size() != 1) {
            return RouteDccProject.EMPTY;
        }
        DccProjectCodeDO project = matches.values().iterator().next();
        return new RouteDccProject(StrUtil.trim(project.getProjectCode()), project);
    }

    private boolean isPublishedRouteVersion(Long routeId, MesProRouteVersionDO routeVersion) {
        if (routeVersion == null || !Objects.equals(routeId, routeVersion.getRouteId())) {
            return false;
        }
        return (Boolean.TRUE.equals(routeVersion.getActive()) && "ACTIVE".equals(routeVersion.getLifecycleStatus()))
                || (Boolean.FALSE.equals(routeVersion.getActive())
                && "SUPERSEDED".equals(routeVersion.getLifecycleStatus()));
    }

    private Set<Long> parseRouteVersionProductIds(MesProRouteVersionDO routeVersion) {
        try {
            JSONObject routeSnapshot = JSONObject.parseObject(routeVersion.getRouteSnapshotJson());
            JSONObject configSnapshots = routeSnapshot == null ? null : routeSnapshot.getJSONObject("configSnapshots");
            Object rawProducts = configSnapshots == null ? null : configSnapshots.get("products");
            Collection<?> products;
            if (rawProducts instanceof JSONObject productsByKey) {
                products = productsByKey.values();
            } else if (rawProducts instanceof JSONArray productsArray) {
                products = productsArray;
            } else {
                throw invalidRouteSnapshot(routeVersion, "configSnapshots.products 缺失或类型无效", null);
            }
            Set<Long> itemIds = new LinkedHashSet<>();
            for (Object value : products) {
                if (!(value instanceof JSONObject product)) {
                    throw invalidRouteSnapshot(routeVersion, "configSnapshots.products 包含非对象元素", null);
                }
                Long itemId = product.getLong("itemId");
                if (itemId == null || itemId <= 0) {
                    throw invalidRouteSnapshot(routeVersion, "configSnapshots.products.itemId 无效", null);
                }
                itemIds.add(itemId);
            }
            return Set.copyOf(itemIds);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw invalidRouteSnapshot(routeVersion, "JSON 解析失败", ex);
        }
    }

    private IllegalStateException invalidRouteSnapshot(MesProRouteVersionDO routeVersion, String reason,
                                                        RuntimeException cause) {
        String message = "已发布工艺路线版本快照无效，routeVersionId="
                + (routeVersion == null ? null : routeVersion.getId()) + "，reason=" + reason;
        return cause == null ? new IllegalStateException(message) : new IllegalStateException(message, cause);
    }

    private PublishedQa selectPublishedQa(DccProjectCodeDO dccProject) {
        MesQaInspectionRegulationDO regulation = regulationMapper.selectByDccProjectCodeId(dccProject.getId());
        if (regulation == null || regulation.getId() == null || regulation.getCurrentVersionId() == null
                || !"PUBLISHED".equals(regulation.getLifecycleStatus())) {
            return PublishedQa.EMPTY;
        }
        MesQaInspectionRegulationVersionDO version = regulationVersionMapper
                .selectById(regulation.getCurrentVersionId());
        if (version == null || version.getId() == null || version.getPublishedAt() == null
                || !"PUBLISHED".equals(version.getLifecycleStatus())) {
            return PublishedQa.EMPTY;
        }
        MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution provenance =
                qaProvenancePort.verify(dccProject, regulation, version);
        if (provenance != null && provenance.isVerifiedFor(dccProject, regulation, version)) {
            return new PublishedQa(regulation, version, provenance);
        }
        if (provenance == null) {
            provenance = new MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution()
                    .setBlockerType("PQC_DCC_QA_PROVENANCE_REQUIRED")
                    .setBlockerMessage("QA 规程缺少可验证的 DCC 项目直接归属关系");
        }
        return new PublishedQa(null, null, provenance);
    }

    private <T> Long uniqueId(List<T> values, Function<T, Long> extractor) {
        List<Long> ids = values.stream().map(extractor).filter(Objects::nonNull).distinct().toList();
        return ids.size() == 1 ? ids.get(0) : null;
    }

    private record PublishedQa(MesQaInspectionRegulationDO regulation,
                               MesQaInspectionRegulationVersionDO version,
                               MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution
                                       provenance) {

        private static final PublishedQa EMPTY = new PublishedQa(null, null, null);
    }

    private record RouteDccProject(String projectCode, DccProjectCodeDO project) {

        private static final RouteDccProject EMPTY = new RouteDccProject(null, null);
    }
}
