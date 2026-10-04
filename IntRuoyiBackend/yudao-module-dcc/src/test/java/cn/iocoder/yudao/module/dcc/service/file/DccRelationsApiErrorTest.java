package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.DccRelationsExceptionHandler;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DccRelationsApiErrorTest {
    @Test void businessFailureExposesStableReasonWithoutTechnicalCauseOrFalseSuccess(){
        var response=new DccRelationsExceptionHandler().handle(new DccRelationFailure("DCC_RELATION_CONCURRENT_CHANGE",new RuntimeException("SECRET_DATA")));
        assertEquals(DccRelationErrorCodes.BUSINESS_FAILURE,response.getCode());assertTrue(response.getMsg().contains("DCC_RELATION_CONCURRENT_CHANGE"));
        assertNotEquals(cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.INVALID.getCode(),response.getCode(),"D relation business failure must not share B attribute validation code");
        assertTrue(response.getMsg().contains("刷新"));assertFalse(response.getMsg().contains("SECRET_DATA"));assertNull(response.getData());
    }
}
