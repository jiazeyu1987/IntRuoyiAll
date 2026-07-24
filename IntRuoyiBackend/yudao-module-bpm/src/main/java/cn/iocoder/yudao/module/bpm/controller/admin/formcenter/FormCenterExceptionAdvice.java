package cn.iocoder.yudao.module.bpm.controller.admin.formcenter;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FormCenterExceptionAdvice {

    @ExceptionHandler(FormCenterException.class)
    public CommonResult<?> handleFormCenterException(FormCenterException ex) {
        return CommonResult.error(ex.getErrorCode().getCode(), ex.getMessage());
    }

}
