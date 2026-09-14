package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookGlobalSettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 记录本配置")
@RestController
@RequestMapping("/mes/pro/edhr-recordbook-setting")
@Validated
public class MesProEdhrRecordbookSettingController {

    @Resource
    private MesProEdhrRecordbookGlobalSettingService recordbookGlobalSettingService;

    @GetMapping("/global")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<EdhrRecordbookGlobalSettingRespVO> getGlobal() {
        return success(recordbookGlobalSettingService.getGlobalSetting());
    }

    @PutMapping("/global")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<EdhrRecordbookGlobalSettingRespVO> updateGlobal(
            @Valid @RequestBody EdhrRecordbookGlobalSettingUpdateReqVO reqVO) {
        return success(recordbookGlobalSettingService.updateGlobalSetting(reqVO));
    }
}
