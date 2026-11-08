// Checks the expiry rules, tip keys and sample data. Run with: npm test
const test = require('node:test');
const assert = require('node:assert/strict');
const schema = require('../schema');
const { buildSampleData, checkAgainstSchema } = require('../seed');

const H = schema.HOUR_MS;
// Helper: a time on 29 Oct 2026 in Malaysia time (UTC+8)
const my = (hh, mm = 0) => Date.UTC(2026, 9, 29, hh - 8, mm);

test('next midnight is 00:00 Malaysia time', () => {
  assert.equal(schema.nextMidnightMs(my(15)), Date.UTC(2026, 9, 29, 16, 0)); // 30 Oct 00:00 MYT = 29 Oct 16:00 UTC
  assert.equal(schema.nextMidnightMs(my(0, 1)), Date.UTC(2026, 9, 29, 16, 0));
  assert.equal(schema.nextMidnightMs(my(23, 59)), Date.UTC(2026, 9, 29, 16, 0));
});

test('new report lasts 2 hours', () => {
  assert.equal(schema.initialExpiryMs(my(10)), my(12));
});

test('new report made at 11pm ends at midnight', () => {
  assert.equal(schema.initialExpiryMs(my(23)), schema.nextMidnightMs(my(23)));
});

test('each confirmation adds 1 hour', () => {
  const created = my(8);
  let exp = schema.initialExpiryMs(created);
  exp = schema.extendedExpiryMs(created, exp);
  assert.equal(exp, my(11));
  exp = schema.extendedExpiryMs(created, exp);
  assert.equal(exp, my(12));
});

test('report never lasts more than 10 hours', () => {
  const created = my(6);
  let exp = schema.initialExpiryMs(created);
  for (let i = 0; i < 20; i++) exp = schema.extendedExpiryMs(created, exp);
  assert.equal(exp, my(16));
});

test('report made at 9pm never passes midnight, even with confirmations', () => {
  const created = my(21);
  let exp = schema.initialExpiryMs(created);
  for (let i = 0; i < 5; i++) exp = schema.extendedExpiryMs(created, exp);
  assert.equal(exp, schema.nextMidnightMs(created));
});

test('time slots follow 06 / 12 / 18 / 22', () => {
  assert.equal(schema.timeSlotAt(my(5, 59)), 'night');
  assert.equal(schema.timeSlotAt(my(6)), 'morning');
  assert.equal(schema.timeSlotAt(my(11, 59)), 'morning');
  assert.equal(schema.timeSlotAt(my(12)), 'afternoon');
  assert.equal(schema.timeSlotAt(my(18)), 'evening');
  assert.equal(schema.timeSlotAt(my(22)), 'night');
});

test('tip keys match the examples in the schema doc', () => {
  assert.equal(schema.tipCacheKey(['rain'], 'afternoon', []), 'rain_afternoon_none');
  assert.equal(schema.tipCacheKey(['rain', 'haze_sensitive'], 'afternoon', ['asthma']), 'haze_sensitive+rain_afternoon_asthma');
  assert.equal(schema.tipCacheKey(['heat'], 'morning', ['uv', 'mobility']), 'heat_morning_mobility+uv');
});

test('tip key drops "normal" when another state is active, and uses it when nothing is', () => {
  assert.equal(schema.tipCacheKey(['normal', 'rain'], 'evening', []), 'rain_evening_none');
  assert.equal(schema.tipCacheKey([], 'evening', []), 'normal_evening_none');
});

test('tip key rejects unknown values', () => {
  assert.throws(() => schema.tipCacheKey(['snow'], 'morning', []));
  assert.throws(() => schema.tipCacheKey(['rain'], 'noon', []));
  assert.throws(() => schema.tipCacheKey(['rain'], 'morning', ['diabetes']));
});

test('health flags map to sensitivity names', () => {
  assert.deepEqual(schema.sensitivitiesOf({ asthma: true, uvSensitive: true }), ['asthma', 'uv']);
  assert.deepEqual(schema.sensitivitiesOf({}), []);
});

test('all sample documents match the schema', () => {
  assert.deepEqual(checkAgainstSchema(buildSampleData()), []);
});

test('schema check catches a bad document', () => {
  const data = buildSampleData();
  data.hazardReports.sample_flood.category = 'tsunami';
  delete data.users.sample_user.isGuest;
  data.tipsCache.rain_afternoon_none.extra = 1;
  const problems = checkAgainstSchema(data);
  assert.ok(problems.some((p) => p.includes('category')));
  assert.ok(problems.some((p) => p.includes('isGuest')));
  assert.ok(problems.some((p) => p.includes('extra')));
});
