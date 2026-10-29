package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.SimpleParserPublishReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesSimpleParserPublishService;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/batch-record-report/simple-parser")
public class MesSimpleParserPublishController {
    @Resource private MesSimpleParserPublishService service;
    @GetMapping("/devices")
    @PreAuthorize("@ss.hasPermission('form:parser:query')")
    public CommonResult<java.util.List<JSONObject>> devices() {
        return success(service.listDevices());
    }
    @PostMapping("/preview")
    @PreAuthorize("@ss.hasPermission('form:parser:production-batch-record:publish')")
    public CommonResult<JSONObject> preview(@Valid @RequestBody SimpleParserPublishReqVO request) {
        return success(service.preview(request));
    }
    @PostMapping("/publish")
    @PreAuthorize("@ss.hasPermission('form:parser:production-batch-record:publish')")
    public CommonResult<JSONObject> publish(@Valid @RequestBody SimpleParserPublishReqVO request) {
        return success(service.publish(request));
    }
}
