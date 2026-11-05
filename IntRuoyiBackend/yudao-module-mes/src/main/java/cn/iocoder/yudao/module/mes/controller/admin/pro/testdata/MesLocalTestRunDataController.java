package cn.iocoder.yudao.module.mes.controller.admin.pro.testdata;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.mes.service.pro.testdata.MesLocalTestRunContracts.*;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.testdata.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mes/pro/work-order/local-test-data")
@RequiredArgsConstructor
@PreAuthorize(
    "@ss.hasPermission('mes:pro-work-order:create') and"
        + " @ss.hasPermission('dcc:project-code:query')")
public class MesLocalTestRunDataController {
  private final MesLocalTestRunPreparationService service;

  @GetMapping("/approved-products")
  public CommonResult<List<ApprovedProduct>> approvedProducts() {
    return success(service.approvedProducts(SecurityFrameworkUtils.getLoginUserId()));
  }

  @PostMapping("/preflight")
  public CommonResult<Preflight> preflight(@RequestBody Request request) {
    return success(service.preflight(request, SecurityFrameworkUtils.getLoginUserId()));
  }

  @PostMapping("/prepare")
  public CommonResult<Result> prepare(@RequestBody Request request) {
    return success(service.prepare(request, SecurityFrameworkUtils.getLoginUserId()));
  }
}
