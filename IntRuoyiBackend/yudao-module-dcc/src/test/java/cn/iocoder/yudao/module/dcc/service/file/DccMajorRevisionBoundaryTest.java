package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DccMajorRevisionBoundaryTest {

    @Test
    void excelRevisionCarryUsesAAAndBA() {
        assertEquals("AA/1", DccWindchillVersionNumber.parse("Z/2").nextRevision().display());
        assertEquals("BA/1", DccWindchillVersionNumber.parse("AZ/2").nextRevision().display());
    }
}
