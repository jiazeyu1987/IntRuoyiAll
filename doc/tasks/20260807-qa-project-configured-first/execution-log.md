# Execution Log

## User Intent

- QA 规程配置页的 DCC 项目代码下拉中，已经配置过 QA 规程的产品排在前面，没有配置的产品排在后面。

## Preflight

- SKILL: `bug-regression-fix-loop` -> LOADED。
- SKILL: `frontend-feature-delivery` -> LOADED。
- RULE: `docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md` -> READ。
- EXPERIENCE: `docs/experience-index.md` -> READ；命中 `docs/backend-development.md#QA 规程配置状态必须来自产品级规程记录`、前端静态契约隔离和复合输入控件交互保留门禁。
- API CONTRACT: `QcTemplateApi.getQaRegulationProjectStatuses(productIds)` 调用 `/mes/qa/inspection-regulation/project-statuses`，响应以正式 `productId` 和布尔 `configured` 表示 QA 规程配置状态。
- BACKEND CONTRACT: `MesQaInspectionRegulationServiceImpl.getProjectStatuses` 对请求产品 ID 去重后按请求顺序返回已配置与未配置状态。

## BDD / TDD

- BDD: 已配置产品优先 -> Given DCC 项目代码候选包含多个正式绑定产品且正式 QA 状态有已配置与未配置混合 / When QA 页面加载或远程搜索项目候选 / Then 所有 `configured=true` 的产品候选位于 `configured=false` 候选之前。
- BDD: 分组内保持原顺序 -> Given 后端 DCC 项目代码候选依次为多个同配置状态产品 / When 页面按正式 QA 状态分组 / Then 同一状态分组内的项目顺序与 DCC 接口响应顺序一致。
- BDD: 状态缺失立即暴露 -> Given DCC 候选中某个已绑定产品没有对应正式 QA 状态或 `configured` 不是布尔值 / When 页面加载候选 / Then 页面显示 DCC 项目代码加载失败，不得把该项目静默归入待配置。
- BDD: 无产品绑定不伪造状态 -> Given 历史 DCC 项目代码没有 `productMasterId` / When 页面加载候选 / Then 该项目不被标记为已配置或未配置，排在已绑定产品之后且现有保存阻塞不变。

## Milestone Evidence

- M1 in progress：已定位 `QaRegulationPage.vue` 的 `loadDccProjectCodeOptions()` 直接按 DCC 接口顺序赋值，尚未调用正式产品级 QA 配置状态接口，因此不满足已配置优先展示。
