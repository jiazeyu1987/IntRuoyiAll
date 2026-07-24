package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPrincipalMappingRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPrincipalMappingSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUnmappedPrincipalRespVO;
import cn.iocoder.yudao.module.dcc.service.permission.DccNasPrincipalMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC NAS Principal Mapping")
@RestController
@RequestMapping("/dcc/nas-permission")
@Validated
public class DccNasPrincipalMappingController {

    @Resource
    private DccNasPrincipalMappingService principalMappingService;

    @GetMapping("/principals/unmapped")
    @Operation(summary = "List unmapped NAS principals for one transfer task")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<DccNasUnmappedPrincipalRespVO> listUnmappedPrincipals(
            @RequestParam("taskId") Long taskId) {
        return success(DccNasUnmappedPrincipalRespVO.of(
                principalMappingService.listUnmappedPrincipals(taskId)));
    }

    @PutMapping("/principal-mappings")
    @Operation(summary = "Save one explicit NAS principal to DCC subject mapping")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<DccNasPrincipalMappingRespVO> savePrincipalMapping(
            @Valid @RequestBody DccNasPrincipalMappingSaveReqVO reqVO) {
        DccNasPrincipalMappingService.SaveMappingCommand command =
                new DccNasPrincipalMappingService.SaveMappingCommand(
                        reqVO.getSourceAuthority(),
                        reqVO.getSourceSid(),
                        reqVO.getSourceName(),
                        reqVO.getAccountName(),
                        reqVO.getAccountType(),
                        reqVO.getTargetSubjectType(),
                        reqVO.getTargetSubjectId(),
                        reqVO.getActive(),
                        reqVO.getChangeReason(),
                        getLoginUserId());
        return success(DccNasPrincipalMappingRespVO.of(principalMappingService.saveMapping(command)));
    }
}
