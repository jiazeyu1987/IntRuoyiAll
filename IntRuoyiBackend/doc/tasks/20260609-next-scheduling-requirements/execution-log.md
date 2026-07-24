# 下次排产任务需求文档整理执行日志

## 2026-06-09

- BDD: 产品路线主数据可支撑排产 -> Given 产品代码、规格型号、工艺路线和工序产能已维护 When 计划员选择订单参与排产 Then 系统能按产品找到有效路线，并按工序编号匹配设备或人工产能。
- BDD: 复制路线带出完整用途配置 -> Given 原工艺路线已维护排产配置、排产禁用工序、批处理配置和批处理禁用工序 When 用户复制该工艺路线 Then 系统自动生成新路线编号，并将基础工序、排产配置、排产工序启用范围、批处理配置和批处理工序启用范围复制到新路线，原路线不受影响。
- BDD: 同一路线分离排产与批处理配置 -> Given 同一条基础工艺路线同时用于排产和批处理 When 用户维护排产参数或批处理模板 Then 系统只更新对应用途配置，不复制基础路线、不污染另一用途配置。
- BDD: 排产与批处理工序独立启禁用 -> Given 基础工艺路线包含多道工序 When 用户在排产配置中禁用某工序并在批处理配置中保留该工序 Then 排产计算排除该工序，批处理执行仍包含该工序，且两个用途互不影响。
- BDD: ERP 增量同步不覆盖排产工单 -> Given 已完成首次最近一年 ERP 生产订单同步并生成生产工单，且部分生产工单已生成排产工单 When 后续每天定时检测 ERP 并按 ERP 最后更新时间水位增量更新生产工单 Then 系统按 ERP 工单编码幂等新增或更新生产工单，并对影响已有排产工单的变更生成差异提示，排产工单只能由计划员手动更新。
- BDD: 生产工单筛选生成排产工单 -> Given 生产工单表已从 ERP 同步订单 When 计划员筛选生产工单、填写承诺交期并加入排产 Then 系统生成或更新排产工单，禁止将同一生产工单拆分为多张有效排产工单，后续排产数量、优先级、承诺交期和风险状态均以排产工单为准。
- BDD: 承诺交期驱动排产风险 -> Given 订单存在承诺交期和优先级 When 系统生成排产计划 Then 系统反推计划开始时间，并在无法满足承诺交期时提示延迟风险和可调整方向。
- BDD: 报工偏差看板与夜间定时重排 -> Given 前一天存在报工完成量和偏差 When 系统每天晚上定时重排 Then 看板展示提前、准时、延迟订单及其原因，报工偏差只作为看板提示。
- READONLY: 已检查现有 `docs/product/prd.md`、`docs/product/user-flows.md`、`docs/product/acceptance-criteria.md`，确认当前文档偏自动排产第一版，本次新增任务级下一轮需求，不直接覆盖既有总 PRD。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS。
- GREEN: `rg --no-ignore -n "复制|路线编号|批量|反向查询|订单池|MAS|产能|优先级|金蝶|承诺交期|延迟风险|报工" doc\tasks\20260609-next-scheduling-requirements` -> PASS，产品方主需求均有覆盖。
- CHANGE: 按用户补充，将需求口径调整为 `ERP生产订单 -> 生产工单表 -> 排产工单表 -> 排产计划`；生产工单只负责 ERP 同步，排产工单作为排产计算的数据来源。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，排产工单口径调整后文档结构仍有效。
- GREEN: `rg --no-ignore -n "生产工单|排产工单|ERP" doc\tasks\20260609-next-scheduling-requirements` -> PASS，生产工单与排产工单边界已写入 PRD、流程和验收标准。
- CHANGE: 按用户确认，新增 ERP 同步规则：首次同步最近一年生产订单，后续每天检测一次 ERP 并按 ERP 最后更新时间水位增量同步，按 ERP 工单编码幂等新增或更新生产工单；生产工单变更只生成差异提示，不直接覆盖排产工单。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，ERP 同步水位和差异提示补充后文档结构仍有效。
- GREEN: `rg --no-ignore -n "首次同步|最后更新时间|水位|工单编码|幂等|差异提示|不直接覆盖排产工单" doc\tasks\20260609-next-scheduling-requirements` -> PASS，ERP 同步水位和生产工单/排产工单差异处理需求已写入 PRD、流程、验收标准和任务记录。
- CHANGE: 按用户补充，将同一条工艺路线拆成“基础工艺路线 + 排产配置 + 批处理配置”需求口径；排产工单保存排产配置快照，批处理执行保存批处理配置快照。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，路线用途配置分离调整后文档结构仍有效。
- GREEN: `rg --no-ignore -n "基础工艺路线|排产配置|批处理配置|配置快照|批处理执行|电子批记录|用途配置" doc\tasks\20260609-next-scheduling-requirements` -> PASS，路线用途配置分离需求已写入 PRD、流程和验收标准。
- CHANGE: 按用户补充，新增排产工序和批处理工序独立启用/禁用需求；基础工序保留，禁用只影响对应用途配置。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，用途内工序启禁用补充后文档结构仍有效。
- GREEN: `rg --no-ignore -n "排产工序|批处理工序|启用范围|禁用|互不影响|删除基础工序" doc\tasks\20260609-next-scheduling-requirements` -> PASS，用途内工序启禁用需求已写入 PRD、流程、验收标准和任务记录。
- CHANGE: 按用户确认，新增工艺路线复制规则：复制路线时同步复制排产配置、排产工序启用范围、批处理配置和批处理工序启用范围；新路线编号由系统自动生成，复制后默认草稿且不影响原路线。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，路线复制带出用途配置和自动编号补充后文档结构仍有效。
- GREEN: `rg --no-ignore -n "自动生成|复制.*排产配置|复制.*批处理配置|默认.*草稿|路线编号|排产工序启用范围|批处理工序启用范围" doc\tasks\20260609-next-scheduling-requirements` -> PASS，路线复制带出用途配置和自动编号需求已写入 PRD、流程、验收标准和任务记录。
- CHANGE: 按用户确认，移除非排产事项，不再作为本任务需求范围。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root doc\tasks\20260609-next-scheduling-requirements` -> PASS，移除非排产事项后文档结构仍有效。
- GREEN: removed-scope keyword scan -> PASS，任务目录中无残留引用。
- CHANGE: 按用户确认，补充排产边界：ERP 生产订单唯一键为工单编码；ERP 不提供承诺交期；承诺交期在生产订单转成排产订单时由排产员填写；排产工单不允许拆分；工艺路线配置版本自动编号；每天检测一次 ERP，新工单自动更新到生产工单；排产工单由排产员手动更新；每日报工偏差只做看板提示；重排由每天晚上定时任务执行。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS，用户确认的排产边界补充后文档结构有效。
- CHANGE: 按用户继续确认，补充排产数量、ERP 同步、夜间重排和权限边界：排产数量必须等于生产工单数量；ERP 同步范围为当前日期往前一年期间的生产订单；ERP 每天晚上 2 点更新；每天晚上必须重排，已经报工的任务不动；ERP 工单状态只区分已完成/未完成；工艺路线配置版本采用清晰编号格式；排产相关信息有权限用户直接修改，不走审批。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS，用户继续确认的排产边界补充后文档结构有效。
