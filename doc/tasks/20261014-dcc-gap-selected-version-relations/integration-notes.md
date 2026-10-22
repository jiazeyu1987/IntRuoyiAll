# G11 A04/A07 最新接入合同（2026-10-15）

A33新增目标正文合同：Panel/StageEvidence将已保存目标生成`buildDccControlledFilePreviewSource(targetId,{intent:'VERSION_HISTORY',controlledFileId:targetId,expectedVersionNo:rawFrozenVersion})`并正式传给Protected viewer；UsageDialog导航第四参由同目标context正式serializer产生。metadata params与binary headers复用同canonical wire，父申请BPM/task不传给目标，后端DAILY原规则保持。previewSource随close/context/unmount清理，late generation不挂载旧目标，Stage permission版号再次比对冻结raw版。

PageDB A33仅自有fixture适配真实源项目读取资格/独立OBSOLETE名称grant，原断言保留；两原失败fresh私有Spring/H2先2/2PASS。整类初次15/16的HTTP仅私有runner漏-parameters；M10明确结束后已用修正runner完整实际复验16/16PASS（0skip），当前Query/Guard/Reference/Controller和测试私有明编。无Query生产修改，最终正式-am整类及冻结整合仍Root统一票，不将私有16称全工程完成。

## 所选版本与申请关系

Panel不导入/调用listCurrentRelations，不跟currentActiveControlledFileId/latestControlledFileId。明确版本视图仅listHistoricalRelations(file.id)，名称/编号/目标版本来自所选版关系快照；精确目标当前权限只授权固定正文，不替换快照文字。

relation-contract.ts 导出的唯一父子合同：

```ts
export interface DetailRelationApplicationSelection {
  controlledFileId: string | number
  applicationType: 'UPLOAD' | 'REVISION' | 'OBSOLETE'
  bpmRound: string
  attributeRound: number
}
```

- applicationSelection undefined：明确只读所选版本关系集合。
- null：处于申请上下文但身份未就绪，Panel阻断零读取；不能用版本history补齐。
- 对象：复核exact file/type/BPM/attribute，调用已有正式getControlledFileApplicationEvidence，复核版本/tenant/master/project/round；完整集合取completeSelections，阶段证据复用DetailRelationStageEvidence保留上传/补充/整改/独立签名。
- 正式relationEvidence=null：明确未记录；undefined或错配：明确错误，不history/current替代。
- G08 getDccVersionDisplayText仅处理Panel/History/UsageDialog可见版，正式身份和raw核验保持。

## History事件已实现（A04转交，G11唯一owner）

DetailApplicationHistory.vue `selection-change` payload为上述类型或null，controlledFileId emit时规范化精确Long串。初次、上下文重置、select开始、失败、卸载同步发null。成功须当前generation和selectionGeneration，文件/type/BPM/attribute完全对应，签名BPM一致，nodeRecords按原showNodeRecords核验。正式未记录/未冻结属性证据保留显式空态，只发null，不伪称已验真属性轮次。

真实Vue父监听+History模板7/7PASS，先0/6RED及空态额外RED已保存。测试包括晚旧响应不能发旧round、foreign/lossy身份、wrong属性/signature/locked round、失败/null、草稿显示且raw保持。

G10负责detail/index.vue与ControlledFileBrowseRecords.vue两个父入口。目前源码已观察到：
- @selection-change消费本事件；所选对象经过file/round/context复核。
- :application-selection传给Panel，未就绪传null，明确版本视图undefined。
- 父层clear/generation另由G10独立验证；G11未写父页，不将观察源码称E2E验收。

## 引用exact-version合同已实现（A07释放BE）

Root R01明确引用来自已有正式持久固定selectedControlledFileId记录，允许后补，不要求整列表申请提交冻结。当前正式schema已有NOT NULL selected_controlled_file_id、created_by/created_at与目的项目/文件夹/master唯一性，无需字段/表/迁移；详见reference-schema-evidence.md。

当前实现：
- DccRelationStore 的global COUNT(DISTINCT project_id)、visible distinct/rows、page均绑定tenant/master/selectedFileId；integrity检查绑定tenant/selectedID并显式wrong-master拒绝，另一版坏记录不阻塞所选版，不静默丢本版坏记录。
- ReferenceService getUsage/getUsagePage贯穿所选ID，逐行复核ID；保留正式目的项目权限/分配硬scope/禁用项目可见范围、精确名称/正文权限及REPEATABLE_READ。create/list/replay/cancel返回的项目count同本次固定ID，原master锁顺序、精确referenceId、审计/事务保留。
- Root复核后getUsage也在名称权限后/COUNT前复用page同一assertUsageIdentity，坏master/缺目标项目或文件夹不得被COUNT滤掉而成功；guard范围仍所选tenant+ID，其他version/tenant坏记录不阻塞本版，无目的标签/正文授权扩展。此补强专属真实H2 4/4，最终私有相邻29/29。
- ReferenceUsage摘要补tenantId/sourceControlledFileId/masterId（Long串），page原有identity字段保留；Controller原请求required selectedFileId足够，未改端点。
- FE relations wrapper校验摘要身份；browse-records在当前selected tenant/master/id核验摘要；reference-usage拒绝任何row.selectedControlledFileId!=context.sourceControlledFileId，保留服务器counts/分页/detailsRestricted，不客户端去重伪总数。
- UsageDialog用保存的正式row preview，重读exact目标权限，拒pending/非正式状态，切上下文/关闭/卸载晚响应不打开窗口。

FE拒错合同需BE上述身份字段同步发布，禁止部署只前端旧BE或用fallback接受master-only摘要。不存在新平行API、申请轮次参数或引用提交快照。

## 当前测试与交接门禁

M05最新正式回执：Root句柄38484/23reactor/exit0，四类80项，其中G11 ExactVersion10+Transaction19正式29PASS，m05-junit两XML SHA与RootJSON核对一致；不只私有证据。票mutable源/drift6，不等于全12项冻结验收。UsagePageDatabase与FormalAuthority未在票内，当前报告旧时点不算本次PASS，建议后续Root唯一票补正式Query/Controller/并发page集成。M05暂停已解除/当前无活跃Maven，worker不保持过期等待，不自行开Maven。

前端97/97PASS（包括已授权旧混合真实依赖/行为迁移），BE私有fresh目标源编译执行H225/25PASS。BaseDb DccReferenceUsagePageDatabaseTest及Controller已fresh私有编译；Spring上下文/真实Query/正式Controller权限与并发读快照仍须唯一Maven票，见maven-validation-request.md。Query/applicationRead/G08 DccProjectReferences.vue和公共审计/签名均未写。

Root可接管20项代码/测试资产；SHA详见changed-files.md，改前见execution-log。Git收尾、全类型/工程票/E2E没有授权，不把上述隔离证据当运行态或全工程completed。
