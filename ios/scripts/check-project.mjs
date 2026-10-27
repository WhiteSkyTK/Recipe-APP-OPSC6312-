#!/usr/bin/env node
// Static checks that catch mistakes a compiler would not (or that we want before building):
//  1. Every localization key used in Swift exists in Localizable.xcstrings.
//  2. Braces, brackets and parentheses balance in every Swift file.
//  3. Every image referenced with Image("name") exists in Assets.xcassets.
//
// Usage: node scripts/check-project.mjs   (exits with status 1 on problems)

import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')), '..');
const appDir = path.join(root, 'HarmonyKitchen');
const catalog = JSON.parse(fs.readFileSync(path.join(appDir, 'Resources', 'Localizable.xcstrings'), 'utf8')).strings;
const assetsDir = path.join(appDir, 'Resources', 'Assets.xcassets');

function swiftFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) return swiftFiles(full);
    return entry.name.endsWith('.swift') ? [full] : [];
  });
}

const files = [...swiftFiles(appDir), ...(fs.existsSync(path.join(root, 'HarmonyKitchenTests')) ? swiftFiles(path.join(root, 'HarmonyKitchenTests')) : [])];
const problems = [];
const usedKeys = new Map();
const note = (key, file) => usedKeys.set(key, [...(usedKeys.get(key) ?? []), path.relative(root, file)]);

/** Removes comments and string literal contents so delimiter counting ignores them. */
function stripCode(source) {
  let out = '';
  let i = 0;
  while (i < source.length) {
    if (source.startsWith('//', i)) {
      while (i < source.length && source[i] !== '\n') i++;
    } else if (source.startsWith('/*', i)) {
      const end = source.indexOf('*/', i + 2);
      i = end === -1 ? source.length : end + 2;
    } else if (source.startsWith('"""', i)) {
      const end = source.indexOf('"""', i + 3);
      i = end === -1 ? source.length : end + 3;
      out += '""';
    } else if (source.startsWith('#"', i)) {
      const end = source.indexOf('"#', i + 2);
      i = end === -1 ? source.length : end + 2;
      out += '""';
    } else if (source[i] === '"') {
      i++;
      let depth = 0;
      while (i < source.length) {
        if (source[i] === '\\' && source[i + 1] === '(') { depth++; i += 2; continue; }
        if (depth > 0 && source[i] === ')') { depth--; i++; continue; }
        if (depth > 0 && source[i] === '(') { depth++; i++; continue; }
        if (source[i] === '\\') { i += 2; continue; }
        if (source[i] === '"' && depth === 0) { i++; break; }
        i++;
      }
      out += '""';
    } else {
      out += source[i++];
    }
  }
  return out;
}

for (const file of files) {
  const source = fs.readFileSync(file, 'utf8');

  for (const match of source.matchAll(/\b(?:L|LF|LP)\(\s*"([^"\\]+)"/g)) note(match[1], file);
  for (const match of source.matchAll(/titleKey: "([^"]+)"/g)) note(match[1], file);
  for (const match of source.matchAll(/"(onboarding_[a-z_]+)"/g)) note(match[1], file);
  // L(cond ? "a" : "b")
  for (const match of source.matchAll(/\bL\([^()"]*\?\s*"([^"]+)"\s*:\s*"([^"]+)"\s*\)/g)) {
    note(match[1], file);
    note(match[2], file);
  }
  // Multi-line ternaries such as L(self == .cuisines\n ? "a"\n : "b")
  for (const match of source.matchAll(/\bL\(self == \.cuisines\s*\?\s*"([^"]+)"\s*:\s*"([^"]+)"\s*\)/g)) {
    note(match[1], file);
    note(match[2], file);
  }
  for (const match of source.matchAll(/return "(validation_[a-z_]+)"/g)) note(match[1], file);

  for (const match of source.matchAll(/Image\("([^"\\]+)"\)/g)) {
    if (!fs.existsSync(path.join(assetsDir, `${match[1]}.imageset`))) {
      problems.push(`Missing image asset "${match[1]}" used in ${path.relative(root, file)}`);
    }
  }

  const code = stripCode(source);
  const pairs = { '{': '}', '(': ')', '[': ']' };
  const stack = [];
  let line = 1;
  for (const ch of code) {
    if (ch === '\n') line++;
    if (pairs[ch]) stack.push({ ch, line });
    else if (Object.values(pairs).includes(ch)) {
      const open = stack.pop();
      if (!open || pairs[open.ch] !== ch) {
        problems.push(`${path.relative(root, file)}:${line}: unexpected "${ch}"${open ? ` (opened "${open.ch}" on line ${open.line})` : ''}`);
        break;
      }
    }
  }
  for (const open of stack) problems.push(`${path.relative(root, file)}:${open.line}: "${open.ch}" never closed`);
}

for (const [key, where] of usedKeys) {
  if (!catalog[key]) problems.push(`Missing localization key "${key}" used in ${[...new Set(where)].join(', ')}`);
}
for (const imageName of ['Logo', 'ic_placeholder', ...Object.values(Object.fromEntries([...fs.readFileSync(path.join(appDir, 'Models', 'Catalogs.swift'), 'utf8').matchAll(/imageName: "([^"]+)"/g)].map((m) => [m[1], m[1]])))]) {
  if (!fs.existsSync(path.join(assetsDir, `${imageName}.imageset`))) problems.push(`Missing image asset "${imageName}"`);
}

console.log(`Checked ${files.length} Swift files, ${usedKeys.size} localization keys.`);
if (problems.length) {
  console.log(`\n${problems.length} problem(s):`);
  for (const problem of problems) console.log(`  - ${problem}`);
  process.exit(1);
}
console.log('No problems found.');
