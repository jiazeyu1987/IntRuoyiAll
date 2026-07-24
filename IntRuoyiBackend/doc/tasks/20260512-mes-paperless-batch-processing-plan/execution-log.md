BDD: paperless batch processing plan separates reuse from new batch-record work -> Given the current `IntRuoyi` MES/BPM capabilities and the PP pressure pump production record sample, When the plan is written, Then reusable forms, new paperless batch-record work, rollout phases, and verification gates are all assigned explicitly.

RED: current code inspection and sample analysis -> FAIL, `IntRuoyi` has editable BPM dynamic forms and MES business forms, but no dedicated electronic batch-record execution workspace or layout-preserving batch-record editor.

GREEN: static capability review and sample parsing summary -> PASS, the pressure pump production record sample is a multi-table batch record suitable for a new MES electronic batch-record subdomain, while simpler structured forms can reuse existing BPM/MES entry points.

GREEN: plan draft written -> PASS

GREEN: `git diff --check` on `ruoyi-vue-pro` -> PASS for the new task documents; unrelated branch work remains outside this task scope and was not staged.

GREEN: backend plan relocation -> PASS, the canonical plan document now lives at `pichuli/mes-paperless-batch-processing/backend-development-plan.md` while task-policy evidence remains under `doc/tasks/20260512-mes-paperless-batch-processing-plan/`.

GREEN: backend plan location restored -> PASS, the backend plan document now lives again at `doc/tasks/20260512-mes-paperless-batch-processing-plan/development-plan.md`.

BDD: phase 1 template-import slice is implementation-ready -> Given the user requires the first slice to stop at template import, preview, list, basic edit, and delete, When the backend plan, frontend plan, and review note are revised, Then `.doc` support, scope boundaries, backend contracts, and frontend slice boundaries are all explicit and aligned.

RED: document review before hardening -> FAIL, the current documents still mixed phase 1 template work with later execution/signoff/export concepts and did not pin the phase 1 backend import model tightly enough.

GREEN: backend phase 1 hardening -> PASS, the backend plan now fixes phase 1 scope, `.doc` mandatory support, three-table import/template model, API surface, class inventory, and parser-chain responsibilities.

GREEN: frontend phase 1 alignment -> PASS, the frontend first slice now only covers template import, preview, list, basic edit, and delete, with execution/signoff/export explicitly removed from phase 1.

GREEN: plan review decision -> PASS, `plan-review.md` now states that phase 1 can start coding and lists the remaining explicit risks.
