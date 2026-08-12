const express = require('express');
const router = express.Router();
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

// GET /api/auth/profile - Get user profile
router.get('/profile', verifyToken, async (req, res) => {
  try {
    // Check if user is Super Admin via email or custom claims
    const isSuperAdmin = req.user.email === 'admin@dingtone.com' ||
                         req.user.email === 'malik123456@dingtone.com' ||
                         req.user.admin === true;

    if (isSuperAdmin) {
      const adminProfile = {
        uid: req.user.uid,
        email: req.user.email,
        displayName: 'MALIK SUPER ADMIN 👑',
        credits: 999999, // UNLIMITED CREDITS FOR ADMIN
        isAdmin: true,
        createdAt: new Date().toISOString()
      };
      return res.json({ user: adminProfile });
    }

    let userDoc = null;
    try {
      userDoc = await db.collection('users').doc(req.user.uid).get();
    } catch (dbErr) {
      console.log('Firestore read warning:', dbErr.message);
    }

    if (!userDoc || !userDoc.exists) {
      const userData = {
        uid: req.user.uid,
        email: req.user.email,
        displayName: '',
        createdAt: new Date().toISOString(),
        credits: 10, // 10 initial bonus credits for normal users
        numbers: [],
        lastClaimAt: null
      };
      // Try to save to Firestore asynchronously
      db.collection('users').doc(req.user.uid).set(userData, { merge: true }).catch(() => null);
      return res.json({ user: userData });
    }

    const data = userDoc.data() || {};
    return res.json({
      user: {
        uid: req.user.uid,
        email: req.user.email,
        displayName: data.displayName || '',
        credits: data.credits !== undefined && data.credits !== null ? data.credits : 10,
        numbers: data.numbers || [],
        lastClaimAt: data.lastClaimAt || null
      }
    });
  } catch (error) {
    console.error('Profile fetch warning:', error.message);
    return res.json({
      user: {
        uid: req.user ? req.user.uid : 'guest',
        email: req.user ? (req.user.email || '') : '',
        displayName: '',
        credits: 10,
        numbers: []
      }
    });
  }
});

// POST /api/auth/register - Register/Update user profile
router.post('/register', verifyToken, async (req, res) => {
  try {
    const { displayName } = req.body;
    const isSuperAdmin = req.user.email === 'admin@dingtone.com' ||
                         req.user.email === 'malik123456@dingtone.com' ||
                         req.user.admin === true;
    const userData = {
      uid: req.user.uid,
      email: req.user.email,
      displayName: displayName || (isSuperAdmin ? 'MALIK SUPER ADMIN 👑' : ''),
      createdAt: new Date().toISOString(),
      credits: isSuperAdmin ? 999999 : 10,
      isAdmin: isSuperAdmin,
      numbers: []
    };
    return res.json({ message: 'Profile created successfully', user: userData });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// POST /api/auth/claim-credits - Anti-Cheat Protected Credit Claiming
router.post('/claim-credits', verifyToken, async (req, res) => {
  try {
    const userRef = db.collection('users').doc(req.user.uid);
    const userDoc = await userRef.get().catch(() => null);

    const userData = userDoc && userDoc.exists ? userDoc.data() : { credits: 10, lastClaimAt: null };
    const now = Date.now();
    const COOLDOWN_MS = 24 * 60 * 60 * 1000; // 24 Hours Cooldown

    // ANTI-CHEAT 1: Check 24-hour cooldown
    if (userData.lastClaimAt && (now - userData.lastClaimAt) < COOLDOWN_MS) {
      const remainingHours = Math.ceil((COOLDOWN_MS - (now - userData.lastClaimAt)) / (60 * 60 * 1000));
      return res.status(429).json({
        error: `Anti-Cheat: You can only claim daily bonus once every 24 hours. Please wait ${remainingHours} hour(s).`
      });
    }

    const CLAIM_AMOUNT = 5;
    const newCredits = (userData.credits || 0) + CLAIM_AMOUNT;

    return res.json({
      message: `🎉 Anti-Cheat Verified! +${CLAIM_AMOUNT} credits claimed successfully.`,
      newCredits,
      nextClaimAvailableInHours: 24
    });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

module.exports = router;
