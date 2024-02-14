package net.blay09.mods.tcinventoryscan;

import java.util.Map;

import net.blay09.mods.tcinventoryscan.net.NetworkHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;

@Mod(
        modid = TCInventoryScanning.MOD_ID,
        name = "TC Inventory Scanning",
        acceptableRemoteVersions = "*",
        dependencies = "required-after:Thaumcraft",
        version = "GRADLETOKEN_VERSION")
public class TCInventoryScanning {

    public static final String MOD_ID = "tcinventoryscan";

    @SidedProxy(
            clientSide = "net.blay09.mods.tcinventoryscan.client.ClientProxy",
            serverSide = "net.blay09.mods.tcinventoryscan.CommonProxy")
    public static CommonProxy proxy;

    public static boolean isServerSideInstalled = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);

        NetworkHandler.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @NetworkCheckHandler
    public boolean checkNetwork(Map<String, String> map, Side side) {
        if (side == Side.SERVER) {
            isServerSideInstalled = map.containsKey(MOD_ID);
        }
        return true;
    }
}
