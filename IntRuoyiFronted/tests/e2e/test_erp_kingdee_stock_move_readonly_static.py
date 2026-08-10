from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
FRONTEND_ROOT = ROOT.parent


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_frontend_has_readonly_kingdee_stock_move_api_and_page() -> None:
    api = read(FRONTEND_ROOT / "src" / "api" / "erp" / "stock" / "kingdeeStockMove" / "index.ts")
    page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "stock" / "kingdeeStockMove" / "index.vue")

    assert "/erp/kingdee-stock-move/page" in api
    assert "getStockMovePage" in api
    assert "金蝶调拨单" in page
    assert "调拨单号" in page
    assert "调出仓库" in page
    assert "调入仓库" in page
    assert "新增" not in page
    assert "审批" not in page
    assert "反审批" not in page
    assert "删除" not in page


def test_stock_move_page_uses_standard_list_template() -> None:
    page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "stock" / "kingdeeStockMove" / "index.vue")

    assert "UnifiedListTemplate" in page
    assert 'table-key="erp.stock.kingdeeStockMove.main"' in page
    assert 'data-user-table-key="erp.stock.kingdeeStockMove.main"' in page
    assert "useTableQuickFilter" in page
    assert "useUserTableColumns" in page
    assert "stockMoveQuickFilterDefinitions" in page
    assert "queryParamKey: 'sourceBillNo'" in page
    assert "queryParamKey: 'documentStatus'" in page
    assert "queryParamKey: 'transferDirect'" in page
    assert "queryParamKey: 'billDate'" in page
    assert "@column-change=\"saveStockMoveColumnConfig\"" in page
    assert "@header-dragend=\"handleStockMoveHeaderDragend\"" in page
    assert "isStockMoveColumnVisible('sourceBillNo')" in page
    assert "isStockMoveColumnVisible('lastSyncTime')" in page
    assert "<Pagination" not in page


def test_stock_move_page_hides_red_box_notice_and_toolbar_reset() -> None:
    page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "stock" / "kingdeeStockMove" / "index.vue")
    actions = page.split("<template #actions>", 1)[1].split("</template>", 1)[0]

    assert "该页面仅展示金蝶直接调拨单同步快照，不接入本地库存调拨业务流程。" not in page
    assert "resetStockMoveQuickFilter" not in actions
    assert "增量同步" in actions
    assert "resetQuickFilter: resetStockMoveQuickFilter" not in page


def test_stock_move_filter_and_actions_use_single_line_toolbar() -> None:
    page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "stock" / "kingdeeStockMove" / "index.vue")
    unified_list_props = page.split("<UnifiedListTemplate", 1)[1].split(">", 1)[0]

    assert "single-line-toolbar" in unified_list_props


def test_stock_move_table_locks_header_and_horizontal_footer_for_scroll() -> None:
    page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "stock" / "kingdeeStockMove" / "index.vue")
    table_open = page.split("<el-table", 1)[1].split(">", 1)[0]
    style = page.split("<style scoped>", 1)[1]

    assert 'class="stock-move-table stock-move-table--locked"' in table_open
    assert 'height="calc(100vh - 304px)"' in table_open
    assert ".stock-move-table--locked" in style
    assert ":deep(.el-table__header-wrapper)" in style
    assert ":deep(.el-scrollbar__bar.is-horizontal)" in style


def test_frontend_registers_stock_move_in_sync_monitor_and_profile_job_config() -> None:
    sync_page = read(FRONTEND_ROOT / "src" / "views" / "erp" / "sync" / "index.vue")
    profile = read(FRONTEND_ROOT / "src" / "views" / "Profile" / "components" / "ProfileErpTableAutoSyncSetting.vue")

    assert "STOCK_MOVE" in sync_page
    assert "kingdeeStockMoveSyncJob" in sync_page
    assert "金蝶调拨单" in sync_page
    assert "STOCK_MOVE" in profile
    assert "kingdeeStockMoveSyncJob" in profile
    assert "金蝶调拨单" in profile
    assert "kingdee-table-auto-sync" not in profile
