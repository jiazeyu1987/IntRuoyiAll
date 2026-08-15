# 开发计划：批记录表单导入重建工艺路线候选版本

- Task ID: task-6586818a22-20260814T121328
- Scope: 根据当前代码设计后续实现计划，不在本任务中修改生产代码。
- Current Baseline: 现有后端已经把已有路线的 Word 重建导入收敛到 createOrUpdateCandidateRouteVersion，当前 ACTIVE 不直接被覆盖；候选生成快照路径目前只保留旧工序基础属性和流程边，尚未迁移逐工序正式批记录表单绑定、formBindings 和工序开始配置。loadPreservedData 是现有保留逻辑的读取入口之一，但后续改造目标必须落在 createOrUpdateCandidateRouteVersion 生成候选快照的主链路上。

## 现状分析

1. 前端 Word 导入已经有预检和确认链路。index.vue 会在导入前加载 DCC 项目候选、调用 preflightUploadedRoute，并在 routeUpgradeRequired 时提示用户确认候选版本更新。
2. 前端提交参数已经具备治理字段。index.ts 的 recognizeUploadedRoute 已支持 dccProjectCodeId、rebuildBatchRecord、routeUpgradeConfirmed、expectedRouteId、expectedRouteVersionId、expectedRouteCandidateVersionId。
3. 后端导入入口已经具备 DCC 和路线版本校验。MesProBatchRecordReportServiceImpl 会在导入前校验 DCC 项目代码、导入动作、批记录升版和路线升版确认。
4. 已有路线当前不会直接覆盖 ACTIVE。MesProBatchRecordRouteGenerationServiceImpl 在 target.existing 分支进入 createOrUpdateCandidateRouteVersion，符合“候选发布后才生效”的业务方向。
5. 当前缺口集中在 createOrUpdateCandidateRouteVersion 生成候选快照的保留范围。现有 loadPreservedData 只保留 routeProcess 的 prepareTime、waitTime、颜色、关键/检验标识、备注和已有流程边；候选快照尚未把旧版本每个工序的正式批记录表单绑定、工序开始配置和表单槽位迁移进去。
6. 发布投影按候选 routeSnapshotJson.configSnapshots 重建正式路线。如果候选快照缺旧绑定，发布后会丢失配置，因此必须在候选生成阶段补齐，而不是发布后临时回填。

## 设计原则

- 勾选“工艺流程”是按 Word 工序顺序重建路线节点和流程关系的唯一入口。未勾选时不得重建工序节点、普通流程边或 START/END 边界；如果仅因批记录表单绑定升版需要候选承载，必须保持原 ACTIVE 的 flowGraph 不变，并与工艺流程重建分开处理。
- 已有路线只写 DRAFT 候选版本，不直接写 ACTIVE 路线。
- DCC 项目代码是路线目标识别的正式身份，不用批记录名称、产品名称字符串或前端文案猜路线。
- 工序节点按 Word 顺序更新，旧配置按正式工序身份迁移。
- 批记录表单、表单槽位 formBindings、工序开始是三条独立链路，迁移时分别处理、分别测试，不能互相补位。
- 工序结束没有业务绑定关系，只保留 END 流程边界。
- 无法唯一映射时 fail fast，不允许静默丢绑定或默认成功。

## Milestone Plan

### 里程碑 1：明确导入入口和用户确认边界

目标：让“工艺流程”勾选项成为按 Word 工序顺序重建工序节点和流程关系的唯一入口。

交付物：
- 未勾选“工艺流程”时，不重建 flowGraph.nodes、edges 或 boundaryEdges；若只生成批记录表单绑定候选，候选 flowGraph 必须沿用原 ACTIVE。
- 勾选“工艺流程”且已有路线时，页面提示生成或更新候选版本，发布后才生效。
- 候选锁定时前端阻断继续导入。

### 里程碑 2：固化后端路线目标和候选版本治理

目标：后端以 DCC 项目代码和预检冻结 ID 精确定位路线，已有路线只创建或更新 DRAFT 候选。

