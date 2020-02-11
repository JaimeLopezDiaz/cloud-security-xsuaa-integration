package com.sap.cloud.security.token;

import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.xsuaa.Assertions;
import com.sap.cloud.security.xsuaa.jwt.DecodedJwt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.sap.cloud.security.token.TokenClaims.USER_NAME;
import static com.sap.cloud.security.token.TokenClaims.XSUAA.*;

/**
 * Decodes and parses encoded access token (JWT) for the Xsuaa identity service
 * and provides access to token header parameters and claims.
 */
public class XsuaaToken extends AbstractToken implements AccessToken {
	static final String UNIQUE_USER_NAME_FORMAT = "user/%s/%s"; // user/<origin>/<logonName>
	static final String UNIQUE_CLIENT_NAME_FORMAT = "client/%s"; // client/<clientid>
	private ScopeConverter scopeConverter;

	/**
	 * Creates an instance.
	 *
	 * @param decodedJwt
	 *            the decoded jwt
	 */
	public XsuaaToken(@Nonnull DecodedJwt decodedJwt) {
		super(decodedJwt);
	}

	/**
	 * Creates an instance.
	 *
	 * @param accessToken
	 *            the encoded access token, e.g. from the {@code Authorization}
	 *            header.
	 */
	public XsuaaToken(@Nonnull String accessToken) {
		super(accessToken);
	}

	/**
	 * Configures a scope converter, e.g. required for the
	 * {@link #hasLocalScope(String)}
	 *
	 * @param converter
	 *            the scope converter, e.g. {@link XsuaaScopeConverter}
	 *
	 * @return the token itself
	 */
	public XsuaaToken withScopeConverter(@Nullable ScopeConverter converter) {
		this.scopeConverter = converter;
		return this;
	}

	/**
	 * Get unique principal name of a user.
	 *
	 * @param origin
	 *            of the access token
	 * @param userLoginName
	 *            of the access token
	 * @return unique principal name
	 *
	 * @throws IllegalArgumentException
	 */
	static String getUniquePrincipalName(String origin, String userLoginName) {
		Assertions.assertHasText(origin,
				"Origin claim not set in JWT. Cannot create unique user name. Returning null.");
		Assertions.assertHasText(userLoginName,
				"User login name claim not set in JWT. Cannot create unique user name. Returning null.");

		if (origin.contains("/")) {
			throw new IllegalArgumentException(
					"Illegal '/' character detected in origin claim of JWT. Cannot create unique user name. Returing null.");
		}

		return String.format(UNIQUE_USER_NAME_FORMAT, origin, userLoginName);
	}

	@Override
	public Set<String> getScopes() {
		LinkedHashSet<String> scopes = new LinkedHashSet<>();
		scopes.addAll(getClaimAsStringList(TokenClaims.XSUAA.SCOPES));
		return scopes;
	}

	@Override
	public Principal getPrincipal() {
		String principalName;
		switch (getGrantType()) {
		case CLIENT_CREDENTIALS:
		case CLIENT_X509:
			principalName = String.format(UNIQUE_CLIENT_NAME_FORMAT, getClaimAsString(CLIENT_ID));
			break;
		default:
			principalName = getUniquePrincipalName(getClaimAsString(ORIGIN), getClaimAsString(USER_NAME));
			break;
		}
		return createPrincipalByName(principalName);
	}

	@Override
	public Service getService() {
		return Service.XSUAA;
	}

	@Override
	public boolean hasScope(String scope) {
		return getScopes().contains(scope);
	}

	/**
	 * Check if a local scope is available in the authentication token. <br>
	 * Requires a {@link ScopeConverter} to be configured with
	 * {@link #withScopeConverter(ScopeConverter)}.
	 *
	 * @param scope
	 *            name of local scope (without the appId)
	 * @return true if local scope is available
	 **/
	@Override
	public boolean hasLocalScope(@Nonnull String scope) {
		Assertions.assertNotNull(scopeConverter,
				"hasLocalScope() method requires a scopeConverter, which must not be null");
		return scopeConverter.convert(getScopes()).contains(scope);
	}

	@Override
	public GrantType getGrantType() {
		return GrantType.from(getClaimAsString(GRANT_TYPE));
	}

}
