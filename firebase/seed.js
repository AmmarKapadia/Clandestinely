#!/usr/bin/env node
// Creates the CuacaUM collections in Firestore with sample documents.
//
//   npm run seed:dry      print the sample documents and check them against the schema (no Firebase needed)
//   npm run seed          write the sample documents to Firestore
//   npm run seed:clean    delete the sample documents again
//
// Writing needs a service account key (see docs/firestore_setup.md):
//   GOOGLE_APPLICATION_CREDENTIALS=./service-account.json npm run seed
// To write to the local emulator instead, set FIRESTORE_EMULATOR_HOST=127.0.0.1:8080.
//
// All sample document IDs start with "sample_" so they are easy to spot and remove.

const { Timestamp, GeoPoint } = require('firebase-admin/firestore');
const schema = require('./schema');

function ts(ms) {
  return Timestamp.fromMillis(ms);
}

function buildSampleData(nowMs = Date.now()) {
  const reportCreated = nowMs - 5 * 60 * 1000; // made 5 minutes ago
  const floodInitial = schema.initialExpiryMs(reportCreated);
  const floodExpires = schema.extendedExpiryMs(reportCreated, floodInitial); // 1 confirmation

  const users = {
    sample_user: {
      displayName: 'Sample Student',
      email: 'sample.student@siswa.um.edu.my',
      isGuest: false,
      createdAt: ts(nowMs),
      updatedAt: ts(nowMs),
      usualLocations: ['KK8', 'FSKTM'],
      health: { asthma: true, heatSensitive: false, uvSensitive: false, mobilityLimited: false },
      settings: { language: 'en', textSize: 'normal' },
      alertPrefs: {
        rain: true, heat: true, haze: true,
        rainLeadMinutes: 60,
        quietStart: '23:00', quietEnd: '07:00',
      },
      fcmTokens: [],
    },
    sample_guest: {
      displayName: 'Guest',
      email: null,
      isGuest: true,
      createdAt: ts(nowMs),
      updatedAt: ts(nowMs),
      usualLocations: [],
      health: { asthma: false, heatSensitive: false, uvSensitive: false, mobilityLimited: false },
      settings: { language: 'ms', textSize: 'large' },
      alertPrefs: { rain: true, heat: true, haze: true, rainLeadMinutes: 60 },
      fcmTokens: [],
    },
  };

  const hazardReports = {
    sample_flood: {
      category: 'flood',
      description: 'Water ankle-deep near the bus stop',
      photoPath: null,
      location: new GeoPoint(3.1209, 101.6538),
      nearestEdgeId: 'KK8-BUSSTOP3',
      reportedBy: 'sample_user',
      createdAt: ts(reportCreated),
      expiresAt: ts(floodExpires),
      confirmCount: 1,
    },
    sample_fallen_tree: {
      category: 'fallen_tree',
      description: 'Branch blocking half the walkway',
      photoPath: null,
      location: new GeoPoint(3.1225, 101.6560),
      nearestEdgeId: null,
      reportedBy: 'sample_guest',
      createdAt: ts(reportCreated),
      expiresAt: ts(schema.initialExpiryMs(reportCreated)),
      confirmCount: 0,
    },
  };

  // sample_guest confirmed sample_user's flood report
  const confirmations = {
    sample_flood: {
      sample_guest: { confirmedAt: ts(reportCreated + 60 * 1000) },
    },
  };

  const tip = (states, timeSlot, sensitivities, text) => ({
    id: schema.tipCacheKey(states, timeSlot, sensitivities),
    data: {
      states: [...states].sort(),
      timeSlot,
      sensitivities: [...sensitivities].sort(),
      text,
      source: 'rule',
      model: null,
      createdAt: ts(nowMs),
      expiresAt: ts(nowMs + schema.TIP_CACHE_MS),
    },
  });

  const tipsList = [
    tip(['rain'], 'afternoon', [],
      'Rain expected this afternoon. Bring an umbrella and use the covered walkways.'),
    tip(['haze_sensitive'], 'afternoon', ['asthma'],
      'The air is unhealthy for people with asthma. Wear a mask outdoors and keep your inhaler with you.'),
    tip(['heat'], 'afternoon', [],
      'Very hot this afternoon. Refill your water bottle and take the shaded route.'),
  ];
  const tipsCache = Object.fromEntries(tipsList.map((t) => [t.id, t.data]));

  return { users, hazardReports, confirmations, tipsCache };
}

