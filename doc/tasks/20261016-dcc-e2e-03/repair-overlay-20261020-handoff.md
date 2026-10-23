# 03/23 共享列设置遮挡修复交接

2026-10-20，int_qms。目标E2E03-R1-UI01 / E2E23-R4-UI-01；原四轮事实不变。

根因：App.vue唯一挂载UserTableColumnGlobalEnhancer，控件统一Teleport至body；styles/index.scss固定2100。真实A/2预览overlay=2055，两个底层控件在elementFromPoint命中。baseline-result.json为有效产品RED，probe/green保留脚本定位校准失败。

最小修改：UserTableColumnGlobalEnhancer/updatePanelPosition读取表格所属.el-overlay的实际zIndex，普通页面取1；控件默认样式同步1。仍使用原Teleport与定位，不改全部Dialog、不无限提高层级、不改权限/版本/业务。

BDD：Given页面/弹窗/抽屉各有表格，When打开更上层预览，Then底层控件不遮挡；关闭后原控件恢复可用。

验证：

- `node IntRuoyiFronted/tests/unit/user-table-column-overlay-stacking.test.cjs`：原实现RED，实际命中nested而非preview；修改后GREEN，依次命中preview/nested/inside/base。测试在真实Chromium布局运行当前定位函数与样式；这是隔离组件测试，不冒充业务E2E。
- `node IntRuoyiFronted/tests/e2e/user-table-column-observer-loop-static.spec.js`：PASS。
- `node IntRuoyiFronted/tests/unit/dcc-shared-attachments-component.test.cjs`：8/8 PASS。
- `node doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020.cjs green2`：exit0，真实U账号、原A/2 #2054545668044084291 RA和原A/1 #2054545668044084286 A/B均逐字内容正确；所有可视控件采样不命中底层列设置。每次关闭预览后真实点击“显示字段”，字段面板打开/关闭正常，无字段变更。
- `user-table-column-config-static.spec.js`：FAIL，详情源码未包含其期望的UserTableColumnSettings；详情页为已有并行改动，本修复未写该文件。不把失败写PASS，也不为旧实现断言扩大修改。
- 新增4份trace CRC、JSON解析、当前凭据字面量及敏感字段扫描PASS，见repair-overlay-20261020/evidence-audit.json；未改写任何trace。green2截图视觉核对：预览可视区无底层控件。fullPage截图在视口以外的底页区域仍可能显示控件，不代表其覆盖可视预览。

23复验入口：保持原三个账号和业务记录，沿第四轮正式浏览入口打开当前B/1 #2054545668044084247 或历史A/1 #2054545668044084233 的原PDF培训附件，只复验底层控件不遮挡工具栏/画布及关闭后列设置可用；按原PDF门禁等待画布实际非空。不重跑审批/下载/已通过权限分支。23结果待其独立复验，03未替代声明PASS。

本轮未新增业务对象、未变更账号/部门/权限/公共配置，未调用API/DB承接业务，无Maven/构建/重启/提交。原8061/PID20976、48061/PID27908监听一致。所有4次自有runner均退出，浏览器关闭/context0。定向GREEN runner exit0；baseline产品RED与两次定位校准失败原值保留。

工程状态：ready_for_closeout后只执行preview；禁止Git和cleanup apply，最终blocked。全量类型检查/构建未运行，交Root统一窗口；本修复不宣称全仓回归通过。
