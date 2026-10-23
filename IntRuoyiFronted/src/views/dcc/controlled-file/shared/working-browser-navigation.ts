const exactId = (value: unknown, label: string): string => {
  if ((typeof value !== 'string' && typeof value !== 'number')
    || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9]\d*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error(`${label}身份无效，请重新读取正式文件`)
  return String(value)
}

export const buildWorkingBrowserRoute = (file: {
  id: string | number; masterId?: string | number | null; directoryId?: string | number | null; fileNumber?: string | null
  dccProjectCodeId?: string | number | null; projectFolderId?: string | number | null
  hasProjectStorageMapping?: boolean
}) => {
  const id = exactId(file.id, '文件'), master = exactId(file.masterId, '逻辑文件')
  if (typeof file.hasProjectStorageMapping !== 'boolean') throw new Error('文件位置类型尚未正式读取，请重新读取文件')
  const projectFolderLocation = file.hasProjectStorageMapping
  let directoryQuery: Record<string, string> = {}
  if (projectFolderLocation) {
    exactId(file.dccProjectCodeId, '项目')
    exactId(file.projectFolderId, '项目文件夹')
    // Project-folder placement is internal storage, not a node of the physical directory tree.
    // The operation page still selects the exact authorized file and Master below.
  } else {
    if (file.projectFolderId != null) throw new Error('物理目录与项目文件夹位置投影矛盾，请重新读取文件')
    const directory = exactId(file.directoryId, '存储目录')
    if (!Number.isSafeInteger(Number(directory))) throw new Error('存储目录身份超出页面安全范围')
    directoryQuery = { directoryId: directory }
  }
  if (typeof file.fileNumber !== 'string' || !file.fileNumber.trim()) throw new Error('文件缺少正式编号，无法定位检出/检入入口')
  return { name: 'DccControlledFileBrowser', query: {
    browserMode: 'storage', ...directoryQuery, scope: projectFolderLocation ? 'global' : 'current',
    keyword: file.fileNumber.trim(), workingFileId: id, workingMasterId: master
  } }
}

export const resolveWorkingBrowserSelection = (
  row: { masterId?: string | number }, options: Array<{ id: string | number }>,
  query: { workingFileId?: unknown; workingMasterId?: unknown }
): string | undefined => {
  if (query.workingFileId === undefined && query.workingMasterId === undefined) return undefined
  const target = exactId(query.workingFileId, '所选文件'), master = exactId(query.workingMasterId, '所选逻辑文件')
  const matches = options.filter(option => exactId(option.id, '版本') === target)
  if (!matches.length) return undefined
  if (matches.length !== 1 || exactId(row.masterId, '列表逻辑文件') !== master)
    throw new Error('所选文件与实际列表逻辑文件不一致，不能定位其它版本')
  return target
}

export const workingBrowserIdentityQuery = (query: { workingFileId?: unknown; workingMasterId?: unknown }): Record<string, string> => {
  if (query.workingFileId === undefined && query.workingMasterId === undefined) return {}
  return { browserMode: 'storage', workingFileId: exactId(query.workingFileId, '所选文件'),
    workingMasterId: exactId(query.workingMasterId, '所选逻辑文件') }
}
