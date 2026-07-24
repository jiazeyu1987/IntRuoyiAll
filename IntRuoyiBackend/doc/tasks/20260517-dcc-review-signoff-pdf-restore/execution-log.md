# Execution Log: DCC 审核会签 PDF 审批矩阵恢复

BDD: `产品技术要求` 的 live 审批矩阵必须与审核会签 PDF 一致 -> Given
`D:\ocr2\resource\审核会签.pdf` 可读取且其 `产品技术要求` 行可确认 / When
恢复 `category_id=1` 的 live active route / Then `审核会签` 必须为
`QA + QMS + 注册`，`批准` 必须为 `编制部门负责人或其授权代表`。

RED: active-route query for `category_id=1` -> FAIL, current active route is
`route_id=65 / version_no=39`, and stage-2 candidate ids are `1`, which does not
match the PDF-required `2,4,5`.

GREEN: visual PDF evidence confirms `产品技术要求` uses signoff positions
`QA`、`QMS`、`注册`, and approval responsibility `编制部门负责人或其授权代表`.

GREEN: post-fix active-route query for `category_id=1` -> PASS, the active route
now resolves to `31 -> 2,4,5 -> 900333,900334 -> 31`.
