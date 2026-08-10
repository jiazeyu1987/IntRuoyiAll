# QA 未识别批记录绑定工序继续显示变更

## Request Summary And Source

- 来源：用户在当前任务中明确说明“组装螺杆八组件、光固外套四组件等都是工序，只是对应的批记录表单还没有识别绑定，也照样显示出来就可以”。
- 请求：QA 检验项目的业务工序不得因为尚未识别或绑定批记录表单而从页面消失或阻断保存/发布。

## Current Baseline Reviewed

- 当前 QA 页面模板已经保留 `processName` 并显示全部来源项目。
- 当前 `resolveQaRegulationItemRouteProcesses` 强制把每个业务工序名称映射到激活路线版本工序；未匹配时在任何写请求前抛错。
- QA 保存 API 仍要求正式 `routeProcessId/processId`，后端按该身份生成工序级规程；批记录绑定摘要本身是可选字段。
- IDI 当前路线的正式工序主数据不包含“组装螺杆八组件、光固外套四组件、装配、整体粘结”等名称，不能通过字符串猜测映射到组装Ⅰ/Ⅱ、光固Ⅰ/Ⅱ等路线工序。

## Classification

- Requirement change / product behavior correction.

## Impact Analysis

- Product: QA 来源列表继续显示所有业务工序；未识别批记录绑定不再阻断保存/发布。
- Design: 未匹配业务工序仅作为 QA 项目分组名称保留，不展示伪造的批记录绑定摘要。
- Data: 已唯一匹配路线工序的项目仍按其正式 `routeProcessId/processId` 发布；未匹配项目归入页面已正式解析的 QA 质检工序，不新增或猜测路线工序身份。
- API: 保持现有 `QaInspectionRegulationSaveReqVO` 不变，不放宽后端必填身份。
- Tests: 新增聚焦静态合同，覆盖未识别工序继续构造载荷、原始 `processName` 继续显示、已识别工序仍按原身份发布，以及禁止为 IDI 猜测组装/光固映射。
- Release: 前端行为变更；无数据库迁移、无远程发布。
- Operations: 真实验证仍使用本机 `芋道源码` 租户和 Playwright；写入必须遵循任务数据清理门禁。

## Decision

- Accepted.
- 用户是当前任务请求人，已明确确认未识别批记录绑定不应阻断业务工序显示；采用现有正式 QA 质检工序承载未匹配项目，可保持 API 身份完整且不猜测路线映射。

## Required Approvals

- 当前用户已批准该显示与发布口径。
- 不包含数据库、远程环境、Git 或发布授权。

## Downstream Skill Reruns

- `frontend-feature-delivery`：补充 BDD/RED/GREEN，修改 QA 页面载荷分组并完成聚焦回归。
- `independent-verification-gate`：复核静态合同、类型检查和真实页面路径。
- `project-experience-consolidation`：收尾前核对长期 QA 多工序门禁是否需要按新口径更新。

## Blockers And Next Action

- 当前无实现 blocker。
- 下一步：先新增未识别业务工序继续显示/构造载荷的 RED 合同，再实现最小前端规则。