function checkAgainstSchema(data) {
  const problems = [];
  const check = (collection, id, doc, spec) => {
    for (const p of schema.validate(doc, spec)) problems.push(`${collection}/${id} -> ${p}`);
  };
  for (const [id, doc] of Object.entries(data.users)) check('users', id, doc, schema.SPEC.users);
  for (const [id, doc] of Object.entries(data.hazardReports)) check('hazard_reports', id, doc, schema.SPEC.hazard_reports);
  for (const [reportId, docs] of Object.entries(data.confirmations)) {
    for (const [id, doc] of Object.entries(docs)) {
      check(`hazard_reports/${reportId}/confirmations`, id, doc, schema.SPEC.confirmations);
    }
  }
  for (const [id, doc] of Object.entries(data.tipsCache)) check('tips_cache', id, doc, schema.SPEC.tips_cache);
  return problems;
}

function printable(value) {
  return JSON.stringify(value, (key, v) => {
    if (v && typeof v === 'object' && typeof v.toDate === 'function') return v.toDate().toISOString();
    if (v instanceof GeoPoint) return { latitude: v.latitude, longitude: v.longitude };
    return v;
  }, 2);
}

/** Database ID from firebase.json ("firestore.database"), or "(default)". */
function firestoreDatabaseId() {
  try {
    const config = JSON.parse(require('fs').readFileSync(require('path').join(__dirname, 'firebase.json'), 'utf8'));
    return (config.firestore && config.firestore.database) || '(default)';
  } catch (e) {
    return '(default)';
  }
}

async function main(argv = process.argv.slice(2)) {
  const DRY_RUN = argv.includes('--dry-run');
  const CLEAN = argv.includes('--clean');
  const data = buildSampleData();

  const problems = checkAgainstSchema(data);
  if (problems.length) {
    console.error('Sample data does not match the schema:');
    for (const p of problems) console.error(`  - ${p}`);
    process.exit(1);
  }

  if (DRY_RUN) {
    console.log(printable(data));
    console.log('\nAll sample documents match the schema. Nothing was written (dry run).');
    return;
  }

  const { initializeApp, applicationDefault } = require('firebase-admin/app');
  const { getFirestore } = require('firebase-admin/firestore');
  const projectId = process.env.GCLOUD_PROJECT || process.env.FIREBASE_PROJECT_ID;
  initializeApp(process.env.FIRESTORE_EMULATOR_HOST
    ? { projectId: projectId || 'demo-cuacaum' }
    : { credential: applicationDefault(), ...(projectId ? { projectId } : {}) });
  const databaseId = firestoreDatabaseId();
  const db = databaseId === '(default)' ? getFirestore() : getFirestore(databaseId);
  console.log(`Using Firestore database "${databaseId}"`);

  const batch = db.batch();
  const refs = [];
  for (const [id, doc] of Object.entries(data.users)) refs.push([db.collection('users').doc(id), doc]);
  for (const [id, doc] of Object.entries(data.hazardReports)) refs.push([db.collection('hazard_reports').doc(id), doc]);
  for (const [reportId, docs] of Object.entries(data.confirmations)) {
    for (const [id, doc] of Object.entries(docs)) {
      refs.push([db.collection('hazard_reports').doc(reportId).collection('confirmations').doc(id), doc]);
    }
  }
  for (const [id, doc] of Object.entries(data.tipsCache)) refs.push([db.collection('tips_cache').doc(id), doc]);

  // Delete confirmations before their parent reports when cleaning.
  const ordered = CLEAN ? [...refs].reverse() : refs;
  for (const [ref, doc] of ordered) {
    if (CLEAN) batch.delete(ref);
    else batch.set(ref, doc);
  }
  await batch.commit();

  console.log(`${CLEAN ? 'Deleted' : 'Wrote'} ${refs.length} sample documents:`);
  for (const [ref] of refs) console.log(`  ${ref.path}`);
}

if (require.main === module) {
  main().catch((err) => {
    console.error(err.message || err);
    if (/credential|Could not load the default credentials/i.test(String(err.message))) {
      console.error('\nSet GOOGLE_APPLICATION_CREDENTIALS to your service account key file. See docs/firestore_setup.md.');
    }
    process.exit(1);
  });
}

module.exports = { buildSampleData, checkAgainstSchema, main, firestoreDatabaseId };
