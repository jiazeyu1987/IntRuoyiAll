package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchIntegrityChunkReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchIntegrityChunkRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchIntegrityService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
@RequestMapping("/mes/pro/edhr-batch-execution/integrity")
public class MesProEdhrBatchIntegrityController {
    @Resource private MesProEdhrBatchIntegrityService integrityService;

    @PostMapping("/check-chunk")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution:query')")
    public CommonResult<EdhrBatchIntegrityChunkRespVO> check(@Valid @RequestBody EdhrBatchIntegrityChunkReqVO request) {
        return success(integrityService.check(request, SecurityFrameworkUtils.getLoginUserId()));
    }
}
