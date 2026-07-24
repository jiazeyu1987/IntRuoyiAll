# 任务：eDHR 批次打印版 PDF 后端实现

## 任务目标

- 将批次最终归档从 manifest 文本 PDF 改为“整批已填写表单打印版 PDF”。
- 在归档生成阶段固化新版打印快照，下载/打印仅消费该快照。
- 对旧版归档直接 fail fast，返回明确的重生成提示。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-mes-work-order-clear-all-blockers\task.md`
- 状态：`COMPLETED`
- 处理说明：上一后端任务已完成，不阻塞本次打印版 PDF 实现。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 后端改动必须先有 RED 测试，再做最小实现。
  - 旧归档缺少新版打印快照时必须显式抛错，不得在下载时自动重生成或临时查现库。
  - 真实归档生成/下载 E2E 前必须先记录 `GREEN: experience-preflight -> PASS` 并跑官方登录预检。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过统一打印快照 schema 和服务端 PDF 渲染器，消除 manifest 文本 PDF 与前端只读表单视图脱节的问题。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 归档生成写入打印快照 -> Given 批次已关闭且存在已审批表单执行记录 / When 生成最终归档 / Then source_manifest_json 写入带 schemaVersion 的打印快照，并包含正文表单快照与特殊节点附录摘要。`
- `BDD: 下载打印版 PDF 使用打印快照 -> Given 批次最新归档为新版打印快照 / When 下载归档 / Then 返回的 PDF 包含真实表单标题、填写值、签名单元格与备注，而不是 manifest 文本章节。`
- `BDD: 旧版归档直接阻塞 -> Given 某归档 source_manifest_json 不含新版 schemaVersion / When 下载或打印 / Then 后端抛出“请先重新生成最终归档后再下载打印版 PDF”。`

## 里程碑

1. M1：补后端任务文档与执行日志。`COMPLETED`
2. M2：新增 RED 单测覆盖新版打印快照和旧归档阻塞。`COMPLETED`
3. M3：实现打印快照构建与 PDF 渲染。`COMPLETED`
4. M4：运行后端定向回归并回填 evidence。`COMPLETED`

## 预期验证

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionArchiveControllerTest test`

## 当前阻塞

- 无。

## 最终验证

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#generateArchive_manifestUsesPrintableSnapshotSchema+generateArchive_downloadPdfContainsQaReadableBatchSections+downloadArchive_legacyManifestFailsFast test` -> PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionArchiveControllerTest" test` -> PASS
