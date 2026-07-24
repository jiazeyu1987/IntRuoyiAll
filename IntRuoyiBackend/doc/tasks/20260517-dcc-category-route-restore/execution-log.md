# Execution Log: DCC 类别审批路线恢复

BDD: 当前 active 类别路线可回切到改动前稳定版本 -> Given `产品技术要求` 的 live 路线历史里已经存在多版由 E2E 写入的 route version / When 用户要求恢复到改动前版本 / Then 系统应只切换 active 路线到目标历史版本，而不是切库或继续生成新版本。

BDD: 恢复动作不切换数据库 -> Given 运行时仍指向 `ruoyi-vue-pro` 本地 MySQL / When 执行路线恢复 / Then 只修改当前库中的 active 路线标记，不切换数据源。

- M1: Completed. Read the current live route history for `category_id = 1` before any restore write.
- M2: Completed. Chose route `id=28 / version_no=2` as the earlier stable four-stage baseline to restore, because it predates the later multi-account and four-real-approver variants while still matching the previously stable fixed-stage structure.
- M3: Completed. Switched `category_id=1` so every route version became inactive except `route_id=28`.
- GREEN: runtime database check -> PASS, the active route row is now `id=28, version_no=2, active=1`.
- GREEN: runtime node check -> PASS, route `28` now exposes:
  - `DOC_CONTROL_REVIEW -> 31`
  - `MATRIX_REVIEW -> 1,2,4,5,31`
  - `MATRIX_APPROVAL -> 900333,900334`
  - `DOC_CONTROL_APPROVAL -> 31`
- GREEN: live API preview -> PASS, `POST /admin-api/dcc/controlled-files/route-preview` for `categoryId=1` returned the restored route shape from the same `ruoyi-vue-pro` MySQL runtime rather than any later E2E route variant.
