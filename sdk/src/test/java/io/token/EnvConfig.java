/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;

/**
 * Parses per env test config.
 */
public class EnvConfig {
    private final Config config;

    public EnvConfig(Config config) {
        this.config = config;
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
}
