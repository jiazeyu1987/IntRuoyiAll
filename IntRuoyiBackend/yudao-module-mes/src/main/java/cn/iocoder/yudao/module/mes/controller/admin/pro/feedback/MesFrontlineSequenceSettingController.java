package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSequenceSettingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mes/pro/frontline-sequence-setting")
public class MesFrontlineSequenceSettingController {
    private final MesFrontlineSequenceSettingService service;
    public record Setting(@NotNull Boolean enabled) {}

    @GetMapping
    @PreAuthorize("@ss.hasPermission('infra:config:query')")
    public CommonResult<Setting> get() { return success(new Setting(service.isEnabled())); }

    @PutMapping
    @PreAuthorize("@ss.hasPermission('infra:config:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody Setting setting) {
        service.update(setting.enabled());
        return success(true);
    }
}
