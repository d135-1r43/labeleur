package net.herhoffer.labeleur;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Quarkus serves every static file as {@code public, immutable, max-age=86400}.
 * That suits the content-hashed bundles under /assets/, but not index.html:
 * Quinoa serves it for /login and /register too, so after one sign-in the
 * browser kept that page for a day and the next "Sign in" never reached the
 * server, and so never reached Keycloak. A deploy stayed invisible just as
 * long. Everything outside /assets/ is checked with the server each time
 * instead.
 */
@ApplicationScoped
public class PageCaching
{
	static final String REVALIDATE = "no-cache";

	void register(@Observes Router router)
	{
		router.route().order(Integer.MIN_VALUE).handler(ctx -> {
			if (!ctx.normalizedPath().startsWith("/assets/"))
			{
				ctx.addHeadersEndHandler(v -> {
					var headers = ctx.response().headers();
					String cacheControl = headers.get(HttpHeaders.CACHE_CONTROL);
					if (cacheControl != null && cacheControl.contains("immutable"))
						headers.set(HttpHeaders.CACHE_CONTROL, REVALIDATE);
				});
			}
			ctx.next();
		});
	}
}
