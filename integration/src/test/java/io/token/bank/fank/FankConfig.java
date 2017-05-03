/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.bank.fank;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;

/**
 * Parses per env fank test config.
 */
public class FankConfig {
    public FankConfig(Config config) {
        this.config = config;
    }

    private final Config config;

    public boolean useSsl() {
        return config.getBoolean("use-ssl");
    }

    public HostAndPort getFank() {
        return HostAndPort.fromParts(
                config.getString("fank.host"),
                config.getInt("fank.port"));
    }
}
