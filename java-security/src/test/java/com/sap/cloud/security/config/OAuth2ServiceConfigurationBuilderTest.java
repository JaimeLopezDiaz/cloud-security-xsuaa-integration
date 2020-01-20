package com.sap.cloud.security.config;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static com.sap.cloud.security.config.cf.CFConstants.URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OAuth2ServiceConfigurationBuilderTest {

	private OAuth2ServiceConfigurationBuilder cut;

	@Before
	public void setUp() {
		cut = OAuth2ServiceConfigurationBuilder.forService(Service.XSUAA);
	}

	@Test
	public void forService() {
		OAuth2ServiceConfiguration configuration = cut.build();

		assertThat(configuration.getService()).isEqualTo(Service.XSUAA);
	}

	@Test
	public void forService_serviceNull_throwsException() {
		assertThatThrownBy(() -> OAuth2ServiceConfigurationBuilder.forService(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not be null");
	}

	@Test
	public void withClientId() {
		String clientId = "theClientId";

		OAuth2ServiceConfiguration configuration = cut.withClientId(clientId).build();

		assertThat(configuration.getClientId()).isEqualTo(clientId);
	}

	@Test
	public void withClientSecret() {
		String clientSecret = "theClientSecret";

		OAuth2ServiceConfiguration configuration = cut.withClientSecret(clientSecret).build();

		assertThat(configuration.getClientSecret()).isEqualTo(clientSecret);
	}

	@Test
	public void withUrl() {
		String url = "http://theUrl.org";

		OAuth2ServiceConfiguration configuration = cut.withUrl(url).build();

		assertThat(configuration.getUrl()).isEqualTo(URI.create(url));
	}

	@Test
	public void withProperty() {
		String propertyName = "propertyName";
		String propertyValue = "value";

		OAuth2ServiceConfiguration configuration = cut.withProperty(propertyName, propertyValue).build();

		assertThat(configuration.getProperty(propertyName)).isEqualTo(propertyValue);
	}

	@Test
	public void withUrl_setViaProperty() {
		String url = "http://theUrl.org";

		OAuth2ServiceConfiguration configuration = cut.withProperty(URL, url).build();

		assertThat(configuration.getUrl()).isEqualTo(URI.create(url));
	}

}