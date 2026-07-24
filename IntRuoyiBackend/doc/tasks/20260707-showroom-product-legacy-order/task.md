# 产品管理按旧产品编号排序

## 任务目标
- 修复展厅产品管理列表默认显示顺序，使正式服芋道源码租户按旧产品编号 `product_XXX` 的自然顺序显示。
- 缺旧产品编号的产品必须排在已有旧编号产品之后，避免影响已确认的历史编号顺序。

## 里程碑
- [x] M1 定位当前产品分页排序规则。
- [x] M2 补充回归测试，复现按 `id` 排序导致旧编号乱序的问题。
- [x] M3 修改产品分页排序规则为旧编号自然顺序。
- [x] M4 运行目标测试并记录正式服验证方式。

## 经验门禁
- PowerShell/中文/命令承载：已读取 `docs/powershell-memory.md`，写中文文档和脚本使用 UTF-8 显式路径。
- 正式服：本阶段先修源码并验证排序契约；正式服发布或数据库写入前必须另做备份/发布门禁。
- 无 fallback：排序必须按真实 `legacy_product_code`，不得用猜测旧编号替代缺失数据。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在产品分页源头统一排序，而不是临时调整页面展示。
- `是否存在临时补丁或绕过`：否。

## Current Status
completed

## 完成记录
- 已新增 `ShowroomPersistentContentServiceTest#productPageShouldOrderRowsByLegacyProductCodeNaturally`，证明 `product_003/product_010/product_020` 必须按旧编号自然顺序返回，缺旧编号产品排最后。
- 已将 `ShowroomProductMapper.selectListOrdered/selectPageOrdered` 统一为旧产品编号排序，`pageProducts` 改为使用统一排序入口。
## 验证结果
- 聚焦回归：`ShowroomPersistentContentServiceTest#productPageShouldOrderRowsByLegacyProductCodeNaturally` PASS。
- 相关回归：`ShowroomPersistentContentServiceTest#productPageShouldOrderRowsByLegacyProductCodeNaturally+hallMappingsShouldPersistInDisplayOrder` PASS。
- 正式服生效方式：需要将包含本次后端代码的发布包按测试服前置验证与正式服发布门禁推进；仅本地提交不会改变正式服运行排序。
