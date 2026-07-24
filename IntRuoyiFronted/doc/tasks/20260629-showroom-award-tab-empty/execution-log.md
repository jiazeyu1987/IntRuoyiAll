# 执行日志：展厅产品管理奖项页签无数据

BDD: 奖项页签展示真实奖项列表 -> Given 奖项分页接口返回真实数据 / When 用户进入奖项页签 / Then 表格显示奖项编码、名称、颁发单位、日期/期限和封面。

BDD: 奖项列表接受嵌套修订结构 -> Given 奖项行字段位于 displayRevision 或 revision / When 前端归一化奖项行 / Then 列表可平铺渲染，不因嵌套结构而空白。

GREEN: experience-preflight -> PASS，已命中并读取 `docs/powershell-memory.md` 与 `docs/login-access.md`；真实登录前置必须先跑官方 `login-preflight.mjs`。

GREEN: `node scripts/showroom-admin-award-list.test.mjs` -> PASS，前端奖项列表当前可从 `displayRevision/revision` 嵌套响应中平铺出 `nameCn/nameEn/issuer/awardDateText/coverImageUrl`。

GREEN: Playwright 只读探针（`测试租户/aoteman`）-> PASS，奖项页签接口返回 `46` 条总记录，页面首屏渲染 `20` 行。

GREEN: Playwright 只读探针（`芋道源码/admin`）-> PASS，奖项页签接口返回 `46` 条总记录，页面首屏渲染 `20` 行。

BLOCKER: reproducibility -> 本机默认两套真实账号都未复现“奖项无数据”，当前缺少用户实际复现上下文。
