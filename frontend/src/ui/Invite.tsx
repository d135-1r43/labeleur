import { useEffect, useState } from 'react';
import { ApiError, membersApi, type InvitationDetails } from '../api/http';

/**
 * /invite/{token}: the invitee, signed in (or just registered), joins the
 * organisation. Shown instead of the app, which would set up an account of
 * their own on its first request and so rule them out.
 */
export function InvitePage({ token }: { token: string }) {
  const [details, setDetails] = useState<InvitationDetails | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    membersApi.invitation(token).then(setDetails, (e: ApiError) => setError(e.message));
  }, [token]);

  const open = () => window.location.replace('/');
  const accept = () => {
    setBusy(true);
    membersApi.accept(token).then(open, (e: ApiError) => {
      setError(e.message);
      setBusy(false);
    });
  };

  if (!details && !error) return <div className="boot">Loading…</div>;
  return (
    <div className="invite-page">
      <article className="invite-card" aria-labelledby="invite-title">
        <span className="brand">Labeleur</span>
        {details ? (
          <>
            <h1 id="invite-title">Join {details.organisation}</h1>
            <p>
              {details.invitedBy} invited you to work on the organisation’s labels with them: the same records, sender, logo, settings and print jobs.
            </p>
          </>
        ) : (
          <h1 id="invite-title">Invitation</h1>
        )}
        {(error ?? details?.problem) && (
          <p className="problem" role="alert">
            {error ?? details?.problem}
          </p>
        )}
        <div className="actions">
          {details && !details.problem && !error ? (
            <button className="btn primary" disabled={busy} onClick={accept}>
              {busy ? 'Joining…' : `Join ${details.organisation}`}
            </button>
          ) : (
            <>
              <button className="btn primary" onClick={open}>
                Open Labeleur
              </button>
              {details?.signedInAs && details.signedInAs !== details.email && (
                <a className="btn ghost" href="/logout">
                  Sign out of {details.signedInAs}
                </a>
              )}
            </>
          )}
        </div>
        {details && <small className="meta">Signed in as {details.signedInAs ?? 'an account without email'}</small>}
      </article>
    </div>
  );
}
