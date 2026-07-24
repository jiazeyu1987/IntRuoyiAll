export interface ShowroomDiscussionComment {
  commentId: number
  productId: number
  targetRevisionId: number
  changeRequestId: number | null
  parentCommentId: number | null
  anchorType: 'FIELD' | 'MODULE' | 'CHANGE_REQUEST'
  anchorKey: string
  content: string
  status: string
  createdBy: number
  resolvedBy: number | null
}

export interface ShowroomDiscussionThread {
  root: ShowroomDiscussionComment
  replies: ShowroomDiscussionComment[]
}

const expectRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`讨论工作台缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const expectNumber = (value: unknown, fieldName: string) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`讨论工作台缺少数值字段：${fieldName}`)
  }
  return value
}

const expectString = (value: unknown, fieldName: string, allowEmpty = false) => {
  if (typeof value !== 'string' || (!allowEmpty && value.trim().length === 0)) {
    throw new Error(`讨论工作台缺少字符串字段：${fieldName}`)
  }
  return value
}

const optionalNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return null
  }
  return expectNumber(value, 'optionalNumber')
}

export const normalizeDiscussionComment = (value: unknown): ShowroomDiscussionComment => {
  const record = expectRecord(value, 'comment')
  return {
    commentId: expectNumber(record.commentId, 'commentId'),
    productId: expectNumber(record.productId, 'productId'),
    targetRevisionId: expectNumber(record.targetRevisionId, 'targetRevisionId'),
    changeRequestId: optionalNumber(record.changeRequestId),
    parentCommentId: optionalNumber(record.parentCommentId),
    anchorType: expectString(record.anchorType, 'anchorType') as ShowroomDiscussionComment['anchorType'],
    anchorKey: expectString(record.anchorKey, 'anchorKey', true),
    content: expectString(record.content, 'content'),
    status: expectString(record.status, 'status'),
    createdBy: expectNumber(record.createdBy, 'createdBy'),
    resolvedBy: optionalNumber(record.resolvedBy)
  }
}

export const normalizeDiscussionPage = (value: unknown): ShowroomDiscussionComment[] => {
  if (!Array.isArray(value)) {
    throw new Error('讨论工作台缺少数组字段：commentPage')
  }
  return value.map((item) => normalizeDiscussionComment(item))
}

export const buildDiscussionThreads = (
  comments: ShowroomDiscussionComment[]
): ShowroomDiscussionThread[] => {
  const byId = new Map(comments.map((comment) => [comment.commentId, comment]))
  const roots = comments.filter((comment) => comment.parentCommentId === null)
  const repliesByRootId = new Map<number, ShowroomDiscussionComment[]>()

  const resolveRootId = (comment: ShowroomDiscussionComment): number | null => {
    let cursor: ShowroomDiscussionComment | undefined = comment
    while (cursor?.parentCommentId !== null) {
      cursor = byId.get(cursor.parentCommentId)
      if (!cursor) {
        return null
      }
    }
    return cursor?.commentId ?? null
  }

  for (const comment of comments) {
    if (comment.parentCommentId === null) {
      continue
    }
    const rootId = resolveRootId(comment)
    if (!rootId) {
      continue
    }
    const list = repliesByRootId.get(rootId) || []
    list.push(comment)
    repliesByRootId.set(rootId, list)
  }

  return roots.map((root) => ({
    root,
    replies: (repliesByRootId.get(root.commentId) || []).sort((left, right) => left.commentId - right.commentId)
  }))
}

const statusTextMap: Record<string, string> = {
  OPEN: '处理中',
  RESOLVED: '已解决'
}

export const resolveDiscussionStatusText = (status: string) => statusTextMap[status] || status

export const resolveDiscussionStatusTagType = (status: string) => {
  return status === 'RESOLVED' ? 'success' : 'warning'
}

export const resolveAnchorTypeText = (anchorType: string) => {
  if (anchorType === 'FIELD') {
    return '字段'
  }
  if (anchorType === 'MODULE') {
    return '模块'
  }
  if (anchorType === 'CHANGE_REQUEST') {
    return '审批单'
  }
  return anchorType
}

export const buildProductOptions = (products: unknown[]) => {
  if (!Array.isArray(products)) {
    throw new Error('讨论工作台缺少真实产品数组：products')
  }
  return products.map((item) => {
    const record = expectRecord(item, 'products')
    const revision =
      record.revision && typeof record.revision === 'object'
        ? (record.revision as Record<string, unknown>)
        : {}
    return {
      value: expectNumber(record.productId, 'productId'),
      label: `${expectString(record.productCode, 'productCode')} · ${
        revision.nameCn ? String(revision.nameCn) : '未命名'
      }`
    }
  })
}
