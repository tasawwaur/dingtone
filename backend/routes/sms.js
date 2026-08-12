const express = require('express');
const router = express.Router();
const twilio = require('twilio');
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

const client = twilio(
  process.env.TWILIO_ACCOUNT_SID,
  process.env.TWILIO_AUTH_TOKEN
);

// POST /webhook/sms - Twilio webhook for incoming SMS
router.post('/incoming', async (req, res) => {
  try {
    const { From, To, Body, MessageSid } = req.body;

    // Find which user owns this number
    const snapshot = await db.collection('virtualNumbers')
      .where('phoneNumber', '==', To)
      .where('active', '==', true)
      .get();

    if (snapshot.empty) {
      console.log(`No user found for number: ${To}`);
      return res.status(200).send('');
    }

    const numberData = snapshot.docs[0].data();

    // Save SMS to Firestore
    const smsData = {
      messageSid: MessageSid,
      from: From,
      to: To,
      body: Body,
      userId: numberData.userId,
      receivedAt: new Date().toISOString(),
      read: false
    };

    await db.collection('messages').add(smsData);

    // Update SMS count on number
    await db.collection('virtualNumbers').doc(snapshot.docs[0].id).update({
      smsCount: (numberData.smsCount || 0) + 1
    });

    console.log(`📩 SMS received: ${From} → ${To}: ${Body}`);

    // Send TwiML response
    const twiml = new twilio.twiml.MessagingResponse();
    res.type('text/xml');
    return res.send(twiml.toString());
  } catch (error) {
    console.error('SMS webhook error:', error);
    return res.status(500).json({ error: error.message });
  }
});

// GET /api/sms/inbox - Get user's SMS inbox
router.get('/inbox', verifyToken, async (req, res) => {
  let messages = [];
  try {
    const { number } = req.query;
    let query = db.collection('messages').where('userId', '==', req.user.uid);

    if (number) {
      query = query.where('to', '==', number);
    }

    const snapshot = await query.get();
    messages = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    messages.sort((a, b) => new Date(b.receivedAt || 0) - new Date(a.receivedAt || 0));
    if (messages.length > 50) messages = messages.slice(0, 50);
  } catch (error) {
    console.log('Firestore inbox read warning:', error.message);
  }
  return res.json({ messages });
});

// PATCH /api/sms/:id/read - Mark SMS as read
router.patch('/:id/read', verifyToken, async (req, res) => {
  try {
    const msgDoc = await db.collection('messages').doc(req.params.id).get();
    if (!msgDoc.exists || msgDoc.data().userId !== req.user.uid) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    await db.collection('messages').doc(req.params.id).update({ read: true });
    return res.json({ message: 'Marked as read' });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// POST /api/sms/send - Send an SMS
router.post('/send', verifyToken, async (req, res) => {
  try {
    const { to, from, body } = req.body;

    // Check user credits
    const userRef = db.collection('users').doc(req.user.uid);
    const userDoc = await userRef.get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: 'User profile not found.' });
    }

    const userData = userDoc.data();
    if ((userData.credits || 0) < 1) {
      return res.status(400).json({ error: 'Insufficient credits. You need 1 credit to send an SMS.' });
    }

    // Verify user owns the 'from' number
    const snapshot = await db.collection('virtualNumbers')
      .where('phoneNumber', '==', from)
      .where('userId', '==', req.user.uid)
      .get();

    if (snapshot.empty) {
      return res.status(403).json({ error: 'You do not own this number' });
    }

    const message = await client.messages.create({ to, from, body });

    // Deduct 1 credit
    await userRef.update({ credits: (userData.credits || 1) - 1 });

    return res.json({ message: 'SMS sent!', sid: message.sid, remainingCredits: userData.credits - 1 });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

module.exports = router;
