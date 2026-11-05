package cn.iocoder.yudao.module.mes.service.qc.sampling;

import java.util.List;
import java.util.Map;

/**
 * GB/T 2828.1-2012，正文第14页表1、第15页表2-A的字码与名义样本量。
 * 这里只查询正常一次名义样本量，不执行 AQL 箭头规则或接收/拒收判定。
 */
public final class MesQcSamplingTable {

    public static final String STANDARD = "GB/T 2828.1-2012 / ISO 2859-1:1999";

    public enum InspectionLevel { S1, S2, S3, S4, I, II, III }

    public record Sample(String sampleCode, int nominalSampleSize) { }

    public record Row(long minQuantity, Long maxQuantity, Map<InspectionLevel, Sample> samples) { }

    public record LookupResult(String standard, long totalQuantity, InspectionLevel inspectionLevel,
                               long minQuantity, Long maxQuantity, String sampleCode,
                               int nominalSampleSize) { }

    private static final String CODES = "ABCDEFGHJKLMNPQR";
    private static final int[] SIZES = {2, 3, 5, 8, 13, 20, 32, 50, 80, 125, 200, 315, 500, 800, 1250, 2000};
    private static final List<Row> ROWS = List.of(
            row(2, 8L, "AAAAAAB"),
            row(9, 15L, "AAAAABC"),
            row(16, 25L, "AABBBCD"),
            row(26, 50L, "ABBCCDE"),
            row(51, 90L, "BBCCCEF"),
            row(91, 150L, "BBCDDFG"),
            row(151, 280L, "BCDEEGH"),
            row(281, 500L, "BCDEFHJ"),
            row(501, 1200L, "CCEFGJK"),
            row(1201, 3200L, "CDEGHKL"),
            row(3201, 10000L, "CDFGJLM"),
            row(10001, 35000L, "CDFHKMN"),
            row(35001, 150000L, "DEGJLNP"),
            row(150001, 500000L, "DEGJMPQ"),
            row(500001, null, "DEHKNQR")
    );

    private MesQcSamplingTable() { }

    public static List<Row> rows() {
        return ROWS;
    }

    public static LookupResult lookup(long totalQuantity, InspectionLevel inspectionLevel) {
        if (totalQuantity < 2) {
            throw new IllegalArgumentException("总数量必须为不小于2的整数，标准表1的批量从2开始");
        }
        if (inspectionLevel == null) {
            throw new IllegalArgumentException("检验水平不能为空");
        }
        for (Row row : ROWS) {
            if (totalQuantity >= row.minQuantity()
                    && (row.maxQuantity() == null || totalQuantity <= row.maxQuantity())) {
                Sample sample = row.samples().get(inspectionLevel);
                return new LookupResult(STANDARD, totalQuantity, inspectionLevel, row.minQuantity(),
                        row.maxQuantity(), sample.sampleCode(), sample.nominalSampleSize());
            }
        }
        throw new IllegalStateException("抽样对照表未覆盖该批量");
    }

    private static Row row(long min, Long max, String codes) {
        Map<InspectionLevel, Sample> samples = new java.util.EnumMap<>(InspectionLevel.class);
        for (InspectionLevel level : InspectionLevel.values()) {
            char code = codes.charAt(level.ordinal());
            samples.put(level, new Sample(String.valueOf(code), SIZES[CODES.indexOf(code)]));
        }
        return new Row(min, max, Map.copyOf(samples));
    }
}
