# MES 工序 Excel 源数据一致性修复

## Task Goal

- 将系统 MES 工序列表与 `C:\Users\BJB110\Desktop\文档\压力泵工序.xlsx` 中的工序数据修正为完全一致。
- 验收口径：系统列表不多、不少、不改名、不乱序；若正式数据链路缺少源文件或目标位置，必须 fail fast，不用默认值、mock 或兜底补齐。

## Milestones

- [ ] 建立源数据基线：解析 Excel 中的 MES 工序清单、字段、数量和顺序。
- [ ] 定位系统 MES 工序列表的数据来源、导入逻辑或种子数据。
- [ ] 编写或更新回归测试，先证明当前系统列表与 Excel 不一致。
- [ ] 实施最小正式修复，使系统数据与 Excel 完全一致。
- [ ] 运行定向验证并记录 RED/GREEN/REGRESSION 证据。
- [ ] 收尾：更新验证报告、经验沉淀、cleanup、提交并推送。

## Expected Verification

- Excel 解析结果可复核：记录 sheet、表头、工序条数、首尾工序和规范化规则。
- RED：定向测试或校验脚本在修复前报告系统列表与 Excel 有差异。
- GREEN：同一测试或校验脚本在修复后通过，差异数为 0。
- REGRESSION：按触发规则运行相关后端/前端/数据库定向验证。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将定位正式数据源并以 Excel 为唯一验收基线。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 已读取 `docs/experience-index.md`，命中方向：MES 工序、工艺路线 Excel 导入、数据库种子/基线数据、PowerShell 编码与任务收尾。
- 待读取并摘录具体门禁：`docs/database-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`（按实际修改范围确认）。

## Known Risks

- 当前 `int_main` 工作区已有大量未提交改动且分支领先 `origin/int_main`，本任务不得覆盖或混入无关改动；若目标文件与既有改动冲突，需先阻塞确认。
