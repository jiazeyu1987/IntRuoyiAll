# 前端与 Website 证据

## 只读检查范围

- 管理前端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-showroom-product-import-sales-country-bu\yudao-ui-admin-vue3`
- Website worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-showroom-product-import-sales-country-bu\Website`
- 前端本地入口基线：`http://localhost:8081`

## 管理前端实现前基线证据

- 以下证据来自文档 worker 的实现前只读检查，用于说明 RED 起点。
- `src/views/showroom-admin/product/contracts.ts:123-210` 实现前字段 contracts 仍把 `pipeline_layout`、`core_selling_points` 及英文变体显示为旧语义/待替换标签。
- `src/views/showroom-admin/index.vue:138-152` 实现前基础信息表单仍用旧语义/待替换标签渲染 `pipelineLayout` 与 `coreSellingPoints`。
- `src/views/showroom-admin/index.vue:1418-1423` 保存时仍提交 `pipeline_layout(_en)` 与 `core_selling_points(_en)`，可保持 key 不变。
- `src/views/showroom-admin/components/ProductListTable.vue:392` 实现前状态项 key 仍命名为 `zh_core_selling_points` / `en_core_selling_points`。
- `src/views/showroom-admin/components/ProductListTable.vue:1146-1167` 实现前状态列通过 `core_selling_points` / `core_selling_points_en` 判断资料状态，短标签仍是旧语义缩写，需改为 `在售国家` 对应信息。
- `src/views/showroom-admin/index.vue:2329-2352` 实现前批量补齐提示仍描述旧语义，需要业务决策后重命名、禁用或移除；不能继续把旧内容语义写入 `在售国家`。
- `scripts/showroom-admin-version-center.test.mjs:173-280` 实现前版本中心测试数据仍断言旧语义标签，需改为新标签。

## Website 实现前基线证据

- `public/mock/showroom-display-website-config.json:68-70,135-137` 实现前 mock 中 `core_selling_points` 的 `labelZh/labelEn` 仍是旧语义/待替换标签。
- `src/showroom-website-config.mock.js:65-66,132-134` 实现前 Website 源码 mock 同样仍使用旧语义/待替换标签。
- `src/medical-kiosk.test.js:110-114,156-160` 实现前前台测试数据仍断言旧语义标签和值。
- `tests/kiosk-detail.spec.js:57-61` 实现前 Playwright 测试数据仍断言旧语义标签和值。

## 前端显示契约

| 位置 | `pipeline_layout` 最终显示 | `core_selling_points` 最终显示 |
| --- | --- | --- |
| 管理前端基础信息 | `BU` | `在售国家` |
| 管理前端英文信息 | `BU` | `Countries on Sale` |
| 字段 contracts | `label: BU`, `labelEn: BU` | `label: 在售国家`, `labelEn: Countries on Sale` |
| 产品列表状态列 | 如展示该字段，使用 `BU` | 使用 `在售国家` / `在售国家(英)` 或等价清晰短标签 |
| 版本中心字段列表 | `BU` | `在售国家` |
| Website `bilingualPublicFields` | `labelZh: BU`, `labelEn: BU` | `labelZh: 在售国家`, `labelEn: Countries on Sale` |
| Website 示例值 | 业务单元，例如 `心内介入 BU` | 国家/地区，例如 `中国`、`China` |

## 前端实现检查清单

- 更新 `contracts.ts` 中 `pipeline_layout`、`pipeline_layout_en`、`core_selling_points`、`core_selling_points_en` 的显示标签。
- 更新基础信息表单静态 label，保持提交 fields key 不变。
- 更新 `ProductListTable.vue` 状态列文案和测试断言，使 `core_selling_points` 的资料完整判断表达为 `在售国家`。
- 更新版本中心脚本测试数据中的字段标签和值。
- 检查所有管理前端旧语义文案；只允许在测试名、迁移说明或旧语义注释中作为“待替换”出现，不得在最终 UI 中出现。
- 对批量补齐入口做 Gate 决策：改为 `在售国家` 补齐、禁用/移除，或由 reviewer 明确延期；不得保留会写错语义的入口。

## Website 实现检查清单

- 更新 `public/mock/showroom-display-website-config.json` 的 `core_selling_points` 展示标签和值。
- 更新 `src/showroom-website-config.mock.js` 的同名 mock。
- 更新 `src/medical-kiosk.test.js` 和 `tests/kiosk-detail.spec.js` 中前台展示断言。
- 若发布包包含 `pipeline_layout`，补充 Website mock/test 验证 `BU` 的公开展示。
- 确认 Website 只使用发布 JSON 的 `fieldCode` 与 label/value 渲染，不在前台硬编码旧业务标签。

## 前端 RED/GREEN 证据模板

RED: `node scripts/showroom-admin-product-list.test.mjs` -> FAIL, expected reason: 状态列仍按旧语义显示 `core_selling_points`。

GREEN: `node scripts/showroom-admin-product-list.test.mjs` -> PASS

RED: `node scripts/showroom-admin-version-center.test.mjs` -> FAIL, expected reason: 版本中心仍断言旧语义标签。

GREEN: `node scripts/showroom-admin-version-center.test.mjs` -> PASS

RED: `pnpm ts:check` in `yudao-ui-admin-vue3` -> FAIL, expected reason: 标签/类型调整前可能暴露 contracts 或状态项命名不一致。

GREEN: `pnpm ts:check` in `yudao-ui-admin-vue3` -> PASS

RED: `pnpm test -- --run` in `Website` -> FAIL, expected reason: Website mock/tests 仍使用旧语义标签。

