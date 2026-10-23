# V2 上传到升版精确 mapping：只读补充 review

2026-10-16，int_qms。Root 已报告核心三类 10 tests 和前端导入五项通过，并确认当前旧定义是 V1。本报告只读追踪当前源码及既有测试；未运行测试、查询实际库、修改源码/配置、部署或启动 E2E。实际新定义 ID 以 C05 正式前端发布回执为准，本文不编造 ID。

## 核心结论

- 在完整保留旧 V1 upload ID/digest -> 旧 V1 revision ID/digest 那一行的基础上，新增 **一行**新 V2 upload ID/digest -> 新 V2 revision ID/digest。两行源身份不同，不冲突；旧行仍用于旧来源的合法继承/重放，不能删除或把旧行目标改成新 revision。
- 同 V2 revision -> revision，无论已受控版本创建下一轮升版，还是同类 WORKING 草稿复制/检入，均复用 `origin` 的精确定义和 digest，不需要 V2 revision 自环 mapping。也不会因为后来部署更高版本而改用 latest。
- “无需 mapping”仅指契约选择阶段；项目/类别权限、同 Master/项目/类型/编号、来源 SHA、已提交属性轮次、正式流程来源、受控 baseline 和 master 指针等守卫仍必须满足。不能用 mapping 修复这些业务身份缺口。

## 精确分支

来源：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationExecutionContractService.java:201`：

```java
Prepared prepared = changedType || freshApplication && !PUBLIC_V2.equals(origin.executionContract())
        ? resolveTransition(origin, keyOf(target)) : origin;
```

按 Java 优先级等价于 `(changedType || (freshApplication && !isV2Origin)) ? transition : origin`。`typeOf` 将 `changeType=NEW` 映射为 UPLOAD，`changeType=REVISION` 映射为 REVISION；不是按 process key 字符串或版本号猜动作。

| 来源 -> 目标 | changedType | freshApplication | 结果 |
| --- | --- | --- | --- |
| V2 已受控 upload -> REVISION | true | true | 必须查唯一精确 mapping，即新 V2 upload -> 新 V2 revision |
| V2 已受控 revision -> 新 REVISION body | false | true | 复用 origin V2 revision ID/digest，不查 mapping |
| V2 REVISION WORKING -> 同类草稿/候选 | false | false | 复用 origin；仍核验直接来源、源属性轮次与内容身份 |
| V1 已受控 upload -> REVISION | true | true | 使用保留的旧 V1 mapping；不因新 latest 自动迁移到 V2 |
| V1 已受控 revision -> 新 REVISION body | false | true | 需要该 V1 revision 源身份的唯一 mapping，不能误认为 V2 的复用规则同样适用 |

同 V2 revision 新应用仍调用 `requireControlledTransition`（第 251 行），要求来源已受控，状态为 ACTIVE/CONTROLLED_PENDING_EFFECTIVE，目标 revisionBaseActiveControlledFileId 精确指向来源、selectedIterationControlledFileId 为空、Master latestControlledFileId 匹配来源。只读历史重放允许相应历史状态；这不是取消 baseline 校验。

## 映射结构与唯一性

配置类 `DccExecutionDefinitionTransitions.java:11`：

```text
dcc.application-execution.definition-transitions
  source-process-definition-id
  source-definition-digest
  target-process-definition-id
  target-definition-digest
