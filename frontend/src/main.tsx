import { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './fonts';
import './styles.css';
import { accountApi } from './api/http';
import { App } from './App';
import { InvitePage } from './ui/Invite';
import { Landing } from './ui/Landing';

// /login and /register only exist to send a signed-out visitor through Keycloak; back here, the app lives at /.
if (/^\/(login|register)\/?$/.test(location.pathname)) history.replaceState(null, '', '/');

/** Signed out, the server sends /invite/… through Keycloak's registration form first. */
const invite = location.pathname.match(/^\/invite\/([A-Za-z0-9_-]+)\/?$/)?.[1];

/** Signed in, the app; signed out, the landing page with the way in. */
function Root() {
  if (invite) return <InvitePage token={invite} />;
  return <AppOrLanding />;
}

function AppOrLanding() {
  const [signedIn, setSignedIn] = useState<boolean | null>(null);
  useEffect(() => {
    void accountApi.signedIn().then(setSignedIn);
  }, []);
  if (signedIn === null) return <div className="boot">Loading…</div>;
  return signedIn ? <App /> : <Landing />;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Root />
  </StrictMode>,
);
