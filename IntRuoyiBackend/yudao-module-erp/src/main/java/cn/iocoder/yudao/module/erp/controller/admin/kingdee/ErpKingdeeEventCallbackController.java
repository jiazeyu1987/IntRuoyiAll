package cn.iocoder.yudao.module.erp.controller.admin.kingdee;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.kingdee.vo.ErpKingdeeEventCallbackRespVO;
import cn.iocoder.yudao.module.erp.service.kingdee.event.ErpKingdeeEventCallbackResult;
import cn.iocoder.yudao.module.erp.service.kingdee.event.ErpKingdeeEventCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.erp.service.kingdee.event.ErpKingdeeEventCallbackService.HEADER_NONCE;
import static cn.iocoder.yudao.module.erp.service.kingdee.event.ErpKingdeeEventCallbackService.HEADER_SIGNATURE;
import static cn.iocoder.yudao.module.erp.service.kingdee.event.ErpKingdeeEventCallbackService.HEADER_TIMESTAMP;

@Tag(name = "管理后台 - ERP Kingdee 事件回调")
@RestController
@RequestMapping("/erp/kingdee/events")
@Validated
public class ErpKingdeeEventCallbackController {

    @Resource
    private ErpKingdeeEventCallbackService callbackService;

    @PostMapping("/callback")
    @PermitAll
    @Operation(summary = "接收 Kingdee BOS 插件事件回调")
    public CommonResult<ErpKingdeeEventCallbackRespVO> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = HEADER_SIGNATURE, required = false) String signature,
            @RequestHeader(value = HEADER_TIMESTAMP, required = false) String timestamp,
            @RequestHeader(value = HEADER_NONCE, required = false) String nonce) {
        ErpKingdeeEventCallbackResult result = callbackService.receive(rawBody, signature, timestamp, nonce);
        return success(ErpKingdeeEventCallbackRespVO.of(result));
    }

}
