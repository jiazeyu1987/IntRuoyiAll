# 一线 PQC 正式提交闭环

## Task Goal

将一线 PQC 页面从依赖路由参数且静默禁用的提交入口，改为能够通过正式生产提交绑定、发布态 QA 规程快照和当前登录人电子签名完成事务提交，并返回可追溯正式回执的闭环。

## Milestones

- [x] M1：确认现有前后端边界、正式数据来源和缺口。
- [x] M2：以 BDD 和严格 TDD 固化正式上下文、签名、提交结果与失败行为。
- [x] M3：实现后端正式提交上下文、电子签名和事务回执。
- [x] M4：实现前端明确阻塞状态、生产提交选择、签名确认和正式提交回执。
- [x] M5：完成定向后端、前端、合同和真实用户路径验证；成功写入型 E2E 因正式测试数据前置缺失而按规则阻塞并留证。
- [x] M6：完成经验沉淀与任务清理收尾。

## Expected Verification

- 后端定向 JUnit：生产提交候选唯一绑定、多候选显式选择、电子签名、数量/设备/规程/任务校验、事务回滚和幂等。
- 前端静态或组件合同：不再从路由读取 `productionSubmitEventId`、`signatureId`；缺前置点击提交显示明确原因；正式请求携带签名密码和结构化损耗数量；成功展示正式回执。
- `pnpm ts:check` 与受影响前端定向测试。
- Maven 受影响模块定向测试和必要回归。
- 具备真实登录、租户、QA 规程、待执行 PQC 任务、生产提交事件和签名凭据时，通过 Playwright 完成真实页面提交并核对正式记录；任一前置缺失时按规则记录精确 blocker，不使用 API-only 或 mock 替代。
- 技能 evidence validator 通过。

## Applicable Experience Gates

- `docs/backend-development.md#MES PQC 项目级检验快照门禁`：提交事实必须来自发布态 QA 规程和结构化 `itemResults[]`；设备、编号、方法、标准及逐件值均由后端按规程重新校验和冻结，禁止使用固定项目、前端默认值或 `rawPayload` 作为权威来源。
- `docs/backend-development.md#PQC 真实提交前置必须覆盖活跃路线全部当前工序`：真实页面必须通过 `active-order/processes` 正式校验；缺任一当前工序发布规程、待执行任务或冻结版本一致性时阻塞，禁止直接调用提交 API 或批量伪造无关规程绕过。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：backend/frontend evidence 必须先通过 validator，再把结果归档到保留报告后执行 cleanup。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；正式上下文、签名和结果均由服务端权威链路生成并校验。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

正式契约、前后端实现、35 个定向 JUnit、前端类型检查、6 组静态合同和真实页面失败路径验证均已完成；backend/frontend 技能证据 validator 通过，结果已归档。成功写入型 E2E 的正式数据前置限制已按门禁留证，经验已合并到现有前端开发门禁，最终 cleanup 无 blocked/warnings，核心任务记录保留。
