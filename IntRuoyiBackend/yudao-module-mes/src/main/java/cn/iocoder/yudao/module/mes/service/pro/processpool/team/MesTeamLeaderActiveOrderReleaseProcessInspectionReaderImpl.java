package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
import org.springframework.stereotype.Service;

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
    private final ActiveOrderSnapshotResolver activeOrderSnapshotResolver;
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
            ActiveOrderSnapshotResolver activeOrderSnapshotResolver,
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
        this.activeOrderSnapshotResolver = activeOrderSnapshotResolver;
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.qaProvenancePort = qaProvenancePort;
    }

    @Override
    public SourceBundle read(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        LockedDccQa lockedDccQa = resolveLockedDccQa(command);
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
                        lockedDccQa))
                .toList();
        return new SourceBundle().setSources(sources);
    }

    private InspectionSource readSource(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
                                        MesPqcInspectionTaskDO task,
                                        List<MesPqcProcessInspectionAggregateDetailDO> details,
                                        LockedDccQa lockedDccQa) {
        DccProjectCodeDO dccProject = lockedDccQa.project();
        Long eventId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getEventId);
        Long reviewId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getReviewId);
        MesProProcessPoolEventDO event = eventId == null ? null : eventMapper.selectById(eventId);
        MesProProcessPoolPqcRecordDO record = eventId == null ? null : pqcRecordMapper.selectByEventId(eventId);
        MesProcessPoolSubmissionReviewDO review = reviewId == null ? null : reviewMapper.selectById(reviewId);
        PublishedQa publishedQa = selectLockedQa(lockedDccQa);
        MesQaInspectionRegulationDO regulation = publishedQa.regulation();
        MesQaInspectionRegulationVersionDO version = publishedQa.version();
        List<MesQaInspectionRegulationItemDO> items = version == null ? List.of()
                : regulationItemMapper.selectListByVersionId(version.getId());
        return new InspectionSource()
                .setTask(task)
                .setAggregateDetails(List.copyOf(details))
                .setEvent(event)
                .setPqcRecord(record)
                .setReview(review)
                .setRegulation(regulation)
                .setRegulationVersion(version)
                .setRegulationItems(items == null ? List.of() : List.copyOf(items))
                .setDccProject(dccProject)
                .setRouteProjectCode(lockedDccQa.projectCode())
                .setQaDccProvenance(publishedQa.provenance());
    }

    private LockedDccQa resolveLockedDccQa(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        if (command == null) {
            throw new IllegalStateException("活跃订单放行命令不能为空");
        }
        ActiveOrderSnapshotResolver.ActiveOrderSnapshot snapshot =
                activeOrderSnapshotResolver.requireEffective(command.getActiveOrderId());
        if (!Objects.equals(command.getRouteId(), snapshot.routeId())
                || !Objects.equals(command.getRouteVersionId(), snapshot.routeVersionId())
                || (command.getWorkOrderId() != null
                && !Objects.equals(command.getWorkOrderId(), snapshot.workOrderId()))) {
            throw new IllegalStateException("活跃订单冻结路线身份与放行命令不一致，activeOrderId="
                    + command.getActiveOrderId());
        }
        DccProjectCodeDO project = dccProjectCodeMapper.selectById(snapshot.dccProjectCodeId());
        if (project == null || project.getId() == null || Boolean.TRUE.equals(project.getDeleted())
                || (command.getTenantId() != null && !Objects.equals(command.getTenantId(), project.getTenantId()))) {
            throw new IllegalStateException("活跃订单冻结 DCC 项目身份无效，activeOrderId="
                    + command.getActiveOrderId() + "，dccProjectCodeId=" + snapshot.dccProjectCodeId());
        }
        return new LockedDccQa(StrUtil.trim(project.getProjectCode()), project,
                snapshot.qaRegulationId(), snapshot.qaRegulationVersionId());
    }

    private PublishedQa selectLockedQa(LockedDccQa lockedDccQa) {
        DccProjectCodeDO dccProject = lockedDccQa.project();
        MesQaInspectionRegulationDO regulation = regulationMapper.selectById(lockedDccQa.qaRegulationId());
        if (regulation == null || regulation.getId() == null
                || !Objects.equals(dccProject.getId(), regulation.getDccProjectCodeId())
                || !MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA.equals(regulation.getOwnerModule())
                || !"PUBLISHED".equals(regulation.getLifecycleStatus())) {
            throw new IllegalStateException("活跃订单冻结 QA 规程身份无效，qaRegulationId="
                    + lockedDccQa.qaRegulationId());
        }
        MesQaInspectionRegulationVersionDO version = regulationVersionMapper
                .selectById(lockedDccQa.qaRegulationVersionId());
        if (version == null || version.getId() == null || version.getPublishedAt() == null
                || !Objects.equals(regulation.getId(), version.getRegulationId())
                || !Set.of("PUBLISHED", "RETIRED").contains(version.getLifecycleStatus())) {
            throw new IllegalStateException("活跃订单冻结 QA 版本身份无效，qaRegulationVersionId="
                    + lockedDccQa.qaRegulationVersionId());
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

    private record LockedDccQa(String projectCode,
                               DccProjectCodeDO project,
                               Long qaRegulationId,
                               Long qaRegulationVersionId) {
    }
}
