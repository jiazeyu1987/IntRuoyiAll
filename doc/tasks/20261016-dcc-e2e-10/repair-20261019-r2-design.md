# E2E10 r2 原失败样本恢复与类型可用性

## 当前阶段

BDD及真实H2行为测试已准备，生产代码尚未修改；按严格TDD等待Root串行执行RED。Maven当前由13占用，本例没有启动Maven、构建、全量类型检查或服务操作。准备不等于RED/GREEN或E2E通过。

## 新授权与历史边界

`repair-dispatch-20261019-r2.md`已明确授权新增正式恢复能力，覆盖上一轮“无入口则保留失败样本”的停止边界。保留原4206、Master9175、source/hash、原驳回4197、原8712完整before/after字符串及旧T1目录mapping。不得改8712补folder、删除旧proof、重传或新建替代Master。当前4206完成正式恢复后，旧运行仍是历史FAIL，新增恢复结果单独登记。

## 恢复动作最小合同

1. 正式入口位于受控浏览的当前WORKING行，不依赖故障详情。原申请人打开“核对类型存储恢复”；预览只读，显示文件/主文件/当前类型/原proof/原目录/正式项目文件夹和目标类别目录配置，明确不重新选择矩阵。只对持有原合法转换及可核验来源的对象可确认。
2. 新控制器预览/确认使用已有update权限并在服务内复核原申请人、当前租户、项目EDIT、源/目标UPLOAD、启用路径/绑定、未送审未检出且无正式受控版。旧proof必须唯一、actor/master/file/source/hash/round/旧执行矩阵/目标canonical全部精确对应；不以ID白名单代替验证。
3. folder事实来自当前placement与原sourceApplicationId的placement严格相同，以及原directory对应的旧category mapping；旧proof没有folder是事实，不伪造其历史。原proof的before/after目录必须相同且当前保持旧目录，缺任何正式来源就拒绝。确认按项目/folder/Master/File顺序锁定并重验，不在预览派生新目录。
4. 复用既有目标Storage.resolve派生/读取目标目录；只CAS当前File/Master目录和该File placement，类型/矩阵/来源/名号不改。追加 `APPLICANT_TYPE_CHANGE_STORAGE_RECOVERY` metadata审计一条，format `DCC_NEW_REJECTED_TYPE_CHANGE_STORAGE_RECOVERY_V1`；包含原proofId、精确SHA256、实际观察的before/after状态（folder事实及目录）。原proof不更新。
5. SHA256覆盖原proof ID/租户/file/Master/项目/actor/source/request/reason与精确before/after JSON字符串（无字符串trim或重序）。执行合同读取原proof时只在额外恢复审计全部合法的情况下投影其有效folder/新directory，再继续全部原有source/round/history/mapping校验；原证明原文始终返回/保留为原始事实。无恢复、digest错误、重复/冲突审计、仅移动目录均拒绝，不作fallback。
6. placement唯一writer增加此明确恢复动作的严格验证（保留原TYPE_CHANGE的现有分支），不开放任意file位置修改。恢复行与目录/placement更新在同一事务；同键同载荷验证回读零写，异键对已恢复对象拒绝；迟到失败全部回滚。新审计不伪装普通CREATE，也不称旧8712曾包含folder。
7. 页面确认成功后回原4206详情，核对同file/Master/项目folder/source、原矩阵和独立恢复收据，然后原4206普通保存、最终提交、原主管新待办。无再次检入/签名驳回/新样本；历史FAIL不可覆盖。

## R0610-14最小合同

当前 `DccApprovalMatrixBaselineService.rejectedSource/savedTypeChangeSource` 从已保存canonical构造Source，绕开Source.read的当前category active检查；后者也未覆盖叶子/父路径启用及叶子与类别双向绑定。

新校验只确认当前类型可用性，不读取最新矩阵、不改历史canonical：租户内category active且file_type_taxonomy_id仍是保存的taxonomy，taxonomy当前完整启用路径及绑定仍解析到该category。普通保存与最终确认在既有category锁下重验。不能调用sources.readForUpdate借最新矩阵替代旧矩阵；公共矩阵新版本时原既有通过合同继续成立。

## 精确拟改文件及调用范围

后端相对 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`：

- 新 `controller/admin/file/DccWorkingTypeStorageRecoveryController.java`及typed请求/只读VO：预览与明确确认，独立于详情Query；不新增菜单或schema。
- 新 `service/file/DccWorkingTypeStorageRecoveryService.java`：上述单一动作编排及严格观察/幂等验证。
- `service/file/DccApplicationExecutionContractService.java`：readTypeChangeBoundary增加明确恢复审计的验证/有效状态投影，旧无恢复分支保持严格；共享source校验复用，不改旧JSON。
- `service/file/DccPublicUploadPlacementService.java`及`service/projectcode/folder/DccProjectFilePlacementService.java`：仅此恢复动作的目标resolve与唯一placement writer；复用既有目录解析，不开放通用修复。
- `service/file/DccApprovalMatrixBaselineService.java`：保存/执行canonical返回前调用当前可用性校验。
- `service/file/DccApprovalMatrixSourceService.java`：复用分类配置锁实现当前category/叶子启用绑定校验；不替换已保存矩阵。

前端：新恢复弹窗/typed API，`src/views/dcc/controlled-file/browser/index.vue`仅添加WORKING行入口及回跳。共享Query、签名、培训权限、旧元数据ACTIVE限制均不改。生产修改等待Root真实RED；如Root要求缩减/另分文件，先按Root精确边界调整。

## 测试与运行态

- `DccSavedMatrixAvailabilityDatabaseTest`：4个真实H2参数（已确认目标/同类型驳回 × category停用/重绑）；分别断言preview/confirm/保存Source拒绝和零业务写。预期当前代码为业务assertion failure。
- `DccTypeChangeStorageRecoveryDatabaseTest`：声明真实旧proof和独立恢复行，正例验证读取/confirm且原proof/原历史/旧mapping不改；负例仅移目录、错digest继续拒绝。这是恢复证据读取RED，明确不是尚未实现命令的原子性测试。
- 实施时补动作真实H2测试：预览零写、确认同对象、同键重放、参数/权限/source/folder/digest改变拒绝、晚失败回滚；前端用实际Vue renderer测试，避免仅搜索字符串证明。
- Root统一RED→GREEN→构建/新Jar加载后，Playwright只对原4206运行恢复/保存/提交/主管待办。R0610-14类型停用/改绑负例采用隔离测试；不得为了重复验证而临时改变现库已通过公共类型。A/B/C/E及D02全部不重跑。

## 尚未完成

Root真实RED、生产最小实现、动作负例及前端渲染测试、GREEN、切包和原4206页面恢复均未完成，不作任何PASS声明。没有Git或清理删除操作。
