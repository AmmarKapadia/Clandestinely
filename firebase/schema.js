// CuacaUM schema constants and helper logic (Node side).
// The Kotlin app has the same logic in HazardExpiry.kt and TipCacheKey.kt.
// Keep the two in sync if a value changes.

const HOUR_MS = 60 * 60 * 1000;
const MY_OFFSET_MS = 8 * HOUR_MS; // Malaysia is UTC+8 all year (no daylight saving)

const HAZARD_CATEGORIES = ['flood', 'fallen_tree', 'blocked_drain', 'other'];
const WEATHER_STATES = ['normal', 'rain', 'thunderstorm', 'heat', 'haze_sensitive', 'haze_unhealthy', 'uv'];
const TIME_SLOTS = ['morning', 'afternoon', 'evening', 'night'];
const SENSITIVITIES = ['asthma', 'heat', 'mobility', 'uv'];

const EXPIRY = {
  defaultMs: 2 * HOUR_MS,         // a new report lasts 2 hours
  perConfirmationMs: 1 * HOUR_MS, // each confirmation adds 1 hour
  maxMs: 10 * HOUR_MS,            // never more than 10 hours after it was made
};
const TIP_CACHE_MS = 1 * HOUR_MS;

/** Next 00:00 Malaysia time after `createdAtMs`. */
function nextMidnightMs(createdAtMs) {
  const local = createdAtMs + MY_OFFSET_MS;
  const startOfLocalDay = Math.floor(local / (24 * HOUR_MS)) * 24 * HOUR_MS;
  return startOfLocalDay + 24 * HOUR_MS - MY_OFFSET_MS;
}

/** expiresAt for a brand-new report. */
function initialExpiryMs(createdAtMs) {
  return Math.min(createdAtMs + EXPIRY.defaultMs, createdAtMs + EXPIRY.maxMs, nextMidnightMs(createdAtMs));
}

/** expiresAt after one more confirmation. */
function extendedExpiryMs(createdAtMs, currentExpiresAtMs) {
  return Math.min(
    currentExpiresAtMs + EXPIRY.perConfirmationMs,
    createdAtMs + EXPIRY.maxMs,
    nextMidnightMs(createdAtMs),
  );
}

/** morning 06–12, afternoon 12–18, evening 18–22, night 22–06 (Malaysia time). */
function timeSlotAt(ms) {
  const hour = new Date(ms + MY_OFFSET_MS).getUTCHours();
  if (hour >= 6 && hour < 12) return 'morning';
  if (hour >= 12 && hour < 18) return 'afternoon';
  if (hour >= 18 && hour < 22) return 'evening';
  return 'night';
}

/** Health flags on a user doc -> sensitivity names used in tip keys. */
function sensitivitiesOf(health = {}) {
  const out = [];
  if (health.asthma) out.push('asthma');
  if (health.heatSensitive) out.push('heat');
  if (health.mobilityLimited) out.push('mobility');
  if (health.uvSensitive) out.push('uv');
  return out.sort();
}

/** Tip cache document ID, e.g. "haze_sensitive+rain_afternoon_asthma". */
function tipCacheKey(states, timeSlot, sensitivities) {
  if (!TIME_SLOTS.includes(timeSlot)) throw new Error(`Unknown time slot: ${timeSlot}`);
  for (const s of states) if (!WEATHER_STATES.includes(s)) throw new Error(`Unknown weather state: ${s}`);
  for (const s of sensitivities) if (!SENSITIVITIES.includes(s)) throw new Error(`Unknown sensitivity: ${s}`);

  let active = [...new Set(states)].filter((s) => s !== 'normal').sort();
  if (active.length === 0) active = ['normal'];
  const sens = [...new Set(sensitivities)].sort();
  return `${active.join('+')}_${timeSlot}_${sens.length ? sens.join('+') : 'none'}`;
}

// ---------- field specs, used to check sample data ----------
// type: string | number | boolean | timestamp | geopoint | array | map
// optional: field may be missing or null

