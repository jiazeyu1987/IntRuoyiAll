package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED;

@Service
@Validated
public class MesTeamLeaderActiveOrderDetailServiceImpl implements MesTeamLeaderActiveOrderDetailService {

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderDetailReadMapper detailReadMapper;

    public MesTeamLeaderActiveOrderDetailServiceImpl(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                      MesProcessPoolActiveOrderDetailReadMapper detailReadMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.detailReadMapper = detailReadMapper;
    }

    @Override
    public MesTeamLeaderActiveOrderDetail getDetail(Long leaderUserId, Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null
                || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)
                || !MesTeamLeaderActiveOrderServiceImpl.STATUS_ACTIVE.equals(activeOrder.getActiveStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        List<MesTeamLeaderActiveOrderDetailReadDO> rows = detailReadMapper.selectByActiveOrderId(activeOrderId);
        if (rows == null || rows.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        MesTeamLeaderActiveOrderDetailReadDO first = rows.get(0);
        requireText(first.getWorkOrderCode(), activeOrderId);
        requireText(first.getRouteName(), activeOrderId);

        Map<ProcessIdentity, ProcessAccumulator> accumulators = new LinkedHashMap<>();
        for (MesTeamLeaderActiveOrderDetailReadDO row : rows) {
            validateProcessRow(row, activeOrderId);
            ProcessIdentity identity = new ProcessIdentity(row.getRouteProcessId(), row.getProcessId());
            ProcessAccumulator accumulator = accumulators.computeIfAbsent(identity,
                    ignored -> new ProcessAccumulator(row));
            accumulator.addSubmission(row, activeOrderId);
        }
        return new MesTeamLeaderActiveOrderDetail()
                .setActiveOrderId(activeOrderId)
                .setWorkOrderId(first.getWorkOrderId())
                .setWorkOrderCode(first.getWorkOrderCode())
                .setRouteName(first.getRouteName())
                .setProcesses(accumulators.values().stream().map(ProcessAccumulator::toDetail).toList());
    }

    private static void validateProcessRow(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
        if (row == null || row.getSnapshotId() == null || row.getRouteProcessId() == null || row.getProcessId() == null
                || row.getRequiredQuantity() == null || row.getRequiredQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
        requireText(row.getProcessName(), activeOrderId);
    }

    private static void requireText(String value, Long activeOrderId) {
        if (value == null || value.isBlank()) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
        }
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private static final class ProcessAccumulator {
        private final MesTeamLeaderActiveOrderDetail.ProcessDetail process;
        private final List<MesTeamLeaderActiveOrderDetail.SubmissionDetail> submissions = new ArrayList<>();
        private BigDecimal submittedQuantity = BigDecimal.ZERO;

        private ProcessAccumulator(MesTeamLeaderActiveOrderDetailReadDO row) {
            this.process = new MesTeamLeaderActiveOrderDetail.ProcessDetail()
                    .setRouteProcessId(row.getRouteProcessId())
                    .setProcessId(row.getProcessId())
                    .setProcessCode(row.getProcessCode())
                    .setProcessName(row.getProcessName())
                    .setRequiredQuantity(row.getRequiredQuantity());
        }

        private void addSubmission(MesTeamLeaderActiveOrderDetailReadDO row, Long activeOrderId) {
            if (row.getEventId() == null) {
                return;
            }
            if (row.getSubmittedQuantity() == null || row.getSubmittedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            requireText(row.getSubmitterName(), activeOrderId);
            if (row.getSubmittedAt() == null) {
                throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED, activeOrderId);
            }
            submissions.add(new MesTeamLeaderActiveOrderDetail.SubmissionDetail()
                    .setEventId(row.getEventId())
                    .setSubmittedQuantity(row.getSubmittedQuantity())
                    .setSubmitterName(row.getSubmitterName())
                    .setReviewerName(row.getReviewerName())
                    .setSubmittedAt(row.getSubmittedAt()));
            submittedQuantity = submittedQuantity.add(row.getSubmittedQuantity());
        }

        private MesTeamLeaderActiveOrderDetail.ProcessDetail toDetail() {
            return process
                    .setSubmittedQuantity(submittedQuantity)
                    .setSubmissionCount(submissions.size())
                    .setSubmissions(List.copyOf(submissions));
        }
    }
}
