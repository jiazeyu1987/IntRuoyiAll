# 执行日志：展柜维护产品打开缓慢原因诊断

BDD: hall mapping dialog should open without long sequential wait -> Given 用户点击 `展柜管理` 列表中的 `维护产品` When 前端加载候选产品数据 Then 页面不应通过逐页串行拉取完整产品池而导致明显长时间等待

NOTE: 本次任务是原因诊断，默认不改生产代码，只输出当前实现和真实请求路径下的结论。

CODE PATH: `src/views/showroom-admin/components/HallProductMappingDialog.vue:165-184` 中的 `loadAllProductOptions()` 会在每次打开弹窗时，从 `pageNo = 1` 开始调用 `ShowroomAdminApi.getProductPage({ pageNo, pageSize: 20 })`，并在 `while` 循环里串行请求直到拉完整个产品分页结果。

LIVE DATA: 当前真实 `/admin-api/showroom/product/page?pageNo=1&pageSize=20` 返回 `total = 180`，因此前端按现有逻辑会拆成 `9` 页。

LIVE MEASUREMENT: 在当前环境中，单页接口耗时约 `4.67s ~ 5.05s`；按前端同样的串行策略顺序拉取 9 页，总耗时约 `43322.87ms`。

ROOT CAUSE: “维护产品”弹窗为了避免遗漏已映射产品，不再只用首屏 20 条候选，而是每次打开都重新把完整产品池逐页拉齐；但实现方式是前端逐页串行请求，没有复用缓存，也没有后端提供适合映射弹窗的一次性候选接口，所以等待时间几乎等于 `全量页数 × 单页接口耗时`。

RISK: 只要产品总数继续增长，或 `product/page` 单页接口继续保持当前 4-5 秒级耗时，弹窗打开时间会线性变得更长。
