# DCC 产品目录注册证有效期日期归一化修复

## 任务目标

分析 DCC 产品目录点击“注册证有效期”后，列表日期 `2027.9.8` 与 NMPA 页面日期 `2027-09-08` 看似一致但页面显示红色的问题，并确认根因与修复落点。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，后续命令显式设置 UTF-8，不使用 `&&` 串联。
- 缺陷修复：按 `bug-regression-fix-loop` 执行，先补可复现回归测试，再做最小修复。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本轮不改 UI 结构，仅修正后端比对结果。
- 高风险动作：本轮不执行真实 E2E、服务器写入、数据库写入、发布、备份、恢复或 worktree 合并；无需 `experience-preflight` 高风险放行。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，统一修正注册证有效期日期解析归一化，保留缺失日期 `UNSUPPORTED`、抓取失败 `FETCH_FAILED`、行键异常 fail fast 的既有合同。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: NMPA 页面短横线日期与列表点分日期一致 -> Given 产品目录列表有效期为 `2027.9.8` 且注册证页面展示 `有效期：2027-09-08` / When 点击“注册证有效期”触发后端比对 / Then 若外站可抓取成功，接口应返回 `MATCH`，本地与外站有效期均归一化为 `2027-09-08`。
- BDD: 真实不同日期仍提示不一致 -> Given 列表有效期与注册证页面有效期年月日不同 / When 执行比对 / Then 接口返回 `MISMATCH`，前端显示红色。

## 里程碑

1. 建立任务台账、BDD 场景与执行日志。completed
2. 补充 `2027.9.8` vs `2027-09-08` 的聚焦回归测试。completed
3. 复核注册证有效期日期解析归一化逻辑。completed
4. 定位真实根因为 NMPA 外站抓取 `HTTP 412`，修复转移到前端状态语义。completed
5. 收尾记录后端分析结论。completed

## 预期验证

- `mvn -pl yudao-module-dcc "-Dtest=DccProductCatalogRegistrationExpiryCompareServiceTest" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260703-dcc-product-catalog-registration-expiry-date-normalization\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260703-dcc-product-catalog-registration-expiry-date-normalization --mode preview`

## 当前状态

completed

## 当前阻塞

- NMPA 外站当前直接抓取详情页返回 `HTTP 412`，后端服务无法稳定读取真实详情正文；本轮未在后端伪造成功状态。

## 完成记录

- 现有后端日期解析已覆盖点分、本地短横线、斜杠和中文年月日格式；新增 `2027.9.8` 对 `2027-09-08` 的聚焦回归测试通过，说明“日期格式不一致”不是当前红色根因。
- 真实根因为 NMPA 外站抓取失败会返回 `FETCH_FAILED`，而前端此前把 `FETCH_FAILED` 与 `MISMATCH` 共用红色样式，造成误报。
- 实际修复落在前端任务 `20260703-dcc-product-catalog-registration-expiry-fetch-color`。
