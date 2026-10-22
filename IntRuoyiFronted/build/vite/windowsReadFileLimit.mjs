import fsPromises from 'node:fs/promises'

const installationKey = Symbol.for('IntRuoyi.windowsReadFileLimit')

function validateLimit(limit) {
  if (!Number.isSafeInteger(limit) || limit < 1) {
    throw new RangeError('Windows readFile concurrency must be a positive safe integer')
  }
}

export function createReadFileLimiter(readFile, limit) {
  validateLimit(limit)
  if (typeof readFile !== 'function') {
    throw new TypeError('A readFile function is required')
  }
  let active = 0
  const queue = []

  const drain = () => {
    while (active < limit && queue.length > 0) {
      const job = queue.shift()
      active += 1
      const execute = async () => {
        try {
          job.resolve(await Reflect.apply(readFile, job.receiver, job.args))
        } catch (error) {
          job.reject(error)
        } finally {
          active -= 1
          drain()
        }
      }
      void execute()
    }
  }

  return function limitedReadFile(...args) {
    return new Promise((resolve, reject) => {
      queue.push({ receiver: this, args, resolve, reject })
      drain()
    })
  }
}

// Vite imports the default fs/promises object, also exposed as fs.promises.
// Limit complete reads, including open/read/close, rather than libuv work items.
export function installWindowsReadFileLimit(limit = 8) {
  validateLimit(limit)
  const installed = fsPromises[installationKey]
  if (installed) {
    if (installed.limit !== limit || fsPromises.readFile !== installed.readFile) {
      throw new Error('Windows readFile limiter installation has changed')
    }
    return installed
  }
  const readFile = createReadFileLimiter(fsPromises.readFile, limit)
  const installation = Object.freeze({ limit, readFile })
  fsPromises.readFile = readFile
  Object.defineProperty(fsPromises, installationKey, { value: installation })
  return installation
}
