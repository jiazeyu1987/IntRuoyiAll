# P6 前端偏差详情模板编译回归

## Bug Summary

偏差管理页动态加载 `DeviationDetail.vue` 时，Vite 返回 500，页面不能进入偏差列表；根因是处理记录卡片的 `template #header` 缺少结束标签。

## Expected Behavior

登录后打开偏差管理路由应完成 SFC 编译，列表、创建弹窗和批记录偏差页签可正常渲染。

## Reproduction

- 真实页面：登录 `http://127.0.0.1:8094` 后进入 `/mes/pro/feedback/edhr-deviation`。
- RED: Playwright run `p6-20260925` 记录 `DeviationDetail.vue` HTTP 500、`Element is missing end tag`，页面出现动态模块加载失败。
- RED: `node IntRuoyiFronted/tests/e2e/edhr-deviation-detail-template-static.spec.cjs` -> FAIL，模板标签计数 `8 !== 7`。

## Root Cause and Fix

在处理记录卡片头部补齐 `</template>`，并保留统一列表模板的排序属性单一来源，移除重复 `sortable=\"custom\"`。

## GREEN Evidence

- GREEN: `node IntRuoyiFronted/tests/e2e/edhr-deviation-detail-template-static.spec.cjs` -> PASS。
- `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs` -> PASS。
- Playwright run `p6-20260925-r4` -> 偏差列表、创建弹窗和批记录详情偏差空态可真实打开，目标页面请求 HTTP 200、无 pageerror；写入路径因任务专属批记录夹具缺失 BLOCKED。
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS。
- 定向 ESLint -> PASS。

## Risk and Scope

修复只触及偏差详情模板结构和列表列排序属性；不改变偏差状态、签名、权限或接口载荷。

## Verification

真实页面、SFC 编译、前端类型、定向 ESLint 和偏差后端回归均已复核；写入型 E2E 的任务专属批记录前置仍未满足。

## Blockers

原先的任务专属批记录前置已解决：通过产品名搜索找到启用路线工单，真实页面创建了批记录 `P6-DEV-20260926-CAP4-577008` / ID `900000001150`，并完成写入型 E2E。

## Handling JSON Save Regression

- BDD: 空 JSON 处理字段 -> Given 处理记录表单未填写调查成员/CAPA附件, When 通过真实页面保存处理记录, Then 空 JSON 字段以 `null` 写入，保存成功，不产生 MySQL JSON 数据截断错误。
- RED: Playwright `P6-DEV-20260926-CAP4-577008-handling-save` -> HTTP 200 business code 500；后端日志显示 `Invalid JSON text` for `investigation_members_json` because the UI sent an empty string。
- RED: `node tests/e2e/mes-edhr-deviation-p3-static.spec.cjs` -> FAIL before the fix because no `normalizeOptionalJson` payload contract existed。
- Fix: `saveHandling` normalizes blank `investigationMembersJson` and `capaAttachmentsJson` to `null`; a valid JSON value such as `[]` remains unchanged。
- GREEN: same static contract -> PASS; `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS; targeted ESLint -> PASS。
- GREEN: real Playwright `P6-DEV-20260926-CAP4-577008-handling-save-r3` -> HTTP 200 business code 0, handling ID `1`, content version `1`, valid `[]` JSON and verification content saved。
- GREEN: real Playwright `P6-DEV-20260926-CAP4-577008-sign-close` -> five signature nodes returned HTTP 200 and normal close returned HTTP 200; final deviation state is `CLOSED/NORMAL_COMPLETED`。
