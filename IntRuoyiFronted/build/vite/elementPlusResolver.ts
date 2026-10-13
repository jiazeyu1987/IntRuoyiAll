import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import type { ComponentResolver } from 'unplugin-vue-components'

// Components and programmatic services must use one runtime module and its injection keys.
export const createElementPlusResolver = (
  options?: Parameters<typeof ElementPlusResolver>[0]
): ComponentResolver[] =>
  ElementPlusResolver(options).map((resolver) => {
    const resolve = typeof resolver === 'function' ? resolver : resolver.resolve
    const canonicalResolve = async (name: string) => {
      const result = await resolve(name)
      if (!result || typeof result === 'string' || result.from !== 'element-plus/es') {
        return result
      }
      return { ...result, from: 'element-plus' }
    }
    return typeof resolver === 'function'
      ? canonicalResolve
      : { ...resolver, resolve: canonicalResolve }
  })
