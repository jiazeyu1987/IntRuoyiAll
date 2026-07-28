# Verification Report

## Summary

- 融合结果：`origin/int_main` 已融合到本地 `int_main`。
- 融合提交：`c8e07b0d4faabc411c45dd0f71f1fe88dd80c479`、`149b58fb7bde33480641f42f805cbd8e85149d2c`、`774f371b514bc22fc470365fcd19edb7667f1faf`。
- 当前差异：已推送；本地 `int_main` 与 `origin/int_main` 一致。

## Commands

- `git fetch origin int_main` -> PASS
- `git merge origin/int_main --no-edit` -> 初次冲突，冲突文件 `docs/experience-index.md`、`docs/frontend-development.md`
- `rg -n "<<<<<<<|=======|>>>>>>>" docs\experience-index.md docs\frontend-development.md` -> PASS
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS
- `git commit --no-edit` -> PASS，commit `c8e07b0d4faabc411c45dd0f71f1fe88dd80c479`
- `pnpm ts:check` -> PASS
- `mvn -pl yudao-server -am -DskipTests compile` -> PASS
- `rg -n "D-Main 本地主线滞后远端|upstream whitespace|branch-runtime-port-guard after merge" docs\worktree-memory.md docs\experience-index.md` -> PASS
- `task_closeout.py --task-id merge-int-main-code-20260728 --mode preview` -> PASS
- `task_closeout.py --task-id merge-int-main-code-20260728 --mode apply` -> PASS
- 推送前 `git fetch origin int_main` -> PASS，远端从 `04dd022c` 前进到 `410d71aa`
- 第二次 `git merge origin/int_main --no-edit` -> PASS，commit `149b58fb7bde33480641f42f805cbd8e85149d2c`
- 第二次融合后 `pnpm ts:check` -> PASS
- 第二次融合后 `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py` -> PASS，15 passed
- 第二次融合后 `git rev-list --left-right --count HEAD...origin/int_main` -> `8 0`
- 更新二次融合证据后复跑 `task_closeout.py --mode preview/apply` -> PASS
- 最终推送前再次 `git fetch origin int_main` -> PASS，远端从 `410d71aa` 前进到 `35a1255c`
- 第三次 `git merge origin/int_main --no-edit` -> PASS，commit `774f371b514bc22fc470365fcd19edb7667f1faf`
- 第三次融合后 `pnpm ts:check` -> PASS
- 第三次融合后 `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js` -> PASS
- 第三次融合后 `git rev-list --left-right --count HEAD...origin/int_main` -> `10 0`
- `git push origin int_main` -> PASS，`35a1255c..4a57f0c3`
- 推送后 `git fetch origin int_main` -> PASS
- 推送后 `git status --short --branch` -> `int_main...origin/int_main`
- 推送后 `git rev-list --left-right --count HEAD...origin/int_main` -> `0 0`

## Notes

`git diff --cached --check` 在融合提交前报告远端历史带入的空行和尾随空格；未在冲突解决文件或当前任务文档中新增 whitespace 问题。

项目经验已合并进现有 `docs/worktree-memory.md`，并在 `docs/experience-index.md` 增加路由；未新建长期经验文档。
