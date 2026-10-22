import type { ProjectFolder } from '@/api/dcc/controlledFile/projectAttributes'

export type ProjectFolderTreeNode = ProjectFolder & { children: ProjectFolderTreeNode[] }
export interface ProjectFolderTreeState {
  projectId?: string
  tree: ProjectFolderTreeNode[]
  error: string
  loading: boolean
  requestSequence: number
}
export const createProjectFolderState = (): ProjectFolderTreeState =>
  ({ tree: [], error: '', loading: false, requestSequence: 0 })

export const buildProjectFolderTree = (projectId: string, rows: ProjectFolder[]): ProjectFolderTreeNode[] => {
  if (!Array.isArray(rows)) throw new Error('项目目录响应缺失')
  const byId = new Map<string, ProjectFolderTreeNode>()
  for (const row of rows) {
    const id = String(row.id)
    if (String(row.projectCodeId) !== projectId || !/^[1-9][0-9]*$/.test(id)
      || !row.name?.trim() || byId.has(id) || !Number.isInteger(row.sortOrder)
      || typeof row.active !== 'boolean') throw new Error('项目目录身份、结构或排序不合法')
    byId.set(id, { ...row, children: [] })
  }
  const roots: ProjectFolderTreeNode[] = []
  for (const node of byId.values()) {
    const visited = new Set<string>()
    let current: ProjectFolderTreeNode | undefined = node
    while (current) {
      const id = String(current.id), parent = String(current.parentId)
      if (visited.has(id)) throw new Error('项目目录存在循环')
      visited.add(id)
      if (parent === '0') break
      current = byId.get(parent)
      if (!current) throw new Error('项目目录上级缺失')
    }
    const parentId = String(node.parentId)
    if (parentId === '0') roots.push(node)
    else byId.get(parentId)!.children.push(node)
  }
  const sort = (nodes: ProjectFolderTreeNode[]) => {
    nodes.sort((left, right) => left.sortOrder - right.sortOrder || String(left.id).localeCompare(String(right.id)))
    nodes.forEach(node => sort(node.children))
  }
  sort(roots)
  return roots
}

export const loadProjectFolderTree = async (
  state: ProjectFolderTreeState, projectId: string,
  load: (id: string) => Promise<ProjectFolder[]>
): Promise<boolean> => {
  const sequence = ++state.requestSequence
  state.projectId = projectId; state.loading = true; state.error = ''; state.tree = []
  try {
    const tree = buildProjectFolderTree(projectId, await load(projectId))
    if (sequence !== state.requestSequence) return false
    state.tree = tree
    return true
  } catch (error) {
    if (sequence !== state.requestSequence) return false
    state.error = error instanceof Error ? error.message : String(error)
    return false
  } finally { if (sequence === state.requestSequence) state.loading = false }
}
