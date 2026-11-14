export interface ReleaseInfo {
  releaseTag: string
  changeSet?: { gitChanges?: string[] }
}

/** A successful HTML response is not release metadata (for example an SPA index page). */
export const readReleaseInfo = async (response: Response): Promise<ReleaseInfo> => {
  if (!response.ok) throw new Error(`release-info.json HTTP ${response.status}`)
  const contentType = response.headers.get('content-type') || ''
  if (!/^application\/json(?:\s*;|$)/i.test(contentType)) {
    throw new Error(`release-info.json 返回类型为 ${contentType || '未提供'}，应为 application/json，请检查版本文件及静态资源路由。`)
  }
  let value: unknown
  try {
    value = await response.json()
  } catch {
    throw new Error('release-info.json 内容不是有效 JSON，请检查发布包中的版本文件。')
  }
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      !('releaseTag' in value) || typeof value.releaseTag !== 'string' || !value.releaseTag.trim()) {
    throw new Error('release-info.json 缺少有效 releaseTag，无法确认运行版本。')
  }
  if ('changeSet' in value && value.changeSet !== undefined) {
    const changes = value.changeSet
    if (!changes || typeof changes !== 'object' || Array.isArray(changes) ||
        ('gitChanges' in changes && changes.gitChanges !== undefined &&
          (!Array.isArray(changes.gitChanges) || changes.gitChanges.some(item => typeof item !== 'string')))) {
      throw new Error('release-info.json 的版本变化数据格式无效。')
    }
  }
  return value as ReleaseInfo
}
