# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 点击测试项“修改”时，按当前测试项已有内容逐条回显测试方法项和测试目标项。
- Non-goal: 不修改后端接口、数据库字段、Runner 执行契约或列表展示规则。

## Requirements And Acceptance IDs

- REQ-1: 修改按钮必须把当前行传入编辑回显流程。
- REQ-2: 编辑弹窗必须将当前 `methodText` 拆成逐条方法项。
- REQ-3: 编辑弹窗必须将当前 `checkpoints` 按 sort 排序并逐条回显。
- REQ-4: 若历史目标项 `expectedText` 含多行内容，编辑时必须拆成多条目标项，便于逐条修改。

## UI Entry Points, Routes, Components, And Owned Files

- Entry: 系统管理 > 测试管理 > 测试项列表 > 修改。
- Component: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`。
- Test: `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`。

## API Contracts And Data States

- API contract unchanged: `CodexTestCaseVO.methodText` remains newline-delimited string and `checkpoints` remains the target-item collection.
- Edit state: `openEdit(row)` fetches detail by current row id, then `applyCaseFormForEdit` normalizes method and target rows.
- Method state: `parseMethodItems(data.methodText)` splits current method text into editable rows.
- Target state: `normalizeCheckpointItems(data.checkpoints)` sorts checkpoints and splits multi-line expected text into editable rows.

## BDD Scenarios

- `BDD: 修改测试项逐条回显 -> Given 测试项已有多条测试方法和多条测试目标 When 用户点击该测试项的“修改”按钮 Then 弹窗应按当前测试项内容逐条回显方法项和目标项，并允许用户在已有条目基础上修改`

## RED Command And Expected Failure

- RED: `pnpm e2e:system:codex-test-management:static` -> FAIL，预期失败在修改按钮仍只传 `row.id`，缺少编辑回显归一化。

## GREEN Command And Passing Result

- GREEN: `pnpm e2e:system:codex-test-management:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive/layout: 继续复用现有方法项和目标项逐条录入布局。
- Accessibility/usability: 修改入口仍为可见“修改”按钮，弹窗先展示已有条目再允许用户编辑。
- Loading: 不改变列表和弹窗加载机制。
- Empty: 若方法或目标为空，仍保留 1 条空行供输入。
- Error: 详情加载失败继续通过 `测试项详情加载失败` 显式提示。
- Permission: 修改按钮权限未变，仍使用 `system:codex-test:update`。

## E2E Or Component Verification Path

- 静态合同：`pnpm e2e:system:codex-test-management:static`。
- 类型检查：`pnpm ts:check`。

## Blockers And Follow-Up Skills

- Blocker: 工作区存在大量非本任务脏改动，本任务不执行 commit/push，避免混入并行任务。
- Follow-up skills: 本次经验已被现有测试管理静态合同与 Element Plus 表单布局门禁覆盖，不新建长期经验文档。
