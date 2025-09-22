package com.infernalsuite.asp.plugin;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.security.CodeSource;

/*
 * We use the boostrap to validate if the plugin is running on ASP.
 * Doing this check in SWPlugin seems to not be as easy without significant changes
 */
public class SlimePluginBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext bootstrapContext) {
        if(!ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("infernalsuite", "advancedslimepaper"))) {

            ComponentLogger logger = bootstrapContext.getLogger();
            logger.error("================================================");
            logger.error("AdvancedSlimePaper Plugin - Incompatible Server");
            logger.error("================================================");
            logger.error("");
            logger.error("It looks like you're trying to run the AdvancedSlimePaper (ASP) plugin");
            logger.error("on a server that is NOT using the AdvancedSlimePaper software.");
            logger.error("");
            logger.error("The ASP plugin only works with the AdvancedSlimePaper server jar.");
            logger.error("Running it on PaperMC or other forks will not work.");
            logger.error("");
            logger.error("To fix this, replace your current server jar");
            logger.error("with a AdvancedSlimePaper compatible jar from our website:");
            logger.error("https://infernalsuite.com");
            logger.error("");
            logger.error("If you need Purpur-specific features, we also maintain");
            logger.error("a less frequently updated Purpur fork that also provides Slime functionality.");
            logger.error("");
            logger.error("================================================");

            throw new UnsupportedOperationException("Attempted to run ASP plugin on non-ASP server");
        }
    }

}
