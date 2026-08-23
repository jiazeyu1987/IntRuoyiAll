# 测试计划：四份材料上传和放行门禁

## 范围

只设计测试，不创建或运行生产代码测试，不运行写入型 E2E。后续实现必须使用真实正式来源和任务自有测试数据；缺少租户、账号、已创建批次执行或可清理文件存储时，记录 BLOCKED。建批前置由流程修复 6/9 测试负责，本任务不把建批当作材料齐套条件。

## BDD 场景

- `BDD: 四份材料齐套才可放行 -> Given` 批次执行存在四个冻结材料节点且每项均有 `COMPLETED` 当前版本、`When` 任一合法放行入口执行预检，`Then` 统一 gate 返回 `MATERIALS_READY` 并冻结四份 manifest。
- `BDD: 完成节点原子回填 -> Given` 活跃订单双进度达到 100%、用户点击“完成”、`When` 流程修复 4 在同一业务事务内回填批记录、过程检验单和损耗分支、`Then` 三类回填全部成功才返回完成成功；任一失败则整体完成/回填失败且不得输出建批前置。
- `BDD: 无实际损耗不生成损耗单 -> Given` 完成节点判定无实际损耗、`When` 完成事务提交、`Then` 不创建或写入损耗单，只保留 `NO_LOSS` 适用性事实供流程 7 映射。
- `BDD: 实际损耗必须有损耗单 -> Given` 完成节点存在实际损耗、`When` 完成事务提交、`Then` 必须创建并回填损耗单；损耗单创建/回填失败则整体完成失败且不得建批。
- `BDD: 材料为空不阻塞合法建批 -> Given` 活跃订单完成节点的三类回填已在同一事务成功、`When` 流程修复 6/9 创建或复用批次执行、`Then` 批次执行成功进入 `MATERIALS_PENDING`，不得调用四材料放行 gate 或因缺件失败。
- `BDD: pre-release 来源映射先于材料门禁 -> Given` 批次已创建但流程修复 7 尚未冻结生产工单、领料单、批记录、过程检验和适用损耗/`NO_LOSS` Origin/TraceLink、`When` 任一放行入口执行材料预检、`Then` 返回跨线程稳定码 `TRACE_MAPPING_BLOCKED`，不进入材料齐套判断，不允许流程 10。
- `BDD: 来源映射冻结后才读取四材料 -> Given` 流程修复 7 pre-release 已输出 `originLinkId/traceLinkHash/sourceSnapshotHash`、`When` 四材料当前版本齐套并执行预检、`Then` gate 返回 `MATERIALS_READY` 并把 traceLinkHash 写入 manifest 上下文。
- `BDD: 缺一份材料必须阻塞 -> Given` 仅三项节点完成，`When` 提交放行，`Then` 返回 `MATERIAL_NODE_MISSING`，不创建放行事务、不签名、不改变批次状态。
- `BDD: 预登记不算完成 -> Given` 节点只有 `UPLOADING` 预登记附件，`When` 预检，`Then` 返回 `MATERIAL_UPLOAD_INCOMPLETE`。
- `BDD: 成品检报告和成品检记录独立 -> Given` 成品检报告完成但成品检记录缺失，`When` 预检，`Then` 只报告成品检记录缺件，不能用成品检报告替代。
- `BDD: 文件 hash 被篡改必须阻塞 -> Given` 数据库附件 SHA-256 与文件对象不一致，`When` 节点完成或放行预检，`Then` 返回 `MATERIAL_HASH_MISMATCH`，不写当前有效版本。
- `BDD: 替换使放行重新预检 -> Given` 四项已齐套且已生成预检 manifest，`When` 任一节点完成新版本替换，`Then` 批次状态变为 `MATERIALS_RECHECK_REQUIRED`，旧 manifest 不可提交。
- `BDD: 并发完成只允许一个版本 -> Given` 两个用户同时完成同一节点，`When` 两个请求使用不同版本提交，`Then` 一个成功、另一个返回 `MATERIAL_VERSION_CONFLICT`，两次均留审计。
- `BDD: 重复请求幂等 -> Given` 同一节点使用相同幂等键和参数重试，`When` 重复调用 prepare/complete 或 release gate，`Then` 返回原结果且不产生重复版本或放行事务。
- `BDD: 参数变化幂等冲突 -> Given` 相同幂等键再次提交不同文件 hash，`When` 调用接口，`Then` 返回 `IDEMPOTENCY_CONFLICT`，不覆盖原记录。
- `BDD: 所有放行入口复用同一门禁 -> Given` 批次详情、管理者代表前置和其它合法放行入口触发最终放行流程，`When` 任一材料缺失，`Then` 所有入口返回相同 blocker，不存在旁路成功；活跃订单完成和建批入口不在本断言范围内。
- `BDD: 门禁通过不等于最终放行 -> Given` 四份当前有效材料齐套、`When` 四材料 gate 返回 `MATERIALS_READY`、`Then` 仅允许进入流程修复 10，流程修复 8 不签名、不批准、不写最终放行状态。
- `BDD: 放行后追溯四份来源 -> Given` 放行已成功，`When` 查询批次追溯，`Then` 可看到四节点、版本/hash、上传人、上传时间、来源批次执行和放行 manifest。

## RED / GREEN / REGRESSION 计划

### RED