GREEN: `pnpm test -- --run` in `Website` -> PASS

RED: Playwright E2E at `http://localhost:8081` -> FAIL, expected reason: 导入验收 Excel 后管理端或 Website 仍未展示 `BU`、`在售国家`。

GREEN: Playwright E2E at `http://localhost:8081` using test tenant and 验收 Excel -> PASS，`产品资料修改版.xlsx` 164 行全部成功发布。

## E2E 真实路径计划

1. 使用 `docs/login-access.md` 中测试环境/测试租户账号登录管理前端。
2. 打开展厅-产品管理，从固定前端入口 `http://localhost:8081` 操作，不通过接口代替前端导入。
3. 选择导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版.xlsx`，该文件 `产品列表` 只含替换后表头：`展品编码`、`产品名-中文`、`产品名-英文`、`展柜名称`、`持证公司`、`在售/在研`、`BU`、`在售国家`、`适应症`、`型号规格`、`注册证信息`、`奖项`、`原材料表单`。
4. 在产品列表检查资料状态列使用 `在售国家` 对应信息。
5. 打开导入后的产品基础信息，检查产品中英文名、`在售/在研` 解析结果、`BU`、`在售国家`、`适应症`、`型号规格`、`注册证信息` 值正确。
6. 使用一条 `持证公司` 与当前所属公司不一致的测试数据验证前端导入反馈失败可见；若 `展柜名称`、`奖项`、`原材料表单` 未实现导入，反馈不能暗示这些列已导入。
7. 触发或检查发布后的 Website 前台，确认公开字段显示新标签。
8. 最后用 API/数据库只做字段值核对，不替代 UI 流程。

## 前端当前状态

管理前端和 Website 实现已完成，目标测试、管理端类型检查、Website Playwright 和真实测试租户导入 E2E 均通过；Gate 3 已放行。

## 前端/Website worker 实现证据（2026-05-26）

### 已完成改动

- 管理端 `product/contracts.ts`、基础信息表单、版本中心脚本测试统一把 `pipeline_layout(_en)` 显示为 `BU`，把 `core_selling_points(_en)` 显示为 `在售国家 / Countries on Sale`；保存与读取仍保持原 fields key。
- 管理端产品列表状态列继续读取 `core_selling_points` / `core_selling_points_en`，但状态项 key 与短标签改为 `zh_countries_on_sale`、`en_countries_on_sale`、`在售国家`、`在售国家(英)`。
- 管理端批量入口按钮改为 `一键在售国家`，内部事件/prop/handler/API wrapper 改为 `batch-generate-sales-countries`、`batchGenerateProductSalesCountries`，调用 `/showroom/product/batch-generate-sales-countries`；脚本测试增加旧路径反向断言。
- Website mock、public JSON mock、Vitest 数据与 Playwright 数据增加 `pipeline_layout` 的 `BU` 展示，并把 `core_selling_points` 示例改为国家/地区值。
- 新增 `Website/src/showroom-website-config.mock.test.js`，锁定 Website 源 mock 与 public JSON mock 不再包含旧业务标签。

### 验证结果

- RED: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` -> FAIL，旧实现仍缺少新标签和新批量入口语义；首次运行还暴露 `@vue/compiler-sfc` 未安装。
- RED: `node tests\e2e\showroom-product-toolbar-layout.spec.js` -> FAIL，旧工具栏仍为 `一键卖点` / `batch-generate-selling-points`。
- GREEN: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` -> PASS，48 tests passed。
- GREEN: `node tests\e2e\showroom-product-toolbar-layout.spec.js` -> PASS。
- GREEN: `pnpm test -- --run` in `Website` -> PASS，8 test files / 73 tests passed。
- GREEN: `npx playwright test kiosk-detail.spec.js` in `Website` -> PASS，2 tests passed。
- REGRESSION: `pnpm ts:check` in `yudao-ui-admin-vue3` -> FAIL。默认 heap 和 8GB heap OOM；16GB heap 后失败于全仓自动导入类型缺失（`computed`、`ref`、`useMessage` 等）。后续 Vite/类型生成产物恢复后，reviewer 复跑通过。
- REVIEWER GREEN: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` -> PASS，48 tests passed。
- REVIEWER GREEN: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=16384` -> PASS。
- REVIEWER GREEN: `node tests\e2e\showroom-product-toolbar-layout.spec.js` -> PASS。
- REVIEWER GREEN: `pnpm test -- --run` in `Website` -> PASS，8 files / 73 tests passed。
- REVIEWER GREEN: `npx playwright test kiosk-detail.spec.js` in `Website` -> PASS，2 tests passed。
- REVIEWER GREEN: `http://localhost:8081` served from task worktree and `http://127.0.0.1:48081/actuator/health` -> 200；Playwright 使用测试租户导入验收 Excel 返回 `totalRows=164`、`successCount=164`、`failureCount=0`。
- REVIEWER FIX: 管理端 `vite.config.ts` 直接 import `graceful-fs`，但 `package.json` 未声明该直接 devDependency，导致独立 worktree Vite 启动失败；已补充 `graceful-fs@4.2.11` devDependency，避免依赖主工作区 node_modules 状态。

### 剩余风险

- 管理端仍保留 `coreSellingPoints` 等表单变量名，是为了避免扩大底层 key 复用范围外的无关改名；可见 UI、测试断言、API 路径已切到新语义。
- 管理端 `coreSellingPoints` 等变量名仍是底层字段兼容命名；如后续要做纯语义重命名，需要单独评估影响范围并按 TDD 执行。
