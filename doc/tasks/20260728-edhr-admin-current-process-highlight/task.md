# 20260728-edhr-admin-current-process-highlight

## Task Goal

让拥有批记录管理员角色的账号在批次执行详情页能看到当前正在进行/当前待处理的批记录工序状态：即使当前账号不是该节点填写人，也要能从左侧工序列表明确识别当前工序的黄色运行态。

## Milestones

- [x] 梳理批次执行详情页当前工序、高亮样式、任务状态和管理员只读/操作权限边界。
- [x] 用 BDD + RED 静态合同复现管理员只能看到待打开/只读时左侧工序未黄色高亮的问题。
- [x] 实现最小前端状态展示修复，不改变后端权限、填写权限或任务流转语义。
- [x] 运行目标静态合同和相关回归验证。
- [x] 完成收尾、记录验证结果和最终状态。

## Expected Verification

- `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js`
- 必要时运行相邻批次执行详情静态合同。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是把“当前工序运行态展示”与“当前用户是否为填写人/是否可打开”解耦。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#前端静态契约隔离门禁`：本任务新增聚焦静态合同覆盖当前缺陷，不改无关宽契约。
- `docs/frontend-development.md#前端-route-query-id-比较门禁`：当前工序/任务 ID 来自接口数字与页面状态，比较时必须用统一 ID 语义，避免字符串/数字导致高亮丢失。
- `docs/frontend-development.md#eDHR 当前工序运行态展示门禁`：当前工序黄色运行态必须用详情接口 `currentProcess*` 与正式工序身份匹配，不能依赖 `OPEN_FORM`、填写人或角色推断。
- `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁`：无填写权限但有查看权限时，只读查看不得触发 `/task/open` 或写请求；本任务只改状态展示，不放宽打开/跳过权限。
- `docs/backend-development.md#eDHR 详情回填门禁`：不得用当前登录人、角色 ID 或前端文案推断填写人；本任务复用详情接口已有 `currentProcess*` 与任务状态，不新增后端推断。

## Final Verification Result

PASS。目标静态合同、相邻状态/权限合同、产品信息虚拟工序合同、前端类型检查、证据校验和 task-closeout preview/apply 均通过；真实浏览器 E2E 未运行，本任务没有启动服务或写入业务数据。
