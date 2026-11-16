package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTraceIdentityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MesProEdhrTraceSearchScopeService {
    private final MesProEdhrTraceIdentityMapper identityMapper;

    public MesProEdhrTraceSearchScope prepare(EdhrBatchExecutionPageReqVO request) {
        if (StrUtil.isBlank(request.getTracePerson())) return MesProEdhrTraceSearchScope.empty();
        // Keep the original mapper call under both TenantLine and DataPermission interceptors.
        // system_users is the only trace identity source registered with department/self rules.
        return new MesProEdhrTraceSearchScope(identityMapper.userIds(
                TenantContextHolder.getRequiredTenantId(), request.getTracePerson().trim()));
    }
}
