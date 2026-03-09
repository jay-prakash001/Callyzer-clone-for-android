// backend/server.js
// Callyzer Backend — Node.js + Express
// FCM: Firebase Admin (messaging only)
// Storage: Cloudinary (recordings)

require('dotenv').config();

const express              = require('express');
const admin                = require('firebase-admin');
const multer               = require('multer');
const cors                 = require('cors');
const { v4: uuid }         = require('uuid');
const { Readable }         = require('stream');
const { v2: cloudinary }   = require('cloudinary');
const path = require('path');

// ── Init Express ──────────────────────────────────────────────────────
const app = express();
app.use(cors({ origin: process.env.WEB_ORIGIN }));
app.use(express.json());

// ── Firebase Admin (FCM only — no storageBucket needed) ───────────────
admin.initializeApp({
  credential: admin.credential.cert(
    require('./firebase-service-account.json')
  ),
  // ← storageBucket line removed entirely
});

// ── Cloudinary ────────────────────────────────────────────────────────
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key:    process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
  secure:     true,   // always use https URLs
});

// ── Multer (memory — buffer piped to Cloudinary) ──────────────────────
// const upload = multer({
//   storage: multer.memoryStorage(),
//   limits:  { fileSize: 50_000_000 },   // 50 MB max
// });

// ── In-memory store (swap for PostgreSQL / MongoDB in production) ─────
const DB = {
  employees:  new Map(),   // employeeId → { fcmToken, name }
  calls:      new Map(),   // callId     → CallRecord
  callLogs:   [],
  recordings: new Map(),   // callId     → { url, publicId, uploadedAt }
};

// ═══════════════════════════════════════════════════════════════════════
//  EMPLOYEE REGISTRATION
// ═══════════════════════════════════════════════════════════════════════
app.post('/api/employees/register', (req, res) => {
  const { employeeId, fcmToken, name } = req.body;
  DB.employees.set(employeeId, { fcmToken, name, registeredAt: Date.now() });
  console.log(`Employee registered: ${employeeId}`);
  res.json({ success: true });
});

