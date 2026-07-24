# Execution Log: DCC v1 backend approval-to-publish chain

BDD: 审批人完成 DCC 审批后受控文件进入发布态 -> Given 已存在一条 `APPROVING` 的 DCC 受控文件，且其 BPM 实例已经生成待办任务 / When 审批人在真实流程中完成当前审批任务 / Then 后端必须把该记录推进到预期的后审批状态，并在需要盖章时生成可读取的受控输出，而不是停留在 `APPROVING` 或静默失败

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260513-dcc-v1-backend-workflow-id-generation-fix/task.md` is completed and committed in `2956613387`.
- M2: Completed. This task document, execution log, and backend evidence file were created before production code changes.
- M3:
  - RED: real isolated-runtime approval of task `cbdacb80-4eca-11f1-a751-00155db32d8f` -> FAIL, `/admin-api/bpm/task/approve` returned `当前通知公告不存在`, and the DCC record stayed `APPROVING` with no stamp row update.
  - RED evidence showed the failure happened inside DCC finalization notification lookup for template codes `dcc_controlled_file_approved` and `dcc_controlled_file_stamp_failed`.
- M4:
  - GREEN: `sql/mysql/20260513_dcc_notify_template_seed.sql` was repaired to match the actual `system_notify_template` schema and to contain valid DCC notify-template rows.
- M5:
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
  - GREEN: after applying the repaired notify-template seed to the isolated runtime, re-approving the same task returned `{"code":0,"msg":"","data":true}`.
  - GREEN: database verification showed:
    - `dcc_controlled_file.status = STAMPED`
    - `dcc_controlled_file.stamped_file_id = 2224`
    - `dcc_controlled_file.approved_time` and `stamped_time` set
    - `dcc_controlled_file_stamp.status = 1`
    - stamped file `dcc-sample_controlled.pdf` persisted under `dcc/stamped/...`
    - `act_ru_task` count for the process instance dropped to `0`
- M6:
  - Pending Git commit in the owning backend repository after evidence finalization.
