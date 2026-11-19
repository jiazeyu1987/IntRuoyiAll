package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteTreeService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/route-tree")
public class MesProRouteTreeController {
    @Resource private MesProRouteTreeService service;
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:query')")
    public CommonResult<List<Map<String,Object>>> list() { return success(service.tree()); }
    @PostMapping("/group")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:create')")
    public CommonResult<Long> group(@Valid @RequestBody GroupRequest request) {
        return success(service.createGroup(request.name, request.parentId, request.sourceRouteId));
    }
    @PutMapping("/group-name")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> renameGroup(@Valid @RequestBody GroupNameRequest request) {
        service.renameGroup(request.id, request.name); return success(true);
    }
    @DeleteMapping("/group")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:delete')")
    public CommonResult<Boolean> deleteGroup(@RequestParam Long id) {
        service.deleteGroup(id); return success(true);
    }
    @PostMapping("/inherit")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:create')")
    public CommonResult<Long> inherit(@RequestParam Long parentId, @Valid @RequestBody MesProRouteCreateReqVO request) {
        return success(service.createInherited(parentId, request));
    }
    @GetMapping("/move-preview")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Map<String,Object>> movePreview(@RequestParam Long id,
            @RequestParam(required=false) Long parentId) { return success(service.movePreview(id, parentId)); }
    @PutMapping("/move")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> move(@Valid @RequestBody MoveRequest request) {
        service.move(request.id, request.parentId, request.fingerprint, request.confirmDifference);
        return success(true);
    }
    @GetMapping("/publish-preview")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-submit')")
    public CommonResult<Map<String,Object>> preview(@RequestParam Long id) { return success(service.preview(id)); }
    @PostMapping("/submit")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:version-create') && @ss.hasPermission('mes:pro-route:version-update') && @ss.hasPermission('mes:pro-route:version-submit')")
    public CommonResult<MesProRouteVersionDO> submit(@Valid @RequestBody SubmitRequest request) {
        return success(service.submit(request.id, request.fingerprint, request.reason));
    }
    @Data public static class GroupRequest {
        @NotBlank private String name;
        private Long parentId;
        private Long sourceRouteId;
    }
    @Data public static class GroupNameRequest {
        @NotNull private Long id;
        @NotBlank private String name;
    }
    @Data public static class MoveRequest {
        @NotNull private Long id;
        private Long parentId;
        @NotBlank private String fingerprint;
        private boolean confirmDifference;
    }
    @Data public static class SubmitRequest {
        @NotNull private Long id;
        @NotBlank private String fingerprint;
        @NotBlank private String reason;
    }
}
