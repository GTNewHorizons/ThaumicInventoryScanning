package net.blay09.mods.tcinventoryscan.client;

import net.blay09.mods.tcinventoryscan.CommonProxy;
import net.blay09.mods.tcinventoryscan.TCInventoryScanning;
import net.blay09.mods.tcinventoryscan.net.MessageScanSlot;
import net.blay09.mods.tcinventoryscan.net.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ScanResult;
import thaumcraft.client.lib.ClientTickEventsFML;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.ScanManager;

public class ClientProxy extends CommonProxy {

    private static final int SCAN_TICKS = 50;
    private static final int SOUND_TICKS = 5;

    private static final int INVENTORY_PLAYER_X = 26;
    private static final int INVENTORY_PLAYER_Y = 8;
    private static final int INVENTORY_PLAYER_WIDTH = 52;
    private static final int INVENTORY_PLAYER_HEIGHT = 70;

    private boolean isValidSlot = false;
    private Item thaumometer;
    /**
     * Slot the cursor is hovering over
     **/
    private Slot hoveringSlot;
    private Slot lastScannedSlot;
    private int ticksHovered = 0;
    private ClientTickEventsFML effectRenderer;
    private ScanResult currentScan = null;
    /**
     * Is the cursor hovering above the player sprite in Inventory
     **/
    private boolean isHoveringOverPlayer;

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        effectRenderer = new ClientTickEventsFML();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        thaumometer = GameRegistry.findItem("Thaumcraft", "ItemThaumometer");
    }

    /**
     * Checks whether the currently hovered item or player would be a valid scan and sets currentScan and isValidScan
     * accordingly
     **/
    private void simulateScan(EntityPlayer player) {
        // Handle scanning player
        ScanResult result = new ScanResult((byte) 2, 0, 0, player, "");
        if (isHoveringOverPlayer && ScanManager.isValidScanTarget(player, result, "@")) {
            currentScan = result;
            isValidSlot = true;
            return;
        }
        // Handle scanning item
        result = new ScanResult(
                (byte) 1,
                Item.getIdFromItem(hoveringSlot.getStack().getItem()),
                hoveringSlot.getStack().getItemDamage(),
                null,
                "");
        if (hoveringSlot.canTakeStack(player) && !(hoveringSlot instanceof SlotCrafting)
                && ScanManager.isValidScanTarget(player, result, "@")
                && !ScanManager.getScanAspects(result, Minecraft.getMinecraft().theWorld.provider.worldObj).aspects
                        .isEmpty()) {
            currentScan = result;
            isValidSlot = true;
            return;
        }
        // Invalid scan
        currentScan = null;
        isValidSlot = false;
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        // Get minecraft and player objects
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        ItemStack selectedItem;
        // Null checks
        if (player == null || hoveringSlot == null
                || hoveringSlot.getStack() == null
                || (selectedItem = player.inventory.getItemStack()) == null)
            return;
        // If thaumometer is unselected/dropped
        if (selectedItem.getItem() != thaumometer) {
            ticksHovered = 0;
            currentScan = null;
            isValidSlot = false;
            return;
        }
        switch ((isValidSlot ? 0 : 1) + ((hoveringSlot.equals(lastScannedSlot) ? 0 : 10))) {
            // Valid item + unchanged slots
            case 0:
                // Scan the item
                ticksHovered++;
                playScanningSoundTick(player);
                if (ticksHovered >= SCAN_TICKS) tryCompleteScan(player);
                break;
            // Invalid item + unchanged slots
            case 1:
                // In case of sudden jump to player sprite, recheck if player is selected
                if (!isHoveringOverPlayer) return;
                // Valid/invalid item + changed slots
            case 10:
            case 11:
                // Cancel scanning
                ticksHovered = 0;
                lastScannedSlot = hoveringSlot;
                // Reevaluate item
                simulateScan(player);
        }
    }

    private void tryCompleteScan(EntityPlayer player) {
        try {
            if (ScanManager.completeScan(player, currentScan, "@"))
                NetworkHandler.instance.sendToServer(new MessageScanSlot(hoveringSlot.slotNumber));
        } catch (StackOverflowError e) {
            // Can't do anything about Thaumcraft freaking out except for calming it down if it
            // does.
            // If Thaumcraft happens to get into a weird recipe loop, we just ignore that and assume
            // the item unscannable.
        }
        ticksHovered = 0;
        isValidSlot = false;
        currentScan = null;
    }

    private void playScanningSoundTick(EntityPlayer entityPlayer) {
        if (ticksHovered > SOUND_TICKS && ticksHovered % 2 == 0) {
            entityPlayer.worldObj.playSound(
                    entityPlayer.posX,
                    entityPlayer.posY,
                    entityPlayer.posZ,
                    "thaumcraft:cameraticks",
                    0.2F,
                    0.45F + entityPlayer.worldObj.rand.nextFloat() * 0.1F,
                    false);
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (TCInventoryScanning.isServerSideInstalled && event.itemStack.getItem() == thaumometer) {
            event.toolTip.add("\u00a76" + I18n.format("tcinventoryscan:thaumometerTooltip"));
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                String[] lines = I18n.format("tcinventoryscan:thaumometerTooltipMore").split("\\\\n");
                for (String line : lines) {
                    event.toolTip.add("\u00a73" + line);
                }
            }
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (TCInventoryScanning.isServerSideInstalled && event.gui instanceof GuiContainer) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer entityPlayer = mc.thePlayer;
            isHoveringOverPlayer = isHoveringPlayer((GuiContainer) event.gui, event.mouseX, event.mouseY);
            hoveringSlot = ((GuiContainer) event.gui).getSlotAtPosition(event.mouseX, event.mouseY);
            if (currentScan != null) {
                renderScanningProgress(event.gui, event.mouseX, event.mouseY, ticksHovered / (float) SCAN_TICKS);
                if (!isHoveringOverPlayer) {
                    event.gui.renderToolTip(hoveringSlot.getStack(), event.mouseX, event.mouseY);
                    effectRenderer.renderAspectsInGui((GuiContainer) event.gui, entityPlayer);
                } else if (ScanManager.hasBeenScanned(entityPlayer, new ScanResult((byte) 2, 0, 0, entityPlayer, ""))) {
                    renderPlayerAspects(event.gui, event.mouseX, event.mouseY);
                }
            }
        }
    }

    public void renderPlayerAspects(GuiScreen gui, int mouseX, int mouseY) {
        GL11.glPushMatrix();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPushAttrib(1048575);
        GL11.glDisable(GL11.GL_LIGHTING);
        int shiftX = Thaumcraft.instance.aspectShift ? -16 : -8;
        int shiftY = Thaumcraft.instance.aspectShift ? -16 : -8;
        int x = mouseX + 17;
        int y = mouseY + 7 - 33;
        EntityPlayer entityPlayer = FMLClientHandler.instance().getClientPlayerEntity();
        AspectList aspectList = ScanManager.generateEntityAspects(entityPlayer);
        if (aspectList != null) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (aspectList.size() > 0) {
                Aspect[] sortedAspects = aspectList.getAspectsSortedAmount();
                for (Aspect aspect : sortedAspects) {
                    if (aspect != null) {
                        x += 18;
                        UtilsFX.bindTexture("textures/aspects/_back.png");
                        GL11.glPushMatrix();
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        GL11.glTranslatef(x + shiftX - 2, y + shiftY - 2, 0f);
                        GL11.glScalef(1.25f, 1.25f, 0f);
                        UtilsFX.drawTexturedQuadFull(0, 0, UtilsFX.getGuiZLevel(gui));
                        GL11.glDisable(GL11.GL_BLEND);
                        GL11.glPopMatrix();
                        if (Thaumcraft.proxy.playerKnowledge
                                .hasDiscoveredAspect(entityPlayer.getCommandSenderName(), aspect)) {
                            UtilsFX.drawTag(
                                    x + shiftX,
                                    y + shiftY,
                                    aspect,
                                    aspectList.getAmount(aspect),
                                    0,
                                    UtilsFX.getGuiZLevel(gui));
                        } else {
                            UtilsFX.bindTexture("textures/aspects/_unknown.png");
                            GL11.glPushMatrix();
                            GL11.glEnable(GL11.GL_BLEND);
                            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                            GL11.glTranslatef(x + shiftX, y + shiftY, 0f);
                            UtilsFX.drawTexturedQuadFull(0, 0, UtilsFX.getGuiZLevel(gui));
                            GL11.glDisable(GL11.GL_BLEND);
                            GL11.glPopMatrix();
                        }
                    }
                }
            }
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    public void renderScanningProgress(GuiScreen gui, int mouseX, int mouseY, float progress) {
        StringBuilder sb = new StringBuilder("\u00a76");
        sb.append(I18n.format("tcinventoryscan:scanning"));
        if (progress >= 0.75f) {
            sb.append("...");
        } else if (progress >= 0.5f) {
            sb.append("..");
        } else if (progress >= 0.25f) {
            sb.append(".");
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        float oldZLevel = gui.zLevel;
        gui.zLevel = 300;
        Minecraft.getMinecraft().fontRenderer
                .drawStringWithShadow(sb.toString(), mouseX, mouseY - 30, Integer.MAX_VALUE);
        gui.zLevel = oldZLevel;
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    }

    public boolean isHoveringPlayer(GuiContainer gui, int mouseX, int mouseY) {
        return gui instanceof GuiInventory && mouseX >= gui.guiLeft + INVENTORY_PLAYER_X
                && mouseX < gui.guiLeft + INVENTORY_PLAYER_X + INVENTORY_PLAYER_WIDTH
                && mouseY >= gui.guiTop + INVENTORY_PLAYER_Y
                && mouseY < gui.guiTop + INVENTORY_PLAYER_Y + INVENTORY_PLAYER_HEIGHT;
    }
}
