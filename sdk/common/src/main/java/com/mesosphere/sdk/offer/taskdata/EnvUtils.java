package com.mesosphere.sdk.offer.taskdata;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Environment;

public class EnvUtils {

    private static final Pattern ENVVAR_INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_]");

    private EnvUtils() {
        // do not instantiate
    }

    /**
     * Returns a Map representation of the provided {@link Environment}.
     * In the event of duplicate labels, the last duplicate wins.
     */
    public static Map<String, String> toMap(Environment environment) {
        // sort labels alphabetically for convenience in debugging/logging:
        Map<String, String> map = new TreeMap<>();
        for (Environment.Variable variable : environment.getVariablesList()) {
            map.put(variable.getName(), variable.getValue());
        }
        return map;
    }

    /**
     * Returns a Protobuf representation of the provided {@link Map}.
     */
    static Environment toProto(Map<String, String> environmentMap) {
        Environment.Builder envBuilder = Environment.newBuilder();
        for (Map.Entry<String, String> entry : environmentMap.entrySet()) {
            envBuilder.addVariablesBuilder()
                .setName(entry.getKey())
                .setValue(entry.getValue());
        }
        return envBuilder.build();
    }

    // TODO(nickbp): SCHEDULER ONLY:

    /**
     * Adds or updates the provided environment variable entry in the provided command builder.
     */
    static void setEnvVar(
            Protos.CommandInfo.Builder builder,
            String key,
            String value) {
        Map<String, String> envMap = toMap(builder.getEnvironment());
        envMap.put(key, value);
        builder.setEnvironment(toProto(envMap));
    }

    /**
     * Returns the value of the provided environment variable, or an empty {@link Optional} if no matching environment
     * variable was found.
     */
    static Optional<String> getEnvVar(Protos.CommandInfo command, String key) {
        if (command.hasEnvironment()) {
            for (Protos.Environment.Variable v : command.getEnvironment().getVariablesList()) {
                if (v.getName().equals(key)) {
                    return Optional.of(v.getValue());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Converts the provided string to a conventional environment variable name, consisting of numbers, uppercase
     * letters, and underscores. Strictly speaking, lowercase characters are not invalid, but this avoids them to follow
     * convention.
     *
     * For example: {@code hello.There999!} => {@code HELLO_THERE999_}
     */
    public static String toEnvName(String str) {
        return ENVVAR_INVALID_CHARS.matcher(str.toUpperCase()).replaceAll("_");
    }

    /**
     * Returns a environment variable-style rendering of the specified port.
     */
    static String getPortEnvironmentVariable(String portName, Optional<String> customEnvKey) {
        String draftEnvName = customEnvKey.isPresent()
                ? customEnvKey.get() // use custom name as-is
                : EnvConstants.PORT_NAME_TASKENV_PREFIX + portName; // PORT_[name]
        // Envvar should be uppercased with invalid characters replaced with underscores:
        return toEnvName(draftEnvName);
    }
}