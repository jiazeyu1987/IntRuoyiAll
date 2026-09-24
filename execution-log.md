# 偏差管理开发文档任务执行日志

## User Intent

用户要求将对话中已确认的偏差管理需求整理成必要的开发文档。本任务仅制作实施用文档包，不开始功能开发。

## Requirement Decisions

- 偏差只关联正式批记录，不关联具体表单、工序或订单；一个批记录可有多条偏差。
- 等级为普通偏差和重大（关键）偏差两档，重大与关键同义。
- 偏差号采用 PC-YYYYMM-0001；按租户/月、Asia/Shanghai 服务端年月递增，四位补零、自然扩位；正式提交分配、唯一且不回收。
- 每条偏差只有一份处理记录，允许修改该记录，不支持多处理轮次。
- 常规路径验证、QA 关闭、质量负责人批准全部完成才关闭；QA 与质量负责人无顺序关系；关键常规路径增加管理者代表签字。
- QA 仅可对关键偏差手动发起批记录级不合格审批；一个评审可关联同批一条或多条关键偏差。发起成功后自动关闭所选偏差，跳过偏差表自身验证/批准；pending 评审仍冻结。
- 未关闭偏差和待处置不合格评审阻止一线生产提交、一线 PQC 提交、PQC 生产放行和上市放行。
- QA 或持有 PQC 生产放行处置权限的人员可选择让步放行、返工或作废；按处置结果与既存门禁控制后续操作。
- 用户界面操作/展示对象为批记录。上市放行后不能新关联偏差。
- 同账号可同时获得多个权限。

## Evidence Reviewed

- 本机用户提供的偏差处理记录表 DOCX；OfficeCLI 提取了唯一连续大表的字段结构。
- 仓库文件 resource/相关文档/电子批记录系统的审核要求8.25.xlsx：审核表有 40 行，包括偏差直发、未关闭拦截、放行阻断和例外汇总，以及签名/审计/归档要求。
- 当前实现线索：UnifiedListTemplate、批记录详情、ActiveOrderSubmissionDetailPanel、BatchExecutionTraceDrawer、MesProEdhrNonconformanceReviewService、电子签名服务、批次放行检查。
- 旧任务 doc/tasks/nonconformance-review-mvp-implementation：已有统一不合格评审、冻结、三类处置及追溯实现记录。

## BDD Scenarios

- BDD: 普通偏差合法发起 -> Given 用户具备发起权限且批记录同租户未上市放行, When 完成字段和签名后提交, Then 分配唯一 PC 编号并进入未处理列表。
- BDD: 未关闭偏差阻断整批四种动作 -> Given 批记录有未关闭偏差, When 尝试一线生产提交/一线 PQC 提交/PQC 生产放行/上市放行, Then 后端分别拒绝。
- BDD: 关键偏差转评审 -> Given QA 为同一批次选择多条未关闭关键偏差, When 成功发起一个不合格审批, Then 审批关联全部偏差、偏差自动关闭但 pending 审批继续冻结。
- BDD: 待处置评审结束按处置控制 -> Given 批次已有待处置不合格审批, When QA/PQC 放行授权人员选择已签处置结论, Then 后续动作根据真实处置状态和其余门禁判断。
- BDD: 签名修改追溯 -> Given 一份处理记录存在已签字段, When 获授权用户说明原因后修改已签内容, Then 审计记录变化且受影响签名失效。
- BDD: 追溯空态与错误分离 -> Given 偏差读取正常返回空集或服务失败, When 打开批记录偏差 Tab, Then 分别显示“没有偏差”或错误信息。

## RED / GREEN 状态

- 本任务只生成开发文档，未改生产代码，未执行功能 RED/GREEN 测试。
- 后续编码阶段的 RED/GREEN 命令模板和预期行为记录在 test-plan.md；实际命令必须绑定实际实现提交/测试名并保存真实输出，不可将本轮文档校验记作业务 RED/GREEN。

## Workspace Safety

- 读取时分支为 int_main，工作区存在大量与 NCR、活跃表单、批记录、登录策略等相关的未提交源码、测试、规则文件和临时文件。
- 本任务只写入 doc/tasks/20260923-edhr-deviation-management/ 下的任务自有文档；不暂存、不删除、不恢复、不提交、不推送其他改动。
- 在后续实施前重新读取 git status 和相关 diff，不能把当前脏工作区当作干净基线。

## Closeout Evidence（初版历史，不能替代本轮复验）

- task-closeout-cleanup preview -> PASS：13 个正式开发文档全部 keep，没有 delete、blocked 或 warnings。
- task-closeout-cleanup apply -> PASS：保留 13 个任务文档，deleted_paths 为空。
- 未执行 Git commit/push：根目录 AGENTS.md 要求无当轮明确授权时不得提交/推送；task-closeout-rules.md 又将提交和推送列为任务完成门禁。文档暂留 ready_for_closeout，等待该门禁处理。

## Open Implementation Notes（初版记录，以下新复审结论优先）

- 活跃表单主偏差 Tab 只在共享面板 FORMAL_BATCH_SOURCE_DETAIL 范围显示；历史追溯列表 BatchExecutionTraceDrawer 与批记录详情 traceRecordDrawer 是两个独立只读追溯宿主，各使用正式 batchExecutionId。
- 现有 NCR direct batch source 和处置实现必须按当前源代码 diff 复核；特别确认 batch linked rework 是否会产生正式返工路径。
- 本线程确认QA直接转审替代偏差验证和批准；本轮按该分支不附加偏差处理完成门槛，详见修订PRD。
- 一个 pending NCR 存在时后来创建的新关键偏差如何追加尚未确定；首版文档给出拒绝第二评审/不改签名评审集合的建议。

