package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationAdministratorRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesDeviationAdministratorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED;

@Service
@cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission(enable = false)
public class MesDeviationAdministratorService {
    @Resource private MesDeviationAdministratorMapper administratorMapper;

    // This business candidate pool spans departments, but always retains explicit tenant/role predicates.
    public List<MesDeviationAdministratorRespVO> getCandidates() {
        return administratorMapper.selectCandidates(TenantContextHolder.getRequiredTenantId());
    }

    public MesDeviationAdministratorRespVO requireReceiver(Long userId) {
        if (userId == null || userId <= 0) throw exception(PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED);
        var receiver = administratorMapper.selectEligibleForUpdate(TenantContextHolder.getRequiredTenantId(), userId);
        if (receiver == null) throw exception(PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED);
        return receiver;
    }
}
