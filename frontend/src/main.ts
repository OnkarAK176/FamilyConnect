import type { RemoteParticipant, Room, RoomEvent, Track } from 'livekit-client'
import './style.css'

type CallTokenResponse = {
  serverUrl: string
  participantToken: string
  participantIdentity: string
}

const API_BASE = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '')
const app = document.querySelector<HTMLDivElement>('#app')

if (!app) {
  throw new Error('Missing #app element')
}

app.innerHTML = `
  <main class="call-app">
    <header class="brand">
      <div class="brand-mark">F</div>
      <div><p class="eyebrow">Private family calls</p><h1>FamilyConnect</h1></div>
    </header>

    <section class="join-card" id="joinCard">
      <p class="eyebrow">A little closer, wherever you are</p>
      <h2 id="welcomeTitle">Start a family call</h2>
      <p class="intro">Invite your family to join you in a secure video call.</p>
      <label for="displayName">Your name</label>
      <input id="displayName" maxlength="60" autocomplete="name" placeholder="Enter your name" />
      <div class="join-actions">
        <button class="primary-button" id="startCall" type="button">Start a call</button>
        <button class="primary-button" id="joinCall" type="button" hidden>Join this call</button>
      </div>
      <p class="call-status" id="joinStatus" aria-live="polite"></p>
      <p class="hint">Your browser will ask for camera and microphone permission when you join.</p>
    </section>

    <section class="call-screen" id="callScreen" hidden>
      <header class="call-header">
        <div><p class="eyebrow">Family room</p><h2 id="roomLabel">Call</h2></div>
        <div class="call-meta">
          <span class="connection-state" id="connectionState" role="status">Connecting…</span>
        </div>
      </header>
      <div class="video-grid" id="participantGrid">
        <article class="video-tile local-tile">
          <video id="localVideo" autoplay playsinline muted></video>
          <div class="camera-placeholder">Camera off</div>
          <span class="video-name" id="localName">You</span>
        </article>
      </div>
      <div class="call-actions">
        <button class="control" id="micButton" type="button" aria-label="Mute mic" aria-pressed="false">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 15a3 3 0 0 0 3-3V5a3 3 0 0 0-6 0v7a3 3 0 0 0 3 3Z"/><path d="M19 11v1a7 7 0 0 1-14 0v-1M12 19v3m-4 0h8"/></svg>
          <span class="control-label">Mute mic</span>
        </button>
        <button class="control" id="cameraButton" type="button" aria-label="Turn camera off" aria-pressed="false">
          <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="6" width="13" height="12" rx="2"/><path d="m16 10 5-3v10l-5-3"/></svg>
          <span class="control-label">Camera</span>
        </button>
        <button class="control share-button" id="copyLink" type="button" aria-label="Copy invite link">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 13a5 5 0 0 0 7.1 0l3-3A5 5 0 0 0 13 2.9l-1.7 1.7"/><path d="M14 11a5 5 0 0 0-7.1 0l-3 3A5 5 0 0 0 11 21.1l1.7-1.7"/></svg>
          <span class="control-label">Copy link</span>
        </button>
        <button class="control danger" id="leaveCall" type="button" aria-label="Leave call">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 14.5a13 13 0 0 1 16 0l1.2 1a1 1 0 0 1 .2 1.4l-1.7 2.2a1 1 0 0 1-1.2.3l-3.2-1.4a1 1 0 0 1-.6-.9v-1.3a10 10 0 0 0-5.4 0v1.3a1 1 0 0 1-.6.9l-3.2 1.4a1 1 0 0 1-1.2-.3L2.6 17a1 1 0 0 1 .2-1.4Z"/></svg>
          <span class="control-label">Leave</span>
        </button>
      </div>
      <p class="call-status" id="callStatus" aria-live="polite"></p>
    </section>

    <footer>Calls use an encrypted media connection. Keep the invite link private.</footer>
  </main>
`

const $ = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector)
const joinCard = $('#joinCard')!
const callScreen = $('#callScreen')!
const displayNameInput = $('#displayName') as HTMLInputElement
const startButton = $('#startCall')!
const joinButton = $('#joinCall')!
const callApp = $('.call-app')!
const welcomeTitle = $('#welcomeTitle')!
const callStatus = $('#callStatus')!
const joinStatus = $('#joinStatus')!
const connectionState = $('#connectionState')!
const participantGrid = $('#participantGrid')!
const localVideo = $('#localVideo') as HTMLVideoElement
const localName = $('#localName')!
const micButton = $('#micButton')!
const cameraButton = $('#cameraButton')!
const micButtonLabel = micButton.querySelector<HTMLElement>('.control-label')!
const cameraButtonLabel = cameraButton.querySelector<HTMLElement>('.control-label')!
const roomCode = new URLSearchParams(location.search).get('room')
const participantTiles = new Map<string, HTMLVideoElement>()
const remoteAudioElements = new Map<string, HTMLMediaElement[]>()

