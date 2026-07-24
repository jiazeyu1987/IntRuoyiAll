# 正式服 product_003~product_009 缺失核查

## 任务目标
- 核对正式服产品数据中 `product_003` 到 `product_009` 是否缺失。
- 判断缺失原因：是否已映射为当前 `INT-*` 编号、被删除、未导入、未发布或前端筛选未显示。
- 如确认数据异常，按现有导入/发布规则修复并验证。

## 里程碑
- [ ] M1 核对本机导出包和底表中的 `product_003~product_009` 对应关系。
- [ ] M2 核对正式服数据库 active/deleted 产品、旧编号映射、展柜引用和发布包文档。
- [ ] M3 判定是否需要恢复/补导入/重发布，并执行修复。
- [ ] M4 验证正式服 admin 与 website 展示数量和语音对齐。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先查真实数据链路再决定修复方式。
- `是否存在临时补丁或绕过`：否。

## 当前状态
- 调查中。

## Current Status
completed

## 发布前状态更新 - 2026-07-07 18:11:10
- 已完成代码根因修复：导入新建缺失 INT 产品时保留 Excel 旧产品编号。
- 已完成正式服数据修正：INT-3~INT-9 映射 product_003~product_009。
- 下一步：检查/补齐正式服展柜布局，手动发布展厅，并验证网站 release、产品数量、语音资产和旧编号映射。
## 完成状态 - 2026-07-07 18:18:02
- 状态：已完成。
- 代码修复：ShowroomApiRuntime 导入新建缺失产品时保留 Excel 旧产品编号，防止 INT-* 新建后 legacy_product_code 为空。
- 正式服数据修正：INT-3~INT-9 已恢复并映射 product_003~product_009；正式服活跃 product_*/e2e* 产品数为 0。
- 发布结果：正式服手动发布成功，当前 release 为 20260707T101649Z-be276b74dfa8-081780e2a98e。
- 最终验证：147 个活跃 INT 产品、147 个展柜产品、147 个产品文档、294 个产品语音资产；INT-3~INT-9 中英文语音均存在；全部展柜布局缺失数为 0；website 根路径 200 且无“更新失败”；manifest、website-index、product-detail 均无 product_*/e2e/E2E 残留。
- 证据：evidence/prod-final-verify-after-company-e2e-clean.txt、evidence/prod-manual-publish-after-company-e2e-clean.json、evidence/green-create-missing-product-legacy.stdout.log。
## Cleanup Candidates
- doc/tasks/20260707-prod-product-003-009-audit/evidence/

## Closeout Status
- Status: completed
- Completed At: 2026-07-07 18:19:47