## Follow-up: Frontend Interactions and Consistency Audit

用户要求增加前端交互文档并检查整套文档是否自洽、符合已确认需求；本轮仅调整任务内开发文档。

- 新增 frontend-interaction.md，明确偏差列表、发起、唯一处理记录、签名、不合格评审选择、状态/错误/空态交互和追溯入口。
- 新增 consistency-review.md，按用户确认规则逐项对照 PRD、设计、验收和测试计划，并记录矛盾和修正。
- NCR 分支修正为 QA 可对未关闭关键偏差直接发起，不等待偏差表验证/批准；调查/处理记录未完成也不是门槛；成功创建时自动关闭，NCR 自身签名/处置仍必需。
- 页面范围修正：偏差主 Tab 仅在 FORMAL_BATCH_SOURCE_DETAIL 显示；普通详情、PQC 放行和 NCR 页面不显示；追溯覆盖历史 BatchExecutionTraceDrawer 和批记录详情自身追溯抽屉。读查询只用正式 batchExecutionId。
- 当前没有产品代码变更；本轮运行文档结构/一致性检查，不运行 Maven、pnpm、数据库或 E2E。

## Revision R2: 完整复审与收敛

用户继续要求前端交互及自洽性修复。本轮沿用原任务，不创建重复任务、不启动子Agent。

- 重读全部当前文档及关键代码，纠正18类遗漏/冲突，详细见consistency-review.md。
- 删除原Word“主要→关键”的擅自映射；补全常规关闭所需部门负责人和编制等签名；区分处理内容版本与签名动作，防止同版签名互相失效。
- 明确关键偏差直接转审可以没有完整处理；成功关闭原偏差但待审NCR持续阻断四项动作，不伪造跳过节点为合格。
- 明确未推送PQC也能在批记录入口发起；只有activeOrderId的正常详情必须由后端正式来源补齐批记录身份，不能长期报缺ID替代功能。
- 顶层Tab和只读/内嵌宿主明确隔离；处理/验证/签名/转审/评审处置可用，避免四项门禁自锁。
- 一张NCR创建时选同批多条；首版沿用一批一待审且不事后追加，未选偏差保留开放；原PQC不合格等独立NCR入口不受关键偏差限定。
- 把未获本次范围明确确认的PDF/归档扩展从必交条件移到候选建议；在线追溯仍完整保留。P5-AC2因此修订为在线只读及关闭原因，原未决P4-AC7改为明确待审冲突/不自锁验收。
- 增强为28项AC与28项BDD一一映射，状态JSON明确未来功能not_started；后续必须写实际RED/GREEN，不能复用本轮文档证据。
- 初版user-flows.md曾被生成脚本写成字面量换行，旧结构检查仅查字符串未检出；本轮已恢复真实换行，并在validator检查行数与行首标题。初版PASS仅代表当时有限检查，不能视作当前完整审查通过。
- 按project-experience-consolidation将此通用换行校验经验并入现有docs/powershell-encoding.md，不新建长期经验文件。
- 临时_revise_docs.py、_finish_revision.py、_finalize_records.py仅本任务生成工具；最终由cleanup删除。validate-docs.py为正式交付保留。

### 本轮文档行为检查

- BDD: DOC-R2 签名规则一致 -> Given 用户要求所有签字位电子签名, When 对照PRD、权限、交互、模型和测试, Then 常规关闭含部门负责人，关键管理者例外不冲突。
- BDD: DOC-R2 状态接管一致 -> Given QA直接转审自动关闭偏差, When 对照列表、接口、动作门禁, Then 已处理与批次受限独立展示，NCR继续阻断。
- BDD: DOC-R2 可用页面身份 -> Given 原入口只有activeOrderId, When 审阅交互与后端来源设计, Then 必须补齐正式批记录身份而不是以报错页交付。

业务RED/GREEN、数据库和E2E本轮均未运行。增强文档校验与cleanup结果将在verification-report.md记录。

### R2 文档校验结果

- python -X utf8 doc/tasks/20260923-edhr-deviation-management/validate-docs.py -> PASS：14份Markdown、28AC/28BDD逐项映射、JSON/阶段/保留清单、签名/身份/门禁定向检查及四类技能校验器全部通过。
- 校验首次发现测试用例角色简称不完整，补齐“部门负责人”后复验PASS。
- git diff --check -- docs/powershell-encoding.md -> PASS。
- git check-ignore只读确认任务目录被.git/info/exclude的/doc/tasks/*/排除；文件真实存在，本轮未改规则或提交。
- 未运行功能RED/GREEN、Maven、pnpm、数据库或E2E；未来功能状态保持not_started。

### R2 Cleanup

- task_closeout.py --task-id 20260923-edhr-deviation-management --mode preview -> PASS：16项正式资产keep，3个本任务临时脚本delete，无blocked/warnings。
- 相同taskId --mode apply -> PASS/applied：只删除_finalize_records.py、_finish_revision.py、_revise_docs.py。
- 文档里程碑完成，documentationStatus/cleanupStatus=completed。未执行Git写操作，总体ready_for_closeout保留项目Git交付门禁，非功能实现状态。
