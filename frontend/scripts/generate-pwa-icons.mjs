import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import sharp from 'sharp'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const svg = path.join(root, 'public', 'favicon.svg')
const outDir = path.join(root, 'public', 'pwa')

await mkdir(outDir, { recursive: true })

async function writePng(size, filename) {
  await sharp(svg)
    .resize(size, size)
    .png()
    .toFile(path.join(outDir, filename))
}

await writePng(192, 'icon-192.png')
await writePng(512, 'icon-512.png')
await writePng(180, 'apple-touch-icon.png')

const maskableInner = 410
const maskablePad = Math.round((512 - maskableInner) / 2)
await sharp(svg)
  .resize(maskableInner, maskableInner)
  .extend({
    top: maskablePad,
    bottom: maskablePad,
    left: maskablePad,
    right: maskablePad,
    background: '#ff4f87'
  })
  .png()
  .toFile(path.join(outDir, 'icon-512-maskable.png'))

console.log('PWA icons written to public/pwa/')
