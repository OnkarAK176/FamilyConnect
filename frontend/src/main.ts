import './style.css'

const API_BASE = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '')
const SIGNAL_URL = `${API_BASE.replace(/^http/, 'ws').replace(/\/api$/, '')}/signal`
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
      <p class="intro">Create a call and send the private link to your brother. He can join from his phone or computer.</p>
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
        <span class="connection-state" id="connectionState" role="status">Connecting…</span>
      </header>
      <div class="video-grid">
        <article class="video-tile local-tile">
          <video id="localVideo" autoplay playsinline muted></video>
          <span class="video-name" id="localName">You</span>
        </article>
        <article class="video-tile remote-tile" id="remoteTile">
          <video id="remoteVideo" autoplay playsinline></video>
          <div class="waiting-message" id="waitingMessage">
            <div class="waiting-avatar">?</div>
            <p>Waiting for your brother to join…</p>
          </div>
          <span class="video-name" id="remoteName">Family</span>
        </article>
      </div>
      <div class="call-actions">
        <button class="control" id="micButton" type="button" aria-pressed="false">Mute mic</button>
        <button class="control" id="cameraButton" type="button" aria-pressed="false">Turn camera off</button>
        <button class="control share-button" id="copyLink" type="button">Copy invite link</button>
        <button class="control danger" id="leaveCall" type="button">Leave call</button>
      </div>
      <p class="call-status" id="callStatus" aria-live="polite"></p>
    </section>

    <footer>Calls connect directly between your devices. Keep the invite link private.</footer>
  </main>
