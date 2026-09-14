# Execution Log

## User Intent

- 当前工序的一线生产 UI 必须链接当前工序绑定的正式批记录表单。
- 多数工序的生产操作记录是重复行；一条正式提交事实对应一条完整目标记录，申请放行生成时依次写入。
- 重复行数量不固定，必须由用户选择模板行和重复区域，并确认系统识别出的候选记录。
- 当前功能只配置对应关系；一线生产提交时不生成或占用批记录行，生产组长点击申请放行时才统一生成。
- 申请放行时生产数据已经封口，本功能不处理数量不一致，也不新增独立复核动作或复核时间判断。
- 生产批号和物料批号本阶段暂不处理。

## BDD

- BDD: 配置当前工序重复行组 -> Given 当前路线版本工序绑定正式批记录表单，When 用户选择模板记录行、重复区域并映射一线生产字段，Then 系统按用户确认的顺序保存该工序和表单版本独立的重复行组。
- BDD: 配置和一线提交不生成批记录 -> Given 重复行组对应关系已保存，When 用户保存配置或一线员工完成正式提交，Then 系统不生成批记录、不写目标单元格且不预占重复行。
- BDD: 申请放行按顺序使用对应关系 -> Given 当前工序存在已封口的正式一线生产提交事实和用户确认的目标记录顺序，When 生产组长点击申请放行，Then 系统按提交顺序把第 N 条来源生成到第 N 条目标记录。
- BDD: 配置页不处理数量和复核时间 -> Given 对应关系已保存，When 用户查看或维护重复行组，Then 页面只维护来源字段、模板记录、目标记录和顺序，不校验数量一致性，也不新增复核时间判断。

## Evidence

- 已读取现有完整报工字段链接任务，确认当前能力是字段到固定单元格映射，尚无重复行组运行语义。
- 已读取产品需求文档技能和 PRD 结构契约。
- 用户已确认继续使用现有“批记录单元格链接”页面，增加“单元格链接 / 重复行组”两种模式。
- 用户进一步确认配置页只做对应关系；正式批记录在生产组长点击“申请放行”时统一生成。
- 已按项目经验门禁区分通用预填与申请放行专用资料生成：正式生产事实由专用申请放行链路读取，不由配置保存或通用预填提前写入。
- 本任务仅形成产品需求，不修改生产代码、运行数据或链接配置。

## Verification Evidence

- STRUCTURE: product-requirements-docs 规定的 PRD、user flows、acceptance criteria 必需章节均已覆盖。
- GREEN: python -X utf8 validate_product_requirements.py --root doc/tasks/20260812-batch-record-repeating-row-link-prd -> PASS。
- REGRESSION: git diff --check -- doc/tasks/20260812-batch-record-repeating-row-link-prd -> PASS。
- GREEN: 2026-08-13 复核用户最新澄清后重新运行产品需求结构校验 -> PASS。
- REGRESSION: 2026-08-13 git diff --check -- doc/tasks/20260812-batch-record-repeating-row-link-prd -> PASS。
- SCOPE-SCAN: 2026-08-13 未发现旧的运行期数量阻断要求残留；文档只保留“本功能不处理数量不一致”的排除边界。
- SCOPE: 文档明确生产批号和物料批号本阶段不处理；未把光固 I 四条示例硬编码为全局规则。
- STATUS: 文档任务 completed；功能实现状态仍为待开发。
- CHANGE: 删除了“一线每次提交立即分配/写入重复行”“本功能校验生产数量”“独立复核动作和复核落值时点”三类错误边界。
- CHANGE: 新增“申请放行前零生成、申请放行时按正式提交顺序统一使用对应关系”的明确合同。
- CHANGE: 2026-08-13 用户澄清：这里只做对应关系；数据生成发生在生产组长点击申请放行时；点击时不会出现数量不一致，也不存在复核人的时间逻辑问题。已从需求中移除运行期数量不足阻断和独立复核时间验收项。
- CHANGE: 2026-08-13 复核发现文档仍残留“可选复核时间/申请放行时间”作为可链接字段的表述，已修订为本期不配置、不生成复核时间字段；复核人仍取点击申请放行的生产组长。
- GREEN: python -X utf8 validate_product_requirements.py --root doc/tasks/20260812-batch-record-repeating-row-link-prd -> PASS，复核时间字段收口后产品需求结构仍有效。
- REGRESSION: git diff --check -- docs/backend-development.md doc/tasks/20260812-batch-record-repeating-row-link-prd/docs/product/*.md -> PASS，仅保留既有 LF/CRLF 提示。
- EXPERIENCE: 已调用 project-experience-consolidation，将“链接配置、生产事实、申请放行资料生成三阶段分离”合并到现有批记录单元格链接预填落库门禁，并更新经验索引关键词。
- CLEANUP: task-closeout-cleanup preview/apply -> PASS；保留 6 个正式需求和任务记录文件，删除 0，blocked 0，warnings 0。

## Blockers

- 当前无文档编写 blocker；开发前只需关闭 PRD 中保留的最小必填映射集合等产品问题，不再以数量来源或复核时点作为本功能 blocker。
