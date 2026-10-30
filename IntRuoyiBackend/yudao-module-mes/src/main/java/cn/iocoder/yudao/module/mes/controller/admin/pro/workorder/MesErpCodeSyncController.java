package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.MesErpCodeSyncService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/erp-code-sync")
@RequiredArgsConstructor
@Validated
public class MesErpCodeSyncController {

    private final MesErpCodeSyncService syncService;

    @PostMapping
    @Operation(summary = "按唯一编号同步ERP数据，完成写入后返回结果")
    @PreAuthorize("@ss.hasPermission('erp:kingdee-sync:query')")
    public CommonResult<MesErpCodeSyncService.Result> sync(@Valid @RequestBody Request request) {
        return success(syncService.sync(request.getSyncType(), request.getCode()));
    }

    @Data
    public static class Request {
        @NotBlank
        private String syncType;
        @NotBlank
        @Size(max = 100)
        private String code;
    }
}
