import { build } from 'esbuild'

await build({
  entryPoints: ['pet3d-preview/entry.js'],
  bundle: true,
  format: 'iife',
  minify: true,
  target: ['es2020'],
  outfile: 'pet3d-preview/bundle.js',
  logLevel: 'info'
})
console.log('BUNDLE_OK')
