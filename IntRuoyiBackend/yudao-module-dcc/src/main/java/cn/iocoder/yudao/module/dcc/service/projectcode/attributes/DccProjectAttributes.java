package cn.iocoder.yudao.module.dcc.service.projectcode.attributes;

import java.util.List;
import java.util.Set;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

/** IC-1 属性值；不可变，默认和实际快照不得共享可变数组。 */
public record DccProjectAttributes(List<String> targetMarkets, String otherMarket,
                                   String licenseHolder, String actualManufacturer,
                                   String documentTransfer, String transferTo) {
    private static final Set<String> MARKETS = Set.of("NMPA", "CE", "FDA", "MADSAP", "OTHER", "NA");
    private static final Set<String> IDENTITIES = Set.of("Y", "N", "NA");
    public DccProjectAttributes {
        if (targetMarkets != null) targetMarkets = List.copyOf(targetMarkets);
    }
    public DccProjectAttributes validated() {
        if (targetMarkets == null || targetMarkets.isEmpty()
                || targetMarkets.stream().anyMatch(value -> !MARKETS.contains(value))
                || targetMarkets.stream().distinct().count() != targetMarkets.size()
                || (targetMarkets.contains("NA") && targetMarkets.size() != 1)
                || licenseHolder == null || !IDENTITIES.contains(licenseHolder)
                || actualManufacturer == null || !IDENTITIES.contains(actualManufacturer)
                || !Set.of("Y", "N").contains(documentTransfer == null ? "" : documentTransfer)) {
            throw fail(INVALID);
        }
        if (targetMarkets.contains("OTHER") != hasText(otherMarket)
                || "Y".equals(documentTransfer) != hasText(transferTo)
                || (otherMarket != null && otherMarket.length() > 512)
                || (transferTo != null && transferTo.length() > 512)) throw fail(INVALID);
        return this;
    }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
