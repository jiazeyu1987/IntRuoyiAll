const exactId = (value: unknown, label: string): string => {
  if ((typeof value !== 'string' && typeof value !== 'number')
    || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9]\d*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error(`${label}身份无效，请重新读取正式文件`)
  return String(value)
}

export const buildWorkingBrowserRoute = (file: {
  id: string | number; masterId?: string | number | null; directoryId?: string | number | null; fileNumber?: string | null
}) => {
  const id = exactId(file.id, '文件'), master = exactId(file.masterId, '逻辑文件')
  const directory = exactId(file.directoryId, '存储目录')
  // The existing storage tree uses numeric IDs; reject an unrepresentable directory explicitly.
  if (!Number.isSafeInteger(Number(directory))) throw new Error('存储目录身份超出页面安全范围')
  if (typeof file.fileNumber !== 'string' || !file.fileNumber.trim()) throw new Error('文件缺少正式编号，无法定位检出/检入入口')
  return { name: 'DccControlledFileBrowser', query: {
    browserMode: 'storage', directoryId: directory, scope: 'current',
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
