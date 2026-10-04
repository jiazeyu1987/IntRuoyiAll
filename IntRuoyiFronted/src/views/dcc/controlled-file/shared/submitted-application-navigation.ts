/** Preserve the existing management entry after the API returns a new formal file identity. */
export const requireSubmittedApplicationId = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value) || BigInt(value) > 9223372036854775807n) {
    throw new Error('提交结果缺少精确正式文件身份，请回到原申请核对')
  }
  return value
}

export const buildSubmittedApplicationRoute = (
  submittedId: string,
  sourceQuery: Record<string, unknown>
) => {
  const id = requireSubmittedApplicationId(submittedId)
  const from = sourceQuery.from
  const returnTo = sourceQuery.returnTo
  const managed = sourceQuery.management === '1' || sourceQuery.mode === 'manage'
  if (!managed || !['browser', 'project-browser', 'workbench'].includes(String(from))
    || typeof from !== 'string' || typeof returnTo !== 'string'
    || !returnTo.startsWith('/dcc/controlled-file/') || /[\x00-\x1f\\]/.test(returnTo)) {
    throw new Error('原文件管理入口或返回目录缺失，请从项目目录重新查看提交结果')
  }
  // Do not carry the predecessor's task, BPM, handling or viewer context into the new candidate.
  return {
    path: `/dcc/controlled-file/detail/${id}`,
    query: { from, management: '1', returnTo }
  }
}
