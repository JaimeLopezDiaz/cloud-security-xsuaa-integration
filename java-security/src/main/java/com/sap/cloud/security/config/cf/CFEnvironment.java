package com.sap.cloud.security.config.cf;

import static com.sap.cloud.security.config.Service.IAS;
import static com.sap.cloud.security.config.Service.XSUAA;
import static com.sap.cloud.security.config.cf.CFConstants.VCAP_SERVICES;
import static com.sap.cloud.security.config.cf.CFEnvParser.loadAll;

import javax.annotation.Nullable;

import com.sap.cloud.security.config.Environment;
import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.config.cf.CFConstants.Plan;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Loads the OAuth configuration ({@link OAuth2ServiceConfiguration}) of a
 * supported identity {@link Service} in the SAP CP Cloud Foundry Environment by
 * parsing the {@code VCAP_SERVICES} system environment variable.
 */
public class CFEnvironment implements Environment {

	private Map<Service, List<OAuth2ServiceConfiguration>> serviceConfigurations;
	private UnaryOperator<String> systemEnvironmentProvider;
	private UnaryOperator<String> systemPropertiesProvider;

	private CFEnvironment() {
		// implemented in getInstance() factory method
	}

	public static CFEnvironment getInstance() {
		return getInstance(System::getenv, System::getProperty);
	}

	static CFEnvironment getInstance(UnaryOperator<String> systemEnvironmentProvider,
			UnaryOperator<String> systemPropertiesProvider) {
		CFEnvironment instance = new CFEnvironment();
		instance.systemEnvironmentProvider = systemEnvironmentProvider;
		instance.systemPropertiesProvider = systemPropertiesProvider;
		instance.serviceConfigurations = loadAll(instance.extractVcapJsonString());
		return instance;
	}

	@Override
	public OAuth2ServiceConfiguration getXsuaaConfiguration() {
		return loadXsuaa();
	}

	@Nullable
	@Override
	public OAuth2ServiceConfiguration getIasConfiguration() {
		return loadAllForService(IAS).stream().filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public int getNumberOfXsuaaConfigurations() {
		return loadAllForService(XSUAA).size();
	}

	@Override
	public OAuth2ServiceConfiguration getXsuaaConfigurationForTokenExchange() {
		if (getNumberOfXsuaaConfigurations() > 1) {
			return loadForServicePlan(XSUAA, Plan.BROKER);
		}
		return getXsuaaConfiguration();
	}

	/**
	 * Loads all configurations of all service instances of the dedicated service.
	 *
	 * @param service
	 *            the service name
	 * @return the list of all found configurations or empty list, in case there are
	 *         no service bindings.
	 * @deprecated as multiple bindings of XSUAA identity service is not anymore
	 *             necessary with the unified broker plan, this method is
	 *             deprecated.
	 */
	@Deprecated
	List<OAuth2ServiceConfiguration> loadAllForService(Service service) {
		return serviceConfigurations.getOrDefault(service, new ArrayList<>());
	}

	@Override
	public Type getType() {
		return Type.CF;
	}

	private String extractVcapJsonString() {
		String env = systemPropertiesProvider.apply(VCAP_SERVICES);
		if (env == null) {
			env = systemEnvironmentProvider.apply(VCAP_SERVICES);
		}
		return env != null ? env : "{}";
	}

	private OAuth2ServiceConfiguration loadXsuaa() {
		Optional<OAuth2ServiceConfiguration> applicationService = Optional
				.ofNullable(loadForServicePlan(XSUAA, Plan.APPLICATION));
		Optional<OAuth2ServiceConfiguration> brokerService = Optional
				.ofNullable(loadForServicePlan(XSUAA, Plan.BROKER));
		if (applicationService.isPresent()) {
			return applicationService.get();
		}
		return brokerService.orElse(null);
	}

	/**
	 * Loads the configuration for a dedicated service plan.
	 *
	 * @param service
	 *            the service name
	 * @param plan
	 *            the plan name
	 * @return the configuration or null, if there is not such binding information
	 *         for the given service plan.
	 */
	@Nullable
	public OAuth2ServiceConfiguration loadForServicePlan(Service service, Plan plan) {
		return loadAllForService(service).stream()
				.filter(configuration -> Plan.from(configuration.getProperty(CFConstants.SERVICE_PLAN)).equals(plan))
				.findFirst()
				.orElse(null);
	}

}
