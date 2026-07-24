# 任务：product_001 透明封面导入与 Website 发布 E2E

## 任务目标

使用本机真实后台、后端和 Website，通过真实 UI 导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx`，确认 `product_001` 的 Excel 产品图、后台封面和 Website 发布后的产品图片都包含透明背景。

## 前序任务检查

- 已检查上一前端任务 `doc/tasks/20260601-showroom-product-import-timeout/task.md`，该任务因用户目标切换已记录为 blocked。
- 当前仓库存在既有未提交改动，本任务只记录和提交 `20260601-product001-cover-website-e2e` 相关证据；不回滚、不纳入无关改动。

## BDD 场景

- BDD: Excel 产品图透明源可识别 -> Given 真实 workbook `产品资料正式版.xlsx` / When 按 `展品编码=product_001` 定位 `产品图` 单元格嵌入图片 / Then 能记录图片 hash、尺寸和透明 alpha 统计。
- BDD: 真实前端导入后后台封面透明 -> Given 登录本机 `芋道源码/admin` 的产品管理 / When 点击导入并上传真实 workbook / Then `product_001` 当前后台封面文件包含透明 alpha。
- BDD: 手动发布后 Website 使用透明封面 -> Given 后台封面已透明且 Website 本机启动 / When 在公司信息页点击“手动发布展厅” / Then Website 中 `product_001` 的图片源文件包含透明 alpha。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：完成前置检查和 Excel `product_001` 图片透明度证据。
- [x] M3：通过 Playwright 真实 UI 执行导入并验证后台封面透明。
- [x] M4：通过 Playwright 真实 UI 执行手动发布并验证 Website 图片透明。
- [x] M5：记录最终证据，运行 closeout 预览并仅提交本任务相关文件。

## 预期验证

- 本机 `8081`、`48081`、`8083` 服务可用，Website scope 为 `yingtai-showroom/TEST`。
- Excel `product_001` 产品图、后台当前封面、Website 当前产品图片均有透明 alpha。
- UI 发布接口 `/admin-api/showroom/release/publish` 返回成功 releaseId。

## 当前状态

status: completed

Excel `product_001` 产品图、导入后的后台封面、发布物源文件中的 Website `product_001` 图片均已确认包含透明 alpha，且 hash 一致为 `b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`。

最终结果：已清理旧 E2E 编排器进程树并修复后端 Website readback HTTP/1.1 兼容问题；真实 UI 手动发布成功返回 `releaseId=20260531T183422Z-be276b74dfa8-428f69663d1f`。真实 Website 根路径进入产品展柜后，`product_001` 卡片与详情页图片源均为 `product-1-preview/b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`，源文件透明像素 `9679/10800`。
