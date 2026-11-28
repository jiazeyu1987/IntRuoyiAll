export interface AddressAreaNode {
  id: number
  name: string
  children?: AddressAreaNode[]
}

export const getAddressAreaName = (areas: AddressAreaNode[], areaId: number): string => {
  const findPath = (nodes: AddressAreaNode[]): string[] | undefined => {
    for (const node of nodes) {
      if (node.id === areaId) return [node.name]
      const childPath = node.children ? findPath(node.children) : undefined
      if (childPath) return [node.name, ...childPath]
    }
    return undefined
  }
  const path = findPath(areas)
  if (!path) throw new Error('收货地址的行政区信息不存在，请检查地区数据')
  return path.join(' / ')
}
