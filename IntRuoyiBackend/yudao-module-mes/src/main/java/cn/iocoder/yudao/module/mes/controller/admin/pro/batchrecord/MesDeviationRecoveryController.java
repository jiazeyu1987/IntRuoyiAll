package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/edhr-deviation-recovery")
@PreAuthorize("@ss.hasPermission('mes:pro-edhr-deviation:handle')")
public class MesDeviationRecoveryController {
    @Resource private MesDeviationAdministratorService administrators;
    @Resource private MesDeviationResponsibilityRecoveryService recovery;
    @Resource private MesProEdhrDeviationHandlingService handling;

    @GetMapping("/receiver-candidates")
    public CommonResult<List<MesDeviationAdministratorRespVO>> candidates() {
        return success(administrators.getCandidates());
    }

    @PostMapping("/assign-receiver")
    public CommonResult<Boolean> assign(@Valid @RequestBody MesDeviationReceiverAssignReqVO req) {
        recovery.assignReceiver(SecurityFrameworkUtils.getLoginUserId(), req);
        return success(true);
    }

    @PostMapping("/withdraw-investigation")
    public CommonResult<MesProEdhrDeviationHandlingRespVO> withdraw(@Valid @RequestBody MesDeviationInvestigationWithdrawReqVO req) {
        return success(handling.withdrawInvestigation(SecurityFrameworkUtils.getLoginUserId(), req));
    }
}
