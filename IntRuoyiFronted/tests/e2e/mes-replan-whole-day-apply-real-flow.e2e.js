// Historical entrypoint retained for existing task docs and operators.
// Manual schedule replan is now a direct business action, not a Form Center/BPM approval.
process.env.MES_REPLAN_E2E_BASE_URL =
  process.env.MES_REPLAN_E2E_BASE_URL || process.env.MES_REPLAN_WHOLE_DAY_E2E_BASE_URL
process.env.MES_REPLAN_E2E_TENANT =
  process.env.MES_REPLAN_E2E_TENANT || process.env.MES_REPLAN_WHOLE_DAY_E2E_TENANT
process.env.MES_REPLAN_E2E_USERNAME =
  process.env.MES_REPLAN_E2E_USERNAME || process.env.MES_REPLAN_WHOLE_DAY_E2E_USERNAME
process.env.MES_REPLAN_E2E_PASSWORD =
  process.env.MES_REPLAN_E2E_PASSWORD || process.env.MES_REPLAN_WHOLE_DAY_E2E_PASSWORD
process.env.MES_REPLAN_E2E_HEADED =
  process.env.MES_REPLAN_E2E_HEADED || process.env.MES_REPLAN_WHOLE_DAY_E2E_HEADED

require('./mes-schedule-order-replan-881mo090863-real-flow.e2e.js')
