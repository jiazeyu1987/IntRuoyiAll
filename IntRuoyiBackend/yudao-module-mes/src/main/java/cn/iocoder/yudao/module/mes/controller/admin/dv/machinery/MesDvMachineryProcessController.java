package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.process.MesDvMachineryProcessRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "\u7BA1\u7406\u540E\u53F0 - MES \u8BBE\u5907\u5DE5\u5E8F\u660E\u7EC6")
@RestController
@RequestMapping("/mes/dv/machinery-process")
@Validated
public class MesDvMachineryProcessController {

    @Resource
    private MesDvMachineryProcessService machineryProcessService;

    @GetMapping("/list-by-machinery")
    @Operation(summary = "\u83B7\u5F97\u8BBE\u5907\u5DE5\u5E8F\u660E\u7EC6\u5217\u8868")
    @Parameter(name = "machineryId", description = "\u8BBE\u5907\u7F16\u53F7", required = true)
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:query')")
    public CommonResult<List<MesDvMachineryProcessRespVO>> getMachineryProcessList(
            @RequestParam("machineryId") Long machineryId) {
        List<MesDvMachineryProcessDO> list = machineryProcessService.getMachineryProcessListByMachineryId(machineryId);
        return success(BeanUtils.toBean(list, MesDvMachineryProcessRespVO.class));
    }
}
