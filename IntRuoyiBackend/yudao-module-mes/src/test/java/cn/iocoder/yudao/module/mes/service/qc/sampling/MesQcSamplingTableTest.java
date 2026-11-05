package cn.iocoder.yudao.module.mes.service.qc.sampling;

import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.mes.service.qc.sampling.MesQcSamplingTable.InspectionLevel.*;
import static org.junit.jupiter.api.Assertions.*;

class MesQcSamplingTableTest {

    @Test
    void standardTableOneMatchesAll105CellsAtBothEndsOfEveryBand() {
        // GB/T 2828.1-2012 正文第14页表1，列顺序 S-1/S-2/S-3/S-4/I/II/III。
        long[] starts = {2, 9, 16, 26, 51, 91, 151, 281, 501, 1201, 3201, 10001, 35001, 150001, 500001};
        long[] ends = {8, 15, 25, 50, 90, 150, 280, 500, 1200, 3200, 10000, 35000, 150000, 500000, Long.MAX_VALUE};
        String[] expected = {
                "A A A A A A B", "A A A A A B C", "A A B B B C D", "A B B C C D E",
                "B B C C C E F", "B B C D D F G", "B C D E E G H", "B C D E F H J",
                "C C E F G J K", "C D E G H K L", "C D F G J L M", "C D F H K M N",
                "D E G J L N P", "D E G J M P Q", "D E H K N Q R"
        };
        assertEquals(15, MesQcSamplingTable.rows().size());
        var levels = MesQcSamplingTable.InspectionLevel.values();
        for (int band = 0; band < starts.length; band++) {
            String[] codes = expected[band].split(" ");
            var row = MesQcSamplingTable.rows().get(band);
            assertEquals(starts[band], row.minQuantity());
            assertEquals(band == 14 ? null : Long.valueOf(ends[band]), row.maxQuantity());
            for (int column = 0; column < levels.length; column++) {
                assertEquals(codes[column], row.samples().get(levels[column]).sampleCode());
                for (long quantity : new long[]{starts[band], ends[band]}) {
                    var result = MesQcSamplingTable.lookup(quantity, levels[column]);
                    assertEquals(codes[column], result.sampleCode(), quantity + "/" + levels[column]);
                    assertEquals(row.minQuantity(), result.minQuantity());
                    assertEquals(row.maxQuantity(), result.maxQuantity());
                    assertEquals(quantity, result.totalQuantity());
                    assertEquals(levels[column], result.inspectionLevel());
                }
            }
        }
    }

    @Test
    void standardTableTwoANominalSizesAreReturnedWithoutAqlOrBatchCapping() {
        // 同一批量切换一般/特殊水平；表2-A的字码样本量，而非百分比计算。
        assertSample(3000, I, "H", 50);
        assertSample(3000, II, "K", 125);
        assertSample(3000, III, "L", 200);
        assertSample(3000, S1, "C", 5);
        assertSample(3000, S2, "D", 8);
        assertSample(3000, S3, "E", 13);
        assertSample(3000, S4, "G", 32);
        assertSample(2, III, "B", 3); // 名义样本量保留3，不冒充实际执行数2。
        assertSample(500001, III, "R", 2000);
        var samples = MesQcSamplingTable.rows().stream().flatMap(row -> row.samples().values().stream())
                .distinct().toList();
        String[] codes = {"A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R"};
        int[] sizes = {2, 3, 5, 8, 13, 20, 32, 50, 80, 125, 200, 315, 500, 800, 1250, 2000};
        for (int i = 0; i < codes.length; i++) {
            assertTrue(samples.contains(new MesQcSamplingTable.Sample(codes[i], sizes[i])));
        }
    }

    @Test
    void invalidPrerequisitesFailAndStandardTableCannotBeEdited() {
        for (long quantity : new long[]{Long.MIN_VALUE, -1, 0, 1}) {
            assertThrows(IllegalArgumentException.class, () -> MesQcSamplingTable.lookup(quantity, II));
        }
        assertThrows(IllegalArgumentException.class, () -> MesQcSamplingTable.lookup(100, null));
        assertThrows(UnsupportedOperationException.class, () -> MesQcSamplingTable.rows().clear());
        assertThrows(UnsupportedOperationException.class, () -> MesQcSamplingTable.rows().get(0).samples().clear());
    }

    private void assertSample(long quantity, MesQcSamplingTable.InspectionLevel level, String code, int size) {
        var result = MesQcSamplingTable.lookup(quantity, level);
        assertEquals(code, result.sampleCode());
        assertEquals(size, result.nominalSampleSize());
    }
}
