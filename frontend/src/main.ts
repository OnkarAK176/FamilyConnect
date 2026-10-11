import './style.css'

type MemberResponse = {
  familyId: string
  email: string
  fullName: string
  role: string
  status: string
}

type InviteResponse = {
  familyId: string
  token: string
  createdByEmail: string
  expiresAt: string
}

type RoomResponse = {
  id: number
  familyId: string
  name: string
  roomCode: string
  status: string
  createdAt: string
}

type TokenResponse = {
  roomCode: string
  familyId: string
  email: string
  token: string
  expiresAt: string
}

const API_BASE = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '')

const app = document.querySelector<HTMLDivElement>('#app')

if (!app) {
  throw new Error('Missing #app element')
}

app.innerHTML = `
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand-wrap">
        <div class="brand-mark">F</div>
        <div>
          <p class="eyebrow">Family</p>
          <h1>FamilyConnect</h1>
        </div>
      </div>

      <div class="field-group">
        <label for="familyIdInput">Family ID</label>
        <input id="familyIdInput" type="text" value="family-demo" />
      </div>

      <div class="field-group">
        <label for="memberEmailInput">Member email</label>
        <input id="memberEmailInput" type="email" value="ava@example.com" />
      </div>

      <button class="primary-button" id="joinMeeting">Join meeting</button>
      <button class="secondary-button" id="inviteMembers">Invite family</button>

      <div class="mini-card">
        <span class="chip success">Live</span>
        <strong>Family room</strong>
        <p id="roomSummaryText">Sunday dinner • room ready</p>
      </div>
    </aside>

    <main class="meeting-panel">
      <header class="topbar">
        <div>
          <p class="eyebrow">Room</p>
          <h2 id="roomTitle">Sunday dinner / family reunion</h2>
        </div>
        <span class="status-badge" id="statusBadge">Secure • ready</span>
      </header>

      <section class="video-stage">
        <div class="speaker-card focus">
          <span class="avatar">AP</span>
          <div>
            <h3>Ava Patel</h3>
            <small>Host</small>
          </div>
        </div>
        <div class="speaker-card muted">
          <span class="avatar alt">LP</span>
          <div>
            <h3>Luca Patel</h3>
            <small>Talking</small>
          </div>
        </div>
        <div class="speaker-card muted">
          <span class="avatar alt2">RM</span>
          <div>
            <h3>Riya Mehta</h3>
            <small>Listening</small>
          </div>
        </div>
      </section>

      <section class="controls-row">
        <button class="control toggled" type="button">Mic</button>
        <button class="control" type="button">Video</button>
        <button class="control" type="button">Screen</button>
        <button class="control danger" type="button">Leave</button>
      </section>

      <section class="panel-grid">
        <div class="info-card">
          <h3>Create family admin</h3>
          <div class="field-grid">
            <input id="adminEmail" type="email" placeholder="admin@example.com" value="ava@example.com" />
            <input id="adminName" type="text" placeholder="Full name" value="Ava Patel" />
          </div>
          <button class="primary-button" id="createAdmin" type="button">Create admin</button>
        </div>

        <div class="info-card">
          <h3>Create invite</h3>
          <div class="field-grid">
            <input id="inviteFamilyId" type="text" placeholder="Family ID" value="family-demo" />
            <input id="inviteAdminEmail" type="email" placeholder="Admin email" value="ava@example.com" />
          </div>
          <button class="secondary-button" id="createInvite" type="button">Create invite</button>
        </div>

        <div class="info-card">
          <h3>Accept invite</h3>
          <div class="field-grid">
            <input id="inviteToken" type="text" placeholder="Invite token" />
            <input id="acceptInviteEmail" type="email" placeholder="New member email" value="luca@example.com" />
            <input id="acceptInviteName" type="text" placeholder="New member name" value="Luca Patel" />
          </div>
          <button class="secondary-button" id="acceptInvite" type="button">Accept invite</button>
        </div>

        <div class="info-card">
          <h3>Create room</h3>
          <div class="field-grid">
            <input id="roomFamilyId" type="text" placeholder="Family ID" value="family-demo" />
            <input id="roomName" type="text" placeholder="Room name" value="Sunday dinner" />
            <input id="roomAdminEmail" type="email" placeholder="Admin email" value="ava@example.com" />
          </div>
          <button class="primary-button" id="createRoom" type="button">Create room</button>
        </div>

        <div class="info-card">
          <h3>Join token</h3>
          <div class="field-grid">
            <input id="tokenFamilyId" type="text" placeholder="Family ID" value="family-demo" />
            <input id="tokenEmail" type="email" placeholder="Member email" value="luca@example.com" />
          </div>
          <button class="secondary-button" id="createToken" type="button">Create token</button>
        </div>
      </section>

      <div class="status-panel" id="statusPanel" aria-live="polite">
        Prototype connected to localhost:8080 and ready for family meeting flows.
      </div>
    </main>
  </div>
`

