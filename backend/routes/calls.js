const express = require('express');
const router = express.Router();
const twilio = require('twilio');
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

const client = twilio(
  process.env.TWILIO_ACCOUNT_SID,
  process.env.TWILIO_AUTH_TOKEN
);

// POST /api/calls/make - Initiate outbound phone call to any number
router.post('/make', verifyToken, async (req, res) => {
  try {
    const { to, from } = req.body;

    if (!to || !from) {
      return res.status(400).json({ error: 'Both "to" and "from" numbers are required.' });
    }

    // 1. Check user profile and credits
    const userRef = db.collection('users').doc(req.user.uid);
    const userDoc = await userRef.get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: 'User profile not found.' });
    }

    const userData = userDoc.data();
    if ((userData.credits || 0) < 1) {
      return res.status(400).json({ error: 'Insufficient credits. You need at least 1 credit to make a call.' });
    }

    // 2. Verify user owns the 'from' virtual number
    const numSnapshot = await db.collection('virtualNumbers')
      .where('phoneNumber', '==', from)
      .where('userId', '==', req.user.uid)
      .where('active', '==', true)
      .get();

    if (numSnapshot.empty) {
      return res.status(403).json({ error: 'You do not own this virtual number.' });
    }

    // 3. Initiate call via Twilio REST API
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:5000';
    const call = await client.calls.create({
      to: to,
      from: from,
      url: `${backendUrl}/webhook/voice/outbound-twiml`
    });

    // 4. Deduct 1 credit from user profile
    await userRef.update({
      credits: (userData.credits || 1) - 1
    });

    // 5. Save call record in Firestore
    const callData = {
      callSid: call.sid,
      from: from,
      to: to,
      userId: req.user.uid,
      status: call.status,
      createdAt: new Date().toISOString()
    };
    await db.collection('calls').add(callData);

    return res.json({
      message: 'Call initiated successfully!',
      sid: call.sid,
      status: call.status,
      remainingCredits: userData.credits - 1
    });
  } catch (error) {
    console.error('Make call error:', error);
    return res.status(500).json({ error: error.message });
  }
});

// GET /api/calls/history - Get call history for user
router.get('/history', verifyToken, async (req, res) => {
  try {
    const snapshot = await db.collection('calls')
      .where('userId', '==', req.user.uid)
      .orderBy('createdAt', 'desc')
      .limit(50)
      .get();

    const calls = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    return res.json({ calls });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// POST /webhook/voice/outbound-twiml - TwiML for outbound calls
router.post('/outbound-twiml', (req, res) => {
  const twiml = new twilio.twiml.VoiceResponse();
  const to = req.body.To || req.query.To;
  const from = req.body.From || req.query.From;

  if (to) {
    const dial = twiml.dial({ callerId: from });
    dial.number(to);
  } else {
    twiml.say('Number unavailable.');
  }

  res.type('text/xml');
  return res.send(twiml.toString());
});

// POST /webhook/voice/incoming - TwiML for incoming voice calls (e.g. WhatsApp Call Me OTP)
router.post('/incoming', (req, res) => {
  const twiml = new twilio.twiml.VoiceResponse();
  console.log('📞 INCOMING VOICE CALL DETECTED on Virtual Number!');
  twiml.say('Hello! Please speak your verification code after the tone.');
  twiml.record({ maxLength: 30, transcribe: true });
  res.type('text/xml');
  return res.send(twiml.toString());
});

// POST /api/calls/token - Generate Twilio Voice token for WebRTC calling
router.post('/token', verifyToken, async (req, res) => {
  try {
    const AccessToken = twilio.jwt.AccessToken;
    const VoiceGrant = AccessToken.VoiceGrant;

    const voiceGrant = new VoiceGrant({
      outgoingApplicationSid: process.env.TWILIO_TWIML_APP_SID,
      incomingAllow: true
    });

    const token = new AccessToken(
      process.env.TWILIO_ACCOUNT_SID,
      process.env.TWILIO_API_KEY,
      process.env.TWILIO_API_SECRET,
      { identity: req.user.uid }
    );

    token.addGrant(voiceGrant);

    return res.json({ token: token.toJwt(), identity: req.user.uid });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

module.exports = router;
