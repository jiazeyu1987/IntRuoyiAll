package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED;

/** A responsibility grants access to its deviation, without granting a management role. */
@Service
public class MesProEdhrDeviationAccessService {
    @Resource private MesProEdhrDeviationMapper deviationMapper;
    @Resource private MesProEdhrDeviationHandlingMapper handlingMapper;
    @Resource private SecurityFrameworkService securityFrameworkService;

    public void requireRead(Long deviationId) {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var deviation = deviationMapper.selectByTenantAndId(tenant, deviationId);
        var handling = handlingMapper.selectByTenantAndDeviationId(tenant, deviationId);
        if (actor == null || deviation == null || !Objects.equals(tenant, deviation.getTenantId())
                || !(securityFrameworkService.hasPermission("mes:pro-edhr-deviation:query")
                || isParticipant(actor, deviation, handling))) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, "查看该偏差");
        }
    }

    public boolean canEditInvestigation(Long actor, MesProEdhrDeviationDO deviation,
                                        MesProEdhrDeviationHandlingDO handling) {
        return actor != null && securityFrameworkService.hasPermission("mes:pro-edhr-deviation:handle")
                && Objects.equals(actor, deviation.getReceiverId());
    }

    public Long scopedReader() {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        if (actor == null) throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, "登录后查看偏差");
        return securityFrameworkService.hasPermission("mes:pro-edhr-deviation:query") ? null : actor;
    }

    boolean isParticipant(Long actor, MesProEdhrDeviationDO deviation, MesProEdhrDeviationHandlingDO handling) {
        if (actor == null) return false;
        if (Objects.equals(actor, deviation.getInitiatorUserId()) || Objects.equals(actor, deviation.getReceiverId())) return true;
        if (handling == null) return false;
        if (Objects.equals(actor, handling.getCorrectiveOwnerId())) return true;
        return handling.getReviewerAssignmentsJson() != null
                && JSON.parseObject(handling.getReviewerAssignmentsJson()).values().stream()
                    .anyMatch(id -> Objects.equals(String.valueOf(actor), String.valueOf(id)));
    }
}
