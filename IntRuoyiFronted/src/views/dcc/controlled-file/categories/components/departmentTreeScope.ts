import type { DeptVO } from '@/api/system/dept'
import { handleTree } from '@/utils/tree'

type NormalizeLabel = (value?: string) => string
const VIRTUAL_TOP_ROOT_NAMES = new Set(['顶级部门'])

const cloneDepartment = (department: DeptVO) => ({
  ...department,
  children: undefined
})

const buildDepartmentTree = (departments: DeptVO[]) => {
  return handleTree(departments.map(cloneDepartment))
}

export const createDepartmentByIdMap = (departments: DeptVO[]) => {
  return new Map(departments.map((department) => [department.id, department]))
}

export const resolveDepartmentCompanyRootId = (
  departmentById: Map<number, DeptVO>,
  deptId?: number
) => {
  if (!deptId) {
    return undefined
  }
  const ancestors: DeptVO[] = []
  let currentId: number | undefined = deptId
  const visited = new Set<number>()
  while (currentId && currentId > 0 && !visited.has(currentId)) {
    visited.add(currentId)
    const department = departmentById.get(currentId)
    if (!department) {
      break
    }
    ancestors.push(department)
    const parentId = department.parentId
    if (!parentId || parentId <= 0 || !departmentById.has(parentId)) {
      break
    }
    currentId = parentId
  }
  const topAncestor = ancestors.at(-1)
  if (!topAncestor) {
    return undefined
  }
  if (VIRTUAL_TOP_ROOT_NAMES.has((topAncestor.name || '').trim()) && ancestors.length >= 2) {
    return ancestors.at(-2)?.id
  }
  return topAncestor.id
}

export const getCompanyChildDepartmentTree = (
  departments: DeptVO[],
  departmentById: Map<number, DeptVO>,
  companyRootId?: number
) => {
  if (!companyRootId) {
    return []
  }
  return buildDepartmentTree(
    departments.filter(
      (department) =>
        department.id !== companyRootId &&
        resolveDepartmentCompanyRootId(departmentById, department.id) === companyRootId
    )
  )
}

export const findExactMatchedDepartmentIdByLabel = (
  departments: DeptVO[],
  normalizeSubjectLabel: NormalizeLabel,
  subjectLabel?: string
) => {
  const label = normalizeSubjectLabel(subjectLabel)
  if (!label) {
    return undefined
  }
  const matched = departments.filter(
    (department) => normalizeSubjectLabel(department.name) === label
  )
  return matched.length === 1 ? matched[0].id : undefined
}
