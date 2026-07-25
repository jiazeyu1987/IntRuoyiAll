# Frontend Feature Evidence

## Feature Goal

- 测试管理列表将原红框区域拆成“测试方法项”和“测试目标项”。
- 方法项来自 `methodText`，支持一行或多行顺序方法。
- 目标项来自 `checkpoints.expectedText`，支持一个或多个目标，按检查点 `sort` 顺序展示。

## Non-Goals

- 不改变后端接口、数据库 schema、权限、路由或执行逻辑。
- 不引入 fallback、mock 数据、兼容分支或静默降级。

## Acceptance

- AC1: 列表显示“测试方法项”列。
- AC2: 列表显示“测试目标项”列。
- AC3: 多行方法和目标以逐行方式展示，不再把目标只压缩为检查点数量。
- AC4: 新增/编辑表单文案同步为方法项与目标项。

## UI Entry

- Route/component: `src/views/system/codex-test-management/index.vue`
- Static test: `tests/e2e/system-codex-test-management-static.spec.js`
- Real E2E assertion file: `tests/e2e/system-codex-test-management-real.e2e.js`

## API Contract

- `CodexTestCaseVO.methodText` 保存测试方法项。
- `CodexTestCaseVO.checkpoints[].expectedText` 保存测试目标项。
- `CodexTestCaseVO.checkpoints[].sort` 控制目标项展示顺序。

## BDD

- BDD: 列表分栏展示方法项与目标项 -> Given 测试项存在多行自然语言方法和一个或多个检查点目标 / When 用户打开测试管理列表 / Then 列表显示“测试方法项”和“测试目标项”两列，方法按行展示，目标按检查点顺序展示。

## RED / GREEN

- RED: `node tests/e2e/system-codex-test-management-static.spec.js` -> FAIL, 页面缺少“测试方法项”列。
- GREEN: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node --check tests/e2e/system-codex-test-management-real.e2e.js` -> PASS。

## State Checks

- Loading/error handling：未改请求流程，原 `showRequestError` 保持显式错误提示。
- Empty state：方法或目标为空时显示 `-`，不伪造成功数据。
- Permission：未改 `v-hasPermi` 权限点。
- Responsive：列宽调整为方法 300、目标 320，并使用自动换行样式避免长文本截断。

## Blockers

- 未执行真实浏览器 E2E；本次未启动/重启本地前后端服务，也未变更登录或租户数据。
- Git closeout 受并发工作区状态限制：当前分支已 ahead `origin/int_main`，且存在其他任务未提交/未跟踪文件。