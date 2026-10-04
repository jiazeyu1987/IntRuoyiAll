package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DccMajorRevisionBoundaryTest {

    @Test
    void undefinedRevisionAfterZIsRejectedWithoutGuessing() {
        assertThrows(IllegalArgumentException.class,
                () -> DccWindchillVersionNumber.parse("Z/2").nextRevision());
        assertNull(DccWindchillVersionNumber.parse("AZ/2"));
        assertThrows(IllegalArgumentException.class, () -> new DccWindchillVersionNumber("AA", 1));
    }
}
