const express = require('express');
const router = express.Router();
const twilio = require('twilio');
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

const client = twilio(
  process.env.TWILIO_ACCOUNT_SID,
  process.env.TWILIO_AUTH_TOKEN
);

// GET /api/numbers/available - Get available numbers to purchase
router.get('/available', verifyToken, async (req, res) => {
  try {
    const country = req.query.country || 'US';
    const numbers = await client.availablePhoneNumbers(country)
      .local
      .list({ limit: 10 });

    const formatted = numbers.map(n => ({
      phoneNumber: n.phoneNumber,
      friendlyName: n.friendlyName,
      region: n.region,
      capabilities: n.capabilities
    }));

    return res.json({ numbers: formatted });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// POST /api/numbers/buy - Buy/Assign a virtual number
router.post('/buy', verifyToken, async (req, res) => {
  try {
    const { phoneNumber } = req.body;

    // Check user credits
    const userDoc = await db.collection('users').doc(req.user.uid).get();
    const userData = userDoc.data();
    if (userData.credits < 5) {
      return res.status(400).json({ error: 'Insufficient credits. You need 5 credits to buy a number.' });
    }

    // Purchase number from Twilio
    const purchased = await client.incomingPhoneNumbers.create({
      phoneNumber: phoneNumber,
      smsUrl: `${process.env.BACKEND_URL}/webhook/sms`,
      voiceUrl: `${process.env.BACKEND_URL}/webhook/voice`
    });

    // Save to Firestore
    const numberData = {
      sid: purchased.sid,
      phoneNumber: purchased.phoneNumber,
      friendlyName: purchased.friendlyName,
      userId: req.user.uid,
      assignedAt: new Date().toISOString(),
      active: true,
      smsCount: 0
    };

    await db.collection('virtualNumbers').doc(purchased.sid).set(numberData);

    // Deduct credits and add number to user
    await db.collection('users').doc(req.user.uid).update({
      credits: userData.credits - 5,
      numbers: [...(userData.numbers || []), purchased.phoneNumber]
    });

    return res.json({ message: 'Number purchased successfully!', number: numberData });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// GET /api/numbers/my - Get user's numbers
router.get('/my', verifyToken, async (req, res) => {
  let numbers = [];
  try {
    const snapshot = await db.collection('virtualNumbers')
      .where('userId', '==', req.user.uid)
      .where('active', '==', true)
      .get();

    numbers = snapshot.docs.map(doc => doc.data());
  } catch (error) {
    console.log('Firestore numbers read warning:', error.message);
  }
  return res.json({ numbers });
});

// DELETE /api/numbers/:sid - Release a number
router.delete('/:sid', verifyToken, async (req, res) => {
  try {
    const { sid } = req.params;

    // Verify ownership
    const numDoc = await db.collection('virtualNumbers').doc(sid).get();
    if (!numDoc.exists || numDoc.data().userId !== req.user.uid) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    // Release from Twilio
    await client.incomingPhoneNumbers(sid).remove();

    // Mark as inactive in Firestore
    await db.collection('virtualNumbers').doc(sid).update({ active: false });

    return res.json({ message: 'Number released successfully' });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

module.exports = router;
