const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');
const path = require('path');
const fs = require('fs');

let serviceAccount;
const keyPath = path.join(__dirname, 'serviceAccountKey.json');

if (fs.existsSync(keyPath)) {
  serviceAccount = require('./serviceAccountKey.json');
} else if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
  const jsonStr = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, 'base64').toString('utf8');
  serviceAccount = JSON.parse(jsonStr);
} else if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
  serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
} else {
  let privateKey = process.env.FIREBASE_PRIVATE_KEY || '';
  if (privateKey.startsWith('"') && privateKey.endsWith('"')) {
    privateKey = privateKey.slice(1, -1);
  }
  privateKey = privateKey.replace(/\\n/g, '\n');

  serviceAccount = {
    type: 'service_account',
    project_id: process.env.FIREBASE_PROJECT_ID,
    private_key: privateKey,
    client_email: process.env.FIREBASE_CLIENT_EMAIL,
  };
}

let app;
if (!getApps().length) {
  app = initializeApp({
    credential: cert(serviceAccount),
  });
} else {
  app = getApps()[0];
}

const db = getFirestore(app);
const auth = getAuth(app);

module.exports = { app, db, auth };
