// One-off dev tool: regenerates the static preset avatar SVGs shipped in
// src/assets/avatars/. Not run as part of the build — see ADR 0046.
// Usage: npm run generate:avatars

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { Avatar, Style } from '@dicebear/core';
import definition from '@dicebear/styles/critters.json' with { type: 'json' };

const SEEDS = [
  'nova',
  'ember',
  'pixel',
  'quill',
  'sage',
  'orbit',
  'comet',
  'willow',
  'juniper',
  'echo',
  'flint',
  'marble',
  'clover',
  'breeze',
  'cosmo',
  'hazel',
];

const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = join(__dirname, '..', 'public', 'avatars');

mkdirSync(outDir, { recursive: true });

const style = new Style(definition);

for (const seed of SEEDS) {
  const avatar = new Avatar(style, { seed });
  writeFileSync(join(outDir, `${seed}.svg`), avatar.toString());
}

console.log(`Generated ${SEEDS.length} avatars into ${outDir}`);