交付物：
- 缺少 dccProjectCodeId、expectedRouteId 或 expectedRouteVersionId 时 fail fast。
- 无现有路线时创建路线、工序、流程关系、DCC 项目代码绑定和初始 ACTIVE 版本。
- 已有路线时创建或更新同源 DRAFT 候选，ACTIVE 发布前不变。
- 预检后 ACTIVE 或候选版本漂移时拒绝写入。

### 里程碑 3：升版候选保留旧工序配置

目标：在 createOrUpdateCandidateRouteVersion 生成候选快照时，把旧路线可唯一映射的逐工序配置迁移到新工序节点。

交付物：
- 候选 flowGraph.nodes 按 Word 顺序生成，节点具备正式 processId 和可投影的 routeProcess 身份。
- batchUseConfigs.batchRecordReports 正式批记录表单绑定迁移到候选新工序。
- batchUseConfigs.formBindings 仅作为表单槽位迁移，不替代正式批记录表单。
- routeStartProductionLeaders、batchRecordAttachmentOwners 等工序开始配置迁移到候选；不新增工序结束绑定。
- 旧配置无法唯一映射时 fail fast。

### 里程碑 4：发布投影与运行态回归

目标：候选发布后，正式 ACTIVE 路线按候选快照投影，且三类配置仍在正确链路上。

交付物：
- 发布后正式路线节点、关系和三类配置与候选一致。
- 发布前批次执行或路线配置仍读取原 ACTIVE。
- 发布投影缺必要配置快照时 fail fast。

### M1：测试先行复现配置丢失缺口

- 目标：先写失败测试证明“已有路线升版候选没有保留旧绑定关系”。
- 后端测试：
  - 在 MesProBatchRecordReportServiceImplDbTest 或新增聚焦 DB 测试中构造 ACTIVE 路线、三个工序、正式批记录表单绑定、formBindings、工序开始配置。
  - 导入同一产品 Word，重排工序顺序并确认升版。
  - 预期 RED：当前候选快照缺旧配置或配置未映射到候选新工序。
- 前端静态测试：
  - 扩展 batch-record-word-import-route-candidate-static 或新增测试，断言未勾选“工艺流程”不进入工艺流程重建确认，不发送重建工序节点/流程关系的参数。
  - 断言勾选后必须带 expectedRouteCandidateVersionId 并显示候选发布后生效提示。
- 交付：RED 记录写入 execution-log.md。

### M2：扩展候选生成的旧配置快照读取

- 目标：在后端升版候选生成前读取旧 ACTIVE 路线三类配置。
- 实现要点：
  - 扩展 RouteUpgradePreservedData，增加旧 routeProcessId 到配置快照的集合。
  - 读取正式批记录表单绑定时，只读取 batchUseConfigs.batchRecordReports 中逐工序正式批记录表单来源，不从 formBindings、默认 MAIN 或工序开始配置推断。
  - 读取 batchUseConfigs.formBindings 时保留其表单槽位身份，不把它写成批记录表单。
  - 读取工序开始配置时只迁移 routeStartProductionLeaders、batchRecordAttachmentOwners 等 START 相关上传人、附件负责人或同类开始节点配置。
  - 不读取或生成“工序结束绑定”。
- 风险控制：
  - 旧配置来源缺必要字段时阻断。
  - 已有路线缺 ACTIVE 版本或候选来源版本漂移时沿用现有 fail-fast 机制。

### M3：建立旧工序到候选新工序的唯一映射

- 目标：让保留配置可以可靠落到候选新节点。
- 实现要点：
  - 以正式 processId 加出现序号建立默认映射，保留 routeProcessId 快照用于发布投影。
  - 如果 Word 中缺少旧配置所在工序，必须列出工序名称/ID 后阻断。
  - 如果同一 processId 的数量或顺序无法证明唯一，不默认选择。
  - 新增工序没有旧配置时允许为空，但必须不影响其它旧工序配置迁移。
- 风险控制：
  - 不用工序名称模糊匹配。
  - 不按 sort 直接对齐跨版本工序，因为 Word 重排会改变 sort。

### M4：写入候选快照并保留发布投影合同

