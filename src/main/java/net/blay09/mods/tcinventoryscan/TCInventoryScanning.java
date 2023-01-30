package net.blay09.mods.tcinventoryscan;

import net.blay09.mods.tcinventoryscan.net.NetworkHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

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

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);

        NetworkHandler.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
