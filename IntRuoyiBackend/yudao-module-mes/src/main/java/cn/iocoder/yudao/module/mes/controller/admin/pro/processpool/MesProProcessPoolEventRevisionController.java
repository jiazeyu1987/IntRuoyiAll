package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolEventRevisionUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventRevisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序池原始记录修改")
@RestController
@RequestMapping("/mes/pro/process-pool/event-revision")
@Validated
public class MesProProcessPoolEventRevisionController {

    private final MesProcessPoolEventRevisionService mesProcessPoolEventRevisionService;

    public MesProProcessPoolEventRevisionController(
            MesProcessPoolEventRevisionService mesProcessPoolEventRevisionService) {
        this.mesProcessPoolEventRevisionService = mesProcessPoolEventRevisionService;
    }

    @PostMapping("/update-original")
    @Operation(summary = "修改工序池原始记录并重新电子签名")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool:event-revision:update')")
    public CommonResult<Long> updateOriginalRecord(@Valid @RequestBody ProcessPoolEventRevisionUpdateReqVO reqVO) {
        return success(mesProcessPoolEventRevisionService.updateOriginalRecord(reqVO.toBO()));
    }
}
