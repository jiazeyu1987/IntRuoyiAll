package cn.iocoder.yudao.module.mdm.service.product;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdmProductCodePolicyTest {

    @Test
    void dccProductCodeMustBeFourteenAlnum() {
        assertTrue(MdmProductCodePolicy.isValidDccProductCode("A1234567890123"));
        assertTrue(MdmProductCodePolicy.isValidDccProductCode("12345678901234"));
        assertFalse(MdmProductCodePolicy.isValidDccProductCode("A123456789012"));
        assertFalse(MdmProductCodePolicy.isValidDccProductCode("A12345678901234"));
        assertFalse(MdmProductCodePolicy.isValidDccProductCode("A12345678901-3"));
    }

    @Test
    void normalizeBlankToNull() {
        assertNull(MdmProductCodePolicy.normalize("  "));
    }

}
