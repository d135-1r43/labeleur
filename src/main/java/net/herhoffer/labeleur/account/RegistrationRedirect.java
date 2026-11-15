package net.herhoffer.labeleur.account;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.OidcRedirectFilter;
import io.quarkus.oidc.Redirect;
import io.quarkus.oidc.Redirect.Location;
import jakarta.enterprise.context.ApplicationScoped;
import net.herhoffer.labeleur.organisation.InviteSignature;

/**
 * Signed out, {@code /register} sends the visitor to Keycloak like every
 * protected path, but asks for the registration form instead of the sign-in
 * form ({@code prompt=create}).
 *
 * So does an invitation link ({@code /invite/{token}?k=…}), whose {@code k}
 * goes along as {@code labeleur_invite}: it lets the invited address past
 * Keycloak's registration allowlist.
 */
@ApplicationScoped
@Unremovable
@Redirect(Location.OIDC_AUTHORIZATION)
public class RegistrationRedirect implements OidcRedirectFilter
{
	public static final String PATH = "/register";
	public static final String INVITE_PATH = "/invite/";

	@Override
	public void filter(OidcRedirectContext context)
	{
		var request = context.routingContext();
		String path = request.normalizedPath();
		if (PATH.equals(path)) context.additionalQueryParams().add("prompt", "create");
		else if (path.startsWith(INVITE_PATH))
		{
			context.additionalQueryParams().add("prompt", "create");
			String k = request.queryParams().get("k");
			if (k != null && k.matches("[A-Za-z0-9_-]{1,100}")) context.additionalQueryParams().add(InviteSignature.PARAM, k);
		}
	}
}
