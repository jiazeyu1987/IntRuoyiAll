# 20260707 本机导出正式服导入并手动发布

## Task Goal
- 在本机 `芋道源码/admin` 导出展厅产品 zip 包。
- 在正式服导入该 zip 包。
- 在正式服执行“手动发布展厅”，并验证发布成功、Website 可访问。
- 中途遇到问题按根因修复，不引入 fallback、不吞异常、不绕过正式链路。

## Milestones
- [x] M1：确认本机与正式服运行态、账号、脚本和数据包契约。
- [x] M2：本机 `芋道源码/admin` 真实登录并导出展厅产品 zip。
- [x] M3：正式服真实登录并导入本机导出的 zip。
- [x] M4：正式服点击“手动发布展厅”并验证发布成功。
- [x] M5：访问正式服 Website，确认当前 release 与页面数据生效。
- [x] M6：按用户要求恢复冻结信息改动，正式服先不加入冻结字段/导入导出冻结信息。
- [x] M7：发布恢复版本到正式服，并验证后端/前端/PDF worker 健康、正式库无冻结列、后端近 30 分钟无冻结字段 SQL 错误。
- [x] M8：清理正式库导入后多出的 9 个无展柜引用孤儿 `INT-*` 产品，并重新发布验证产品数量、语音和 Website 当前 release。

## Expected Verification
- 本机导出的 zip 文件存在、可读，且不包含冻结信息字段或 sheet。
- 正式服导入接口或真实页面导入返回成功，并能查到导入后产品/语音数据。
- 正式服手动发布返回 `code=0` 和新 `releaseId`。
- `http://172.30.30.57:8083/` 可访问且不再显示 release 更新失败。
- 正式服活跃 `INT-*` 产品数量与发布产品文档数量一致，且每个发布产品都有中文/英文语音资产。

## 经验门禁
- 已读取 `docs/powershell-memory.md`，PowerShell 中文和多行命令必须 UTF-8 显式处理，禁止 `&&` 和 Bash heredoc。
- 已读取 `docs/server-access.md`、`docs/login-access.md`、`docs/release-backup-restore.md`，正式服操作限本次用户授权目标。
- 高风险动作前必须记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；恢复正式服不支持的冻结字段、修正导入按钮布局遮挡、补齐发布所需展柜画布布局，并清理不属于导入包且无展柜引用的孤儿产品。
- `是否存在临时补丁或绕过`：否；正式服导入按钮因旧前端 CSS 被搜索框覆盖时，使用 DOM click 触发同一个真实导入按钮事件完成当次验证，并已在前端源码修复布局问题。

## Current Status
- 已完成。
- 本机 `芋道源码/admin` 重新导出 zip：`evidence/showroom-products-local-admin-20260707T080819Z.zip`，大小 `599585060` bytes。
- 最新 zip 合约验证通过：包含 `manifest.json` / `product-data.xlsx`，未包含冻结字段或冻结文案。
- 正式服已导入最新 zip；导入响应 `totalRows=140`、`successCount=21`、`skippedCount=119`、`failureCount=0`，荣誉 `awardSuccessCount=46`。
- 正式服首次手动发布失败根因是 `hall canvas layout is required`；已通过现有 `calculate-bu-canvas-layout` 与 `update-item-canvas-layout` 业务接口补齐正式租户 10 个发布展柜画布布局，所有展柜面积覆盖均为 `1.000000`。
- 正式服发现 9 个活跃 `INT-*` 产品不在导入包内、没有旧编号、没有展柜引用：`INT-64`、`INT-69`、`INT-70`、`INT-71`、`INT-72`、`INT-73`、`INT-74`、`INT-75`、`INT-83`。已通过正式产品删除接口清理，清理后活跃 `INT-*` 产品数 = 140，展柜引用 `INT-*` 产品数 = 140，活跃旧编号产品数 = 0。
- 最终正式服手动发布成功：`20260707T085711Z-be276b74dfa8-485abb12668a`。
- Website 当前 release 已切到 `20260707T085711Z-be276b74dfa8-485abb12668a`，`manifestHash=3ab2fe4e01deaaf1c5d0880d1e60377bea730d8e9cffca463953728d6c6b6ce2`。

## Final Verification
- `mvn -pl yudao-module-showroom '-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomApiRuntimeProductPageTest' test`：PASS。
- 测试服发布 `20260707_showroom_freeze_restore_v2`：PASS。
- `mark-tested`：PASS。
- 正式服 dry-run preflight：PASS。
- 正式服发布 `20260707_showroom_freeze_restore_v2`：PASS。
- 本机重新导出 zip：PASS，证据 `evidence/local-admin-export-rerun-summary.txt`、`evidence/local-admin-export-rerun-contract.json`。
- 正式服导入 zip：PASS，证据 `evidence/prod-import-response-1783413341552.json`。
- 正式服手动发布：PASS，最终 release `20260707T085711Z-be276b74dfa8-485abb12668a`，证据 `evidence/prod-manual-publish-after-orphan-delete.out.log`。
- Website release 验证：PASS，`/release/current` 返回最终 release，根页面 200，无 `更新失败` / `SHOWROOM_RELEASE_INSTALL_FAILED`，证据 `evidence/prod-final-publish-verify.txt`。
- 发布包产品/语音验证：PASS，产品文档 140，产品语音资产 280，对应 140 对中英文语音，缺语音产品 0，manifest 中无精确旧产品编号 `product_数字`，证据 `evidence/prod-final-publish-verify.txt`。
- 正式库最终计数：PASS，活跃 `INT-*` 产品 140，展柜引用 `INT-*` 产品 140，活跃 `product_*` / `e2e*` 产品 0，已删除孤儿产品 9，活跃孤儿产品 0。