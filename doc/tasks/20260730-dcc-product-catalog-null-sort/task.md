# DCC 产品目录项目字段空值排序修复

## Task Goal

修复 DCC 产品目录中“项目名称”“项目代码”表头排序点击后未按该列内容排序的问题；空单元格在升序/降序时必须集中到最前或最后，不能保持原列表顺序分散展示。

## Milestones

- [x] 复现产品目录项目字段排序缺陷并定位排序链路
- [x] 增加最小静态回归契约，先证明当前实现缺少项目字段空值排序
- [x] 修复前端排序字段映射，保证项目名称/项目代码参与列表排序
- [x] 执行定向静态契约、类型检查和收尾门禁
- [ ] 提交并推送 `int_main`

## Expected Verification

- DCC 产品目录项目名称、项目代码列点击排序时，排序请求必须传递到正式列表查询参数。
- 空值在升序/降序中由后端或统一列表查询集中排序，不得只在当前页本地伪排序。
- `node tests\e2e\dcc-product-catalog-project-sort-static.spec.js` 先 RED 后 GREEN。
- 相邻产品目录静态契约和 `pnpm ts:check` 通过。

## Current Status

blocked

## Experience Gates

- 前端静态契约隔离门禁：本任务使用产品目录专用静态合同证明排序字段传递，不扩大到无关页面。
- Windows 换行与脚本行为同步门禁：静态合同读取 SFC 时归一化 CRLF/LF，避免换行误判。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修复统一列表排序查询链路而不是硬编码当前页排序。
- 是否存在临时补丁或绕过：否。

## Blocker

- `git push origin int_main` 连续两次失败：`fatal: unable to access 'https://github.com/jiazeyu1987/IntRuoyiAll.git/': Recv failure: Connection was reset`。
- Impact: 本地实现和收尾记录已提交，但未推送到 `origin/int_main`；按项目规则任务不能标记 `completed`。
- Pending local commits: `88e796d5 fix: support DCC product catalog project sorting`，`30026eea docs: record DCC product catalog sort closeout`。
