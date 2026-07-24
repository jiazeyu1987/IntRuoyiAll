# 下次排产任务需求文档整理

## 任务目标

将产品方提出的下一轮排产需求整理为可开发、可验收的需求文档，明确第一版本范围、非目标、业务规则、用户流程、验收标准、开放问题和阻塞项，为后续系统设计、BDD/TDD 计划和实现拆分提供输入。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-workstation-shift-hours-runtime-schema/task.md`。
- 检查结果：该任务已标记 `completed` 并已提交。
- 相关已完成能力：工艺路线结构化排产资源、人工产能编辑、工作站单人小时产能默认补齐、工作站班次小时字段。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只整理需求，明确缺路线、缺产能、缺交期、缺同步数据时必须暴露为提示或阻塞，不允许静默成功。
- `是否从根因和长期维护角度解决`：是。需求要求继续复用现有工艺路线、产品、工序、设备、工作站、人力、ERP 生产订单、生产工单、排产工单和报工数据；生产工单负责 ERP 同步，排产工单负责进入排产，避免排产数据与底层来源割裂。
- `是否存在临时补丁或绕过`：否。本任务只整理排产需求，不纳入排产范围外事项。

## BDD 场景

- BDD: 产品路线主数据可支撑排产 -> Given 产品代码、规格型号、工艺路线和工序产能已维护 / When 计划员选择订单参与排产 / Then 系统能按产品找到有效路线，并按工序编号匹配设备或人工产能。
- BDD: 复制路线带出完整用途配置 -> Given 原工艺路线已维护排产配置、排产禁用工序、批处理配置和批处理禁用工序 / When 用户复制该工艺路线 / Then 系统自动生成新路线编号，并将基础工序、排产配置、排产工序启用范围、批处理配置和批处理工序启用范围复制到新路线，原路线不受影响。
- BDD: 同一路线分离排产与批处理配置 -> Given 同一条基础工艺路线同时用于排产和批处理 / When 用户维护排产参数或批处理模板 / Then 系统只更新对应用途配置，不复制基础路线、不污染另一用途配置。
- BDD: 排产与批处理工序独立启禁用 -> Given 基础工艺路线包含多道工序 / When 用户在排产配置中禁用某工序并在批处理配置中保留该工序 / Then 排产计算排除该工序，批处理执行仍包含该工序，且两个用途互不影响。
- BDD: ERP 增量同步不覆盖排产工单 -> Given 已完成首次最近一年 ERP 生产订单同步并生成生产工单，且部分生产工单已生成排产工单 / When 后续每天定时检测 ERP 并按 ERP 最后更新时间水位增量更新生产工单 / Then 系统按 ERP 工单编码幂等新增或更新生产工单，并对影响已有排产工单的变更生成差异提示，排产工单只能由计划员手动更新。
- BDD: 生产工单筛选生成排产工单 -> Given 生产工单表已从 ERP 同步订单 / When 计划员筛选生产工单、填写承诺交期并加入排产 / Then 系统生成或更新排产工单，禁止将同一生产工单拆分为多张有效排产工单，后续排产数量、优先级、承诺交期和风险状态均以排产工单为准。
- BDD: 承诺交期驱动排产风险 -> Given 订单存在承诺交期和优先级 / When 系统生成排产计划 / Then 系统反推计划开始时间，并在无法满足承诺交期时提示延迟风险和可调整方向。
- BDD: 报工偏差看板与夜间定时重排 -> Given 前一天存在报工完成量和偏差 / When 系统每天晚上定时重排 / Then 看板展示提前、准时、延迟订单及其原因，报工偏差只作为看板提示。

## 里程碑

- [x] M1：梳理用户原始需求与既有自动排产/工艺路线资源文档。
- [x] M2：形成 PRD，明确第一版本范围、非目标、业务规则和开放问题。
- [x] M3：形成用户流程和验收标准。
- [x] M4：运行产品需求文档校验并提交。

## 预期验证

- `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements`
- 人工检查：文档包含产品方列出的排产主数据、生产工单到排产工单、排产工单池、产能匹配、优先级、ERP 同步、承诺交期、延迟风险、每日重排看板需求。

## 当前状态

completed

## 完成记录

- 已新增下一轮排产任务 PRD：`doc/tasks/20260609-next-scheduling-requirements/docs/product/prd.md`。
- 已新增用户流程：`doc/tasks/20260609-next-scheduling-requirements/docs/product/user-flows.md`。
- 已新增验收标准：`doc/tasks/20260609-next-scheduling-requirements/docs/product/acceptance-criteria.md`。
- 文档将产品方需求整理为排产主数据、生产工单到排产工单、排产工单池、产能匹配、优先级、ERP 同步、承诺交期、延迟风险、产能调整、报工偏差看板和夜间定时重排。
- 已按用户补充口径明确：生产工单只负责从 ERP 同步数据；排产工单来自生产工单筛选，是排产计算、排产数量、优先级、承诺交期、风险和看板的数据来源。
- 已按用户确认口径明确：ERP 生产订单唯一键为工单编码；ERP 不提供承诺交期，承诺交期由计划员在生产工单转排产工单时填写；排产工单不允许拆分；工艺路线配置版本自动编号；每天检测一次 ERP，新工单自动更新到生产工单；生产工单变更不直接覆盖排产工单，排产工单由计划员手动更新；每日报工偏差只做看板提示，重排由每天晚上定时任务执行。
- 已按用户继续确认口径明确：排产数量必须等于生产工单数量；ERP 同步范围为当前日期往前一年期间的生产订单；ERP 每天晚上 2 点更新；每天晚上必须重排，已经报工的任务不动；ERP 工单状态只区分已完成/未完成；工艺路线配置版本采用清晰编号格式；有权限用户可直接修改排产相关信息，不走审批。
- 已按用户补充口径明确：基础工艺路线只表达真实工序顺序；排产配置与批处理配置分别挂在路线/工序下维护，只有真实工序顺序变化时才复制或新建路线版本。
- 已按用户确认口径明确：复制工艺路线时同步复制排产配置、排产工序启用范围、批处理配置和批处理工序启用范围；新路线编号由系统自动生成，复制后默认草稿且不影响原路线。
- 已按用户补充口径明确：排产配置和批处理配置各自拥有独立工序启用范围；禁用某用途工序不得删除基础工序，也不得影响另一用途。

## 最终验证

- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS。
- GREEN: `rg --no-ignore -n "复制|路线编号|批量|反向查询|订单池|MAS|产能|优先级|金蝶|承诺交期|延迟风险|报工" doc\tasks\20260609-next-scheduling-requirements` -> PASS，产品方主需求均有覆盖。
- GREEN: `rg --no-ignore -n "生产工单|排产工单|ERP" doc\tasks\20260609-next-scheduling-requirements` -> PASS，生产工单与排产工单边界已写入需求。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，ERP 同步水位和差异提示补充后文档结构有效。
- GREEN: `rg --no-ignore -n "首次同步|最后更新时间|水位|工单编码|幂等|差异提示|不直接覆盖排产工单" doc\tasks\20260609-next-scheduling-requirements` -> PASS，ERP 同步水位和生产工单/排产工单差异处理需求已写入文档。
- GREEN: `rg --no-ignore -n "基础工艺路线|排产配置|批处理配置|配置快照|批处理执行" doc\tasks\20260609-next-scheduling-requirements` -> PASS，路线用途配置分离需求已写入文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，用途内工序启禁用补充后文档结构有效。
- GREEN: `rg --no-ignore -n "排产工序|批处理工序|启用范围|禁用|互不影响|删除基础工序" doc\tasks\20260609-next-scheduling-requirements` -> PASS，用途内工序启禁用需求已写入文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，路线复制带出用途配置和自动编号补充后文档结构有效。
- GREEN: `rg --no-ignore -n "自动生成|复制.*排产配置|复制.*批处理配置|默认.*草稿|路线编号|排产工序启用范围|批处理工序启用范围" doc\tasks\20260609-next-scheduling-requirements` -> PASS，路线复制带出用途配置和自动编号需求已写入文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，移除非排产事项后文档结构有效。
- GREEN: removed-scope keyword scan -> PASS，任务目录中无残留引用。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS，用户确认的 ERP 工单编码、承诺交期手填、排产工单不拆分、版本自动编号、报工偏差看板和夜间重排规则补充后文档结构有效。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS，排产数量等于生产工单数量、ERP 晚上 2 点更新、已报工任务不动和直接修改不走审批等补充口径已通过文档结构校验。

## Cleanup Keep

- `doc/tasks/20260609-next-scheduling-requirements/docs/product/prd.md`
- `doc/tasks/20260609-next-scheduling-requirements/docs/product/user-flows.md`
- `doc/tasks/20260609-next-scheduling-requirements/docs/product/acceptance-criteria.md`
