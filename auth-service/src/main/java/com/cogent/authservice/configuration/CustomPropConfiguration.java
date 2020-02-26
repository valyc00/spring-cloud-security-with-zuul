package com.cogent.authservice.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author smriti
 * THIS CLASS REPRESENTS CONFIGURATION TO PROVIDE PROPERTIES FILES TO SPRING ENVIRONMENT
 */
@Configuration
@ConfigurationProperties("custom-route")
public class CustomPropConfiguration {
    private static Map<String, Route> routes = new HashMap<>();

    public Map<String, Route> getRoutes() {
        return routes;
    }
    
    public void setRoutes(HashMap<String, Route> routes) {
        this.routes = routes;
    }
    
    
    public static class Route {
        private String path;
        private String role;
        private boolean permitAll;
        
		public boolean isPermitAll() {
			return permitAll;
		}
		public void setPermitAll(boolean permitAll) {
			this.permitAll = permitAll;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public String getRole() {
			return role;
		}
		public void setRole(String role) {
			this.role = role;
		}
        

        // [getters + setters]
    }
}