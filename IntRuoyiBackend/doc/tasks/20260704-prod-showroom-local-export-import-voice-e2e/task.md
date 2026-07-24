# 正式服展厅产品资源包语音 E2E 验证

## 任务目标

在本机导出展厅产品 zip 资源包，通过正式服真实前端 E2E 导入，并核对导入后的 INT-* 产品是否具备中文/英文产品语音，且语音绑定版本与导入后的产品当前版本一致。

## 里程碑

1. 完成任务门禁、登录/服务器/发布备份经验预检。
2. 在本机导出资源包，并检查 zip 内产品编号、manifest、讲解音频 sheet、assets 路径是否按 INT-* 对齐。
3. 通过正式服标准登录预检后，用 Playwright 真实页面导入该 zip。
4. 核对正式服导入结果：产品当前版本、中文语音、英文语音、语音 source revision 是否一致。
5. 记录验证证据、阻塞或结论，提交本任务产生的文档改动。

## 预期验证

- 本地导出的 zip 若存在 INT-* 产品，必须同时具备中文/英文产品语音。
- 正式服导入走后台页面真实上传路径，不使用接口绕过导入。
- 导入后抽查 INT-12 及导入清单内代表产品，产品语音弹窗不应出现“未生成/未记录”。
- 数据库或接口最终核对中，产品 current_revision_id 与中文/英文产品语音 source_revision_id 保持一致。

## 当前状态

进行中：已开始任务门禁和脚本路径识别。

## 经验门禁

- `docs/powershell-memory.md`：PowerShell 中文、here-string、重定向和管道必须显式 UTF-8；禁止使用 `&&` 串联命令。
- `docs/login-access.md`：涉及正式登录和真实 E2E 前，必须先跑官方 `login-preflight.mjs`，不得自行猜测登录路径或旁路登录。
- `docs/server-access.md`：正式服写入/导入属于高风险操作，只在用户明确授权范围内执行；本次用户已授权正式服 E2E 导入验证。
- `docs/release-backup-restore.md`：涉及正式环境写入前确认健康、版本和可恢复性证据；本次不执行备份恢复、发布或删除共享盘。
- `docs/experience-index.md`：高风险动作前必须在 execution-log 记录 `GREEN: experience-preflight -> PASS` 或阻塞原因。


## Current Status

completed

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；仅验证 zip 包产品语音按 INT-* 精确对齐，不做 product_* 到 INT-* 猜测映射。
- 是否存在临时补丁或绕过：否。
## 验证结论

- 本机真实页面导出的 `showroom-product-resource-package.zip` 包含 `149` 个 `INT-*` 产品，但产品语音为 `0` 条：`讲解音频` sheet 中 `PRODUCT` 语音条数为 `0`，`manifest.json` 中 `PRODUCT` 语音条数为 `0`，`assets/narration/product/` 为空。
- 因本地导出包缺少所有 `INT-*` 产品中文/英文语音，本次正式服导入已按门禁阻断，未把不完整 zip 上传正式服。
- 正式服只读核验：`INT-12` 产品存在，`productId=744`，当前版本 `currentRevisionId=5196`，`revisionNo=1`，中文名 `球囊扩张压力泵`。
- 正式服只读核验：`INT-12` 在当前版本 `5196` 下查询中文/英文产品语音均返回 `SHOWROOM_TARGET_NOT_FOUND: narration not found`，没有可对齐到当前版本的产品语音。

## 当前状态

已阻塞：正式服不能导入本次本机导出的包；根因是本地导出源数据没有 `INT-*` 产品语音，不是正式服导入动作已经修好或成功。
## 2026-07-04 19:33 进展

- 已定位并修复本机导出 zip 混入非本次产品列表语音的根因：产品行按展柜映射过滤，但产品语音行未跟随实际导出产品列表。
- 已新增并通过回归测试：`exportProductExcelShouldScopeProductNarrationsToExportedProductRows`。
- 下一步：重建/重启本机后端，重新生成本机 zip，验证包内 INT-* 产品与中英文语音一一对应，再导入正式服验证。
## 2026-07-04 20:15 完成

- 本机已生成并发布 INT 产品中英文产品语音，重新导出 zip 资源包。
- 本机 zip 已通过导入前门禁：产品数据与产品语音按 INT-* 编号对齐，无 product_* / e2e* 混入。
- 正式服已执行覆盖导入。
- 正式服只读核验通过：INT-12、INT-1、INT-10 当前产品版本均存在 PRODUCT/PUBLIC 中文与英文语音，sourceRevisionId 与 currentRevisionId 一致且音频字段存在。
- 当前状态：Completed。