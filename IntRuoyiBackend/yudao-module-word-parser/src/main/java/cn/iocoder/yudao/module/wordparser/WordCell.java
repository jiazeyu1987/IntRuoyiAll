package cn.iocoder.yudao.module.wordparser;

public record WordCell(
        String text,
        int rowSpan,
        int colSpan,
        Integer columnIndex,
        Integer logicalColumnIndex,
        Integer logicalColSpan,
        boolean bold,
        int fontSize,
        String horizontalAlign,
        String verticalAlign,
        int widthPx,
        int heightPx,
        boolean diagonalSlash,
        String topBorderStyle,
        String bottomBorderStyle,
        String leftBorderStyle,
        String rightBorderStyle,
        String backgroundColor) {

    public WordCell {
        text = text == null ? "" : text;
    }
}