const statusBadge = document.querySelector<HTMLSpanElement>('#statusBadge')
const statusPanel = document.querySelector<HTMLDivElement>('#statusPanel')
const roomSummaryText = document.querySelector<HTMLParagraphElement>('#roomSummaryText')
const roomTitle = document.querySelector<HTMLHeadingElement>('#roomTitle')

const familyIdInput = document.querySelector<HTMLInputElement>('#familyIdInput')
const memberEmailInput = document.querySelector<HTMLInputElement>('#memberEmailInput')
const inviteFamilyId = document.querySelector<HTMLInputElement>('#inviteFamilyId')
const inviteAdminEmail = document.querySelector<HTMLInputElement>('#inviteAdminEmail')
const inviteToken = document.querySelector<HTMLInputElement>('#inviteToken')
const acceptInviteEmail = document.querySelector<HTMLInputElement>('#acceptInviteEmail')
const acceptInviteName = document.querySelector<HTMLInputElement>('#acceptInviteName')
const roomFamilyId = document.querySelector<HTMLInputElement>('#roomFamilyId')
const roomName = document.querySelector<HTMLInputElement>('#roomName')
const roomAdminEmail = document.querySelector<HTMLInputElement>('#roomAdminEmail')
const tokenFamilyId = document.querySelector<HTMLInputElement>('#tokenFamilyId')
const tokenEmail = document.querySelector<HTMLInputElement>('#tokenEmail')

const adminEmail = document.querySelector<HTMLInputElement>('#adminEmail')
const adminName = document.querySelector<HTMLInputElement>('#adminName')

function updateStatus(message: string, isError = false) {
  if (!statusPanel) return
  statusPanel.textContent = message
  statusPanel.classList.toggle('error', isError)
}