const SPEC = {
  users: {
    displayName: { type: 'string' },
    email: { type: 'string', optional: true },
    isGuest: { type: 'boolean' },
    createdAt: { type: 'timestamp' },
    updatedAt: { type: 'timestamp' },
    usualLocations: { type: 'array', optional: true },
    health: {
      type: 'map',
      fields: {
        asthma: { type: 'boolean' },
        heatSensitive: { type: 'boolean' },
        uvSensitive: { type: 'boolean' },
        mobilityLimited: { type: 'boolean' },
      },
    },
    settings: {
      type: 'map',
      fields: {
        language: { type: 'string', oneOf: ['en', 'ms'] },
        textSize: { type: 'string', oneOf: ['normal', 'large'] },
      },
    },
    alertPrefs: {
      type: 'map',
      fields: {
        rain: { type: 'boolean' },
        heat: { type: 'boolean' },
        haze: { type: 'boolean' },
        rainLeadMinutes: { type: 'number' },
        quietStart: { type: 'string', optional: true },
        quietEnd: { type: 'string', optional: true },
      },
    },
    fcmTokens: { type: 'array', optional: true },
  },
  hazard_reports: {
    category: { type: 'string', oneOf: HAZARD_CATEGORIES },
    description: { type: 'string', optional: true, maxLength: 200 },
    photoPath: { type: 'string', optional: true },
    location: { type: 'geopoint' },
    nearestEdgeId: { type: 'string', optional: true },
    reportedBy: { type: 'string' },
    createdAt: { type: 'timestamp' },
    expiresAt: { type: 'timestamp' },
    confirmCount: { type: 'number' },
  },
  confirmations: {
    confirmedAt: { type: 'timestamp' },
  },
  tips_cache: {
    states: { type: 'array', itemsOneOf: WEATHER_STATES },
    timeSlot: { type: 'string', oneOf: TIME_SLOTS },
    sensitivities: { type: 'array', itemsOneOf: SENSITIVITIES },
    text: { type: 'string', maxLength: 300 },
    source: { type: 'string', oneOf: ['ai', 'rule'] },
    model: { type: 'string', optional: true },
    createdAt: { type: 'timestamp' },
    expiresAt: { type: 'timestamp' },
  },
};

function typeOf(value) {
  if (value === null || value === undefined) return 'null';
  if (Array.isArray(value)) return 'array';
  if (typeof value === 'object') {
    if (typeof value.toMillis === 'function') return 'timestamp';
    if (typeof value.latitude === 'number' && typeof value.longitude === 'number') return 'geopoint';
    return 'map';
  }
  return typeof value;
}

/** Returns a list of problems; an empty list means the document matches the spec. */
function validate(doc, spec, path = '') {
  const problems = [];
  for (const key of Object.keys(doc)) {
    if (!(key in spec)) problems.push(`${path}${key}: field is not in the schema`);
  }
  for (const [key, rule] of Object.entries(spec)) {
    const value = doc[key];
    const where = `${path}${key}`;
    if (value === undefined || value === null) {
      if (!rule.optional) problems.push(`${where}: required field is missing`);
      continue;
    }
    const actual = typeOf(value);
    if (actual !== rule.type) {
      problems.push(`${where}: expected ${rule.type}, got ${actual}`);
      continue;
    }
    if (rule.oneOf && !rule.oneOf.includes(value)) problems.push(`${where}: "${value}" is not one of ${rule.oneOf.join(', ')}`);
    if (rule.maxLength && value.length > rule.maxLength) problems.push(`${where}: longer than ${rule.maxLength} characters`);
    if (rule.itemsOneOf) {
      for (const item of value) if (!rule.itemsOneOf.includes(item)) problems.push(`${where}: "${item}" is not allowed`);
    }
    if (rule.fields) problems.push(...validate(value, rule.fields, `${where}.`));
  }
  return problems;
}

module.exports = {
  HOUR_MS,
  HAZARD_CATEGORIES,
  WEATHER_STATES,
  TIME_SLOTS,
  SENSITIVITIES,
  EXPIRY,
  TIP_CACHE_MS,
  nextMidnightMs,
  initialExpiryMs,
  extendedExpiryMs,
  timeSlotAt,
  sensitivitiesOf,
  tipCacheKey,
  SPEC,
  validate,
};
