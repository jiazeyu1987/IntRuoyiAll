# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: PQC 组长新增 `人员管理` tab，使用标准列表模板展示关联 PQC 检验员，并通过弹窗新增正式检验员。
- Non-goals: 不新增 PQC 临时工，不复用生产人员档案的签名密码管理。

## UI Entry Points

- `IntRuoyiFronted/src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`

## API Contracts

- `GET /mes/pro/process-pool/team-leader/pqc-personnel/list`
- `GET /mes/pro/process-pool/team-leader/pqc-personnel/formal-candidates`
- `POST /mes/pro/process-pool/team-leader/pqc-personnel/formal/link`
- `PUT /mes/pro/process-pool/team-leader/pqc-personnel/status/update`

## BDD Scenarios

- PQC组长查看人员管理：默认 tab 和标准列表可见。
- PQC组长关联检验员：新增弹窗选择正式员工并刷新列表。
- PQC组长维护检验员状态：启用/禁用操作明确调用后端。

## RED

- `node tests/e2e/pqc-leader-personnel-tab-static.spec.js`：FAIL，旧实现缺少 personnel 默认 tab。

## GREEN

- PQC personnel static contract、相邻 tab contract、生产组长回归 contract：PASS。
- `pnpm ts:check`：PASS。

## Checks

- Loading: 列表加载和提交按钮分别显示 loading。
- Empty: 无关联人员时表格显示空数据。
- Error: API 失败使用 `ElMessage.error` 暴露，不吞异常。
- Permission: 使用现有班组长 query/maintain 权限。