async function requestJson<T>(endpoint: string, payload: Record<string, string>): Promise<T> {
  const response = await fetch(`${API_BASE}${endpoint}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  const text = await response.text()
  if (!response.ok) {
    throw new Error(text || `Request failed with status ${response.status}`)
  }

  return text ? (JSON.parse(text) as T) : ({} as T)
}

function syncFamilyContext(familyId?: string, email?: string) {
  if (familyId && familyIdInput) familyIdInput.value = familyId
  if (email && memberEmailInput) memberEmailInput.value = email
  if (familyId && inviteFamilyId) inviteFamilyId.value = familyId
  if (familyId && roomFamilyId) roomFamilyId.value = familyId
  if (familyId && tokenFamilyId) tokenFamilyId.value = familyId
}

async function createAdmin() {
  if (!adminEmail || !adminName) return

  try {
    const result = await requestJson<MemberResponse>('/family/admins', {
      email: adminEmail.value,
      fullName: adminName.value,
    })
    syncFamilyContext(result.familyId, result.email)
    updateStatus(`Admin ready: ${result.email} (${result.familyId})`)
    if (statusBadge) statusBadge.textContent = 'Secure • family ready'
    if (roomSummaryText) roomSummaryText.textContent = `${result.fullName} • family active`
  } catch (error) {
    updateStatus(error instanceof Error ? error.message : 'Could not create admin.', true)
  }
}

async function createInvite() {
  try {
    const familyId = inviteFamilyId?.value || familyIdInput?.value || 'family-demo'
    const adminEmailValue = inviteAdminEmail?.value || adminEmail?.value || 'ava@example.com'
    const result = await requestJson<InviteResponse>('/family/invite', {
      familyId,
      adminEmail: adminEmailValue,
    })
    if (inviteToken) inviteToken.value = result.token
    updateStatus(`Invite created for ${familyId}. Token: ${result.token}`)
    syncFamilyContext(familyId, adminEmailValue)
  } catch (error) {
    updateStatus(error instanceof Error ? error.message : 'Could not create invite.', true)
  }
}

async function acceptInvite() {
  try {
    const result = await requestJson<MemberResponse>('/family/invite/accept', {
      token: inviteToken?.value || '',
      email: acceptInviteEmail?.value || 'luca@example.com',
      fullName: acceptInviteName?.value || 'Luca Patel',
    })
    syncFamilyContext(result.familyId, result.email)
    updateStatus(`Accepted invite for ${result.email} in family ${result.familyId}`)
  } catch (error) {
    updateStatus(error instanceof Error ? error.message : 'Could not accept invite.', true)
  }
}

async function createRoom() {
  try {
    const familyId = roomFamilyId?.value || familyIdInput?.value || 'family-demo'
    const adminEmailValue = roomAdminEmail?.value || adminEmail?.value || 'ava@example.com'
    const roomLabel = roomName?.value || 'Sunday dinner'
    const result = await requestJson<RoomResponse>('/rooms', {
      familyId,
      name: roomLabel,
      adminEmail: adminEmailValue,
    })
    if (roomTitle) roomTitle.textContent = `${result.name} / family reunion`
    if (roomSummaryText) roomSummaryText.textContent = `${result.name} • ${result.roomCode}`
    updateStatus(`Room created: ${result.name} (${result.roomCode})`)
    syncFamilyContext(familyId, adminEmailValue)
  } catch (error) {
    updateStatus(error instanceof Error ? error.message : 'Could not create room.', true)
  }
}

async function createToken() {
  try {
    const familyId = tokenFamilyId?.value || familyIdInput?.value || 'family-demo'
    const email = tokenEmail?.value || memberEmailInput?.value || 'luca@example.com'
    const result = await requestJson<TokenResponse>('/rooms/token', {
      familyId,
      email,
    })
    if (statusBadge) statusBadge.textContent = 'Secure • token issued'
    updateStatus(`Token issued for ${email} in ${familyId}: ${result.token.substring(0, 26)}...`)
    syncFamilyContext(familyId, email)
  } catch (error) {
    updateStatus(error instanceof Error ? error.message : 'Could not create token.', true)
  }
}

const joinButton = document.querySelector<HTMLButtonElement>('#joinMeeting')
joinButton?.addEventListener('click', async () => {
  if (!tokenEmail || !tokenFamilyId) return
  tokenEmail.value = memberEmailInput?.value || tokenEmail.value
  tokenFamilyId.value = familyIdInput?.value || tokenFamilyId.value
  await createToken()
})

const inviteButton = document.querySelector<HTMLButtonElement>('#inviteMembers')
inviteButton?.addEventListener('click', () => {
  if (inviteFamilyId && familyIdInput) inviteFamilyId.value = familyIdInput.value
  void createInvite()
})

document.querySelector<HTMLButtonElement>('#createAdmin')?.addEventListener('click', () => {
  void createAdmin()
})

document.querySelector<HTMLButtonElement>('#createInvite')?.addEventListener('click', () => {
  void createInvite()
})

document.querySelector<HTMLButtonElement>('#acceptInvite')?.addEventListener('click', () => {
  void acceptInvite()
})

document.querySelector<HTMLButtonElement>('#createRoom')?.addEventListener('click', () => {
  void createRoom()
})

document.querySelector<HTMLButtonElement>('#createToken')?.addEventListener('click', () => {
  void createToken()
})
