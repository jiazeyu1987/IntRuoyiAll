# 任务：展厅 Excel 初始化导入

## 目标

将 `D:/ProjectPackage/Int/IntRuoyi/resource/展厅产品与描述清单.xlsx` 的数据一次性初始化为展厅管理与产品管理的正式业务数据，并保持现有前端接口契约不变。

## 范围

- 将 showroom 的 company/product/hall 读写改为数据库持久化。
- 提供一份可重复验证的 Excel seed 生成脚本。
- 导入 166 条产品、8 个展厅、15 个公司主数据及其映射关系。
- 保持 `GET /admin-api/showroom/product/page`、`GET /admin-api/showroom/hall/page`、`GET /admin-api/showroom/company/current` 的外部契约不变。

## 里程碑

- [x] M1: 补齐 showroom 持久化数据对象、Mapper 和基础读取/写入能力。
- [x] M2: 实现 Excel 解析与种子生成脚本。
- [x] M3: 实现一次性初始化导入与完整回滚保护。
- [x] M4: 补充测试并验证 API 和前端冒烟。
- [x] M5: 记录验证结果并完成收尾。

## 预期验证

- `mvn -pl yudao-module-showroom test`
- 认证后 `GET /admin-api/showroom/product/page` 返回 166 条产品
- 认证后 `GET /admin-api/showroom/hall/page` 返回 8 个展厅
- 前端 `showroom-admin` 页面展示产品/展厅摘要计数

## 当前状态

已完成。

## 最终验证结果

- PASS: `mvn -pl yudao-module-showroom test`
- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_excel_seed_tooling.py -q`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: 使用本地运行参数对应的 MySQL `127.0.0.1:23306/ruoyi-vue-pro` 成功执行 showroom schema 与 Excel seed
- PASS: 认证后 `GET /admin-api/showroom/company/current` 返回空草稿脚手架
- PASS: 认证后 `GET /admin-api/showroom/product/page` 返回 166 条产品快照
- PASS: 认证后 `GET /admin-api/showroom/hall/page` 返回 8 个展厅与 166 条映射
- PASS: 真实前端路径 `首页 -> 数字展厅入口 -> 进入展厅后台 -> 产品管理/展厅管理标签` 可见 `166 个产品` 与 `8 个展厅`

## Cleanup Keep

- `doc/tasks/20260519-showroom-excel-init-import/task.md`
- `doc/tasks/20260519-showroom-excel-init-import/execution-log.md`