- 目标：候选版本保存完整 configSnapshots，发布时自然投影为正式配置。
- 实现要点：
  - 在 createOrUpdateCandidateRouteVersion 生成 flowGraph.nodes 后，同步生成 batchUseConfigs.batchRecordReports、batchUseConfigs.formBindings、routeStartProductionLeaders 和 batchRecordAttachmentOwners 配置快照。
  - 对旧工序正式批记录表单绑定，仅重写候选中的 routeProcessId 节点引用；permissionScopeId、recordCategorySnapshotHash、slotConfigSnapshotHash 属于已冻结的正式权限范围与绑定快照，必须原样保留，不得改写成 routeProcessId 或重新计算。
  - 对 Word 新增工序的正式批记录表单绑定，permissionScopeId 必须引用正式创建的权限范围；clientRouteProcessId 只是候选发布映射身份，不得作为 permissionScopeId 发布。
  - 对 formBindings，仅更新对应新节点身份，保留槽位类型、模板版本和业务字段。
  - 对工序开始配置，仅迁移开始节点配置并保留 START 边界。
  - 保持 candidate DRAFT 的原 ID 更新逻辑，不重复创建 V3。
- 风险控制：
  - 序列化后立即重读快照，断言无 Fastjson $ref、nodes 非空、普通/START/END 边完整。
  - 候选为 PENDING_APPROVAL 或 READY_TO_PUBLISH 时拒绝写入。

### M5：前端提示和参数边界回归

- 目标：让用户明确知道导入后只是候选版本，发布后才生效。
- 实现要点：
  - 未勾选“工艺流程”时，不把 rebuildBatchRecord 作为重建工序节点和流程关系的开关传入后端；如果存在批记录表单绑定候选链路，前端文案和参数必须把它与“工艺流程重建”区分开。
  - 勾选“工艺流程”时，如果已有路线，确认弹窗文案说明“生成或更新候选版本，不覆盖当前生效路线”。
  - 如果同源 DRAFT 存在，文案说明“更新当前草稿”；如果候选锁定，阻断。
- 风险控制：
  - 前端只负责展示和回传预检冻结字段，最终权限和版本锁由后端决定。

### M6：发布投影与运行态验证

- 目标：候选发布后，正式 ACTIVE 路线保留新节点和旧配置。
- 实现要点：
  - 扩展 MesProRouteVersionPublishProjectionServiceImpl 相关测试，验证发布后 batchUseConfigs、formBindings、工序开始配置落到正式 routeProcess。
  - 扩展批次执行或路线配置只读验证，确认“批记录表单”字段来自逐工序正式绑定，不被 formBindings 替代。
- 风险控制：
  - 发布投影缺配置快照时 fail fast。
  - 不用 API-only 成功替代真实页面路径；UI 交互范围内使用 Playwright。

## Suggested Implementation Order

1. 补后端 RED：候选快照丢正式批记录表单绑定。
2. 补后端 RED：候选快照丢工序开始配置。
3. 补后端 RED：formBindings 与批记录表单互不替代。
4. 补前端 RED：未勾选工艺流程不触发工序节点/流程关系重建确认和参数。
5. 扩展 preserved data 读取与映射。
6. 写候选快照迁移逻辑。
7. 补发布投影测试。
8. 跑目标后端测试、前端静态合同、必要真实 E2E。

## Expected Files To Change Later

- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteGenerationServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImplDbTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteGovernanceContractTest.java
- IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue
- IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts
- IntRuoyiFronted/tests/e2e/batch-record-word-import-route-candidate-static.spec.js
- 新增聚焦测试文件可按现有命名靠近 Word route candidate、batch record binding preservation。

## Verification Gate

- 后端目标测试必须先 RED 后 GREEN。
- 前端静态合同必须覆盖勾选和未勾选两条路径。
- 真实 E2E 只有在确认本地 int_main 前后端、登录账号、租户和可写测试数据可用后执行；缺前置时记录 blocker，不降级为 mock 或 API-only。
- 完成后需再次核对：ACTIVE 发布前不变、DRAFT 候选完整、发布后正式路线完整、批记录表单/formBindings/工序开始三条链路互不替代。
