package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MesReleaseFlowExceptionAdvice {

    @ExceptionHandler(MesReleaseFlowBlockerException.class)
    public CommonResult<MesReleaseFlowFailureRespVO> handleBlocker(MesReleaseFlowBlockerException exception) {
        CommonResult<MesReleaseFlowFailureRespVO> result = CommonResult.error(
                MesReleaseFlowErrorCodeConstants.RELEASE_FLOW_BLOCKED.getCode(), exception.getMessage());
        result.setData(exception.getFailure());
        return result;
    }
}
