/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses per env test config.
 */
public class EnvConfig {
    private final boolean useSsl;
    private final String bankId;
    private final HostAndPort gateway;
    private final List<Pattern> blacklist;

    public EnvConfig(Config config) {
        this.useSsl = config.getBoolean("use-ssl");
        this.bankId = config.getString("bank-id");
        this.gateway = HostAndPort.fromParts(
                config.getString("gateway.host"),
                config.getInt("gateway.port"));
        this.blacklist = new ArrayList<>();
        if (config.hasPath("tests.blacklist")) {
            for (String s : config.getStringList("tests.blacklist")) {
                this.blacklist.add(globToPattern(s));
            }
        }
    }

    public boolean useSsl() {
        return useSsl;
    }

    public String getBankId() {
        return bankId;
    }

    public HostAndPort getGateway() {
        return gateway;
    }

    public List<Pattern> getBlackList() {
        return blacklist;
    }

    private static Pattern globToPattern(String glob) {
        StringBuilder pattern = new StringBuilder();
        for (char c : glob.toCharArray()) {
           switch (c) {
               case '.':
                   pattern.append("\\.");
                   break;
               case '*':
                   pattern.append(".*");
                   break;
               default:
                   pattern.append(c);
           }
        }
        return Pattern.compile(pattern.toString());
    }
}
