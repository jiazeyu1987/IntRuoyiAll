# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 测试管理列表中“测试方法项 / 测试目标项”每个独立项占用一条 Element Plus 表格行。
- Goal: 同一测试项的选择框、测试项、检查点、默认方法、状态和操作列合并显示，避免重复视觉噪音。
- Goal: 排产手动重排样例中黄色范围核验描述归入测试目标项，方法项只保留操作步骤。
- Non-goal: 不改变测试管理 API 契约、菜单权限、Runner 执行流程或真实业务数据模型。

## Requirements And Acceptance

- AC-1: 多个方法项按原顺序展开到多条表格行。
- AC-2: 多个目标项按原顺序展开到多条表格行。
- AC-3: 方法项和目标项数量不一致时，较短列空白补齐，公共列仍按测试项合并。
- AC-4: 表格选择变更按测试项 ID 去重，不因展开行导致重复执行。
- AC-5: 手动重排样例方法文本不再包含“完成后核验...”，相关核验内容在目标项中维护。

## UI Entry And Owned Files

- Entry: `系统管理 > 测试管理`。
- Component: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`。
- Static contract: `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`。
- Real E2E syntax target: `IntRuoyiFronted/tests/e2e/system-codex-test-management-real.e2e.js`。
- Sample script: `doc/tasks/20260725-test-management-manual-replan-881mo/test-management-manual-replan-full.e2e.cjs`。
- Backend root-cause support: `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/codextest/CodexTestCheckpointMapper.java` and `IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestCaseServiceImplTest.java`。

## API Contracts And Data States

- Case list API remains unchanged; frontend derives display rows from existing `methodText` and `checkpoints`.
- No mock data or fallback state was introduced.
- Backend checkpoint replacement now physically deletes old checkpoint rows before inserting replacements, preserving the existing update API behavior while avoiding soft-delete unique-key conflicts.
- Existing local sample case id `1` was corrected so method text contains only the two operation steps and target/checkpoints retain the four expected verification targets.

## BDD Scenarios

- BDD: 方法目标展开成表格行 -> Given 一个测试项有多个方法项和多个目标项 / When 用户打开测试管理列表 / Then 每个方法项或目标项占用独立表格行，同一测试项公共列合并显示。
- BDD: 排产手动重排目标归属 -> Given 手动重排样例包含“重排成功、仅目标两个工单产品编号变橙色、最近一次成功排产时间更新、生产排产甘特图范围” / When 用户查看测试管理列表 / Then 这些核验描述显示在测试目标项列，方法项列只保留操作步骤。
- BDD: 检查点重复替换 -> Given 已存在测试项多次编辑目标项 / When 后端更新检查点集合 / Then 旧检查点被真实删除，新检查点可以按同一 caseId 和 sort 重建，不触发软删除唯一键冲突。

## RED And GREEN Evidence

- RED: `node tests/e2e/system-codex-test-management-static.spec.js` -> FAIL, 旧静态合同缺少展开表格行契约。
- RED: `mvn -pl yudao-module-system -am -Dtest=CodexTestCaseServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增重复替换回归暴露软删除唯一键冲突。
- GREEN: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/system-codex-test-management-real.e2e.js` -> PASS。
- GREEN: `node --check ..\doc\tasks\20260725-test-management-manual-replan-881mo\test-management-manual-replan-full.e2e.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-system -am -Dtest=CodexTestCaseServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `pnpm ts:check` -> PASS, exit code 0。

## UI State Checks

- Responsive: 沿用现有 Element Plus 表格布局、列宽和紧凑样式；未引入独立响应式断点。
- Accessibility: 保留现有 `el-table`、选择列和操作按钮结构；展开行为不改变可点击控件语义。
- Loading: 保留 `v-loading="caseLoading"`。
- Empty: `formatMethodItems` 和 `formatTargetItems` 仍返回 `['-']` 作为空态。
- Error: 现有请求错误仍走 `showRequestError`，未吞异常。
- Permission: 未变更菜单、路由或权限绑定。

## E2E Or Component Verification Path

- 静态合同覆盖页面必须使用 `caseTableRows`、`caseRowSpanMethod`、`displayMethodItem`、`displayTargetItem` 和 `:span-method="caseRowSpanMethod"`。
- 真实 E2E 脚本语法检查覆盖 `测试方法项` 与 `测试目标项` 可见文案。
- 样例脚本语法检查覆盖新增目标项按钮、目标项归属和 `case-only` 模式。

## Blockers And Follow-Up Skills

- Blockers: 无本任务阻塞。
- Follow-up skills: 不需要新增技能；本次按 `frontend-feature-delivery` 与 `task-closeout-cleanup` 收尾。
