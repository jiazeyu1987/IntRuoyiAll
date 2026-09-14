# Verification Report

## Summary

Added six versioned 智能文控 Codex Runner test-item seeds covering upload/approval/publish, revision version chain, obsolete approval, controlled browser/download/watermark logs, distribution/training, and project-code recognition/assignment.

## Results

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> 4 passed.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_codex_test_management_migration.py IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> 6 passed.
- GREEN: filtered migration policy gate excluding unrelated `20260725_mes_edhr_recordbook_global_setting.sql` -> passed, `migrationCount=374`.
- BLOCKED: full migration policy gate remains blocked by pre-existing unrelated `config-seed` metadata type in `20260725_mes_edhr_recordbook_global_setting.sql`.

## Seeded Test Items

- 智能文控受控文件上传审批发布闭环
- 智能文控受控文件修订版本链闭环
- 智能文控作废审批与受控浏览收敛
- 智能文控受控浏览下载水印与访问日志
- 智能文控分发培训闭环
- 智能文控项目代码识别分配闭环
