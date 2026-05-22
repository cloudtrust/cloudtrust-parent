package io.cloudtrust.keycloak.test.container;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import com.github.stefanbirkner.systemlambda.SystemLambda.WithEnvironmentVariables;
import io.cloudtrust.exception.CloudtrustRuntimeException;

import java.util.Map;
import java.util.Map.Entry;

public class SystemEnv {
    public interface RunnableEx {
        void run() throws Exception;
    }

    public interface FunctionEx<T> {
        T run() throws Exception;
    }

    private static WithEnvironmentVariables createContext() {
        Map<String, String> env = ConfigurationFactory.get().getDefault().getEnvironment();
        WithEnvironmentVariables context = null;
        for (Entry<String, String> entry : env.entrySet()) {
            if (context == null) {
                context = SystemLambda.withEnvironmentVariable(entry.getKey(), entry.getValue());
            } else {
                context = context.and(entry.getKey(), entry.getValue());
            }
        }
        return context;
    }

    public static void withKeycloakEnvironment(RunnableEx runnable) {
        try {
            WithEnvironmentVariables context = createContext();
            if (context == null) {
                runnable.run();
            } else {
                context.execute(runnable::run);
            }
        } catch (Exception e) {
            throw new CloudtrustRuntimeException("Failed to run commands with environment variables", e);
        }
    }

    public static <T> T withKeycloakEnvironment(SystemEnv.FunctionEx<T> runnable) {
        try {
            WithEnvironmentVariables context = createContext();
            if (context == null) {
                return runnable.run();
            }
            return context.execute(runnable::run);
        } catch (Exception e) {
            throw new CloudtrustRuntimeException("Failed to run commands with environment variables", e);
        }
    }

}
