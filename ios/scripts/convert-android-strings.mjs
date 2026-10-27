#!/usr/bin/env node
// Converts the Android app's res/values*/strings.xml files into an Xcode String Catalog
// (Localizable.xcstrings) plus a small Swift file describing the string arrays.
//
// Usage: node scripts/convert-android-strings.mjs <android res dir> <ios project dir>
//
// Android -> iOS conversions:
//   %1$s -> %1$@, %s -> %@, %1$d -> %1$lld, %d -> %lld
//   <string-array name="x"> items become keys "x.0", "x.1", ...
//   <plurals> become plural variations.
// No npm dependencies; the XML in these files is simple enough to parse with regexes.

import fs from 'node:fs';
import path from 'node:path';

const [, , resDir, iosDir] = process.argv;
if (!resDir || !iosDir) {
  console.error('Usage: node convert-android-strings.mjs <android res dir> <ios project dir>');
  process.exit(1);
}

// Android resource qualifier -> iOS language code.
const LOCALES = { values: 'en', 'values-nso': 'nso', 'values-ts': 'ts', 'values-ve': 've' };

function unescapeAndroid(raw) {
  let s = raw.trim();
  const cdata = s.match(/^<!\[CDATA\[([\s\S]*)\]\]>$/);
  if (cdata) return cdata[1];
  // A fully double-quoted value preserves whitespace and is unquoted by aapt;
  // otherwise aapt collapses whitespace runs (including raw line breaks) to one space.
  if (s.length >= 2 && s.startsWith('"') && s.endsWith('"')) s = s.slice(1, -1);
  else s = s.replace(/\s+/g, ' ');
  s = s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)))
    .replace(/&#x([0-9a-fA-F]+);/g, (_, n) => String.fromCodePoint(parseInt(n, 16)))
    .replace(/&amp;/g, '&');
  s = s.replace(/\\(.)/g, (_, c) => ({ n: '\n', t: '\t', "'": "'", '"': '"', '\\': '\\', '@': '@', '?': '?' }[c] ?? c));
  return s;
}

function toIOSFormat(s) {
  return s
    .replace(/%(\d+\$)?s/g, (_, pos) => `%${pos ?? ''}@`)
    .replace(/%(\d+\$)?d/g, (_, pos) => `%${pos ?? ''}lld`);
}

function parseFile(file) {
  const xml = fs.readFileSync(file, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
  const strings = {};
  const plurals = {};
  const arrays = {};
  for (const m of xml.matchAll(/<string\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) {
    strings[m[1]] = unescapeAndroid(m[2]);
  }
  for (const m of xml.matchAll(/<plurals\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/plurals>/g)) {
    const items = {};
    for (const it of m[2].matchAll(/<item\s+quantity="([^"]+)"\s*>([\s\S]*?)<\/item>/g)) {
      items[it[1]] = unescapeAndroid(it[2]);
    }
    plurals[m[1]] = items;
  }
  for (const m of xml.matchAll(/<string-array\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string-array>/g)) {
    arrays[m[1]] = [...m[2].matchAll(/<item\s*>([\s\S]*?)<\/item>/g)].map((it) => unescapeAndroid(it[1]));
  }
  return { strings, plurals, arrays };
}

const parsed = {};
for (const [folder, lang] of Object.entries(LOCALES)) {
  const file = path.join(resDir, folder, 'strings.xml');
  if (fs.existsSync(file)) parsed[lang] = parseFile(file);
}
const en = parsed.en;
if (!en) throw new Error('values/strings.xml not found');

// Resolves "@string/foo" array items against the same locale, falling back to English.
function resolveRef(value, lang) {
  const ref = value.match(/^@string\/(\w+)$/);
  if (!ref) return value;
  return parsed[lang]?.strings[ref[1]] ?? en.strings[ref[1]] ?? value;
}

const catalog = { sourceLanguage: 'en', strings: {}, version: '1.0' };
const unit = (value) => ({ stringUnit: { state: 'translated', value } });

for (const key of Object.keys(en.strings).sort()) {
  const localizations = {};
  for (const lang of Object.keys(parsed)) {
    const v = parsed[lang].strings[key];
    if (v !== undefined) localizations[lang] = unit(toIOSFormat(v));
  }
  catalog.strings[key] = { extractionState: 'manual', localizations };
}

for (const key of Object.keys(en.plurals).sort()) {
  const localizations = {};
  for (const lang of Object.keys(parsed)) {
    const items = parsed[lang].plurals[key];
    if (!items) continue;
    const plural = {};
    for (const [quantity, value] of Object.entries(items)) plural[quantity] = unit(toIOSFormat(value));
    if (!plural.other) plural.other = plural.one ?? unit('');
    localizations[lang] = { variations: { plural } };
  }
  catalog.strings[key] = { extractionState: 'manual', localizations };
}

const arrayCounts = {};
for (const name of Object.keys(en.arrays).sort()) {
  arrayCounts[name] = en.arrays[name].length;
  en.arrays[name].forEach((_, i) => {
    const key = `${name}.${i}`;
    const localizations = {};
    for (const lang of Object.keys(parsed)) {
      const v = parsed[lang].arrays[name]?.[i];
      if (v !== undefined) localizations[lang] = unit(toIOSFormat(resolveRef(v, lang)));
    }
    catalog.strings[key] = { extractionState: 'manual', localizations };
  });
}

// iOS-only strings (scripts/ios-strings.json): { "key": { "en": "...", "ts": "..." } }
const extrasFile = path.join(iosDir, 'scripts', 'ios-strings.json');
if (fs.existsSync(extrasFile)) {
  const extras = JSON.parse(fs.readFileSync(extrasFile, 'utf8'));
  for (const [key, values] of Object.entries(extras)) {
    if (key.startsWith('_')) continue;
    const localizations = {};
    for (const [lang, value] of Object.entries(values)) localizations[lang] = unit(value);
    catalog.strings[key] = { extractionState: 'manual', localizations };
  }
}

const outCatalog = path.join(iosDir, 'HarmonyKitchen', 'Resources', 'Localizable.xcstrings');
fs.mkdirSync(path.dirname(outCatalog), { recursive: true });
fs.writeFileSync(outCatalog, JSON.stringify(catalog, null, 2) + '\n');

const swift = `// Generated by scripts/convert-android-strings.mjs. Do not edit by hand.

enum StringArrays {
    /// Number of items in each Android <string-array>; item i is the localized key "<name>.<i>".
    static let counts: [String: Int] = [
${Object.entries(arrayCounts).map(([k, v]) => `        "${k}": ${v},`).join('\n')}
    ]
}
`;
const outSwift = path.join(iosDir, 'HarmonyKitchen', 'Utilities', 'StringArrays.generated.swift');
fs.mkdirSync(path.dirname(outSwift), { recursive: true });
fs.writeFileSync(outSwift, swift);

const total = Object.keys(catalog.strings).length;
const perLang = Object.fromEntries(
  Object.keys(parsed).map((l) => [l, Object.values(catalog.strings).filter((s) => s.localizations[l]).length]),
);
console.log(`Wrote ${total} keys to ${outCatalog}`);
console.log('Coverage per language:', perLang);
console.log('Arrays:', arrayCounts);
