# 任务：公司版本页签历史版本补齐语音播放

## 任务目标

- 在 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 的 `showroom/company-version` 页签中，为历史版本“查看版本”弹窗补齐双语讲解稿与音频播放器。
- 复用现有 `/showroom/version-center/detail` 的 `COMPANY` detail 合同作为历史语音唯一来源，不新增后端接口，不改列表行布局。

## 非目标

- 不在版本历史表格行内新增播放器或语音列。
- 不修改 `ruoyi-vue-pro` 后端、数据库、版本中心独立路由或公开 preview asset 展示。
- 不引入 mock 数据、fallback 字段或静默降级。

## 前序任务检查

- 已检查上一任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-frontend-test-build-emfile-fix\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成且当前工作树干净，不阻塞本次公司版本页签历史语音补齐。

## 里程碑

- [x] M1：建立任务记录并锁定公司版本页签历史预览改用版本中心 detail 合同。
- [x] M2：先补 RED 测试，证明历史预览仍依赖旧 `getCompany(...revisionId)` 且未展示语音播放器。
- [x] M3：最小修改 `CompanyVersionWorkbench.vue`，补齐双语讲解稿与音频播放器。
- [x] M4：完成定向测试、lint，并完成本地真实页面验证。
- [x] M5：更新任务记录、整理验证证据并提交本任务改动。

## 预期验证

- `node --test scripts/showroom-admin-company-version-tab.test.mjs scripts/showroom-admin-version-center.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue scripts/showroom-admin-company-version-tab.test.mjs`
- 真实验证：`http://localhost:8081/showroom/company-version`

## 当前状态

- 状态：已完成

## Completed Work

- `CompanyVersionWorkbench.vue` 的历史版本详情弹窗已改为调用 `/showroom/version-center/detail?targetType=COMPANY`。
- 历史详情弹窗保留 `中文 / English` tab，并在每个 tab 内补齐：
  - 该 revision 的字段值
  - 内容图片
  - 讲解稿
  - 语音版本号
  - 音色
  - `<audio controls preload="none">`
  - 无音频时的显式提示
- 已补前端 RED/GREEN 定向测试与 lint 通过。

## Verification Evidence

- `node --test scripts/showroom-admin-company-version-tab.test.mjs` -> RED，按预期失败
- `node --test scripts/showroom-admin-company-version-tab.test.mjs scripts/showroom-admin-version-center.test.mjs` -> PASS
- `pnpm exec eslint src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue scripts/showroom-admin-company-version-tab.test.mjs` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-version-audio-preview run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-company-version-audio-preview\scripts\verify-showroom-company-version-audio-preview.mjs` -> PASS
- 运行时 API 复核：`revision 7 / 8` 的公司版本中心 detail 均返回 `code=0` 且带真实双语音频 URL

## Residual Data Notes

- 本地运行环境最初缺少版本中心后端合同，需要重打后端 jar 并重启 48081。
- 本地开发库最初缺少版本中心最小 schema，需要补齐 `showroom_company_revision` 的 snapshot 列和 `showroom_version_bundle` 表后，`showroom/company/current` 才能恢复。
- 本地 `revision 1/3/4/5/6` 当前仍无已发布双语语音，因此不会生成 bundle；这属于真实历史数据缺口，不在本次前端交付范围内。

## Impact

- 前端实现已完成，并已用本地真实历史语音数据（`revision 7 / 8`）完成页面验证。
- 对于本地仍无历史双语语音的更早 revision，页面会继续暴露后端真实缺口，而不会伪造语音成功。

## Assumptions

- 语音展示位置固定为现有“查看版本”详情弹窗，不放到列表行。
- 历史版本需要展示双语语音，而不是单语言语音。
- 若当前运行实例不存在带历史语音的公司 revision，则必须显式记录真实验证前置条件缺失并停止，不伪造成功。
