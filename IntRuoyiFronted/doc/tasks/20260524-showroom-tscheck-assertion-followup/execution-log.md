# Execution Log

BDD: Showroom payload assertions stay runtime-equivalent while satisfying TypeScript -> Given the showroom admin and frontstage helpers read unknown API payloads, When frontend `ts:check` runs, Then the code should use explicit two-step narrowing without changing the existing runtime extraction logic.

RED: historical `pnpm ts:check` -> FAIL, direct assertions in `src/views/showroom-admin/narration/NarrationWorkspace.vue` and `src/views/showroom-frontstage/shared/payload.ts` were rejected during the prior runtime-control verification pass.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
