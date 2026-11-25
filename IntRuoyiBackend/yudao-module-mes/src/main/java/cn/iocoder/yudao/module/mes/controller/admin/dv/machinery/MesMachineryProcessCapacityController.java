package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.process.MesMachineryProcessCapacitySaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.process.MesMachineryProcessCapacityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/dv/machinery-process")
@Validated
public class MesMachineryProcessCapacityController {
    @Resource
    private MesMachineryProcessCapacityService capacityService;

    @PutMapping("/capacity")
    @Operation(summary = "保存设备工序标准小时产能")
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:update')")
    public CommonResult<Boolean> saveCapacity(@Valid @RequestBody MesMachineryProcessCapacitySaveReqVO reqVO) {
        capacityService.saveCapacity(reqVO);
        return success(true);
    }
}
