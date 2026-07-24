BDD: paperless batch processing frontend plan defines the electronic batch-record workspace -> Given the current MES and BPM frontend pages plus the PP pressure pump production record sample, When the frontend plan is written, Then reusable current pages, new electronic batch-record pages, route entry points, and phased UI delivery are all assigned explicitly.

GREEN: previous frontend task state -> PASS, `20260512-hide-sidebar-menu-tabs` is already explicitly blocked for repository-wide verification issues outside this planning task.

GREEN: frontend branch readiness -> PASS, the plan is being written on `feature/mes-paperless-batch-plan`.

GREEN: frontend workstream plan written -> PASS.

BDD: frontend phase 1 slice matches backend phase 1 -> Given phase 1 must stop at template import, preview, list, basic edit, and delete, When the frontend workstream plan is revised, Then no execution, signoff, or export capability remains in the phase 1 frontend slice.

RED: frontend scope review before hardening -> FAIL, the earlier frontend document still described execution editor, signoff panel, export panel, and a broader first slice than the backend phase 1 scope.

GREEN: frontend phase 1 scope rewrite -> PASS, the frontend plan now limits phase 1 to template import, preview, list, basic edit, and delete, and removes execution-related components from the first slice.
