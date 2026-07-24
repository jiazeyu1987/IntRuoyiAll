# Task: Showroom Company Narration Revision Align

## Goal

修复本地 `IntRuoyi` 运行时匿名 `GET /showroom/display/app-config` 因 `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch` 返回 500 的问题，使当前公司 live revision 与公司讲解 live narration 的 `source_revision_id` 对齐，恢复 Website 侧公司详情读取。

## Scope

- 本地 `ruoyi-vue-pro` MySQL 运行库中的 showroom live company / narration / preview 数据
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-narration-revision-align\**`

## Non-Scope

- 不修改 `Website` 前端逻辑。
- 不扩展新的 showroom Java 合同。
- 不为缺失 live 数据添加 fallback 或跳过校验。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\task.md`
- Status before this task: `Completed`
- Impact: `app-config.company.publicFields` 合同已完成，本次可继续处理当前 live 数据与 narration revision 的真实不一致问题。

## Milestones

1. 记录当前 app-config 真实失败信息，并盘点 company current revision / company live narration / company preview 现状。
2. 先记录 RED 证据，锁定当前 500 根因为公司讲解 `source_revision_id` 与当前 company revision 不一致。
3. 以最小本地数据修复方式对齐 company live narration 到当前 company revision，并保留 fail-fast 语义不变。
4. 重新验证匿名 `GET /showroom/display/app-config` 恢复 200，并回写任务记录。

## Expected Verification

- `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config`
- 必要时查询：
  - `showroom_company`
  - `showroom_company_revision`
  - `showroom_narration_version`
  - `showroom_preview_asset_version`

## Current Status

- Status: Completed
- Completed work:
  - 已通过本地 MySQL 数据回填，把 company preview / company narration / product preview / product narration / hall mapping 对齐到可用状态。
  - 已重新验证匿名 `GET /showroom/display/app-config` 恢复返回 `code=0`，不会再被 `source revision mismatch` 阻塞。
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `Get-Content -Raw -Encoding utf8 ...\\restore-local-app-config-minimal.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro`
- PASS: `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config` -> HTTP 200 with `code=0`
