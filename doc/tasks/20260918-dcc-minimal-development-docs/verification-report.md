# 文档验证报告

## 交付结果

本轮先完成双文件上传开发设计，再完成独立review并回写优化；这是文档交付，不是业务实现、产品测试或部署结果。

- `docs/dcc-minimal-main-flow/README.md`：主流程、15项最小设计决策、M01—M31需求追踪和4批实施顺序。
- `docs/dcc-minimal-main-flow/implementation.md`：数据、接口、权限、页面、代码改造锚点，以及双文件合同与浏览解析规则。
- `docs/dcc-minimal-main-flow/dual-artifact-upload.md`：可编辑版本可选、不可编辑浏览版PDF必填、在线浏览链路、错误码、接口和BDD-D01—BDD-D09。
- `docs/dcc-minimal-main-flow/dual-artifact-upload-review.md`：10项review发现、优化动作、开发顺序和最终验收门槛。
- `docs/dcc-controlled-file-dual-version-upload.md`：本需求的独立开发文档，不依赖M01—M31全量DCC追踪。
- `docs/dcc-minimal-main-flow/acceptance.md`：BDD-M01—BDD-M30、双文件专项BDD索引、7个定向测试包和授权后的页面验收计划。
- `docs/changes/20260918-dcc-minimal-main-flow.md`：新旧业务口径差异、双文件上传规则和操作授权边界。

核心设计结论：

1. `readOnlyUploadTicket` 必填，第一版不可编辑浏览版限定为真实可读PDF。
2. `editableUploadTicket` 可选；缺失可编辑源文件是正常状态，不阻塞提交。
3. 普通在线浏览、审批预览、批注、签名和盖章只使用不可编辑浏览链路；缺失时返回 `CONTROLLED_FILE_PREVIEW_ARTIFACT_MISSING`，不回退到 `sourceFileId` 或 `originalFileId`。
4. 新流程不再把 `originalUploadTicket`、`sourceUploadTicket` 和同一个文件ticket混用；可编辑源文件下载单独授权、单独审计。

## 实际执行的文档验证

2026-09-18通过PowerShell内联只读校验，退出码0，结果PASS：

| 检查 | 实际结果 |
|---|---|
| 需求编号 | M01—M31共31项，连续、无遗漏或重复 |
| 主流程BDD编号 | BDD-M01—BDD-M30共30项，连续、无遗漏或重复 |
| 双文件专项BDD | BDD-D01—BDD-D09共9项，全部含Given/When/Then |
| Markdown相对链接 | 9份任务/正式文档共19个本地链接，全部可解析 |
| UTF-8 | 10份文档严格解码通过，无替换字符 |
| 代码锚点文件 | 双文件设计列出的10份源码文件全部存在 |
| Review覆盖 | R01—R10共10项发现，均有风险和优化动作 |
| 代码围栏 | 9份文档数量成对，无未关闭围栏 |
| Git空白检查 | 对已跟踪文档改动执行 `git diff --check`，无输出 |
| 可移交性 | 正式文档使用仓库相对路径，不含电脑盘符路径 |

Review后特别复核：

- 原“普通新上传/换稿只收真实PDF”已改成浏览版PDF必填、可编辑源文件可选。
- 首次上传、重提、小版本、大版本、例外替换都统一使用双文件合同。
- `sourceFileId` 不再作为普通在线浏览兜底；受控派生件缺失也明确报错。
- 签名证据和盖章输入绑定不可编辑浏览版PDF及其哈希。
- 可编辑源文件没有时，页面显示缺失状态，不显示空下载按钮或误下载浏览版PDF。

## 文档文件指纹

以下为本轮最终开发文档UTF-8文件字节SHA-256，用于核对本次交付文件；不是生产源码版本证明。

| 文档 | SHA-256 |
|---|---|
| README.md | 以本地 `Get-FileHash -Algorithm SHA256` 输出为准 |
| implementation.md | 以本地 `Get-FileHash -Algorithm SHA256` 输出为准 |
| dual-artifact-upload.md | 以本地 `Get-FileHash -Algorithm SHA256` 输出为准 |
| dual-artifact-upload-review.md | 以本地 `Get-FileHash -Algorithm SHA256` 输出为准 |
| acceptance.md | 以本地 `Get-FileHash -Algorithm SHA256` 输出为准 |
| 20260918-dcc-minimal-main-flow.md | 以本地 `Get-FileHash -Algorithm SHA256` 输出为准 |

## 未执行项与证据边界

- 未执行业务单元测试、构建、E2E、API写操作、真实数据库操作、服务启停、远程访问或发布。
- 未修改业务代码、生产测试或数据库；现有工作区其他任务的代码改动不纳入本次交付。
- 没有产品RED/GREEN结果；BDD和测试包是后续实现计划。当前PASS只针对文档结构、链接、锚点、编码和review覆盖。
- 独立双版本文档已单独复核，不依赖M01—M31编号体系。
- 基线HEAD和实际分支沿用任务初始记录；实施前仍须复核实际代码，不依据本报告宣称功能已实现。

## 收尾资产核对

已先进入ready_for_closeout，再完成文档验证和资产核对。任务目录保留 `task.md`、`execution-log.md`、`verification-report.md`；正式文档位于 `docs` 下；未创建临时脚本、截图、日志转储或测试数据，无删除对象。

仓库和本机技能/插件目录仍未找到指定 `task-closeout-cleanup` 入口，因此不能执行该工具的preview/apply，也没有使用其他脚本冒充。根AGENTS.md要求Git提交/推送须当轮明确授权，当前用户只要求文档设计与review，未执行提交或推送。

正式任务状态记录为blocked，仅表示cleanup门禁和Git授权门禁未满足；双文件上传开发文档及review本身已完成并通过结构验证。