let room: Room | undefined
let activeRoom = ''
let activeName = ''
let muted = false
let cameraOff = false
let shuttingDown = false
let releaseRoomLock: (() => void) | undefined

if (roomCode && /^[a-f0-9]{32}$/.test(roomCode)) {
  welcomeTitle.textContent = 'You’ve been invited to a family call'
  startButton.setAttribute('hidden', '')
  joinButton.removeAttribute('hidden')
} else if (roomCode) {
  joinStatus.textContent = 'This invite link is invalid. Ask your family member to send a new one.'
  joinStatus.classList.add('error')
}

function setConnectionState(message: string, error = false) {
  connectionState.textContent = message
  connectionState.classList.toggle('error', error)
}

function setCallStatus(message: string, error = false) {
  callStatus.textContent = message
  callStatus.classList.toggle('error', error)
}

function createParticipantTile(participant: RemoteParticipant) {
  const existing = participantTiles.get(participant.identity)
  if (existing) return existing

  const tile = document.createElement('article')
  tile.className = 'video-tile participant-tile'
  tile.dataset.participant = participant.identity

  const video = document.createElement('video')
  video.autoplay = true
  video.playsInline = true
  tile.append(video)

  const waiting = document.createElement('div')
  waiting.className = 'waiting-message'
  const avatar = document.createElement('div')
  avatar.className = 'waiting-avatar'
  avatar.textContent = (participant.name || 'F').trim().charAt(0).toUpperCase()
  const label = document.createElement('p')
  label.className = 'waiting-label'
  label.textContent = participant.name || 'Family member'
  waiting.append(avatar, label)
  tile.append(waiting)

  const name = document.createElement('span')
  name.className = 'video-name'
  name.textContent = participant.name || 'Family member'
  tile.append(name)

  participantGrid.append(tile)
  participantTiles.set(participant.identity, video)
  return video
}

function removeParticipant(participant: RemoteParticipant) {
  participantTiles.get(participant.identity)?.closest('.video-tile')?.remove()
  participantTiles.delete(participant.identity)
  for (const element of remoteAudioElements.get(participant.identity) ?? []) {
    element.remove()
  }
  remoteAudioElements.delete(participant.identity)
}

function subscribeTrack(track: Track, participant: RemoteParticipant) {
  if (track.kind === 'video') {
    const video = createParticipantTile(participant)
    track.attach(video)
    video.closest('.video-tile')?.classList.add('has-video')
    return
  }

  const audio = track.attach()
  audio.classList.add('remote-audio')
  document.body.append(audio)
  const elements = remoteAudioElements.get(participant.identity) ?? []
  elements.push(audio)
  remoteAudioElements.set(participant.identity, elements)
}

function unsubscribeTrack(track: Track, participant: RemoteParticipant) {
  const elements = track.detach()
  for (const element of elements) {
    if (element instanceof HTMLAudioElement) {
      element.remove()
      const remaining = (remoteAudioElements.get(participant.identity) ?? []).filter((audio) => audio !== element)
      if (remaining.length) remoteAudioElements.set(participant.identity, remaining)
      else remoteAudioElements.delete(participant.identity)
    } else if (element instanceof HTMLVideoElement) {
      const tile = element.closest('.video-tile')
      tile?.classList.remove('has-video')
      element.srcObject = null
    }
  }
}

