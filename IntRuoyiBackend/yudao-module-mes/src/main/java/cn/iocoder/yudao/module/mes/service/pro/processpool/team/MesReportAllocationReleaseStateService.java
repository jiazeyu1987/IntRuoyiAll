package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MesReportAllocationReleaseStateService {

    private static final String RELEASED = "RELEASED";

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrReleaseTransactionMapper transactionMapper;

    public MesReportAllocationReleaseStateService(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProEdhrReleaseTransactionMapper transactionMapper) {
        this.applicationMapper = applicationMapper;
        this.transactionMapper = transactionMapper;
    }

    public Set<Long> findReleasedActiveOrderIds(Collection<Long> activeOrderIds) {
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications =
                applicationMapper.selectListByActiveOrderIds(activeOrderIds);
        return resolveReleasedActiveOrderIds(applications, false);
    }

    public Set<Long> findReleasedActiveOrderIdsForUpdate(Collection<Long> activeOrderIds) {
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications =
                applicationMapper.selectListByActiveOrderIdsForUpdate(activeOrderIds);
        return resolveReleasedActiveOrderIds(applications, true);
    }

    private Set<Long> resolveReleasedActiveOrderIds(
            List<MesProcessPoolActiveOrderReleaseApplicationDO> applications, boolean forUpdate) {
        if (applications == null || applications.isEmpty()) {
            return Set.of();
        }
        List<Long> transactionIds = applications.stream()
                .map(MesProcessPoolActiveOrderReleaseApplicationDO::getReleaseTransactionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (transactionIds.isEmpty()) {
            return Set.of();
        }
        List<MesProEdhrReleaseTransactionDO> transactions = forUpdate
                ? transactionMapper.selectListByIdsForUpdate(transactionIds)
                : transactionMapper.selectListByIds(transactionIds);
        Map<Long, MesProEdhrReleaseTransactionDO> byId = transactions.stream()
                .collect(Collectors.toMap(MesProEdhrReleaseTransactionDO::getId, Function.identity(), (a, b) -> a,
                        HashMap::new));
        Set<Long> released = new HashSet<>();
        for (MesProcessPoolActiveOrderReleaseApplicationDO application : applications) {
            MesProEdhrReleaseTransactionDO transaction = byId.get(application.getReleaseTransactionId());
            if (transaction != null && RELEASED.equals(transaction.getReleaseStatus())) {
                released.add(application.getActiveOrderId());
            }
        }
        return Set.copyOf(released);
    }
}
