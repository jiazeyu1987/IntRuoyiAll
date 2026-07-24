package cn.iocoder.yudao.module.srm.controller.admin.naslocator;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorFileRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorStatusRespVO;
import cn.iocoder.yudao.module.srm.service.naslocator.SrmNasLocatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM NAS定位")
@RestController
@RequestMapping("/srm/nas-locator")
@Validated
public class SrmNasLocatorController {

    @Resource
    private SrmNasLocatorService nasLocatorService;

    @GetMapping("/status")
    @Operation(summary = "获得 NAS定位 状态摘要")
    @PreAuthorize("@ss.hasPermission('srm:nas-locator:query')")
    public CommonResult<SrmNasLocatorStatusRespVO> getStatus() {
        return success(nasLocatorService.getStatus());
    }

    @GetMapping("/blacklist")
    @Operation(summary = "获得 NAS定位 黑名单规则")
    @PreAuthorize("@ss.hasPermission('srm:nas-locator:config')")
    public CommonResult<SrmNasLocatorBlacklistRespVO> getBlacklist() {
        return success(nasLocatorService.getBlacklist());
    }

    @PutMapping("/blacklist")
    @Operation(summary = "保存 NAS定位 黑名单规则")
    @PreAuthorize("@ss.hasPermission('srm:nas-locator:config')")
    public CommonResult<Boolean> saveBlacklist(@Valid @RequestBody SrmNasLocatorBlacklistSaveReqVO reqVO) {
        nasLocatorService.saveBlacklist(reqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "分页搜索 NAS 文件")
    @PreAuthorize("@ss.hasPermission('srm:nas-locator:query')")
    public CommonResult<PageResult<SrmNasLocatorFileRespVO>> getPage(@Valid SrmNasLocatorPageReqVO pageReqVO) {
        return success(nasLocatorService.getFilePage(pageReqVO));
    }

    @PostMapping("/refresh")
    @Operation(summary = "触发 NAS 索引刷新")
    @PreAuthorize("@ss.hasPermission('srm:nas-locator:refresh')")
    public CommonResult<Boolean> refresh() {
        nasLocatorService.triggerRefresh();
        return success(true);
    }

    @GetMapping("/download")
    @Operation(summary = "按缓存记录下载 NAS 文件")
    @Parameter(name = "id", description = "NAS 索引文件记录编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:nas-locator:download')")
    public void download(@RequestParam("id") Long id, HttpServletResponse response) throws Exception {
        nasLocatorService.download(id, response);
    }
}
