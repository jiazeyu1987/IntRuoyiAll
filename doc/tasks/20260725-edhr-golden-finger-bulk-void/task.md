# 20260725 eDHR 金手指批次执行一键作废

## Task Goal

在批次执行页面新增仅金手指权限可见的一键作废测试功能：按当前筛选条件跨页选择所有可作废批次，并通过金手指直通路径直接作废，不修改全局审核流程，也不影响现有单条作废审批流程。

## Milestones

1. 建立任务文档、BDD 场景和 RED/GREEN 验证记录。
2. 新增后端金手指批量作废接口、请求/响应契约、权限校验和服务逻辑。
3. 新增前端 API、金手指按钮、确认弹窗、提交和结果刷新逻辑。
4. 补充后端与前端静态/契约测试，验证现有单条作废流程不变。
5. 完成验证、证据归档、cleanup、提交和推送。

## Expected Verification

- 后端定向测试覆盖金手指权限、批量直通作废、不可作废批次处理和不创建 BPM 审批流程。
- 前端静态测试覆盖按钮权限、当前筛选跨页文案、接口路径和不调用审批解析接口。
- 回归验证现有单条作废申请仍使用 `/mes/pro/edhr-change/void-batch-execution/request`。
- 若本地运行态、测试账号和数据满足条件，再补充真实 Playwright E2E；否则记录阻塞原因，不用 mock 或 API-only 冒充通过。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只新增用户明确要求的金手指直通作废入口，不引入隐式 fallback、默认成功或吞异常。
- `是否从根因和长期维护角度解决`：是。复用现有直通作废领域服务和审计链路，后端强校验权限，单条正式审批流程保持不变。
- `是否存在临时补丁或绕过`：否。直通范围限定在新增金手指批量接口，不修改全局 BPM/审批配置。

## Experience Gates

### Element Plus 表格选择门禁

- Trigger: 批次执行列表批量操作、表格复选框、表头全选、跨页选择。
- Preflight check: 写入型 E2E 不得用表头全选或数组下标定位；本功能设计为提交当前筛选条件，避免把当前页 DOM 选择误认为跨页选择。
- Blocker: 若真实 E2E 需要勾选可见行，必须先断言已选业务唯一键集合；若选中集合不精确，停止写入验证。
- Verification: 前端静态测试断言一键作废提交筛选参数而非表格 header checkbox；真实 E2E 若执行，记录选中/筛选范围和最终状态。
- Forbidden action: 禁止用 Element Plus 表头全选、坐标点击、接口数组下标、API-only 或直接 SQL 替代真实页面写入路径。
- Evidence: `docs/e2e-rules.md#element-plus-表格选择门禁`

### eDHR 批次执行数据库夹具与证据文件门禁

- Trigger: 运行或修改批次执行真实 E2E、创建写入型批次执行测试数据、覆盖 E2E evidence。
- Preflight check: 真实写入 E2E 前必须确认本机 Docker MySQL、授权租户/账号、可追踪任务自有测试数据、证据路径和清理方式。
- Blocker: 本地数据库不可达、授权账号/租户缺失、测试数据不可追踪、证据路径会覆盖非当前任务历史 PASS 时停止。
- Verification: 记录 E2E 命令、入口 URL、租户/账号标签、批次执行 ID、写入结果、清理方式和证据文件路径。
- Forbidden action: 禁止使用 mock、API-only、生产/未授权租户、未记录数据库直改或默认成功冒充真实 E2E 通过。
- Evidence: `docs/e2e-rules.md#edhr-批次执行数据库夹具与证据文件门禁`
