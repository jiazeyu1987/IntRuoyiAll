# Execution Log

## User Intent

用户要求将职责文档统一为以下口径：

- ERP 同步形成“本地生产订单列表/候选池”，生产组长确认后才进入“活跃订单池”，后续报工分配、PQC、批记录、放行都只认活跃订单。
- 系统文档改为“员工选择工序，系统保留现场上下文，订单在组长分配后确定”。
- PQC 任务来源统一为“活跃订单池中未异常终止、已关联路线工序、且存在有效 QA 检验规程的订单”。
- ERP 正式同步口径为夜间批量同步，同时支持手动点击同步。
- PQC 组长可作为实际检验人。
- 生产组长只上报异常订单，不做质量判定。
- 首检、上午巡检、下午巡检默认存在，QA 设置中首检、上午巡检、下午巡检为必须项；只有末检根据产品不同而不同。

## Command / Rule Evidence

- 读取 `docs/powershell-encoding.md`，确认中文文本使用 UTF-8 读写。
- 读取 `docs/task-closeout-rules.md`，确认任务记录、验证和收尾要求。
- 执行 `git -C E:\IntRuoyi status --short --branch`，发现进入任务前已有既有改动且分支 ahead 5。
- 读取 `docs/experience-index.md`，确认本任务适用 UTF-8/PowerShell 文档处理门禁。

## Milestones

- BDD: 职责口径统一 -> Given 职责目录中存在生产、系统、QA、PQC 文档 When 按用户确认的正式口径更新 Then 文档不再混用候选订单池、活跃订单池、员工选订单、PQC 任务来源和检验默认规则。
- Updated: `系统.md` -> 统一本地生产订单列表 / 候选池、活跃订单池、员工不选订单、PQC 来源、默认首检/上午巡检/下午巡检和放行口径。
- Updated: `生产组长.md` -> 统一 ERP 夜间批量同步 + 手动点击同步、异常订单上报和不做质量判定。
- Updated: `QA.md` -> 统一首检、上午巡检、下午巡检为必需配置，末检按产品/工序差异配置。
- Updated: `pqc检验员.md` -> 统一 PQC 任务来源、PQC 组长可作为实际检验人、必需巡检规则阻塞条件。
- Updated: `pqc组长.md` -> 统一 PQC 任务来源、PQC 组长可作为实际检验人但不得自审、质量异常和生产异常边界。

## Verification Evidence

- GREEN: `rg -n --encoding utf-8 "已确认且未异常终止|系统生产订单池|选择或确认生产订单|如果需要巡检|配置该工序是否需要首检|巡检比例 =|QA 设置的巡检比例|生产组长.*质量判定" "C:\Users\BJB110\Desktop\文档\职责"` -> PASS，未发现旧冲突表达；输出只剩新口径中的“生产组长只上报异常订单和生产侧影响，不做质量判定”以及“上午/下午巡检比例”示例。
- GREEN: `rg -n --encoding utf-8 "本地生产订单列表 / 候选池|活跃订单池中未异常终止|员工不选择生产订单|订单归属在生产组长|夜间批量同步|手动点击同步|上午巡检比例|下午巡检比例|PQC 组长可以作为实际检验人|不确认自己作为实际检验人|生产组长只上报异常订单" "C:\Users\BJB110\Desktop\文档\职责"` -> PASS，新口径在系统、生产组长、QA、PQC 检验员、PQC 组长文档中均可定位。
- GREEN: `Get-ChildItem ... | Get-Content -Encoding utf8` -> PASS，6 份职责 Markdown 均 UTF-8 可读。
- REGRESSION: 文档口径调整，无生产代码、数据库、构建或 E2E 变更；按任务范围执行文本一致性回归。

## Closeout Status

- 当前状态：`ready_for_closeout`。
- Blocker: 进入任务前仓库已存在 unrelated dirty changes，且 `int_main` 已 ahead `origin/int_main` 5 commits。本任务未提交/推送，避免混入既有无关改动。

## 2026-08-01 Scope Adjustment

- User update: 生产班组长可以在金蝶 ERP 填写调拨申请单；本系统只同步读取。
- User update: 工序应完成数量按生产订单数量乘以工序生产系数计算，生产系数必须配置。
- Updated: `生产组长.md` -> 增加生产组长在金蝶 ERP 填写调拨申请单，本系统不生成/编辑调拨申请单；补强生产系数必填和缺失阻塞。
- Updated: `系统.md` -> 增加生产班组长在金蝶 ERP 填写调拨申请单，本系统只同步读取；补强生产系数必填和缺失阻塞。
