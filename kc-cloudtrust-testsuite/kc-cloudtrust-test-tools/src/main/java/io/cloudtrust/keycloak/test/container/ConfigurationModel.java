package io.cloudtrust.keycloak.test.container;

import java.util.List;
import java.util.Map;

public class ConfigurationModel {
	private List<String> modules;
	private List<String> classes;
	private Map<String, String> buildArguments;
	private Map<String, String> properties;

	public List<String> getModules() {
		return modules;
	}

	public void setModules(List<String> jarModules) {
		this.modules = jarModules;
	}

	public List<String> getClasses() {
		return classes;
	}

	public void setClasses(List<String> classes) {
		this.classes = classes;
	}

	public Map<String, String> getBuildArguments() {
		return buildArguments;
	}

	public void setBuildArguments(Map<String, String> buildArguments) {
		this.buildArguments = buildArguments;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}
