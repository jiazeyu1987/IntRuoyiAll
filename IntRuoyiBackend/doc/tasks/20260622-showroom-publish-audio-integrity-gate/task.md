# 任务: 展厅发布前音频完整性门禁

## 任务目标

把展厅手工发布路径里的“公司讲解音频对象缺失”正式收口为发布门禁，而不是继续以底层文件读取异常的形式散落暴露。发布时不临时生成音频；若当前 live narration 引用的音频对象在当前环境不可读，必须直接阻断发布并返回可定位的业务错误。

## 上一任务检查

- 上一相关任务：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260622-test-server-showroom-source-missing-analysis\task.md`
- 状态：`completed`
- 处理：上一任务已确认测试服手工发布失败根因是 `showroom_company` 当前 live narration 引用了 `audio_file_id=9198110021401/9198110021402`，但测试服对象存储缺少对应音频对象。本任务在该根因证据基础上做正式代码收口，不重复定位环境数据问题。

## 用户要求与执行边界

- 用户要求：
  - 发布的时候不应该临时生成音频。
  - 发布的时候应该先做检验，缺少音频就停止发布。
  - 需要长期方案，而不是临时数据补丁。
- 本任务边界：
  - 只修改展厅发布后端代码与直接相关回归测试、任务证据。
  - 不改测试服数据，不补传测试服 MinIO 对象，不引入 fallback 或发布时补生成链路。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
  - 发布失败不得用 mock、默认成功、静默跳过或自动降级掩盖。
  - 发布链路缺少文件记录与对象一致性时必须 fail fast。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。把公司级音频对象缺失收口为明确的发布门禁。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 公司讲解音频对象缺失时阻断发布 -> Given 当前待发布版本沿用了 live 公司讲解 `audio_file_id` / When 发布入口开始解析 release source snapshot / Then 若当前环境无法读取该音频对象，发布直接失败并返回公司级发布阻断错误。
- BDD: 手工发布不临时生成音频 -> Given 当前 live narration 文案存在但音频对象缺失 / When 用户点击手动发布展厅 / Then 系统只做完整性校验并阻断，不进入临时生成音频路径。

## 当前状态

completed

## 里程碑

1. 建立任务记录，明确本次修复属于发布门禁而不是音频生成链路。`DONE`
2. 增加公司讲解音频对象缺失的回归测试。`DONE`
3. 最小调整发布组装器，把公司级缺失音频收口为显式 `SHOWROOM_RELEASE_COMPANY_BLOCKED`。`DONE`
4. 跑通目标测试并补齐证据与收尾。`DONE`

## 验证结果

- `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest#shouldFailFastWhenCompanyNarrationAudioObjectIsMissing test` -> PASS
- `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest test` -> PASS

## 最终结论

- 手工发布路径没有改成“临时生成音频”；当前正式行为仍然是基于 live narration 的 `audio_file_id` 做发布。
- 本次改动把“公司讲解音频对象缺失”从底层 `SHOWROOM_RELEASE_SOURCE_MISSING` 读文件异常，收口成显式的公司级发布阻断 `SHOWROOM_RELEASE_COMPANY_BLOCKED`，更符合“发布前完整性校验”的长期方案。
- 测试服当前要恢复手工发布，仍需补齐对应音频对象或重建该环境的公司讲解音频数据；代码不会再尝试在发布瞬间补生成。
