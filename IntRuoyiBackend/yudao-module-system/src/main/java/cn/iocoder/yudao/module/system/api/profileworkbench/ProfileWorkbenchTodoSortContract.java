package cn.iocoder.yudao.module.system.api.profileworkbench;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
public final class ProfileWorkbenchTodoSortContract {
    public static final List<String> TEXT_KEYS = List.of("taskType", "source", "detail", "statusLabel");
    private ProfileWorkbenchTodoSortContract() { }
    public static String text(ProfileWorkbenchTodoRowDTO row, String key) {
        if (key == null) return "";
        return switch (key) {
            case "taskType" -> row.getTaskType();
            case "source" -> row.getSource();
            case "detail" -> row.getDetail();
            case "statusLabel" -> row.getStatusLabel();
            default -> throw new IllegalArgumentException("Unsupported workbench sort key");
        };
    }
    private static String empty(String value) { return value == null ? "" : value; }
    public static int binaryCompare(String left, String right) {
        return Arrays.compareUnsigned(empty(left).getBytes(StandardCharsets.UTF_8), empty(right).getBytes(StandardCharsets.UTF_8));
    }
    public static Comparator<ProfileWorkbenchTodoRowDTO> comparator(ProfileWorkbenchTodoQueryDTO query) {
        return (left, right) -> {
            if (query.getSortKey() != null) {
                int custom = binaryCompare(text(left, query.getSortKey()), text(right, query.getSortKey()));
                if (custom != 0) return "desc".equals(query.getSortOrder()) ? -custom : custom;
            }
            boolean leftDue = left.getDueAt() != null, rightDue = right.getDueAt() != null;
            if (leftDue != rightDue) return leftDue ? -1 : 1;
            int date;
            if (leftDue) date = left.getDueAt().compareTo(right.getDueAt());
            else {
                LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);
                date = (right.getCreatedAt() == null ? epoch : right.getCreatedAt())
                    .compareTo(left.getCreatedAt() == null ? epoch : left.getCreatedAt());
            }
            if (date != 0) return date;
            int source = Integer.compare(left.getSourceId().ordinal(), right.getSourceId().ordinal());
            return source != 0 ? source : Long.compare(right.getNumericId(), left.getNumericId());
        };
    }
}
