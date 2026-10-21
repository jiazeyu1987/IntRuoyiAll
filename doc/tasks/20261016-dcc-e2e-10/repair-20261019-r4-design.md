# E2E10 r4 原4206恢复

## 已实施边界

后端仍须有效行为RED，Maven等待Root在17、03后明确放行，本例未运行。前端独立取得真实renderer RED后已实现最小行入口、恢复弹窗及typed API；定向GREEN/相邻回归23项通过，实际命令/退出码/输出见r4-front-*.json。这些是隔离单元测试，未访问真实服务，不等于恢复命令或E2E通过。

## 正式入口审查

现有 `DccControlledFileController` 的working-type-change预览/确认和working-attributes读取实际使用 `dcc:controlled-file:submit`，r2/r3提案写update有误。r4沿用正式submit权限；前端不要求doc_control角色代替原申请人，服务端仍须原actor、tenant、项目EDIT与旧/新类别UPLOAD。

当前恢复UI从browser行打开，完全不读取故障详情Query。复用既有canSubmitWorkingIteration状态边界，且只对最新NEW工作稿显示；前端不推断旧proof是否满足恢复资格，独立只读预览必须由服务核验。A/B/C/E/D02不重跑。

新增typed API约定：

- GET `/dcc/controlled-files/{fileId}/working-type-storage-recovery/preview`。
- POST `/dcc/controlled-files/{fileId}/working-type-storage-recovery`，载荷为 `expectedOriginalProofId/expectedOriginalProofSha256/expectedObservedSha256/requestId/changeReason/confirmed=true`。
- Preview/receipt字段与 `src/api/dcc/controlledFile/workingTypeStorageRecovery.ts` 一致。所有Long为字符串。预览返回已保存类型、项目/folder、原proof摘要、当前观察摘要、正文/冻结矩阵摘要、旧目录、目标base和可能不存在的目标derived目录；不得在预览创建目录。恢复成功必须recovered=true及独立recoveryEvidenceId，原proofId/hash保持。
- 前端取消、切换文件、迟到结果均不确认。确认期间不允许关闭/重复写；已发送载荷和key固定，错误内联并以原载荷重试；明确重新核对才重新取得事实。只有匹配file/Master/proof的成功回执才能关闭并刷新原行，绝不自动提交或再选矩阵。

后端尚未实现这些接口，旧包不具备能力；新UI不能作为新包已通过证据，原4206仍FAIL_NOT_RECOVERED。

## 后端最小实现与RED门槛

继续使用r3真实H2测试，RED精确选择8个类型可用性参数和1个合法恢复证据消费正例。只有业务断言失败可记录有效RED；编译、夹具、字段长度、注入错误不能算。尚无实际后端命令、退出码或XML，handoff相应字段明确为null/NOT_RUN。

有效RED后优先在既有具体WorkingApplicationTypeChangeService/Controller增加明确恢复preview/confirm方法及必要typed VO，复用现有鉴权、source ownership、round、prepared BPM、storage.resolve与placement唯一writer。旧confirm的同键回读仍零写，不把旧重放变为隐式恢复。不新增DDL或通用恢复平台。

必须区别当前类型的合法绑定与当前错误目录：现有requireFile会要求目录在当前category绑定下；若目标base与旧base不同，直接调用会在恢复前错误拒绝。仅提取共同身份/源件守卫，原普通类型动作继续保留原binding守卫；恢复专用路径必须以原proof旧类别mapping、当前及source placement、目标启用binding分别验证，不能删掉守卫或用fallback绕过。

独立metadata记录source为31字符APPLICANT_TYPE_STORAGE_RECOVERY，format为DCC_NEW_REJECTED_TYPE_CHANGE_STORAGE_RECOVERY_V1。新记录引用原proof精确摘要，before/after记录本次观察事实，不更新8712。恢复仅移动当前File/Master/当前placement，旧mapping及历史placement不变。锁顺序为project/folder/Master/File，CAS与审计同事务；同key同payload零写回读，异key/载荷冲突拒绝。迟到失败必须连新目录/mapping/审计全回滚。

ExecutionContract严格验证原proof及唯一恢复记录，形成明确有效folder/新目录事实后继续原source/round/原驳回BPM/canonical合同。无恢复、错proof digest、actor/source/hash/folder不符、重复记录、只移动目录都拒绝。恢复before/after只允许目录变化；随后普通保存仍沿用原有合法名单编辑合同，不能把原proof中名单与当前值不同误作历史改写。新恢复不替代旧proof，更不伪造旧proof包含folder。

类型可用性校验只读当前category启用/仍绑定保存taxonomy、完整启用路径与唯一类别映射；保存/确认复用正式配置锁。保存canonical和原执行矩阵不能替换成公共最新route。新增依赖需补齐受影响测试真实路径/注入，不用NPE或默认mock代表检查通过。

## 待执行验证

有效后端RED→最小实现→实际动作preview/confirm/replay/conflict/权限/rollback测试与GREEN→Root统一新包和运行态确认→原4206页面预览/取消/再预览/明确确认、普通保存/重开、最终提交、原主管新待办。原D历史失败和8712原文保留，新恢复结果单独登记。旧包、前端23PASS或准备完成均不能算该闭环通过。

当前无本轮浏览器、真实业务资产、Maven或服务进程；未改Query、Git、共享配置或数据库。本轮新增前端代码尚未被全量类型检查覆盖，14之前的PASS不包含这些新文件，需Root安排更新后检查。
