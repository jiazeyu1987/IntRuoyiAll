# Feature

Flow8 dossier requirement page is a read-only presentation of the four permanent required materials.

# Acceptance

The page shows incoming inspection report, sterilization report, finished product inspection report, and finished product inspection record as fixed required items. It renders no editable switches and does not call the legacy update API.

# BDD: fixed-required-materials-page -> Given/When/Then

Given a user opens the eDHR dossier requirement page, when the page renders, then the four materials are shown as fixed required items and no control can disable them.

# RED: previous component contract -> FAIL, editable switches and the legacy update API were present

# GREEN: static component inspection -> PASS, switch/update code and the misleading disable copy were removed

# Verification

- `pnpm ts:check` -> PASS.
- `pnpm build:prod` -> NOT PASS due to unrelated baseline `TrainingRulesReadonlyTab.vue` having no `<template>` or `<script>` block.
- `git diff --check` -> PASS for the Flow8 changes.

# Blockers

- Existing frontend baseline build error in `src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue` is outside Flow8 ownership.
- Real Playwright requires a writable tenant, role accounts, an existing batch execution, four cleanable files, and cleanup authority.
