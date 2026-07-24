package cn.iocoder.yudao.module.erp.service.kingdee.event;

public interface ErpKingdeeEventCallbackService {

    String HEADER_SIGNATURE = "X-Kingdee-Signature";
    String HEADER_TIMESTAMP = "X-Kingdee-Timestamp";
    String HEADER_NONCE = "X-Kingdee-Nonce";

    ErpKingdeeEventCallbackResult receive(String rawBody, String signature, String timestamp, String nonce);

}