// ═══════════════════════════════════════════════════════════════════════
//  INITIATE CALL — web dashboard → FCM → Android device
// ═══════════════════════════════════════════════════════════════════════
app.post('/api/calls/initiate', async (req, res) => {
  const { employeeId, customerPhone, customerName } = req.body;

  const employee = DB.employees.get(employeeId);
  if (!employee?.fcmToken) {
    return res.status(404).json({ error: 'Employee device not registered or offline' });
  }

  const callId = `CALL-${uuid()}`;
  const callRecord = {
    callId, employeeId, customerPhone, customerName,
    status:        'INITIATED',
    initiatedAt:   Date.now(),
    fcmDelivered:  false,
    statusHistory: [{ status: 'INITIATED', ts: Date.now() }],
  };
  DB.calls.set(callId, callRecord);

  try {
    const fcmResult = await admin.messaging().send({
      token: employee.fcmToken,
      data: {
        type:          'DIAL_COMMAND',
        callId,
        customerPhone,
        customerName,
        employeeId,
        initiatedBy:   'web_dashboard',
      },
      android: {
        priority: 'high',
        ttl:      30_000,
      },
    });

    callRecord.fcmMessageId = fcmResult;
    callRecord.fcmDelivered = true;
    callRecord.status       = 'FCM_SENT';
    callRecord.statusHistory.push({ status: 'FCM_SENT', ts: Date.now() });

    console.log(`FCM sent: ${callId} → ${employee.name} (${customerPhone})`);
    res.json({ callId, status: 'FCM_SENT' });

  } catch (err) {
    callRecord.status   = 'FCM_FAILED';
    callRecord.fcmError = err.message;
    console.error('FCM failed:', err.message);
    res.status(500).json({ error: 'FCM delivery failed', detail: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════
//  CALL STATUS — Android reports state changes
// ═══════════════════════════════════════════════════════════════════════
app.patch('/api/calls/:callId/status', (req, res) => {
  const { callId } = req.params;
  const { status, duration } = req.body;

  const call = DB.calls.get(callId);
  if (!call) return res.status(404).json({ error: 'Call not found' });

  call.status = status;
  call.statusHistory.push({ status, ts: Date.now() });
  if (duration !== undefined) call.durationSecs = parseInt(duration);

  console.log(`Call ${callId} → ${status}`);
  res.json({ success: true, callId, status });
});

// Web dashboard polls this for live status
app.get('/api/calls/:callId/status', (req, res) => {
  const call = DB.calls.get(req.params.callId);
  if (!call) return res.status(404).json({ error: 'Not found' });
  res.json({
    callId:       call.callId,
    status:       call.status,
    durationSecs: call.durationSecs,
    history:      call.statusHistory,
  });
});

// ═══════════════════════════════════════════════════════════════════════
//  CALL LOGS
// ═══════════════════════════════════════════════════════════════════════
app.post('/api/calllogs', (req, res) => {
  DB.callLogs.push({ ...req.body, receivedAt: Date.now() });
  res.json({ success: true });
});

app.get('/api/calllogs', (req, res) => {
  const { employeeId, type, limit = 50, offset = 0 } = req.query;
  let logs = DB.callLogs;
  if (employeeId) logs = logs.filter(l => l.employeeId === employeeId);
  if (type)       logs = logs.filter(l => l.callType   === type);
  logs = logs.sort((a, b) => b.timestamp - a.timestamp);
  res.json({
    total: logs.length,
    logs:  logs.slice(Number(offset), Number(offset) + Number(limit)),
  });
});

// ═══════════════════════════════════════════════════════════════════════
//  RECORDING UPLOAD  ← Cloudinary replaces Firebase Storage here
// ═══════════════════════════════════════════════════════════════════════



const storage = multer.diskStorage({

  destination: (req, file, cb) => {
    cb(null, 'uploads/recordings');
  },

  filename: (req, file, cb) => {
    const uniqueName = Date.now() + "-" + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname) || ".mp3";

    cb(null, uniqueName + ext);
  }

});

const upload = multer({ storage });

app.post('/api/recordings/upload0', upload.single('recording'), async (req, res) => {


  console.log("File ",req.file)
console.log("Request:", req.body);  if (!req.file) {
    return res.status(400).json({ error: 'No file uploaded' });
  }

  try {

    const fileUrl = `/uploads/recordings/${req.file.filename}`;

    console.log(`Recording saved → ${req.file.path}`);

    console.log(fileUrl)
    res.json({
      success: true,
      recordingUrl: fileUrl
    });

  } catch (err) {

    console.error('Upload failed:', err);

    res.status(500).json({
      error: 'Upload failed',
      detail: err.message
    });
  }
});

// Get recording info for a call
app.get('/api/recordings/:callId', (req, res) => {
  const rec = DB.recordings.get(req.params.callId);
  if (!rec) return res.status(404).json({ error: 'Recording not found' });
  res.json(rec);
});

app.post("/send-notification0", async (req, res) => {
    const { token, data } = req.body;

    if (!token) {
        return res.status(400).json({ error: "token is required" });
    }

    try {
        const message = {
            token: token,
            data: {
                type:          data?.type          ?? "DIAL_COMMAND",
                customerPhone: data?.customerPhone ?? "",
                customerName:  data?.customerName  ?? "",
            },
            android: {
                priority: "high",
                ttl:      3000,
            }
        };

        const response = await admin.messaging().send(message);
        console.log("FCM sent:", response);
        res.json({ success: true, messageId: response });

    } catch (err) {
        console.error("FCM error:", err);
        res.status(500).json({ error: err.message });
    }
});

app.post("/send-notification", async (req, res) => {

  const { token, title, body, data } = req.body;

  try {

    const message = {
      token: token,

      notification: {
        title: title,
        body: body
      },

      data: data || {},

      android: {
        priority: "high"
      }
    };

    const response = await admin.messaging().send(message);

    console.log("Notification sent:", response);

    res.json({
      success: true,
      messageId: response
    });

  } catch (error) {

    console.error("FCM Error:", error);

    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// Delete a recording (optional — useful for compliance/GDPR)
app.delete('/api/recordings/:callId', async (req, res) => {
  const rec = DB.recordings.get(req.params.callId);
  if (!rec) return res.status(404).json({ error: 'Recording not found' });

  try {
    await cloudinary.uploader.destroy(rec.publicId, { resource_type: 'video' });
    DB.recordings.delete(req.params.callId);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Delete failed', detail: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════
//  CONTACTS
// ═══════════════════════════════════════════════════════════════════════
app.get('/api/contacts', (_req, res) => {
  res.json({ contacts: [] });  // replace with your CRM query
});



// ── Start ─────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Callyzer backend running on :${PORT}`));
module.exports = app;

// ═══════════════════════════════════════════════════════════════════════
//  HELPER — wrap Cloudinary's stream-based upload in a Promise
// ═══════════════════════════════════════════════════════════════════════
function uploadToCloudinary(buffer, options) {
  return new Promise((resolve, reject) => {
    const uploadStream = cloudinary.uploader.upload_stream(
      options,
      (error, result) => {
        if (error) reject(error);
        else       resolve(result);
      }
    );
    // Pipe the multer memory buffer into the Cloudinary stream
    Readable.from(buffer).pipe(uploadStream);
  });
}