- `node tests/e2e/production-release-four-material-static.spec.js` -> 预期 FAIL：当前前端/后端合同未证明服务端固定四节点且所有放行入口调用统一材料 gate；旧配置/前端开关不得作为放行决策输入。
- `mvn -pl yudao-module-mes -Dtest=MesProductionCompletionAtomicBackfillTest test` -> 预期 FAIL：尚无合同证明点击“完成”在单一事务内原子回填批记录、过程检验单和损耗分支，且失败不会进入建批。
- `mvn -pl yudao-module-mes -Dtest=MesProductionNoLossNoLossOrderTest,MesProductionActualLossOrderRequiredTest test` -> 预期 FAIL：尚无合同锁定无损耗不生成损耗单、实际损耗必须生成损耗单。
- `mvn -pl yudao-module-mes -Dtest=MesProductionReleaseBatchCreationWithoutMaterialsTest test` -> 预期 FAIL：尚无合同证明完成事务三类回填成功后，材料为空时流程修复 6/9 仍可合法创建/复用批次，且创建不调用材料 gate。
- `mvn -pl yudao-module-mes -Dtest=MesProductionReleaseOriginTraceLinkPreReleaseTest test` -> 预期 FAIL：尚无合同证明流程修复 7 pre-release 映射在四材料预检前完成并冻结来源 hash。
- `mvn -pl yudao-module-mes -Dtest=MesProductionReleaseFourMaterialGateTest test` -> 预期 FAIL：尚无覆盖四节点版本、替换回退、manifest hash 和并发冲突的统一 gate 测试。
- `mvn -pl yudao-module-mes -Dtest=MesProductionReleaseReportNodeServiceTest test` -> 预期 FAIL：当前节点完成至少一份附件的行为尚未被测试锁定为四节点分别完成且不可互相替代。

### GREEN

- `mvn -pl yudao-module-mes -Dtest=MesProductionReleaseReportNodeServiceTest,MesProductionReleaseFourMaterialGateTest test` -> 四节点状态、文件核验、hash、版本和幂等测试 PASS。
- `mvn -pl yudao-module-mes -Dtest=MesProductionReleaseOriginTraceLinkPreReleaseTest,MesProductionReleaseFourMaterialGateTest test` -> 流程 7 pre-release 映射、traceLinkHash 冻结、四材料 gate 和流程 10 前置合同 PASS。
- `node tests/e2e/production-release-four-material-static.spec.js` -> 静态合同 PASS：四节点常量、服务端固定必填、统一 gate 调用、旧配置不参与决策和 blocker 展示存在。
- `node tests/e2e/production-release-four-material-real-flow.e2e.js` -> 后续在授权测试租户中通过真实页面上传四份材料、替换一份材料、预检、放行和追溯。

### REGRESSION

- 活跃订单点击“完成”是唯一回填节点；流程修复 4 在同一事务内完成批记录、过程检验单和损耗分支，流程修复 5 只提供判定规则；任一回填失败不得进入流程修复 6/9 建批。
- 无实际损耗不得生成或写入损耗单，只保留 `NO_LOSS` 事实；实际损耗必须存在已回填损耗单。
- 完成事务全部成功后，流程修复 6/9 才创建/复用批次执行。
- 批次创建/复用不因四份材料缺件而失败；批次初始材料状态可以是 `MATERIALS_PENDING`。
- 流程修复 8 的门禁只影响流程修复 10 的最终放行尝试，不拥有建批或最终放行状态。
- 流程修复 7 pre-release Origin/TraceLink 必须在材料 gate 读取前完成；post-release 只能在流程修复 10 唯一放行成功后执行。
- 一线生产、一线 PQC、生产组长复核、PQC 组长复核不会提前创建批次或放行。
- 生产工单、领料单、生产/PQC/损耗来源仍由流程修复 4 完成事务（流程修复 5 提供损耗判定）及流程修复 6/7/9 的正式来源快照提供，本任务不重复推断。
- 管理者代表候选和签名权限仍来自流程修复 10，签名不能绕过材料 gate。
- 批次执行历史详情、归档、操作审计和附件链不会因新增版本而丢失。
- 动态表单 `formBindings`、`MAIN`、工序开始上传人和旧最新附件不能被误识别为四材料。
- 流程 7 任一来源缺失、hash 变化或 post-release 失败不得被默认值、旧附件或材料 manifest 伪造补齐。

流程修复 9 的多入口前置、状态所有者、幂等和追溯合同，以及流程修复 11 的测试/回归/迁移总门禁，均作为跨线程回归输入；本任务只验证材料 gate 不阻塞建批、并阻塞不齐套的最终放行。

## 测试数据

- 任务自有测试租户、已由流程修复 6/9 创建或复用的批次执行、四节点负责人和流程修复 7 的追溯关联；上游双 100% 和三类回填 evidence 只作为已验证输入。
- 四个可验证文件，分别使用不同内容和 SHA-256；另备同名不同内容、相同 hash 重试、损坏文件和删除文件对象样本。
- 两个具备正式权限的上传人及管理者代表账号；所有密码仅通过环境变量注入。

## 禁止作为通过证据

静态文案、隐藏按钮、API-only、mock、默认材料、文件名匹配、最新附件、旧放行快照、直接 SQL、无业务码的 HTTP 200、或把上传预登记当完成。

## 测试阻塞

本任务不具备当前可运行服务和授权写入测试环境；因此 RED/GREEN/REGRESSION 仅为后续执行计划，不得写成已通过。

## 本任务状态

completed（测试计划文档交付完成）；实际生产测试、回归和 E2E 为 NOT RUN，由后续实现线程执行。
