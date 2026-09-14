package cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant;

import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthInvoiceVoucherPrintAssistantStatusRespVO;

public interface InvoiceVoucherPrintAssistantService {

    AuthInvoiceVoucherPrintAssistantStatusRespVO getStatus();

    AuthInvoiceVoucherPrintAssistantStatusRespVO start();

}
