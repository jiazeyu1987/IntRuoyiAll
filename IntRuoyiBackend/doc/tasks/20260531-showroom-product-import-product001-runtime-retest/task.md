# 任务：重启本地后端复验 product_001 导入封面变化

## 任务目标

将本地 `http://127.0.0.1:48081` 后端重启到包含提交 `20ab7c1f4a` 的新构建，并用真实导入路径复验 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 中 `product_001` 的产品图差异不再被归入跳过产品。

## 前序任务检查

- 已确认上一后端任务 `doc/tasks/20260531-showroom-product-import-product001-image-diff/task.md` 状态为 completed，不阻塞本任务。
- 后端仓库当前仅有无关未跟踪 `runtime/`，本任务不触碰、不提交。
- 前端仓库存在无关改动，本任务不触碰、不提交。

## BDD 场景

- BDD: 新后端导入 product_001 图片差异算变化 -> Given 本地 48081 运行包含封面 hash 修复的新后端 / When 通过前端或接口导入 `产品资料正式版.xlsx` / Then `product_001` 不再因为旧固定导入封面 URL 被列入跳过产品。
- BDD: 旧后端跳过结果可解释 -> Given 48081 仍运行 2026-05-31 17:35 的旧 jar / When 导入同一 workbook / Then 弹窗仍可能显示 `product_001` 在跳过产品列表。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：构建包含当前提交的新后端 jar。
- [x] M3：停止旧 48081/48082 进程并启动新后端。
- [x] M4：通过真实导入路径复验 `product_001` 结果。
- [x] M5：记录验证结果并提交任务记录。

## 预期验证

- `mvn -pl yudao-server -am -DskipTests package` 或等价可运行后端构建通过。
- `http://127.0.0.1:48081` 进程启动 jar 时间晚于提交 `20ab7c1f4a`。
- 真实导入 `产品资料正式版.xlsx` 后，`product_001` 不因封面 URL 复用进入跳过产品。

## Current Status

completed

## 当前状态

status: completed

已完成本地后端重启和复验：8081 实际请求 `localhost:48082`，已将 48082 重启为 `backend-showroom-import-20260531-193000.jar`。测试租户中 `product_001` 当前封面 hash 已与 workbook 图片 hash `b7a35f69730887ea` 一致，因此继续跳过是正确结果；默认租户仍保留旧固定 URL，刷新后使用新 48082 导入会走新判定。任务记录已更新，待提交。
