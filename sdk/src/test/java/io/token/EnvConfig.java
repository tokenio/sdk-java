/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses per env test config.
 */
public class EnvConfig {
    private static Logger logger = LoggerFactory.getLogger(EnvConfig.class);
    private final Config config;

    public EnvConfig(String envName) {
        logger.info("Loading settings for env: {}", envName);
        this.config = ConfigFactory.load(envName);
    }

    public boolean useSsl() {
        return config.getBoolean("use-ssl");
    }

    public String getBankId() {
        return config.getString("bank-id");
    }

    public HostAndPort getGateway() {
        return HostAndPort.fromParts(
                config.getString("gateway.host"),
                config.getInt("gateway.port"));
    }

    public HostAndPort getFank() {
        return HostAndPort.fromParts(
                config.getString("fank.host"),
                config.getInt("fank.port"));
    }
}
