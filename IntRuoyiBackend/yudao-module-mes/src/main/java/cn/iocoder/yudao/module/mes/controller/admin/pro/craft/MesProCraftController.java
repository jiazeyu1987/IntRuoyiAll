package cn.iocoder.yudao.module.mes.controller.admin.pro.craft;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.*;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/craft")
public class MesProCraftController {
    @Resource private MesProCraftService service;
    @GetMapping("/list")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-craft:query','mes:pro-route:version-query')")
    public CommonResult<List<MesProCraftDO>> list(@RequestParam(value="processId",required=false) Long processId) {
        return success(service.list(processId));
    }
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-craft:create')")
    public CommonResult<MesProCraftDO> create(@Valid @RequestBody MesProCraftSaveReqVO request) {
        return success(service.create(request));
    }
    @PostMapping("/preview")
    @PreAuthorize("@ss.hasPermission('mes:pro-craft:update')")
    public CommonResult<Map<String,Object>> preview(@Valid @RequestBody MesProCraftSaveReqVO request) {
        return success(service.preview(request));
    }
    @PostMapping("/update-all")
    @PreAuthorize("@ss.hasPermission('mes:pro-craft:update') && @ss.hasPermission('mes:pro-route:version-create') && @ss.hasPermission('mes:pro-route:version-update') && @ss.hasPermission('mes:pro-route:version-submit')")
    public CommonResult<MesProCraftChangeDO> update(@Valid @RequestBody MesProCraftSaveReqVO request) {
        return success(service.updateAll(request));
    }
    @GetMapping("/initialization-preview")
    @PreAuthorize("@ss.hasPermission('mes:pro-craft:initialize')")
    public CommonResult<Map<String,Object>> initializationPreview() {
        return success(service.initializationPreview());
    }
    @PostMapping("/initialize")
    @PreAuthorize("@ss.hasPermission('mes:pro-craft:initialize')")
    public CommonResult<MesProCraftChangeDO> initialize(@RequestParam("impactToken") String impactToken) {
        return success(service.initialize(impactToken));
    }
    @GetMapping("/changes")
    @PreAuthorize("@ss.hasPermission('mes:pro-craft:query')")
    public CommonResult<List<MesProCraftChangeDO>> changes() { return success(service.changes()); }
    @PostMapping("/cancel-change")
    @PreAuthorize("@ss.hasPermission('mes:pro-craft:update')")
    public CommonResult<Boolean> cancel(@RequestParam("id") Long id) {
        service.cancelChange(id); return success(true);
    }
    @PostMapping("/bind-existing")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-update')")
    public CommonResult<Boolean> bindExisting(@Valid @RequestBody MesProCraftBindingTargetReqVO request) {
        service.bindExistingCandidate(request); return success(true);
    }
    @PostMapping("/bind")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-update')")
    public CommonResult<Boolean> bind(@Valid @RequestBody MesProCraftBindReqVO request) {
        service.bindCandidate(request); return success(true);
    }
}
