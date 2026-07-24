# Execution Log

## Bug Summary

- 进入公司页面时仍出现 `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch`。

## Expected Behavior

- 公司页面加载不应再被 live 公司讲解与当前公司 revision 的历史错位阻塞。

## Reproduction

- 真实运行时探针：`Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config`
- 失败响应：`{"success":false,"message":"SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch","code":500,...}`

## BDD Scenarios

- BDD: company page should not fail after company revision changes -> Given 当前公司已有 live revision 且 live 公司讲解可用于同一公司 When 用户进入公司页面 Then 页面依赖的后端接口不应因旧 narration revision 绑定而失败

## Root Cause

- 当前本地运行库里主公司 `showroom_company.id = 1` 的 `current_revision_id = 8`，但 latest live 公司中英文讲解与 company preview 仍停留在 `source_revision_id = 7`。
- 管理页读取的 `GET /admin-api/showroom/company/current` 本身不校验 narration revision；真正报错的是展厅前台聚合接口 `GET /showroom/display/website-config`。
- `ShowroomReleaseAssembler` 在组装 `website-config` 时会严格要求 live company narration 的 `source_revision_id` 与当前 company revision 一致，因此历史坏数据被 fail-fast 暴露。

## TDD Evidence

- BDD: company page should not fail after company revision changes -> Given 当前公司已有 live revision 且 live 公司讲解可用于同一公司 When 用户进入公司页面 Then 页面依赖的后端接口不应因旧 narration revision 绑定而失败
- RED: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config` -> FAIL, 返回 `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch`
- GREEN: `Get-Content -Raw -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-page-revision-mismatch-followup\align-local-live-revisions.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro` -> PASS
- GREEN: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config` -> PASS，返回 `code=0`

## Verification

- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT c.id, c.current_revision_id, zh.source_revision_id, en.source_revision_id ..."` -> PASS，公司 latest live ZH/EN narration 已对齐到 `source_revision_id = 8`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT c.id, c.current_revision_id, pv.source_revision_id ..."` -> PASS，company preview 已对齐到 `source_revision_id = 8`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT COUNT(DISTINCT p.id) ... ORDER BY v2.version_no DESC, v2.id DESC ..."` -> PASS，当前 hall-mapped product latest published preview mismatch 数量为 `0`

## Blockers

- 无。