```

`resolveTransition`（第 267 行）先按 **sourceProcessDefinitionId 与 sourceDefinitionDigest 两字段完全相等**筛选，要求 `matches.size() == 1`；零行或多行均抛 `EXECUTION_CONTRACT_EXACT_ACTION_TRANSITION_REQUIRED`，不会取第一行。其筛选没有 targetKey、版本号、tenant 或 executionContract 条件；tenant 和目标 key 在后续验证。

选中后依次检查：

1. targetDefinitionDigest 必须为 64 位小写 hex。
2. `DccWorkflowDefinitionCatalog.find(targetKey, targetDigest)` 必须登记目标完整图；错误为 `EXECUTION_CONTRACT_REGISTERED_TARGET_GRAPH_REQUIRED`。
3. `verify(registered.executionContract(), targetId, targetKey, targetDigest)` 重新核验真实定义存在、精确 ID、当前租户、流程 key、deploymentId、完整图和 catalog 匹配；不能凭配置声称目标 V2 已可用。
4. 启动阶段还有正式 BPM 定义激活、权限和候选检查；mapping 自身不保证目标可发起。

因此：

- 新旧源 ID 不同，即使某次重部署 XML 的完整图 digest 相同，也可以各有一行；同源 ID+digest 重复两行则必然阻断。
- 不要为同一旧 V1 upload 额外添加“改到 V2 revision”的第二行；这会与保留旧行重复。保持旧历史路线和新增 V2 来源路线是两组不同源身份。
- 顺序不改变查找结果；不支持通配符、仅流程键或“latest”映射。
- 正式部署不会自动把此配置补齐；源码初始列表为空，需由 Root 在实际运行配置中提供完整有效列表。本线程未读取或修改运行配置；保留旧行的现状依据 Root 本轮回执。

## Root 本次配置应产生的有效列表

以下仅为带明确占位符的结构说明，不是可直接应用的配置文件；只写在 review 文档，不进入运行配置。

```yaml
dcc:
  application-execution:
    definition-transitions:
      - source-process-definition-id: "<保持原V1-upload-definition-id>"
        source-definition-digest: "<保持原V1-upload-digest>"
        target-process-definition-id: "<保持原V1-revision-definition-id>"
        target-definition-digest: "<保持原V1-revision-digest>"
      - source-process-definition-id: "<C05新发布V2-upload-definition-id>"
        source-definition-digest: "36bc2379b56d0ada0571fccd573024fb05f951632e0963cbcd9ab1702af7b2c2"
        target-process-definition-id: "<C05新发布V2-revision-definition-id>"
        target-definition-digest: "84d039f7fab6c81159fb049637a0ff70b6eb3e57d6dfc6b47a64475e421935d5"
```

两个新 digest 是当前 catalog 预期值，Root 仍须对新发布实际 BpmnModel 重算并匹配。配置生效后应核对 **最终绑定的整个列表** 同时有旧、新两行，不能只凭某配置来源写了“第二行”便认定旧行仍在；不能借此覆盖其它已存在且有效的映射。

## 定向证据和未验证边界

已确认存在的相关测试（本轮均未执行）：

- `DccPreparedExecutionInheritanceDatabaseTest#controlledUploadRequiresAnExplicitExactRevisionTransitionAndKeepsOldHistory`：跨动作缺 mapping 拒绝，登记后继承精确定义且历史不变，不读取 active/latest。
- 同类 `#invalidTransitionNeverPreparesOrStartsTheNewRevision`：DIGEST、WRONG_KEY、DUPLICATE、MISSING_ID、UNKNOWN_GRAPH 五种失败；重复行被拒绝，不会准备/启动。
- 同类 `#missingCrossKeyRegistrationRejectsReplayWithoutRewritingExistingRevision`：已存在 revision 的合法重放也需要保留原来源映射，删除旧行会破坏重放。
- `DccW08PreparedCreationDatabaseTest#actualCrossActionCheckinUsesOnlyTheRegisteredExactRevisionDefinition`、`#actualCrossActionCheckinWithoutMappingCannotGuessTheRevisionDefinition`：真实 Query 检入到 Revision 创建链路使用精确映射；无映射明确拒绝且不能猜 latest。

上述继承/检入测试的主要 fixture 是历史 V4，不能声称已经专门跑过“新 V2 upload -> 新 V2 revision -> 再次 V2 revision”的运行链。V2 revision -> revision 无须映射的结论来自第 201 行现有明确分支，不是本轮测试 PASS。Root 若要运行定向验证，可运行这两类（工作目录 IntRuoyiBackend）：

```powershell
mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccPreparedExecutionInheritanceDatabaseTest,DccW08PreparedCreationDatabaseTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dfile.encoding=UTF-8" test
```

Root 对 03 的实际验证应记录：新 V2 upload 源 ID/digest、创建的 REVISION prepared ID/digest 精确命中新 V2 revision；随后同 V2 revision 新轮仍保留该 origin ID/digest且未借 latest。部署/配置/页面预检成功只是前置恢复，不能代替 03 的完整业务终态。
