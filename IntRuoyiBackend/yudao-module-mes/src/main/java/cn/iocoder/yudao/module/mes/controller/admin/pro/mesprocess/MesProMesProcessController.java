package cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessRespVO;
import cn.iocoder.yudao.module.mes.service.pro.mesprocess.MesProMesProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序只读目录")
@RestController
@RequestMapping("/mes/pro/mes-process")
@Validated
public class MesProMesProcessController {

    @Resource
    private MesProMesProcessService mesProcessService;

    @GetMapping("/page")
    @Operation(summary = "获得 MES 工序只读目录分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-mes-process:query')")
    public CommonResult<PageResult<MesProMesProcessRespVO>> getMesProcessPage(
            @Valid MesProMesProcessPageReqVO pageReqVO) {
        return success(mesProcessService.getMesProcessPage(pageReqVO));
    }
}