function registerRoomEvents(callRoom: Room, roomEvents: typeof RoomEvent) {
  callRoom
    .on(roomEvents.Connected, () => {
      setConnectionState('Connected')
      setCallStatus('Your family call is ready. Share the invite link to bring everyone in.')
    })
    .on(roomEvents.Reconnecting, () => setConnectionState('Reconnecting…'))
    .on(roomEvents.Reconnected, () => setConnectionState('Connected'))
    .on(roomEvents.Disconnected, () => {
      if (!shuttingDown) {
        setConnectionState('Disconnected', true)
        setCallStatus('You were disconnected from the call. Please leave and rejoin.', true)
      }
    })
    .on(roomEvents.ConnectionStateChanged, (state) => {
      if (state === 'connecting') setConnectionState('Connecting…')
    })
    .on(roomEvents.ParticipantConnected, (participant) => {
      createParticipantTile(participant)
    })
    .on(roomEvents.ParticipantDisconnected, (participant) => {
      removeParticipant(participant)
    })
    .on(roomEvents.TrackSubscribed, (track, _publication, participant) => {
      subscribeTrack(track, participant)
    })
    .on(roomEvents.TrackUnsubscribed, (track, _publication, participant) => {
      unsubscribeTrack(track, participant)
    })
    .on(roomEvents.LocalTrackPublished, (publication) => {
      if (publication.track?.kind === 'video') {
        publication.track.attach(localVideo)
        localVideo.closest('.video-tile')?.classList.toggle('has-video', !cameraOff)
      }
    })
    .on(roomEvents.LocalTrackUnpublished, (publication) => {
      if (publication.track?.kind === 'video') {
        publication.track.detach(localVideo)
        localVideo.srcObject = null
        localVideo.closest('.video-tile')?.classList.remove('has-video')
      }
    })
    .on(roomEvents.TrackMuted, (_publication, participant) => {
      if (participant.isLocal) return
      const video = participantTiles.get(participant.identity)
      if (video && !participant.isCameraEnabled) video.closest('.video-tile')?.classList.remove('has-video')
    })
    .on(roomEvents.TrackUnmuted, (_publication, participant) => {
      if (participant.isLocal) return
      const video = participantTiles.get(participant.identity)
      if (video && participant.isCameraEnabled) video.closest('.video-tile')?.classList.add('has-video')
    })
}

