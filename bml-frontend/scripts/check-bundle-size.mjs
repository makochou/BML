import { readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

const assetsDir = join(process.cwd(), 'dist', 'assets')
const maxBytes = Number(process.env.BML_MAX_BUNDLE_BYTES || 800000)

const oversized = []

for (const file of readdirSync(assetsDir)) {
  if (!file.endsWith('.js')) {
    continue
  }
  const fullPath = join(assetsDir, file)
  const size = statSync(fullPath).size
  if (size > maxBytes) {
    oversized.push({ file, size })
  }
}

if (oversized.length > 0) {
  console.error(`Bundle budget check failed. Max allowed: ${maxBytes} bytes`)
  for (const item of oversized) {
    console.error(` - ${item.file}: ${item.size} bytes`)
  }
  process.exit(1)
}

console.log(`Bundle budget check passed. Max allowed: ${maxBytes} bytes`)
