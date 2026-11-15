import { describe, expect, it } from 'vitest';
import type { InventoryItem } from '../db/types';
import type { Measurer } from '../engine/fonts';
import { typeSystemById } from '../engine/fonts';
import { makeFrame } from '../engine/frame';
import { mediaById } from '../engine/media';
import { assetKind } from '../kinds/asset/kind';

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
