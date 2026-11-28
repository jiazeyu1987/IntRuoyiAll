package cn.iocoder.yudao.module.mes.controller.admin.pro.craft;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.MesSharedChangeReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftChangeDO;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProSharedChangeService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;

@RestController
@RequestMapping("/mes/pro/shared-change/{subjectType}")
public class MesSharedChangeController {
    private static final String EDIT = "((#subjectType == 'PROCESS' && @ss.hasPermission('mes:pro-process:update')) || "
            + "(#subjectType == 'CRAFT' && @ss.hasPermission('mes:pro-craft:update')) || "
            + "(#subjectType == 'DEVICE' && @ss.hasPermission('mes:dv-machinery:update')))";
    @Resource private MesProSharedChangeService service;

    @GetMapping("/source")
    @PreAuthorize(EDIT)
    public CommonResult<Map<String,Object>> source(@PathVariable String subjectType, @RequestParam Long id) {
        return success(service.source(subjectType, id));
    }
    @PostMapping("/preview")
    @PreAuthorize(EDIT)
    public CommonResult<Map<String,Object>> preview(@PathVariable String subjectType, @Valid @RequestBody MesSharedChangeReqVO request) {
        if (!subjectType.equals(request.getSubjectType())) throw invalid("变更对象类型不一致");
        return success(service.preview(request));
    }
    @GetMapping("/options")
    @PreAuthorize(EDIT)
    public CommonResult<java.util.List<com.alibaba.fastjson.JSONObject>> options(@PathVariable String subjectType, @RequestParam Long sourceId) {
        return success(service.options(subjectType, sourceId));
    }
    @GetMapping("/crafts")
    @PreAuthorize(EDIT)
    public CommonResult<java.util.List<cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO>> crafts(@PathVariable String subjectType, @RequestParam Long processId) {
        return success(service.parameterOptions(processId));
    }
    @PostMapping("/submit")
    @PreAuthorize(EDIT)
    public CommonResult<MesProCraftChangeDO> submit(@PathVariable String subjectType, @Valid @RequestBody MesSharedChangeReqVO request) {
        if (!subjectType.equals(request.getSubjectType())) throw invalid("变更对象类型不一致");
        return success(service.submit(request));
    }
}
