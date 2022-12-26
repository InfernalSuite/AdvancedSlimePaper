package com.grinderwolf.swm.plugin.upgrade;

import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import com.grinderwolf.swm.plugin.upgrade.v117.v117WorldUpgrade;
import com.grinderwolf.swm.plugin.upgrade.v1_14.v1_14WorldUpgrade;
import com.grinderwolf.swm.plugin.upgrade.v1_16.v1_16WorldUpgrade;
import com.grinderwolf.swm.plugin.upgrade.v1_18.v118WorldUpgrade;

import java.util.HashMap;
import java.util.Map;

public class WorldUpgrader {

    private static final Map<Byte, Upgrade> upgrades = new HashMap<>();

    static {
        upgrades.put((byte) 0x04, new v1_14WorldUpgrade());
        // Todo we need a 1_14_WorldUpgrade class as well for 0x05
        upgrades.put((byte) 0x06, new v1_16WorldUpgrade());
        upgrades.put((byte) 0x07, new v117WorldUpgrade());
        upgrades.put((byte) 0x08, new v118WorldUpgrade());
    }

    public static void upgradeWorld(SlimeLoadedWorld world) {
        byte serverVersion = SWMPlugin.getInstance().getNms().getWorldVersion();

        for (byte ver = (byte) (world.getVersion() + 1); ver <= serverVersion; ver++) {
            Upgrade upgrade = upgrades.get(ver);

            if (upgrade == null) {
                Logging.warning("Missing world upgrader for version " + ver + ". World will not be upgraded.");
                continue;
            }

            upgrade.upgrade(world);
        }

        world.updateVersion(serverVersion);
    }

}
