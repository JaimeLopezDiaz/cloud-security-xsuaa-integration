package com.sap.cloud.security.token.validation.validators;

import static com.sap.cloud.security.xsuaa.Assertions.assertHasText;

import com.sap.cloud.security.token.Token;
import com.sap.cloud.security.token.validation.ValidationResult;
import com.sap.cloud.security.token.validation.ValidationResults;
import com.sap.cloud.security.token.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates if the jwt access token is intended for the OAuth2 client of this
 * application. The aud (audience) claim identifies the recipients the JWT is
 * issued for.
 *
 * Validates whether there is one audience that matches one of the configured
 * OAuth2 client ids.
 */
public class JwtAudienceValidator implements Validator<Token> {
	private static final Logger logger = LoggerFactory.getLogger(JwtAudienceValidator.class);
	private static final char DOT = '.';

	private final List<String> clientIds = new ArrayList();

	public JwtAudienceValidator(String clientId) {
		configureAnotherServiceInstance(clientId);
	}

	public JwtAudienceValidator configureAnotherServiceInstance(String clientId) {
		assertHasText(clientId, "clientId must not be null or empty.");
		clientIds.add(clientId);
		logger.info("configured JwtAudienceValidator with clientId {}.", clientId);

		return this;
	}

	@Override
	public ValidationResult validate(Token token) {
		List<String> allowedAudiences = getAllowedAudiences(token);
		for (String configuredClientId : clientIds) {
			if (allowedAudiences.contains(configuredClientId)) {
				return ValidationResults.createValid();
			}
		}
		return ValidationResults
				.createInvalid("Jwt token with audience {} is not issued for these clientIds: {}.", allowedAudiences,
						clientIds);
	}

	/**
	 * Retrieve audiences from token. In case the audience list is empty, take
	 * audiences based on the scope names.
	 *
	 * @param token
	 * @return (empty) list of audiences
	 */
	static List<String> getAllowedAudiences(Token token) {
		List<String> audiences = new ArrayList<>();

		for (String audience : token.getAudiences()) {
			if (audience.contains(".")) {
				String aud = audience.substring(0, audience.indexOf(".")).trim();
				if (!aud.isEmpty()) {
					audiences.add(aud);
				}
			} else {
				audiences.add(audience);
			}
		}
		return audiences;
	}

}