`

const $ = <T extends HTMLElement>(selector: string) => document.querySelector<T>(selector)
const joinCard = $('#joinCard')!
const callScreen = $('#callScreen')!
const displayNameInput = $('#displayName') as HTMLInputElement
const startButton = $('#startCall')!
const joinButton = $('#joinCall')!
const welcomeTitle = $('#welcomeTitle')!
const callStatus = $('#callStatus')!
const joinStatus = $('#joinStatus')!
const connectionState = $('#connectionState')!
const localVideo = $('#localVideo') as HTMLVideoElement
const remoteVideo = $('#remoteVideo') as HTMLVideoElement
const remoteTile = $('#remoteTile')!
const waitingMessage = $('#waitingMessage')!
const micButton = $('#micButton')!
const cameraButton = $('#cameraButton')!
const inviteButton = $('#copyLink')!
const roomCode = new URLSearchParams(location.search).get('room')

let localStream: MediaStream | undefined
let remoteStream: MediaStream | undefined
let peerConnection: RTCPeerConnection | undefined
let signalingSocket: WebSocket | undefined
let activeRoom = ''
let activeName = ''
let shuttingDown = false
let pendingCandidates: RTCIceCandidateInit[] = []

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

function sendSignal(message: Record<string, unknown>) {
  if (signalingSocket?.readyState !== WebSocket.OPEN) {
    throw new Error('The call connection was interrupted. Please leave and rejoin.')
  }
  signalingSocket.send(JSON.stringify(message))
}

async function flushCandidates() {
  if (!peerConnection?.remoteDescription) return
  for (const candidate of pendingCandidates) {
    await peerConnection.addIceCandidate(candidate)
  }
  pendingCandidates = []
}

async function createPeerConnection() {
  if (peerConnection) return peerConnection

  const connection = new RTCPeerConnection({
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
  })
  peerConnection = connection
  remoteStream = new MediaStream()
  remoteVideo.srcObject = remoteStream

  for (const track of localStream!.getTracks()) {
    connection.addTrack(track, localStream!)
  }

  connection.ontrack = (event) => {
    for (const track of event.streams[0]?.getTracks() ?? [event.track]) {
      if (!remoteStream!.getTracks().some((existing) => existing.id === track.id)) {
        remoteStream!.addTrack(track)
      }
    }
    waitingMessage.setAttribute('hidden', '')
    remoteTile.classList.add('has-video')
  }

  connection.onicecandidate = (event) => {
    if (event.candidate) {
      try {
        sendSignal({ type: 'candidate', candidate: event.candidate.toJSON() })
      } catch (error) {
        setConnectionState('Call disconnected', true)
        setCallStatus(error instanceof Error ? error.message : 'Call signaling stopped.', true)
      }
    }
  }

  connection.onconnectionstatechange = () => {
    if (connection.connectionState === 'connected') {
      setConnectionState('Connected')
      setCallStatus(`You’re talking with ${$('#remoteName')!.textContent}.`)
    } else if (connection.connectionState === 'failed') {
      setConnectionState('Connection failed', true)
      setCallStatus('A direct connection could not be made on this network. Try another network or contact support.', true)
    } else if (connection.connectionState === 'disconnected') {
      setConnectionState('Reconnecting…')
    }
  }

  return connection
}

async function handleSignal(event: MessageEvent<string>) {
  const message = JSON.parse(event.data) as {
    type: string
    candidate?: RTCIceCandidateInit
    description?: RTCSessionDescriptionInit
  }

  if (message.type === 'peer-present') {
    setConnectionState('Your brother is in the call')
    return
  }
  if (message.type === 'peer-joined') {
    setConnectionState('Calling your brother…')
    const connection = await createPeerConnection()
    const offer = await connection.createOffer()
    await connection.setLocalDescription(offer)
    sendSignal({ type: 'offer', description: connection.localDescription })
    return
  }
  if (message.type === 'peer-left') {
    setConnectionState('Your brother left the call')
    setCallStatus('Your brother has left the call.')
    remoteStream?.getTracks().forEach((track) => track.stop())
    remoteStream = new MediaStream()
    remoteVideo.srcObject = remoteStream
    waitingMessage.removeAttribute('hidden')
    remoteTile.classList.remove('has-video')
    peerConnection?.close()
    peerConnection = undefined
    return
  }

  const connection = await createPeerConnection()
  if (message.type === 'offer' && message.description) {
    await connection.setRemoteDescription(message.description)
    await flushCandidates()
    const answer = await connection.createAnswer()
    await connection.setLocalDescription(answer)
    sendSignal({ type: 'answer', description: connection.localDescription })
  } else if (message.type === 'answer' && message.description) {
    await connection.setRemoteDescription(message.description)
    await flushCandidates()
  } else if (message.type === 'candidate' && message.candidate) {
    if (connection.remoteDescription) {
      await connection.addIceCandidate(message.candidate)
    } else {
      pendingCandidates.push(message.candidate)
    }
  }
}

async function joinRoom(room: string) {
  activeName = displayNameInput.value.trim()
  if (!activeName) {
    displayNameInput.focus()
    joinStatus.textContent = 'Enter your name to join the call.'
    joinStatus.classList.add('error')
    return
  }

  shuttingDown = false
  activeRoom = room
  joinStatus.textContent = ''
  joinStatus.classList.remove('error')
  setCallStatus('')
  joinCard.setAttribute('hidden', '')
  callScreen.removeAttribute('hidden')
  $('#roomLabel')!.textContent = `Family call · ${room.slice(0, 6)}`
  $('#localName')!.textContent = `${activeName} (you)`
  $('#remoteName')!.textContent = 'Your brother'
  setConnectionState('Requesting camera and microphone…')

  try {
    localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true })
    localVideo.srcObject = localStream
    setConnectionState('Waking the call server…')
    const health = await fetch(`${API_BASE}/health`)
    if (!health.ok) throw new Error('The call server did not become ready. Please retry in a moment.')
    setConnectionState('Connecting to the call…')

    signalingSocket = new WebSocket(`${SIGNAL_URL}?room=${encodeURIComponent(room)}`)
    signalingSocket.addEventListener('open', () => {
      setConnectionState('Waiting for your brother…')
      inviteButton.removeAttribute('disabled')
    })
    signalingSocket.addEventListener('message', (event: MessageEvent<string>) => {
      void handleSignal(event).catch((error: unknown) => {
        setConnectionState('Could not establish the call', true)
        setCallStatus(error instanceof Error ? error.message : 'Call setup failed.', true)
      })
    })
    signalingSocket.addEventListener('error', () => {
      setConnectionState('Call server unavailable', true)
      setCallStatus('Could not connect to the call server. Please try again in a moment.', true)
    })
    signalingSocket.addEventListener('close', () => {
      if (!shuttingDown) {
        setConnectionState('Call disconnected', true)
        setCallStatus('The call server disconnected. Please leave and rejoin.', true)
      }
    })
  } catch (error) {
    setConnectionState('Could not join the call', true)
    setCallStatus(
      error instanceof DOMException && error.name === 'NotAllowedError'
        ? 'Camera or microphone permission was denied. Allow access in your browser settings, then try again.'
        : error instanceof Error
          ? error.message
          : 'Could not start your camera and microphone.',
      true,
    )
    localStream?.getTracks().forEach((track) => track.stop())
    localStream = undefined
    callScreen.setAttribute('hidden', '')
    joinCard.removeAttribute('hidden')
  }
}

function leaveCall() {
  shuttingDown = true
  signalingSocket?.close()
  peerConnection?.close()
  localStream?.getTracks().forEach((track) => track.stop())
  remoteStream?.getTracks().forEach((track) => track.stop())
  signalingSocket = undefined
  peerConnection = undefined
  localStream = undefined
  remoteStream = undefined
  pendingCandidates = []
  localVideo.srcObject = null
  remoteVideo.srcObject = null
  waitingMessage.removeAttribute('hidden')
  remoteTile.classList.remove('has-video')
  callScreen.setAttribute('hidden', '')
  joinCard.removeAttribute('hidden')
  setCallStatus('')
  if (roomCode) {
    location.assign(location.pathname)
  } else {
    activeRoom = ''
    history.replaceState(null, '', location.pathname)
  }
}

startButton.addEventListener('click', () => {
  const room = crypto.randomUUID().replaceAll('-', '')
  history.replaceState(null, '', `${location.pathname}?room=${room}`)
  void joinRoom(room)
})

joinButton.addEventListener('click', () => {
  if (roomCode && /^[a-f0-9]{32}$/.test(roomCode)) void joinRoom(roomCode)
})

micButton.addEventListener('click', () => {
  const track = localStream?.getAudioTracks()[0]
  if (!track) return
  track.enabled = !track.enabled
  micButton.textContent = track.enabled ? 'Mute mic' : 'Unmute mic'
  micButton.setAttribute('aria-pressed', String(!track.enabled))
})

cameraButton.addEventListener('click', () => {
  const track = localStream?.getVideoTracks()[0]
  if (!track) return
  track.enabled = !track.enabled
  cameraButton.textContent = track.enabled ? 'Turn camera off' : 'Turn camera on'
  cameraButton.setAttribute('aria-pressed', String(!track.enabled))
})

inviteButton.addEventListener('click', async () => {
  const inviteUrl = `${location.origin}${location.pathname}?room=${activeRoom}`
  try {
    await navigator.clipboard.writeText(inviteUrl)
    setCallStatus('Invite link copied. Send it to your brother.')
  } catch {
    setCallStatus(`Copy this invite link and send it to your brother: ${inviteUrl}`)
  }
})

$('#leaveCall')!.addEventListener('click', leaveCall)
