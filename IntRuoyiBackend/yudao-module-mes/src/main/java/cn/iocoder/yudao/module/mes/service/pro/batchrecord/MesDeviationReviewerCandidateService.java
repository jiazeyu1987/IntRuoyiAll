package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationReviewerRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesDeviationReviewerCandidateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesDeviationReviewerCandidateService {
    @Resource private MesDeviationReviewerCandidateMapper candidateMapper;
    @Resource private MesProEdhrDeviationReviewTaskService reviewTaskService;

    // Node responsibilities can span departments; the explicit tenant predicate remains mandatory.
    @DataPermission(enable = false)
    public List<MesDeviationReviewerRespVO> list(String node) {
        MesProEdhrDeviationReviewTaskService.permission(node);
        return candidateMapper.selectEnabledByTenant(TenantContextHolder.getRequiredTenantId()).stream()
                .filter(user -> reviewTaskService.isEligibleAssignee(node, user))
                .map(user -> {
                    var result = new MesDeviationReviewerRespVO();
                    result.setId(user.getId());
                    result.setUsername(user.getUsername());
                    result.setNickname(user.getNickname());
                    return result;
                }).toList();
    }
}
