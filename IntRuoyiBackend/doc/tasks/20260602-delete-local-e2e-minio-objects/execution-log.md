# 执行日志：20260602-delete-local-e2e-minio-objects

BDD: 本机 E2E 测试桶对象可整体清理 -> Given 本机 MinIO 存在 `yudao-dcc-e2e/**` 测试对象 / When 执行本次清理 / Then `yudao-dcc-e2e` bucket 保留但对象数量为 0，且 `yudao` bucket 不被删除或修改。

BDD: 本机 E2E 测试桶可删除 -> Given 用户明确要求删除 `yudao-dcc-e2e` bucket / When bucket 中不存在任何当前对象、历史版本和 delete markers / Then S3 `delete_bucket` 应成功，且默认业务 bucket `yudao` 仍存在。

RED: `python -X utf8 - <boto3 list yudao-dcc-e2e>` -> FAIL, 本机 MinIO bucket `yudao-dcc-e2e` 当前存在 1174 个对象，总大小 1529549927 bytes，样例包括 `showroom/**` 以外的 `dcc/original/20260602/**` 测试对象。

GREEN: `python -X utf8 - <boto3 delete_objects Bucket=yudao-dcc-e2e>` -> PASS, 已删除 `yudao-dcc-e2e/**` 当前对象并保留 bucket；后续 `list_objects_v2` 返回当前对象数量 0。

BLOCKER: `python -X utf8 - <boto3 delete historical versions Bucket=yudao-dcc-e2e>` -> FAIL, MinIO 对 `codex-object-lock-test.txt` 版本 `0226b192-1fe9-4d41-a2aa-54b05eb66cc6` 返回 `InvalidRequest: Object is WORM protected and cannot be overwritten`。

INFO: `get_object_retention` + `get_object_legal_hold` -> `codex-object-lock-test.txt` 版本 `0226b192-1fe9-4d41-a2aa-54b05eb66cc6` 为 `COMPLIANCE` 模式，保留至 `2026-06-08T20:03:36Z`，legal hold 为 `ON`。

REGRESSION: `python -X utf8 - <boto3 list current objects and versions>` -> PASS, `yudao-dcc-e2e` 当前对象数量为 0；默认 bucket `yudao` 仍存在。INFO: 历史版本数量为 10629，delete markers 数量为 9956，永久清理仍阻塞。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260602-delete-local-e2e-minio-objects --mode preview` -> PASS, 返回 `status: ready`，keep 仅包含 `task.md` 与 `execution-log.md`，delete/blocked/warnings 均为 none。

BLOCKER: commit -> SKIP, 任务仍被 Object Lock 历史版本永久清理阻塞，按提交策略不提交。

BLOCKER: `python -X utf8 - <boto3 delete_bucket Bucket=yudao-dcc-e2e>` -> FAIL, S3 返回 `BucketNotEmpty: The bucket you tried to delete is not empty. You must delete all versions in the bucket.`；`head_bucket` 同时确认默认业务 bucket `yudao` 仍存在。
