# Execution Log: DCC 文件类别真实 DMR 绑定与路径显示修正

BDD: 文件类别绑定必须使用 DMR 真实一级目录 -> Given `3.DMR` 下同时存在空壳重名目录和带编号的真实目录 / When 系统为文件类别建立目录绑定 / Then 应优先绑定到真实目录数据所在的一级目录，例如 `图纸 -> 3.DMR/01.图纸`。

BDD: 文件类别列表显示完整绑定路径 -> Given 文件类别已经绑定到 DMR 一级目录 / When 用户打开 `DCC文件类别` 列表页 / Then `绑定目录` 列应显示完整路径，例如 `3.DMR/01.图纸`，而不是仅显示叶子名或 `-`。

RED: real frontend verification via Playwright before this fix -> FAIL, `DCC文件类别` 页面中 `INTAUTH-1` 仍显示 `3.DMR`，`INTAUTH-2..10` 仍显示 `-`，没有按真实 DMR 一级目录展示。

RED: runtime API inspection before rebinding -> FAIL, live `GET /admin-api/dcc/file-categories` returned `INTAUTH-1 -> directoryId 1` and `INTAUTH-2..10 -> directoryId null`, proving the runtime dataset had not been bound to real DMR first-level directories.

GREEN: runtime rebinding through real backend API -> PASS, all `48` runtime `INTAUTH-*` categories were rebound to `3.DMR` first-level directories, preferring the numbered real-data directory when duplicate normalized names existed.

GREEN: post-rebind runtime samples -> PASS:
- `INTAUTH-1 -> 10.产品技术要求`
- `INTAUTH-4 -> 02.说明书`
- `INTAUTH-7 -> 04.物资采购清单`
- `INTAUTH-8 -> 05.采购技术要求`
- `INTAUTH-9 -> 01.图纸`

RED: full-path Playwright verification before frontend code change -> FAIL, the page showed only leaf names like `01.图纸` and `04.物资采购清单`, not full paths like `3.DMR/01.图纸`.

GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS.

GREEN: real frontend verification after the fix -> PASS, `DCC文件类别` page now shows full paths:
- `INTAUTH-1 -> 3.DMR/10.产品技术要求`
- `INTAUTH-2 -> 3.DMR/生产用设备清单`
- `INTAUTH-3 -> 3.DMR/检验用设备清单`
- `INTAUTH-4 -> 3.DMR/02.说明书`
- `INTAUTH-7 -> 3.DMR/04.物资采购清单`
- `INTAUTH-8 -> 3.DMR/05.采购技术要求`
- `INTAUTH-9 -> 3.DMR/01.图纸`
- `INTAUTH-10 -> 3.DMR/工艺流程图`
