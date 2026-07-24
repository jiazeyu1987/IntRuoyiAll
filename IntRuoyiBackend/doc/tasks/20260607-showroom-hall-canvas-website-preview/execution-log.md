# 执行日志：展柜画布候选产品补充封面图字段

- CHECK: 上一后端任务状态 -> PASS，`doc/tasks/20260607-dcc-preview-detail-panel/task.md` 已记录为 `blocked`，当前线程可切换到新问题。
- BDD: 候选产品返回封面图 -> Given 展柜画布请求候选产品 / When 后端返回 `HallProductOptionRespVO` / Then 响应包含可直接访问的 `previewImageUrl`。
- BDD: 无封面图返回空字符串 -> Given 产品当前展示版本没有 `cover_image` / When 后端返回候选产品 / Then `previewImageUrl` 返回空字符串，由前端显式占位。
- BDD: 现有布局接口不变 -> Given 用户保存展柜画布布局 / When 请求 `updateHallCanvasLayout` / Then 请求响应契约不增加任何封面相关字段。
- RED: 变更前 `HallProductOptionRespVO` -> FAIL，expected reason：候选产品响应缺少 `previewImageUrl` 字段。
- GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#hallProductOptionsShouldReturnCandidateRowsWithHallIdsInSingleCall" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- CHECK: 整个 `ShowroomHttpApiIntegrationTest` 类回归 -> NOT USED，本地仓库存在与本任务无关的既有失败，不能作为本任务放行依据。
