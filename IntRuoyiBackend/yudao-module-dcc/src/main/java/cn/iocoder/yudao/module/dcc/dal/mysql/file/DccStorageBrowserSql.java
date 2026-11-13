package cn.iocoder.yudao.module.dcc.dal.mysql.file;

/** Project display text before binding user conditions, so complex labels occur only once. */
public final class DccStorageBrowserSql {
    private DccStorageBrowserSql() {}
    public static String count() { return statement(true); }
    public static String page() { return statement(false); }
    private static String statement(boolean count) {
        return "<script>"+DccStorageBrowserMapper.CTE+", display_facts AS (SELECT f.*,"+
                DccBrowserListFilterSql.storageDisplayProjection()+" "+DccStorageBrowserMapper.JOIN+
                "), display_rows AS (SELECT f.*,CONCAT(f.filter_name,' ',f.filter_name,' ',f.filter_metadata) AS filter_file_name,"+
                "CONCAT(f.filter_number,' ',f.filter_metadata) AS filter_file_number FROM display_facts f) SELECT "+
                (count?"COUNT(*)":"f.*")+" FROM display_rows f WHERE 1=1 "+
                "<if test='ew != null and !ew.emptyOfNormal'>AND (${ew.sqlSegment})</if>"+
                (count?"":" ORDER BY f.create_time DESC,f.id DESC LIMIT #{size} OFFSET #{offset}")+"</script>";
    }
}
