package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

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

    public MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl(
            MesPqcInspectionTaskMapper taskMapper,
            MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesQaInspectionRegulationMapper regulationMapper,
            MesQaInspectionRegulationVersionMapper regulationVersionMapper,
            MesQaInspectionRegulationItemMapper regulationItemMapper,
            MesQaInspectionRegulationItemEquipmentMapper regulationItemEquipmentMapper) {
        this.taskMapper = taskMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
        this.eventMapper = eventMapper;
        this.pqcRecordMapper = pqcRecordMapper;
        this.reviewMapper = reviewMapper;
        this.regulationMapper = regulationMapper;
        this.regulationVersionMapper = regulationVersionMapper;
        this.regulationItemMapper = regulationItemMapper;
        this.regulationItemEquipmentMapper = regulationItemEquipmentMapper;
    }

    @Override
    public SourceBundle read(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command) {
        List<MesPqcInspectionTaskDO> tasks = taskMapper.selectListByActiveOrderId(command.getActiveOrderId());
        List<MesPqcProcessInspectionAggregateDetailDO> details =
                aggregateDetailMapper.selectListByActiveOrderId(command.getActiveOrderId());
        Map<Long, List<MesPqcProcessInspectionAggregateDetailDO>> detailsByTask = details.stream()
                .filter(Objects::nonNull)
                .filter(detail -> detail.getPqcTaskId() != null)
                .collect(Collectors.groupingBy(MesPqcProcessInspectionAggregateDetailDO::getPqcTaskId));
        List<InspectionSource> sources = tasks.stream()
                .filter(Objects::nonNull)
                .map(task -> readSource(command, task, detailsByTask.getOrDefault(task.getId(), List.of())))
                .toList();
        return new SourceBundle().setSources(sources);
    }

    private InspectionSource readSource(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
                                        MesPqcInspectionTaskDO task,
                                        List<MesPqcProcessInspectionAggregateDetailDO> details) {
        Long eventId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getEventId);
        Long reviewId = uniqueId(details, MesPqcProcessInspectionAggregateDetailDO::getReviewId);
        MesProProcessPoolEventDO event = eventId == null ? null : eventMapper.selectById(eventId);
        MesProProcessPoolPqcRecordDO record = eventId == null ? null : pqcRecordMapper.selectByEventId(eventId);
        MesProcessPoolSubmissionReviewDO review = reviewId == null ? null : reviewMapper.selectById(reviewId);
        MesQaInspectionRegulationDO regulation = regulationMapper.selectPublishedByRouteProcess(
                command.getProductId(), command.getRouteId(), command.getRouteVersionId(),
                task.getRouteProcessId(), task.getProcessId());
        MesQaInspectionRegulationVersionDO version = regulation == null || regulation.getCurrentVersionId() == null
                ? null : regulationVersionMapper.selectById(regulation.getCurrentVersionId());
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
                .setRegulationItemEquipment(equipment == null ? List.of() : List.copyOf(equipment));
    }

    private <T> Long uniqueId(List<T> values, Function<T, Long> extractor) {
        List<Long> ids = values.stream().map(extractor).filter(Objects::nonNull).distinct().toList();
        return ids.size() == 1 ? ids.get(0) : null;
    }
}
