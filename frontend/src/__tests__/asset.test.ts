import { describe, expect, it } from 'vitest';
import type { InventoryItem } from '../db/types';
import type { Measurer } from '../engine/fonts';
import { typeSystemById } from '../engine/fonts';
import { makeFrame } from '../engine/frame';
import { mediaById } from '../engine/media';
import { assetKind, printedAttributes } from '../kinds/asset/kind';

// Monospaced fake: every glyph is 0.5 em.
const m: Measurer = {
  width: (t) => [...t].length * 0.5,
  metrics: () => ({ cap: 0.7, xHeight: 0.5, ascent: 0.9, descent: 0.25 }),
  ink: (t) => ({ left: 0, right: [...t].length * 0.5 }),
};

const item = { id: 'i1', assetTag: 'DRV-001', name: 'Backup Samples', category: 'Drives', serial: 'WX12345', location: 'Office PAF', uri: 'https://inv.example/1' } as InventoryItem;

function texts(mediaId: string, readAlong: boolean, lengthMm = 0) {
  const env = {
    m,
    frame: makeFrame({ media: mediaById(mediaId), readAlong, lengthMm }),
    ts: typeSystemById('plex'),
    locale: 'en' as const,
    org: { name: 'Acme', address: {} } as never,
    now: new Date(),
    appUrl: 'https://labeleur.test',
  };
  const { ops } = assetKind.layout(item, assetKind.defaultOptions, env);
  return ops.flatMap((op) => ('text' in op ? [op.text] : [])).join(' ');
}

describe('asset tag layout', () => {
  it('prints name, details and ownership beside the code on a 62 × 45 mm cut', () => {
    const t = texts('DK-22205', false, 45);
    expect(t).toContain('001');
    expect(t).toContain('S/N WX12345');
    expect(t).toMatch(/Backup/);
    expect(t).toMatch(/property of acme/i);
  });

  it('keeps small square labels to code and tag', () => {
    const t = texts('DK-11221', false);
    expect(t).toContain('001');
    expect(t).not.toMatch(/Backup|S\/N/);
  });
});

describe('asset attributes', () => {
  const jacket = { ...item, attributes: [{ key: 'Size', value: 'L' }, { key: 'Colour', value: 'Navy' }, { key: 'Fabric', value: '' }] } as InventoryItem;
  const layoutText = (attributes: string[]) => {
    const env = {
      m,
      frame: makeFrame({ media: mediaById('DK-22205'), readAlong: false, lengthMm: 45 }),
      ts: typeSystemById('plex'),
      locale: 'en' as const,
      org: { name: 'Acme', address: {} } as never,
      now: new Date(),
      appUrl: 'https://labeleur.test',
    };
    return assetKind.layout(jacket, { ...assetKind.defaultOptions, attributes }, env).ops.flatMap((op) => ('text' in op ? [op.text] : [])).join(' | ');
  };

  it('prints only the chosen keys, matching them regardless of case', () => {
    expect(printedAttributes(jacket, { ...assetKind.defaultOptions, attributes: ['colour'] })).toEqual([{ key: 'Colour', value: 'Navy' }]);
    expect(layoutText(['size', 'Colour'])).toContain('Size: L  ·  Colour: Navy');
    expect(layoutText([])).not.toContain('Size');
  });

  it('skips attributes without a value', () => {
    expect(printedAttributes(jacket, { ...assetKind.defaultOptions, attributes: ['Fabric'] })).toEqual([]);
  });
});

describe('asset name', () => {
  it('keeps the name when attributes, details and a long ownership line compete for room', () => {
    const env = {
      m,
      frame: makeFrame({ media: mediaById('DK-22205'), readAlong: false, lengthMm: 45 }),
      ts: typeSystemById('plex'),
      locale: 'de' as const,
      org: { name: 'Nordlicht Manufaktur GmbH', address: {}, logo: { w: 200, h: 60, ink: new Float32Array(200 * 60).fill(1) } } as never,
      now: new Date(),
      appUrl: 'https://labeleur.test',
    };
    const rec = { ...item, attributes: [{ key: 'Size', value: 'L' }] } as InventoryItem;
    const { ops, issues } = assetKind.layout(rec, { ...assetKind.defaultOptions, attributes: ['Size'] }, env);
    expect(ops.flatMap((op) => ('text' in op ? [op.text] : [])).join(' ')).toMatch(/Backup/);
    expect(issues.map((i) => i.id)).not.toContain('asset-name');
  });
});
