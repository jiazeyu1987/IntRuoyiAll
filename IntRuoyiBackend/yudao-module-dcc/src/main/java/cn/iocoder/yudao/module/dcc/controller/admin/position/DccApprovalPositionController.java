package cn.iocoder.yudao.module.dcc.controller.admin.position;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccApprovalPositionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccApprovalPositionImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccApprovalPositionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccPositionAssignmentRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccPositionAssignmentSaveReqVO;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionAdminService;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionConfigPackageService;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - DCC 审批岗位")
@RestController
@RequestMapping("/dcc/approval-positions")
@Validated
public class DccApprovalPositionController {

    @Resource
    private DccApprovalPositionAdminService positionAdminService;
    @Resource
    private DccApprovalPositionConfigPackageService positionConfigPackageService;

    @GetMapping
    @Operation(summary = "获取审批岗位列表")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccApprovalPositionRespVO>> getPositionList() {
        return success(convertList(positionAdminService.getPositionList(), item -> {
            DccApprovalPositionRespVO respVO = BeanUtils.toBean(item, DccApprovalPositionRespVO.class);
            respVO.setAssignments(convertList(positionAdminService.getAssignments(item.getId()),
                    assignment -> BeanUtils.toBean(assignment, DccPositionAssignmentRespVO.class)));
            return respVO;
        }));
    }

    @PostMapping
    @Operation(summary = "新增 IntAuth 审批岗位")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:position:manage')")
    public CommonResult<DccApprovalPositionRespVO> createPosition(@Valid @RequestBody DccApprovalPositionCreateReqVO reqVO) {
        var position = positionAdminService.createPosition(reqVO.getName(), reqVO.getChangeReason());
        DccApprovalPositionRespVO respVO = BeanUtils.toBean(position, DccApprovalPositionRespVO.class);
        respVO.setAssignments(convertList(positionAdminService.getAssignments(position.getId()),
                assignment -> BeanUtils.toBean(assignment, DccPositionAssignmentRespVO.class)));
        return success(respVO);
    }

    @PostMapping("/import-intauth")
    @Operation(summary = "从 IntAuth 一次性导入审批岗位")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:position:manage')")
    public CommonResult<DccApprovalPositionImportRespVO> importPositionsFromIntAuth() {
        var result = positionAdminService.importPositionsFromIntAuth();
        DccApprovalPositionImportRespVO respVO = new DccApprovalPositionImportRespVO();
        respVO.setTotalCount(result.getTotalCount());
        respVO.setCreatedCount(result.getCreatedCount());
        respVO.setAdoptedCount(result.getAdoptedCount());
        respVO.setUpdatedCount(result.getUpdatedCount());
        respVO.setDisabledCount(result.getDisabledCount());
        return success(respVO);
    }

    @PutMapping("/{id}/assignments")
    @Operation(summary = "替换岗位分配")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:position:manage')")
    public CommonResult<List<DccPositionAssignmentRespVO>> replaceAssignments(@PathVariable("id") Long id,
                                                                              @Valid @RequestBody List<DccPositionAssignmentSaveReqVO> reqVOList) {
        return success(convertList(positionAdminService.replaceAssignments(id, reqVOList),
                item -> BeanUtils.toBean(item, DccPositionAssignmentRespVO.class)));
    }

    @GetMapping("/config-package/export")
    @Operation(summary = "导出审批岗位配置包")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:position:manage')")
    public void exportConfigPackage(HttpServletResponse response) throws IOException {
        byte[] data = positionConfigPackageService.exportPackage();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("审批角色配置包.json"));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(data);
    }

    @PostMapping("/config-package/import")
    @Operation(summary = "导入审批岗位配置包")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:position:manage')")
    public CommonResult<Boolean> importConfigPackage(@RequestParam("file") MultipartFile file) throws IOException {
        positionConfigPackageService.importPackage(file.getBytes());
        return success(true);
    }
}
