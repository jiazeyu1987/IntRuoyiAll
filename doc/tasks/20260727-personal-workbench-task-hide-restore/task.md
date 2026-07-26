# 个人工作台任务隐藏/恢复

## Task Goal

实现个人工作台任务的隐藏与恢复能力：用户可在个人工作台隐藏不想展示的任务，并可在恢复入口查看隐藏任务后恢复展示。

## Milestones

- [x] 建立任务文档与经验门禁
- [x] 定位个人工作台任务前后端契约、数据模型与测试入口
- [x] 记录 BDD 场景并补充 RED 测试
- [x] 实现后端隐藏/恢复接口与前端操作入口
- [x] 完成 GREEN/回归验证并记录证据
- [x] 收尾清理、经验沉淀、提交与推送

## Expected Verification

- 后端定向测试覆盖隐藏、恢复、用户隔离、租户隔离与非法请求 fail-fast。
- 前端静态契约覆盖隐藏操作、隐藏列表入口、恢复操作、API 调用与角标刷新触发。
- 前端补充校验覆盖 SFC 解析与本任务影响文件 ESLint。
- 全量 `pnpm ts:check` 已尝试两次，均因超时未产出诊断；未作为通过证据。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是在正式任务数据链路中增加可审计的隐藏状态，不以前端本地过滤或默认空结果绕过。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/database-rules.md`。
- `docs/experience-index.md` 存在；当前需求命中前端静态契约隔离门禁、真实 E2E 需读取 E2E 规则、涉及 schema 或菜单权限时需按数据库规则先核对。

## Cleanup Keep

- doc/tasks/20260727-personal-workbench-task-hide-restore/backend-api-evidence.md
- doc/tasks/20260727-personal-workbench-task-hide-restore/frontend-feature-evidence.md
