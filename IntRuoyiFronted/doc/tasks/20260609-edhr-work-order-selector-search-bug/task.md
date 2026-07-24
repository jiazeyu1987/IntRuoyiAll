# eDHR 工单下拉搜索缺陷修复任务

- Task ID: `20260609-edhr-work-order-selector-search-bug`
- Status: `completed`
- Branch: `int_main`

## 任务目标

修复 eDHR 批次执行“打开/创建”弹窗中输入 `881` 后未出现实际存在的未冻结生产工单下拉项的问题，确保用户可以输入工单编码并选择有效未冻结工单。

## 里程碑

1. 复现：确认输入 `881` 的实际请求参数、接口返回和数据库中目标工单状态。
2. RED：新增或更新回归测试，先证明当前筛选口径会错误过滤用户期望的未冻结工单。
3. GREEN：最小修复前端/后端查询口径，使下拉显示有效未冻结工单。
4. REGRESSION：运行静态契约、TypeScript 检查和真实前端只读验证。

## 预期验证

- `node tests/e2e/edhr-batch-work-order-select-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 打开 `http://localhost:8081`，登录测试租户，进入 eDHR 执行列表，打开创建弹窗，输入 `881`，确认出现真实工单下拉项。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；查询失败必须显示明确错误，不回退手填 ID。
- `是否从根因和长期维护角度解决`：是；前端不再把有效未冻结工单误限定为已确认工单，并保留未取消过滤。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 用户证据：输入 `881` 时下拉显示“无数据”，但生产工单列表存在 `881MOO90863`、`881MOO90880` 两个未冻结工单。
- 根因：eDHR 工单下拉请求额外携带 `status=1`，把“有效未冻结”误收窄为“已确认且未冻结”。
- 修复：请求改为 `code=<keyword>&temporaryFrozen=false`，前端过滤已取消工单，文案改为“未取消且未临时冻结”。
- 验证：静态契约、TypeScript 检查、既有 eDHR 批次执行脚本检查、Playwright 真实前端只读验证均通过。
