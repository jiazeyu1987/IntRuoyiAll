# E2E10 r3 定向修复准备

## 范围与状态

原D4206合法恢复、R0610-14当前类型启用/绑定校验；沿用r2的不可改写历史、鉴权、事务、幂等与严格证据合同。A/B/C/E/D02保持原PASS，不重跑。当前只有测试与设计准备，没有有效RED、生产修改或新E2E结论。Maven由Root按24→13→17→03→10放行；14独占全量类型检查。

## 本轮准备检查发现

正式迁移 `IntRuoyiBackend/sql/mysql/20260712_dcc_project_code_assignment_audit.sql` 和测试 `create_tables.sql` 均定义metadata_change.source为VARCHAR(32)。r2拟用的APPLICANT_TYPE_CHANGE_STORAGE_RECOVERY超长，会造成夹具插入错误，不能算业务RED。r3更正为31字符的 `APPLICANT_TYPE_STORAGE_RECOVERY`，不扩字段或新增DDL。formatVersion仍为 `DCC_NEW_REJECTED_TYPE_CHANGE_STORAGE_RECOVERY_V1`。r2文件是历史提案，当前来源名以r3及测试为准。

原“只移动目录”负例只更新File且目标目录不存在，拒绝结果不足以证明恢复证据不可缺。r3先创建真实目标directory/mapping，同时移动当前File、Master及placement，再验证无恢复证据依然拒绝。其他新增参数验证恢复actor、原proof引用、观察hash、来源folder和重复恢复不可借用旧proof。

类型可用性增加4个真实五级路径参数：TARGET/REJECTED × LEAF/PARENT。通过真实taxonomy service先证明路径和绑定正常，停用后证明同一服务返回FILE_TYPE_TAXONOMY_INACTIVE，再要求保存矩阵预览/确认及目标普通保存同样拒绝。当前正式代码绕过该检查，预期业务断言失败；编译、夹具、NPE和数据过长不算有效RED。

## 优先复用正式流程

先复用 `DccControlledFileController` 的登录身份与现有update权限、`DccWorkingApplicationTypeChangeService` 的正式动作编排、已有source ownership/项目权限/类型解析/目标storage.resolve/metadata审计及placement唯一writer。拟新增明确的恢复预览和确认方法，不把旧幂等重放变成有写入的恢复，也不重新选目标类型或矩阵。

浏览行必须有不依赖故障详情Query的最小入口及确认界面；已存类型、来源、目标目录、原proof与恢复原因明确可见。预览/取消零写；确认需核对预览身份与原proof精确摘要。不为一个动作新建通用恢复平台、菜单、数据表或迁移。若复用现有动作服务会模糊严格守卫，再采用r2独立具体service，须在交接中说明必要性。

`DccApplicationExecutionContractService.readTypeChangeBoundary` 在旧proof不可改写前提下验证独立恢复记录，只有完整匹配时形成明确的有效存储事实再执行原严格合同。没有记录、错摘要、重复记录或单独移动目录均拒绝。当前/来源placement必须正式证明同项目folder，before/after观察除目录外一致；旧proof、驳回历史及旧mapping不变。完整设计约束见r2设计，超长来源名已由本文件替代。

`DccApprovalMatrixBaselineService.rejectedSource/savedTypeChangeSource` 返回保存canonical前，需要当前category启用、类别仍绑定保存taxonomy、实际完整路径启用且唯一解析回该category。预览只读；保存/确认复用正式配置锁。不得重新读取最新route替换保存canonical。新增依赖须补齐受影响测试的真实有效路径与注入，不能把NPE当类型不可用、用默认mock成功绕过。

## 后续有效验证

1. Root精确执行r3 handoff的RED：8个配置参数及1个恢复正例。保存完整JUnit、命令、实际退出码和首个业务断言；仅确认真实失败后开始生产修复。
2. 最小实现及真实动作测试：预览零写、原对象确认、当前目录和唯一placement原子变更、原proof/历史不改、同键重放零新增、异键/载荷/身份冲突拒绝、晚失败全回滚。动作测试不得用声明恢复行替代正式命令。
3. Root串行GREEN及影响范围回归；前端聚焦真实renderer验证入口、错误、预览取消和确认防重放。全量类型检查由14窗口统一处理。
4. Root新Jar/前端及运行态证明就绪后，原申请人对4206真实页面预览→取消→再预览→确认→普通保存/重开→最终提交；原主管独立登录看到且进入新待办。所有业务通过Playwright，零API/SQL业务写。
5. 新恢复结果单独引用运行包、trace和页面证据，保留原D历史FAIL、8712原文及A/B/C/E/D02证据。新运行可能形成后继提交版本，必须从页面核对真实ID和同一Master，禁止预写成功对象。

## 运行态与剩余

当前已知120105旧包不含本能力，不能验证新修复。无本轮浏览器/服务/构建进程；未修改共享Query或他人资产。实际RED、生产实现、动作GREEN、切包和页面闭环均未完成；Git提交推送仍未授权。
