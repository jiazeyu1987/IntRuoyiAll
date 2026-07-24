# 任务：修复产品资料导入 product001 图片差异被判相同

## 任务目标

修复展厅产品管理导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 时，`product001` 的 `产品图` 与系统当前封面不同却被导入结果归入“跳过无变化/相同产品”的问题。

## 前序任务检查

- 已确认上一后端任务 `doc/tasks/20260531-showroom-product-import-cover-change-detection/task.md` 状态为 completed，不阻塞本任务。
- 本任务只改本机仓库，不操作测试服、正式服或远程服务器。
- 后端仓库存在无关未跟踪运行态文件 `runtime/runtime-control/runtime-ops/*.json`，本任务不触碰、不提交。
- 前端仓库存在无关改动，本任务先限定后端导入判定；若定位为前端展示问题再另行处理。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：解析真实 workbook，复现 product001 图片差异被判无变化的根因。
- [x] M3：添加 RED 回归测试，证明导入图片存在但旧逻辑会复用同一导入封面 URL。
- [x] M4：最小修复图片提取或判定逻辑。
- [x] M5：运行 GREEN、相关回归、证据校验与 closeout 预览。
- [x] M6：提交本任务直接相关改动。

## BDD 场景

- BDD: product001 导入图片不同必须算变化 -> Given 系统已有 `product001` 且当前封面与 Excel `产品图` 内容不同 / When 导入 `产品资料正式版.xlsx` 中的 product001 行 / Then `product001` 不出现在 `skippedProductCodes`，并发布新封面版本。
- BDD: 空产品图仍保留当前封面 -> Given 导入行无嵌入产品图 / When 导入产品 / Then 保留当前封面，若其他字段也无变化才允许跳过。
- BDD: 同一产品图仍跳过无变化 -> Given 导入图片内容与当前封面完全一致 / When 导入产品 / Then 不上传新封面、不增加 revision。

## 预期验证

- RED：先运行目标导入测试失败，证明旧逻辑未识别真实 workbook 中 product001 的差异图片。
- GREEN：目标导入测试通过。
- REGRESSION：产品导入与封面图片相关测试通过。

## Current Status

completed

## 当前状态

status: completed

已完成修复：导入封面上传文件名加入图片内容 SHA-256 前 16 位摘要，避免不同图片复用同一产品同一天的固定 URL。已通过封面服务单测、导入集成测试、相关回归测试、bug regression 证据校验和 task-closeout-cleanup 预览；提交时仅纳入本任务相关后端文件。
