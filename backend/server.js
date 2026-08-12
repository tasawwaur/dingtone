require('dotenv').config();
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const rateLimit = require('express-rate-limit');

const authRoutes = require('./routes/auth');
const numberRoutes = require('./routes/numbers');
const smsRoutes = require('./routes/sms');
const callRoutes = require('./routes/calls');

const app = express();
const PORT = process.env.PORT || 5000;

// Rate Limiting (DDoS protection)
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100,
  message: 'Too many requests, please try again later.'
});

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use('/api/', limiter);

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/numbers', numberRoutes);
app.use('/api/sms', smsRoutes);
app.use('/api/calls', callRoutes);

// Twilio Webhooks
app.use('/webhook/sms', smsRoutes);
app.use('/webhook/voice', callRoutes);

// Health check
app.get('/', (req, res) => {
  res.json({ 
    status: 'ok', 
    message: 'Dingtone Clone API is running! 🚀',
    version: '1.0.0'
  });
});

app.listen(PORT, () => {
  console.log(`✅ Server running on port ${PORT}`);
});

module.exports = app;
