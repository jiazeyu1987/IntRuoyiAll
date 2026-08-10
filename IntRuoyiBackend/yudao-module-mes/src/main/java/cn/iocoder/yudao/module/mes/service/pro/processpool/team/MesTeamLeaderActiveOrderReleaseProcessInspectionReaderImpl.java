package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        this.dccProjectCodeMapper = dccProjectCodeMapper;
        this.qaProvenancePort = qaProvenancePort;
    }

    @Override
    public SourceBundle read(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        DccProjectCodeDO dccProject = resolveUniqueDccProject(command.getProductId());
        List<MesPqcInspectionTaskDO> tasks = taskMapper.selectListByActiveOrderId(command.getActiveOrderId());
        List<MesPqcProcessInspectionAggregateDetailDO> details =
                aggregateDetailMapper.selectListByActiveOrderId(command.getActiveOrderId());
        Map<Long, List<MesPqcProcessInspectionAggregateDetailDO>> detailsByTask = details.stream()
                .filter(Objects::nonNull)
                .filter(detail -> detail.getPqcTaskId() != null)
                .collect(Collectors.groupingBy(MesPqcProcessInspectionAggregateDetailDO::getPqcTaskId));
        List<InspectionSource> sources = tasks.stream()
                .filter(Objects::nonNull)
                .map(task -> readSource(command, task, detailsByTask.getOrDefault(task.getId(), List.of()), dccProject))
                .toList();
        return new SourceBundle().setSources(sources);
    }

    private InspectionSource readSource(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
                                        MesPqcInspectionTaskDO task,
                                        List<MesPqcProcessInspectionAggregateDetailDO> details,
                                        DccProjectCodeDO dccProject) {
        Long eventId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getEventId);
        Long reviewId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getReviewId);
        MesProProcessPoolEventDO event = eventId == null ? null : eventMapper.selectById(eventId);
        MesProProcessPoolPqcRecordDO record = eventId == null ? null : pqcRecordMapper.selectByEventId(eventId);
        MesProcessPoolSubmissionReviewDO review = reviewId == null ? null : reviewMapper.selectById(reviewId);
        PublishedQa publishedQa = dccProject == null ? PublishedQa.EMPTY
                : selectLatestPublishedQa(command, task, dccProject);
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
                .setQaDccProvenance(publishedQa.provenance());
    }

    private DccProjectCodeDO resolveUniqueDccProject(Long productId) {
        if (productId == null) {
            return null;
        }
        List<DccProjectCodeDO> enabledProjects = dccProjectCodeMapper.selectEnabledList();
        List<DccProjectCodeDO> matches = (enabledProjects == null ? List.<DccProjectCodeDO>of() : enabledProjects).stream()
                .filter(Objects::nonNull)
                .filter(project -> Objects.equals(productId, project.getProductMasterId()))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private PublishedQa selectLatestPublishedQa(
            MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
            MesPqcInspectionTaskDO task,
            DccProjectCodeDO dccProject) {
        List<MesQaInspectionRegulationDO> regulations = regulationMapper.selectPublishedListByStableProcess(
                command.getProductId(), command.getRouteId(), task.getProcessId());
        List<PublishedQa> verified = new ArrayList<>();
        List<PublishedQa> blocked = new ArrayList<>();
        for (MesQaInspectionRegulationDO regulation : regulations == null ? List.<MesQaInspectionRegulationDO>of()
                : regulations) {
            if (regulation == null || regulation.getId() == null || regulation.getCurrentVersionId() == null) {
                continue;
            }
            MesQaInspectionRegulationVersionDO version = regulationVersionMapper
                    .selectById(regulation.getCurrentVersionId());
            if (version == null || version.getId() == null || version.getPublishedAt() == null
                    || !"PUBLISHED".equals(version.getLifecycleStatus())) {
                continue;
            }
            MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution provenance =
                    qaProvenancePort.verify(dccProject, regulation, version);
            PublishedQa candidate = new PublishedQa(regulation, version, provenance);
            if (provenance != null && provenance.isVerifiedFor(dccProject, regulation, version)) {
                verified.add(candidate);
            } else {
                blocked.add(candidate);
            }
        }
        Comparator<PublishedQa> latest = Comparator
                .comparing((PublishedQa value) -> value.version().getPublishedAt())
                .thenComparing(value -> value.version().getId())
                .thenComparing(value -> value.regulation().getId());
        PublishedQa selected = verified.stream()
                .max(latest)
                .orElse(null);
        if (selected != null) {
            return selected;
        }
        MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution blockedProvenance = blocked.stream()
                .max(Comparator.comparing((PublishedQa value) -> value.version().getPublishedAt())
                        .thenComparing(value -> value.version().getId())
                        .thenComparing(value -> value.regulation().getId()))
                .map(PublishedQa::provenance)
                .orElse(null);
        if (blockedProvenance == null && !blocked.isEmpty()) {
            blockedProvenance = new MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution()
                    .setBlockerType("PQC_DCC_QA_PROVENANCE_REQUIRED")
                    .setBlockerMessage("QA 版本缺少可验证的显式 DCC 项目来源关系");
        }
        return new PublishedQa(null, null, blockedProvenance);
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
}