async function joinRoom(roomName: string) {
  activeName = displayNameInput.value.trim()
  if (!activeName) {
    displayNameInput.focus()
    joinStatus.textContent = 'Enter your name to join the call.'
    joinStatus.classList.add('error')
    return
  }

  if ('locks' in navigator) {
    const lockState = await new Promise<{ acquired: boolean; error?: unknown }>((resolve) => {
      void navigator.locks.request(
        `familyconnect-call-${roomName}`,
        { mode: 'exclusive', ifAvailable: true },
        async (lock) => {
          resolve({ acquired: Boolean(lock) })
          if (lock) {
            await new Promise<void>((release) => {
              releaseRoomLock = release
            })
          }
        },
      ).catch((error: unknown) => {
        resolve({ acquired: false, error })
      })
    })
    if (!lockState.acquired) {
      joinStatus.textContent = lockState.error instanceof Error
        ? `Could not secure this call tab: ${lockState.error.message}`
        : 'This call is already open in another tab on this device. Close that tab before joining here.'
      joinStatus.classList.add('error')
      return
    }
  }

  shuttingDown = false
  activeRoom = roomName
  joinStatus.textContent = ''
  joinStatus.classList.remove('error')
  setCallStatus('')
  joinCard.setAttribute('hidden', '')
  callScreen.removeAttribute('hidden')
  callApp.classList.add('in-call')
  document.body.classList.add('in-call')
  $('#roomLabel')!.textContent = `Family call · ${roomName.slice(0, 6)}`
  localName.textContent = `${activeName} (you)`
  setConnectionState('Connecting to the call…')

  try {
    const response = await fetch(`${API_BASE}/calls/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roomName, participantName: activeName }),
    })
    if (!response.ok) {
      const body = await response.text()
      let message = body
      try {
        const errorBody = JSON.parse(body) as { message?: string; detail?: string }
        message = errorBody.message ?? errorBody.detail ?? body
      } catch {
        // Keep the server response text when it is not a JSON error.
      }
      throw new Error(message || `Could not prepare call (${response.status}).`)
    }

    const credentials = await response.json() as CallTokenResponse
    const liveKit = await import('livekit-client')
    const callRoom = new liveKit.Room({ adaptiveStream: true, dynacast: true })
    room = callRoom
    registerRoomEvents(callRoom, liveKit.RoomEvent)

    await callRoom.prepareConnection(credentials.serverUrl, credentials.participantToken)
    await callRoom.connect(credentials.serverUrl, credentials.participantToken)
    const localTracks = await liveKit.createLocalTracks({
      audio: true,
      video: { resolution: liveKit.VideoPresets.h360.resolution, frameRate: 20 },
    })
    for (const track of localTracks) {
      await callRoom.localParticipant.publishTrack(track)
      if (track.kind === 'video') {
        track.attach(localVideo)
        localVideo.closest('.video-tile')?.classList.add('has-video')
      }
    }
  } catch (error) {
    await room?.disconnect()
    room = undefined
    releaseRoomLock?.()
    releaseRoomLock = undefined
    setConnectionState('Could not join the call', true)
    const message =
      error instanceof DOMException && error.name === 'NotAllowedError'
        ? 'Camera or microphone permission was denied. Allow access in your browser settings, then try again.'
        : error instanceof Error
          ? error.message
          : 'Could not start the call. Please try again.'
    localVideo.srcObject = null
    localVideo.closest('.video-tile')?.classList.remove('has-video')
    callScreen.setAttribute('hidden', '')
    callApp.classList.remove('in-call')
    document.body.classList.remove('in-call')
    joinCard.removeAttribute('hidden')
    joinStatus.textContent = message
    joinStatus.classList.add('error')
  }
}

async function leaveCall() {
  shuttingDown = true
  await room?.disconnect()
  room = undefined
  releaseRoomLock?.()
  releaseRoomLock = undefined
  for (const video of participantTiles.values()) {
    video.closest('.video-tile')?.remove()
  }
  participantTiles.clear()
  for (const elements of remoteAudioElements.values()) {
    for (const element of elements) element.remove()
  }
  remoteAudioElements.clear()
  localVideo.srcObject = null
  localVideo.closest('.video-tile')?.classList.remove('has-video')
  callScreen.setAttribute('hidden', '')
  callApp.classList.remove('in-call')
  document.body.classList.remove('in-call')
  joinCard.removeAttribute('hidden')
  setCallStatus('')
  muted = false
  cameraOff = false
  micButtonLabel.textContent = 'Mute mic'
  cameraButtonLabel.textContent = 'Camera'
  micButton.setAttribute('aria-label', 'Mute mic')
  cameraButton.setAttribute('aria-label', 'Turn camera off')
  micButton.setAttribute('aria-pressed', 'false')
  cameraButton.setAttribute('aria-pressed', 'false')

  if (roomCode) {
    location.assign(location.pathname)
  } else {
    activeRoom = ''
    history.replaceState(null, '', location.pathname)
  }
}

startButton.addEventListener('click', () => {
  const newRoom = crypto.randomUUID().replaceAll('-', '')
  history.replaceState(null, '', `${location.pathname}?room=${newRoom}`)
  void joinRoom(newRoom)
})

joinButton.addEventListener('click', () => {
  if (roomCode && /^[a-f0-9]{32}$/.test(roomCode)) void joinRoom(roomCode)
})

micButton.addEventListener('click', () => {
  if (!room) return
  muted = !muted
  void room.localParticipant.setMicrophoneEnabled(!muted).then(() => {
    micButtonLabel.textContent = muted ? 'Unmute' : 'Mute mic'
    micButton.setAttribute('aria-label', muted ? 'Unmute mic' : 'Mute mic')
    micButton.setAttribute('aria-pressed', String(muted))
  }).catch((error: unknown) => {
    muted = !muted
    setCallStatus(error instanceof Error ? error.message : 'Could not change microphone state.', true)
  })
})

cameraButton.addEventListener('click', () => {
  if (!room) return
  cameraOff = !cameraOff
  void room.localParticipant.setCameraEnabled(!cameraOff).then(() => {
    cameraButtonLabel.textContent = cameraOff ? 'Camera on' : 'Camera'
    cameraButton.setAttribute('aria-label', cameraOff ? 'Turn camera on' : 'Turn camera off')
    cameraButton.setAttribute('aria-pressed', String(cameraOff))
    localVideo.closest('.video-tile')?.classList.toggle('has-video', !cameraOff)
  }).catch((error: unknown) => {
    cameraOff = !cameraOff
    setCallStatus(error instanceof Error ? error.message : 'Could not change camera state.', true)
  })
})

$('#copyLink')!.addEventListener('click', async () => {
  const inviteUrl = `${location.origin}${location.pathname}?room=${activeRoom}`
  try {
    await navigator.clipboard.writeText(inviteUrl)
    setCallStatus('Invite link copied. Send it privately to your family.')
  } catch {
    setCallStatus(`Copy this invite link and send it to your family: ${inviteUrl}`)
  }
})

$('#leaveCall')!.addEventListener('click', () => void leaveCall())
