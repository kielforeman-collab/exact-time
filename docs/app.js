(() => {
  'use strict';

  /* ========================================================================
     State
     ======================================================================== */
  const state = {
    theme: 'dark',
    clockMode: 'dial', // 'dial' | 'digital'
    use24Hour: true,
    showCentiseconds: true,
    tickCadenceSeconds: 0, // 0, 1, 5, 10, 30
    lastTickEpochSecond: -1,
    activePreset: 'market',
    nightRed: false,
    cityIndex: 0
  };

  /* ========================================================================
     DOM References
     ======================================================================== */
  const rootEl = document.documentElement;
  const themeToggleBtn = document.getElementById('theme-toggle-btn');
  const themeToggleLabel = document.getElementById('theme-toggle-label');
  const themeMeta = document.querySelector('meta[name="theme-color"]');

  const clockStage = document.getElementById('clock-stage');
  const dialCanvas = document.getElementById('precision-dial-canvas');
  const dialCtx = dialCanvas ? dialCanvas.getContext('2d') : null;

  const heroTimeMain = document.getElementById('hero-time-main');
  const heroTimeFrac = document.getElementById('hero-time-frac');
  const heroTimeAmPm = document.getElementById('hero-time-ampm');
  const heroDateTz = document.getElementById('hero-date-tz');
  const heroSecondFill = document.getElementById('hero-second-fill');

  const modeDialBtn = document.getElementById('mode-dial-btn');
  const modeDigitalBtn = document.getElementById('mode-digital-btn');
  const toggle24hBtn = document.getElementById('toggle-24h-btn');
  const toggleCentisBtn = document.getElementById('toggle-centis-btn');
  const tickButtons = document.querySelectorAll('[data-tick]');

  const standbyDevice = document.getElementById('standby-device');
  const standbyNightBtn = document.getElementById('standby-night-btn');
  const standbyTimeMain = document.getElementById('standby-time-main');
  const standbyTimeFrac = document.getElementById('standby-time-frac');
  const standbyDate = document.getElementById('standby-date');

  const presetButtons = document.querySelectorAll('[data-preset]');
  const inspectorTime = document.getElementById('inspector-time');
  const inspectorName = document.getElementById('inspector-name');
  const inspectorCategory = document.getElementById('inspector-category');
  const inspectorSchedule = document.getElementById('inspector-schedule');
  const inspectorLead = document.getElementById('inspector-lead');
  const inspectorVibration = document.getElementById('inspector-vibration');
  const inspectorFade = document.getElementById('inspector-fade');
  const inspectorCountdown = document.getElementById('inspector-countdown');

  const cityButtons = document.querySelectorAll('[data-city]');
  const ephCityLabel = document.getElementById('eph-city-label');
  const ephSolarElev = document.getElementById('eph-solar-elev');
  const ephDaylightStatus = document.getElementById('eph-daylight-status');
  const ephDaylightFill = document.getElementById('eph-daylight-fill');
  const ephSunrise = document.getElementById('eph-sunrise');
  const ephSunset = document.getElementById('eph-sunset');
  const ephMoonName = document.getElementById('eph-moon-name');
  const ephMoonIllum = document.getElementById('eph-moon-illum');
  const ephMoonLit = document.getElementById('eph-moon-lit');
  const ephLunarAge = document.getElementById('eph-lunar-age');
  const ephUnixEpoch = document.getElementById('eph-unix-epoch');
  const ephUtcIso = document.getElementById('eph-utc-iso');
  const ephIsoWeek = document.getElementById('eph-iso-week');
  const ephDayYear = document.getElementById('eph-day-year');

  const copyBuildBtn = document.getElementById('copy-build-cmd-btn');
  const footerUtcClock = document.getElementById('footer-utc-clock');

  /* ========================================================================
     Theme Management (Obsidian Dark / Alabaster Light)
     ======================================================================== */
  function applyTheme(theme) {
    state.theme = theme === 'light' ? 'light' : 'dark';
    rootEl.setAttribute('data-theme', state.theme);
    if (themeToggleLabel) {
      themeToggleLabel.textContent = state.theme === 'dark' ? 'Obsidian' : 'Alabaster';
    }
    if (themeMeta) {
      themeMeta.setAttribute('content', state.theme === 'dark' ? '#0D0F12' : '#F4F4EE');
    }
    try {
      localStorage.setItem('exacttime-theme', state.theme);
    } catch (_) {
      // Ignore storage errors in restricted contexts
    }
  }

  try {
    const savedTheme = localStorage.getItem('exacttime-theme');
    if (savedTheme === 'light' || savedTheme === 'dark') {
      applyTheme(savedTheme);
    }
  } catch (_) {
    // Default Obsidian dark
  }

  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      applyTheme(state.theme === 'dark' ? 'light' : 'dark');
    });
  }

  /* ========================================================================
     Web Audio Horological Escapement Tick
     ======================================================================== */
  let audioCtx = null;

  function playEscapementTick(accentBeat) {
    try {
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextCtor) return;
      if (!audioCtx) {
        audioCtx = new AudioContextCtor();
      }
      if (audioCtx.state === 'suspended') {
        audioCtx.resume();
      }
      const now = audioCtx.currentTime;
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(accentBeat ? 1950 : 1420, now);
      osc.frequency.exponentialRampToValueAtTime(280, now + 0.018);

      gain.gain.setValueAtTime(accentBeat ? 0.14 : 0.08, now);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.022);

      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start(now);
      osc.stop(now + 0.025);
    } catch (_) {
      // Audio playback blocked or unavailable
    }
  }

  /* ========================================================================
     Formatting Helpers
     ======================================================================== */
  const pad2 = (n) => String(Math.floor(Math.abs(n))).padStart(2, '0');

  function formatTimeParts(date, use24h) {
    const rawH = date.getHours();
    const m = date.getMinutes();
    const s = date.getSeconds();
    const cs = Math.floor(date.getMilliseconds() / 10);
    if (use24h) {
      return {
        main: `${pad2(rawH)}:${pad2(m)}:${pad2(s)}`,
        frac: `.${pad2(cs)}`,
        ampm: ''
      };
    }
    const h12 = rawH % 12 === 0 ? 12 : rawH % 12;
    return {
      main: `${pad2(h12)}:${pad2(m)}:${pad2(s)}`,
      frac: `.${pad2(cs)}`,
      ampm: rawH >= 12 ? 'PM' : 'AM'
    };
  }

  function formatDateAndTz(date) {
    const dayStr = date.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric'
    }).toUpperCase();
    const offsetMin = -date.getTimezoneOffset();
    const sign = offsetMin >= 0 ? '+' : '-';
    const absMin = Math.abs(offsetMin);
    const utcOffset = `UTC${sign}${pad2(Math.floor(absMin / 60))}:${pad2(absMin % 60)}`;
    return `${dayStr} · ${utcOffset}`;
  }

  /* ========================================================================
     Precision Swiss Analog Dial Canvas
     ======================================================================== */
  function drawPrecisionDial(date) {
    if (!dialCtx || state.clockMode !== 'dial') return;

    const w = dialCanvas.width;
    const h = dialCanvas.height;
    const cx = w / 2;
    const cy = h / 2;
    const r = Math.min(cx, cy) - 12;

    const isLight = state.theme === 'light';
    const faceBg = isLight ? '#F8F8F4' : '#11151B';
    const ringOuter = isLight ? '#D5D6CC' : '#2B3340';
    const tickMajor = isLight ? '#14171C' : '#F4F6F9';
    const tickMinor = isLight ? '#9CA4B0' : '#434D5E';
    const handPrimary = isLight ? '#14171C' : '#F4F6F9';
    const handSecondary = isLight ? '#4A5362' : '#C5CCD8';
    const vermilion = isLight ? '#D9381E' : '#FF4D2E';
    const numeralColor = isLight ? '#545D6B' : '#7D8796';

    dialCtx.clearRect(0, 0, w, h);

    // Outer bezel & subtle metallic dial gradient
    const bgGrad = dialCtx.createRadialGradient(cx, cy - r * 0.28, r * 0.08, cx, cy, r);
    bgGrad.addColorStop(0, isLight ? '#FFFFFF' : '#1A2029');
    bgGrad.addColorStop(1, faceBg);

    dialCtx.beginPath();
    dialCtx.arc(cx, cy, r, 0, Math.PI * 2);
    dialCtx.fillStyle = bgGrad;
    dialCtx.fill();
    dialCtx.lineWidth = 3;
    dialCtx.strokeStyle = ringOuter;
    dialCtx.stroke();

    // Inner precision track ring
    dialCtx.beginPath();
    dialCtx.arc(cx, cy, r * 0.84, 0, Math.PI * 2);
    dialCtx.lineWidth = 1.25;
    dialCtx.strokeStyle = isLight ? 'rgba(20,23,28,0.08)' : 'rgba(244,246,249,0.08)';
    dialCtx.stroke();

    // 60-second tick marks
    for (let i = 0; i < 60; i++) {
      const angle = (i * 6 - 90) * (Math.PI / 180);
      const isQuarter = i % 15 === 0;
      const isHour = i % 5 === 0;
      const outerR = r * 0.94;
      const innerR = isQuarter ? r * 0.80 : isHour ? r * 0.84 : r * 0.89;

      dialCtx.beginPath();
      dialCtx.moveTo(cx + Math.cos(angle) * innerR, cy + Math.sin(angle) * innerR);
      dialCtx.lineTo(cx + Math.cos(angle) * outerR, cy + Math.sin(angle) * outerR);
      dialCtx.strokeStyle = isQuarter ? vermilion : isHour ? tickMajor : tickMinor;
      dialCtx.lineWidth = isQuarter ? 4 : isHour ? 2.6 : 1.4;
      dialCtx.lineCap = 'round';
      dialCtx.stroke();
    }

    // Cardinal index labels
    dialCtx.fillStyle = numeralColor;
    dialCtx.font = '600 20px "JetBrains Mono", monospace';
    dialCtx.textAlign = 'center';
    dialCtx.textBaseline = 'middle';
    const cardinals = [
      { label: '60', angle: -90 },
      { label: '15', angle: 0 },
      { label: '30', angle: 90 },
      { label: '45', angle: 180 }
    ];
    cardinals.forEach(({ label, angle }) => {
      const rad = angle * (Math.PI / 180);
      dialCtx.fillText(label, cx + Math.cos(rad) * (r * 0.67), cy + Math.sin(rad) * (r * 0.67));
    });

    const ms = date.getMilliseconds();
    const sec = date.getSeconds() + ms / 1000;
    const min = date.getMinutes() + sec / 60;
    const hr = (date.getHours() % 12) + min / 60;

    // Hour hand
    const hrAngle = (hr * 30 - 90) * (Math.PI / 180);
    dialCtx.beginPath();
    dialCtx.moveTo(cx - Math.cos(hrAngle) * (r * 0.14), cy - Math.sin(hrAngle) * (r * 0.14));
    dialCtx.lineTo(cx + Math.cos(hrAngle) * (r * 0.50), cy + Math.sin(hrAngle) * (r * 0.50));
    dialCtx.strokeStyle = handPrimary;
    dialCtx.lineWidth = 8;
    dialCtx.lineCap = 'round';
    dialCtx.stroke();

    // Minute hand
    const minAngle = (min * 6 - 90) * (Math.PI / 180);
    dialCtx.beginPath();
    dialCtx.moveTo(cx - Math.cos(minAngle) * (r * 0.16), cy - Math.sin(minAngle) * (r * 0.16));
    dialCtx.lineTo(cx + Math.cos(minAngle) * (r * 0.74), cy + Math.sin(minAngle) * (r * 0.74));
    dialCtx.strokeStyle = handSecondary;
    dialCtx.lineWidth = 5.2;
    dialCtx.lineCap = 'round';
    dialCtx.stroke();

    // Sweeping Vermilion Second Hand
    const secAngle = (sec * 6 - 90) * (Math.PI / 180);
    dialCtx.beginPath();
    dialCtx.moveTo(cx - Math.cos(secAngle) * (r * 0.22), cy - Math.sin(secAngle) * (r * 0.22));
    dialCtx.lineTo(cx + Math.cos(secAngle) * (r * 0.88), cy + Math.sin(secAngle) * (r * 0.88));
    dialCtx.strokeStyle = vermilion;
    dialCtx.lineWidth = 3;
    dialCtx.lineCap = 'round';
    dialCtx.stroke();

    // Center pivot cap
    dialCtx.beginPath();
    dialCtx.arc(cx, cy, 9, 0, Math.PI * 2);
    dialCtx.fillStyle = vermilion;
    dialCtx.fill();

    dialCtx.beginPath();
    dialCtx.arc(cx, cy, 3.5, 0, Math.PI * 2);
    dialCtx.fillStyle = faceBg;
    dialCtx.fill();
  }

  /* ========================================================================
     Interactive Alarm Preset Inspector
     ======================================================================== */
  const ALARM_PRESETS = {
    market: {
      time: '09:30:00',
      name: 'NYSE Opening Bell',
      category: 'Market',
      schedule: 'Weekly · Mon–Fri',
      lead: 'T-10s Pre-Alarm Warning',
      vibration: 'Sharp Pulse',
      fade: 'Sound On · Instant',
      targetH: 9,
      targetM: 30,
      targetS: 0
    },
    transit: {
      time: '07:42:30',
      name: 'Express Platform Gate',
      category: 'Transit',
      schedule: 'Weekly · Mon–Fri',
      lead: 'T-30s Pre-Alarm Warning',
      vibration: 'Double Tap',
      fade: 'Sound On · 10s Fade-In',
      targetH: 7,
      targetM: 42,
      targetS: 30
    },
    interval: {
      time: 'Every 00:05:00',
      name: 'Lab Telemetry Sample',
      category: 'Precision',
      schedule: 'Interval · 300s Cycle',
      lead: 'T-5s Pre-Alarm Warning',
      vibration: 'Sharp Pulse',
      fade: 'Sound On · Instant',
      intervalSec: 300
    },
    quick: {
      time: 'Next :00',
      name: 'Quick Minute Sync',
      category: 'Focus',
      schedule: 'One-Time · Top of Minute',
      lead: 'Off',
      vibration: 'Double Tap',
      fade: 'Sound On · Instant',
      alignNextMinute: true
    }
  };

  function renderAlarmPreset(key) {
    const preset = ALARM_PRESETS[key] || ALARM_PRESETS.market;
    state.activePreset = key;

    presetButtons.forEach((btn) => {
      const isActive = btn.dataset.preset === key;
      btn.classList.toggle('active', isActive);
      btn.setAttribute('aria-selected', String(isActive));
    });

    if (inspectorTime) inspectorTime.textContent = preset.time;
    if (inspectorName) inspectorName.textContent = preset.name;
    if (inspectorCategory) inspectorCategory.textContent = preset.category;
    if (inspectorSchedule) inspectorSchedule.textContent = preset.schedule;
    if (inspectorLead) inspectorLead.textContent = preset.lead;
    if (inspectorVibration) inspectorVibration.textContent = preset.vibration;
    if (inspectorFade) inspectorFade.textContent = preset.fade;
  }

  function updateAlarmCountdown(now) {
    if (!inspectorCountdown) return;
    const preset = ALARM_PRESETS[state.activePreset];
    if (!preset) return;

    let remainingSec = 0;
    if (preset.alignNextMinute) {
      remainingSec = 60 - now.getSeconds();
    } else if (preset.intervalSec) {
      const epochSec = Math.floor(now.getTime() / 1000);
      remainingSec = preset.intervalSec - (epochSec % preset.intervalSec);
    } else {
      const target = new Date(now);
      target.setHours(preset.targetH, preset.targetM, preset.targetS, 0);
      if (target <= now) {
        target.setDate(target.getDate() + 1);
      }
      remainingSec = Math.max(1, Math.ceil((target.getTime() - now.getTime()) / 1000));
    }

    const hrs = Math.floor(remainingSec / 3600);
    const mins = Math.floor((remainingSec % 3600) / 60);
    const secs = remainingSec % 60;
    inspectorCountdown.textContent = `T-${pad2(hrs)}:${pad2(mins)}:${pad2(secs)}`;
  }

  presetButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      renderAlarmPreset(btn.dataset.preset);
      updateAlarmCountdown(new Date());
    });
  });

  /* ========================================================================
     NOAA Solar & Synodic Lunar Ephemeris (from EphemerisCalculator.kt)
     ======================================================================== */
  const REFERENCE_CITIES = [
    { name: 'Local Meridian', region: 'Device Time Zone', lat: 38.0, lon: 0.0, tz: null },
    { name: 'Zurich', region: 'CET / Horology', lat: 47.3769, lon: 8.5417, tz: 'Europe/Zurich' },
    { name: 'New York', region: 'Eastern / NYSE', lat: 40.7128, lon: -74.0060, tz: 'America/New_York' },
    { name: 'London', region: 'UTC / Greenwich', lat: 51.5074, lon: -0.1278, tz: 'Europe/London' },
    { name: 'Tokyo', region: 'JST / TSE', lat: 35.6762, lon: 139.6503, tz: 'Asia/Tokyo' },
    { name: 'San Francisco', region: 'Pacific / PST', lat: 37.7749, lon: -122.4194, tz: 'America/Los_Angeles' },
    { name: 'Sydney', region: 'AEST / ASX', lat: -33.8688, lon: 151.2093, tz: 'Australia/Sydney' }
  ];

  const SYNODIC_MONTH_DAYS = 29.53058867;
  const KNOWN_NEW_MOON_UNIX_SECONDS = 947182440.0;

  function getTzParts(date, timeZone) {
    if (!timeZone) {
      const offsetHours = -date.getTimezoneOffset() / 60;
      return {
        year: date.getFullYear(),
        month: date.getMonth() + 1,
        day: date.getDate(),
        hour: date.getHours(),
        minute: date.getMinutes(),
        second: date.getSeconds(),
        offsetHours
      };
    }
    const dtf = new Intl.DateTimeFormat('en-US', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
    const parts = Object.fromEntries(dtf.formatToParts(date).map((p) => [p.type, p.value]));
    const year = Number(parts.year);
    const month = Number(parts.month);
    const day = Number(parts.day);
    const hour = Number(parts.hour) % 24;
    const minute = Number(parts.minute);
    const second = Number(parts.second);

    const asUtc = Date.UTC(year, month - 1, day, hour, minute, second);
    const offsetHours = (asUtc - date.getTime()) / 3600000;
    return { year, month, day, hour, minute, second, offsetHours };
  }

  function computeIsoWeek(year, month, day) {
    const tmp = new Date(Date.UTC(year, month - 1, day));
    const dayNum = tmp.getUTCDay() || 7;
    tmp.setUTCDate(tmp.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(tmp.getUTCFullYear(), 0, 1));
    return Math.ceil((((tmp - yearStart) / 86400000) + 1) / 7);
  }

  function formatMinutesOfDay(minutes) {
    const wrapped = ((minutes % 1440) + 1440) % 1440;
    const totalSec = Math.floor(wrapped * 60);
    const h = Math.floor(totalSec / 3600) % 24;
    const m = Math.floor((totalSec % 3600) / 60);
    const s = totalSec % 60;
    return `${pad2(h)}:${pad2(m)}:${pad2(s)}`;
  }

  function updateEphemeris(now) {
    const rawCity = REFERENCE_CITIES[state.cityIndex] || REFERENCE_CITIES[0];
    const p = getTzParts(now, rawCity.tz);
    const effectiveLon = state.cityIndex === 0 ? p.offsetHours * 15.0 : rawCity.lon;
    const effectiveLat = state.cityIndex === 0 ? 38.0 : rawCity.lat;

    const startOfYear = Date.UTC(p.year, 0, 1);
    const currentDayUtc = Date.UTC(p.year, p.month - 1, p.day);
    const dayOfYear = Math.floor((currentDayUtc - startOfYear) / 86400000) + 1;
    const isLeap = (p.year % 4 === 0 && p.year % 100 !== 0) || p.year % 400 === 0;
    const daysInYear = isLeap ? 366 : 365;
    const isoWeek = computeIsoWeek(p.year, p.month, p.day);

    const localMinutes = p.hour * 60 + p.minute + p.second / 60;
    const gamma = ((2 * Math.PI) / daysInYear) * (dayOfYear - 1 + (localMinutes / 60 - 12) / 24);
    const eqTimeMinutes = 229.18 * (
      0.000075 +
      0.001868 * Math.cos(gamma) -
      0.032077 * Math.sin(gamma) -
      0.014615 * Math.cos(2 * gamma) -
      0.040849 * Math.sin(2 * gamma)
    );
    const declRad =
      0.006918 -
      0.399912 * Math.cos(gamma) +
      0.070257 * Math.sin(gamma) -
      0.006758 * Math.cos(2 * gamma) +
      0.000907 * Math.sin(2 * gamma) -
      0.002697 * Math.cos(3 * gamma) +
      0.00148 * Math.sin(3 * gamma);

    const latRad = (effectiveLat * Math.PI) / 180;
    const zenithRad = (90.833 * Math.PI) / 180;
    const cosHourAngle = Math.max(
      -1,
      Math.min(1, Math.cos(zenithRad) / (Math.cos(latRad) * Math.cos(declRad)) - Math.tan(latRad) * Math.tan(declRad))
    );
    const hourAngleDeg = (Math.acos(cosHourAngle) * 180) / Math.PI;

    const solarNoonMinutes = 720 - 4 * effectiveLon - eqTimeMinutes + p.offsetHours * 60;
    const sunriseMinutes = solarNoonMinutes - hourAngleDeg * 4;
    const sunsetMinutes = solarNoonMinutes + hourAngleDeg * 4;

    const trueSolarTime = localMinutes + eqTimeMinutes + 4 * effectiveLon - 60 * p.offsetHours;
    const currentHourAngleRad = ((trueSolarTime / 4 - 180) * Math.PI) / 180;
    const sinElevation = Math.max(
      -1,
      Math.min(1, Math.sin(latRad) * Math.sin(declRad) + Math.cos(latRad) * Math.cos(declRad) * Math.cos(currentHourAngleRad))
    );
    const solarElevationDeg = (Math.asin(sinElevation) * 180) / Math.PI;

    const isDaylight = localMinutes >= sunriseMinutes && localMinutes <= sunsetMinutes;
    const daylightSpan = Math.max(1, sunsetMinutes - sunriseMinutes);
    const daylightProgress = Math.max(0, Math.min(1, (localMinutes - sunriseMinutes) / daylightSpan));

    // Synodic Lunar Phase
    const unixSeconds = now.getTime() / 1000;
    const daysSinceKnownNew = (unixSeconds - KNOWN_NEW_MOON_UNIX_SECONDS) / 86400;
    const cycles = daysSinceKnownNew / SYNODIC_MONTH_DAYS;
    const phaseFraction = Math.max(0, Math.min(1, cycles - Math.floor(cycles)));
    const lunarAgeDays = phaseFraction * SYNODIC_MONTH_DAYS;
    const illumination = Math.max(0, Math.min(100, Math.round((1 - Math.cos(phaseFraction * 2 * Math.PI)) * 50)));

    let moonPhaseName = 'Waning Crescent';
    if (phaseFraction < 0.03 || phaseFraction > 0.97) moonPhaseName = 'New Moon';
    else if (phaseFraction < 0.22) moonPhaseName = 'Waxing Crescent';
    else if (phaseFraction < 0.28) moonPhaseName = 'First Quarter';
    else if (phaseFraction < 0.47) moonPhaseName = 'Waxing Gibbous';
    else if (phaseFraction < 0.53) moonPhaseName = 'Full Moon';
    else if (phaseFraction < 0.72) moonPhaseName = 'Waning Gibbous';
    else if (phaseFraction < 0.78) moonPhaseName = 'Last Quarter';

    if (ephCityLabel) ephCityLabel.textContent = `${rawCity.name} · ${rawCity.region}`;
    if (ephSolarElev) {
      const sign = solarElevationDeg >= 0 ? '+' : '';
      ephSolarElev.textContent = `${sign}${solarElevationDeg.toFixed(1)}°`;
    }
    if (ephDaylightStatus) {
      ephDaylightStatus.textContent = isDaylight
        ? `Daylight · ${Math.round(daylightProgress * 100)}% elapsed`
        : 'Below Horizon · Night';
    }
    if (ephDaylightFill) ephDaylightFill.style.width = `${(daylightProgress * 100).toFixed(1)}%`;
    if (ephSunrise) ephSunrise.textContent = formatMinutesOfDay(sunriseMinutes);
    if (ephSunset) ephSunset.textContent = formatMinutesOfDay(sunsetMinutes);

    if (ephMoonName) ephMoonName.textContent = moonPhaseName;
    if (ephMoonIllum) ephMoonIllum.textContent = `${illumination}% illuminated`;
    if (ephLunarAge) ephLunarAge.textContent = `${lunarAgeDays.toFixed(1)} d`;
    if (ephMoonLit) {
      const waxing = phaseFraction <= 0.5;
      ephMoonLit.style.width = `${illumination}%`;
      ephMoonLit.style.left = waxing ? 'auto' : '0';
      ephMoonLit.style.right = waxing ? '0' : 'auto';
    }

    if (ephUnixEpoch) ephUnixEpoch.textContent = String(Math.floor(unixSeconds));
    if (ephUtcIso) ephUtcIso.textContent = now.toISOString().replace(/\.\d{3}Z$/, 'Z');
    if (ephIsoWeek) ephIsoWeek.textContent = `W${pad2(isoWeek)}`;
    if (ephDayYear) ephDayYear.textContent = `${dayOfYear} / ${daysInYear}`;
  }

  cityButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      state.cityIndex = Number(btn.dataset.city) || 0;
      cityButtons.forEach((b) => b.classList.toggle('active', b === btn));
      updateEphemeris(new Date());
    });
  });

  /* ========================================================================
     Chronometer & StandBy Controls
     ======================================================================== */
  function setClockMode(mode) {
    state.clockMode = mode === 'digital' ? 'digital' : 'dial';
    if (clockStage) {
      clockStage.classList.toggle('mode-digital', state.clockMode === 'digital');
    }
    if (modeDialBtn) modeDialBtn.classList.toggle('active', state.clockMode === 'dial');
    if (modeDigitalBtn) modeDigitalBtn.classList.toggle('active', state.clockMode === 'digital');
  }

  if (modeDialBtn) modeDialBtn.addEventListener('click', () => setClockMode('dial'));
  if (modeDigitalBtn) modeDigitalBtn.addEventListener('click', () => setClockMode('digital'));

  if (toggle24hBtn) {
    toggle24hBtn.addEventListener('click', () => {
      state.use24Hour = !state.use24Hour;
      toggle24hBtn.classList.toggle('active', state.use24Hour);
      toggle24hBtn.textContent = state.use24Hour ? '24-Hour' : '12-Hour';
    });
  }

  if (toggleCentisBtn) {
    toggleCentisBtn.addEventListener('click', () => {
      state.showCentiseconds = !state.showCentiseconds;
      toggleCentisBtn.classList.toggle('active', state.showCentiseconds);
      if (heroTimeFrac) heroTimeFrac.style.display = state.showCentiseconds ? '' : 'none';
      if (standbyTimeFrac) standbyTimeFrac.style.display = state.showCentiseconds ? '' : 'none';
    });
  }

  tickButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      state.tickCadenceSeconds = Number(btn.dataset.tick) || 0;
      tickButtons.forEach((b) => b.classList.toggle('active', b === btn));
      if (state.tickCadenceSeconds > 0) {
        playEscapementTick(true);
      }
    });
  });

  if (standbyNightBtn && standbyDevice) {
    standbyNightBtn.addEventListener('click', () => {
      state.nightRed = !state.nightRed;
      standbyDevice.classList.toggle('night-red', state.nightRed);
      standbyNightBtn.setAttribute('aria-pressed', String(state.nightRed));
      standbyNightBtn.textContent = state.nightRed ? 'Night Red: On' : 'Night Red: Off';
    });
  }

  if (copyBuildBtn) {
    copyBuildBtn.addEventListener('click', async () => {
      const cmd = [
        'git clone https://github.com/kielforeman-collab/exact-time.git',
        'cd exact-time',
        './gradlew assembleDebug'
      ].join('\n');
      try {
        await navigator.clipboard.writeText(cmd);
        copyBuildBtn.textContent = 'Copied';
        setTimeout(() => {
          copyBuildBtn.textContent = 'Copy';
        }, 1800);
      } catch (_) {
        copyBuildBtn.textContent = 'Select & Copy';
      }
    });
  }

  /* ========================================================================
     Main Synchronized Render Loop
     ======================================================================== */
  let lastSecondUpdated = -1;

  function tickFrame() {
    const now = new Date();
    const epochSec = Math.floor(now.getTime() / 1000);
    const parts = formatTimeParts(now, state.use24Hour);

    if (heroTimeMain) heroTimeMain.textContent = parts.main;
    if (heroTimeFrac && state.showCentiseconds) heroTimeFrac.textContent = parts.frac;
    if (heroTimeAmPm) heroTimeAmPm.textContent = parts.ampm;

    if (standbyTimeMain) standbyTimeMain.textContent = parts.main;
    if (standbyTimeFrac && state.showCentiseconds) standbyTimeFrac.textContent = parts.frac;

    if (heroSecondFill) {
      const pct = ((now.getSeconds() + now.getMilliseconds() / 1000) / 60) * 100;
      heroSecondFill.style.width = `${pct.toFixed(2)}%`;
    }

    drawPrecisionDial(now);

    if (epochSec !== lastSecondUpdated) {
      lastSecondUpdated = epochSec;

      if (heroDateTz) heroDateTz.textContent = formatDateAndTz(now);
      if (standbyDate) {
        standbyDate.textContent = now
          .toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })
          .toUpperCase();
      }
      if (footerUtcClock) {
        footerUtcClock.textContent = `UTC ${pad2(now.getUTCHours())}:${pad2(now.getUTCMinutes())}:${pad2(now.getUTCSeconds())}`;
      }

      updateAlarmCountdown(now);
      updateEphemeris(now);

      // Audio tick cadence
      if (state.tickCadenceSeconds > 0 && epochSec !== state.lastTickEpochSecond) {
        const secOfMin = now.getSeconds();
        if (secOfMin % state.tickCadenceSeconds === 0) {
          state.lastTickEpochSecond = epochSec;
          playEscapementTick(secOfMin === 0);
        }
      }
    }

    requestAnimationFrame(tickFrame);
  }

  // Initial render
  renderAlarmPreset('market');
  updateEphemeris(new Date());
  requestAnimationFrame(tickFrame);
})();
