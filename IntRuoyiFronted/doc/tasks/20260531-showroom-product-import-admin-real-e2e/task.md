# 任务：芋道源码/admin 真实数据产品导入 E2E

## 任务目标

使用真实前端路径登录本地 `http://localhost:8081` 的 `芋道源码/admin`，导入真实 Excel `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx`，验证产品管理导入不再把 `product_001` 的产品图变化误判为“跳过无变化”。

## 前序任务检查

- 已确认上一前端任务 `doc/tasks/20260531-vite-dayjs-locale-data-export/task.md` 状态为 completed。
- 复用原 `doc/tasks/20260531-showroom-product-import-admin-real-e2e/` 任务目录；该目录此前因未获得真实租户写入授权而 blocked。
- 用户本轮明确要求“用真实数据在芋道源码/admin里做E2E测试”，视为对本地 `芋道源码/admin` 执行真实导入写入的授权。
- 当前前端仓库存在无关改动 `src/views/showroom-admin/shared/structuredError.ts`、`scripts/showroom-structured-network-error.test.mjs` 和旧任务目录，本任务不触碰、不提交这些无关文件。

## 安全边界

- 本次只操作本机 `http://localhost:8081` + `http://localhost:48081`，不操作测试服/正式服。
- E2E 通过 Playwright 操作真实前端；接口仅用于导入后最终核对。
- 使用默认“跳过”相同产品处理，不手动选择“覆盖”，验证图片差异/旧封面 URL 规范化是否会产生发布版本。

## BDD 场景

- BDD: 芋道源码/admin 可进入产品导入入口 -> Given 使用本地芋道源码/admin 登录真实前端 / When 进入展厅产品管理并打开产品导入弹窗 / Then 能看到“相同产品处理”以及“跳过/覆盖”选择。
- BDD: 真实 Excel 默认跳过导入不应跳过 product_001 图片变化 -> Given `product_001` 当前显示 `V44` 且封面是旧式导入 URL / When 使用 `产品资料正式版.xlsx` 执行默认跳过导入 / Then `product_001` 发布新版本，封面 URL 变为带内容哈希的导入 URL。
- BDD: 空白单元格提示保持保留当前值 -> Given 打开产品 Excel 导入弹窗 / When 查看导入说明 / Then 页面提示“空白单元格会保留当前数据”。

## 执行结果

- 本地端口确认：`8081` 前端在线，`48081` 后端在线，后端包为 `backend-20260531-233551-import-cover-fix.jar`。
- Playwright 登录 `http://localhost:8081/login?redirect=/index`，租户 `芋道源码`、用户 `admin`，登录接口返回 `code=0`。
- 进入 `http://localhost:8081/showroom/product`，产品列表加载成功，导入前 `product_001` 显示为 `V44`。
- 打开产品 Excel 导入弹窗，弹窗显示“空白单元格会保留当前数据”，并显示“相同产品处理 / 跳过 / 覆盖”。
- 使用真实文件 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 选中文件成功，弹窗显示 `产品资料正式版.xlsx`。
- 点击“确定”后，后端日志记录 `/admin-api/showroom/product/import-excel` 完成，耗时 `38992 ms`。Playwright 在等待响应时没有捕获到响应事件，关闭页面后该请求显示 `net::ERR_ABORTED`，但后端已经完成写入。
- 重新通过前端进入产品管理，`product_001` 显示为 `V45`。
- 最终接口核对 `product_001`：`revisionNo=45`，`status=PUBLISHED`，`cover_image=/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-product_001-imported-cover-b7a35f69730887ea.png`。
- 最终文件核对：该封面 URL 返回内容 SHA-256 为 `b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`。

## 观察到的非阻塞问题

- 导入写入后本地后端定时任务记录 `Dirty showroom release requires configured auto-publish site key and stage.`，说明本地自动发布 scope 未配置；不影响本次产品导入验证，但若要验证自动发布，需要补齐本地 release auto-publish 配置。

## Current Status

completed

## 当前状态

status: completed

真实数据 E2E 已完成。`product_001` 未再被视为“跳过无变化”，导入后发布到 `V45`，封面 URL 已切换为带图片内容哈希的新地址。
