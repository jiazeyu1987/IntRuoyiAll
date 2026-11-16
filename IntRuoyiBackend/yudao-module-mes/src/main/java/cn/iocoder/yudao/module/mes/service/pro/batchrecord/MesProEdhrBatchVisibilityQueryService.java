package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchVisibilityMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchVisibilityQuery;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchVisibilitySql;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTraceSearchSql;
import jakarta.annotation.Resource;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID;

@Service
public class MesProEdhrBatchVisibilityQueryService {
    @Resource private MesProEdhrBatchExecutionMapper batchMapper;
    @Resource private MesProEdhrBatchVisibilityMapper visibilityMapper;
    @Resource private MesProEdhrBatchExecutionVisibilityService visibilityService;
    @Resource private MesProEdhrCandidateResolver candidateResolver;

    public MesProEdhrBatchVisibilityQuery prepare(EdhrBatchExecutionPageReqVO request, Long userId) {
        if (cn.hutool.core.util.StrUtil.isNotBlank(request.getTracePerson())) {
            throw new IllegalArgumentException("EDHR_TRACE_IDENTITY_SCOPE_REQUIRED");
        }
        return prepare(request, userId, MesProEdhrTraceSearchScope.empty());
    }

    public MesProEdhrBatchVisibilityQuery prepare(EdhrBatchExecutionPageReqVO request, Long userId,
                                                 MesProEdhrTraceSearchScope traceScope) {
        java.util.Objects.requireNonNull(traceScope, "EDHR_TRACE_IDENTITY_SCOPE_REQUIRED");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        boolean overview = visibilityService.hasOverviewPermission(userId);
        if (overview) return new MesProEdhrBatchVisibilityQuery(tenantId, userId, true, List.of());
        if (visibilityMapper.exists(query(request, tenantId, traceScope).apply(MesProEdhrBatchVisibilitySql.invalidManagerSnapshot(userId))))
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        if (visibilityMapper.exists(query(request, tenantId, traceScope).apply(MesProEdhrBatchVisibilitySql.duplicateApplication(userId))))
            throw new TooManyResultsException("Expected one formal batch release application");
        if (visibilityMapper.exists(query(request, tenantId, traceScope).apply(MesProEdhrBatchVisibilitySql.invalidPqcSnapshot(userId))))
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        if (visibilityMapper.exists(query(request, tenantId, traceScope).apply(MesProEdhrBatchVisibilitySql.duplicateReport(userId))))
            throw new TooManyResultsException("Expected one bound batch record report");
        var rules = visibilityMapper.selectEffectiveRules(query(request, tenantId, traceScope)
                .apply(MesProEdhrBatchVisibilitySql.handoverReadable(userId))
                .apply("NOT " + MesProEdhrBatchVisibilitySql.earlierGrant(userId))
                .apply(MesProEdhrBatchVisibilitySql.taskForRule()));
        Map<Source, Boolean> membership = new HashMap<>();
        List<Long> ruleIds = new ArrayList<>();
        for (var rule : rules) {
            Source source = new Source(rule.getCandidateSourceType(), rule.getCandidateSourceIds());
            boolean member = membership.computeIfAbsent(source, ignored ->
                    MesProEdhrWorkTaskAuthorization.containsCandidate(
                            candidateResolver.resolveProcessFormRule(rule).userSnapshot(), userId));
            if (member) ruleIds.add(rule.getId());
        }
        return new MesProEdhrBatchVisibilityQuery(tenantId, userId, false, ruleIds);
    }

    private LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query(EdhrBatchExecutionPageReqVO request, Long tenantId,
                                                               MesProEdhrTraceSearchScope traceScope) {
        var query = batchMapper.buildPageQuery(request);
        MesProEdhrTraceSearchSql.apply(query, request, traceScope);
        query.eq(MesProEdhrBatchExecutionDO::getTenantId, tenantId).eq(MesProEdhrBatchExecutionDO::getDeleted, false);
        query.getExpression().getOrderBy().clear();
        return query;
    }
    private record Source(String type, String ids) {}
}
