package mal.lootbags.gui;

import org.lwjgl.opengl.GL11;

import mal.lootbags.network.LootbagWrapper;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

public class LootbagGui extends GuiContainer{

	private static LootbagContainer cont;
	private LootbagWrapper wrapper;
	private InventoryPlayer player;
	
	public LootbagGui(InventoryPlayer iplayer, LootbagWrapper wrap) {
		super(cont = new LootbagContainer(iplayer, wrap));
		wrapper = wrap;
		player = iplayer;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(new ResourceLocation("lootbags", "textures/gui/lootbagGui.png"));
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
	}

    /**
     * This function is what controls the hotbar shortcut check when you press a number key when hovering a stack.
     */
	@Override
    protected boolean checkHotbarKeys(int p_146983_1_)
    {
/*        if (this.mc.thePlayer.inventory.getItemStack() == null && this.slotdummy != null && slotdummy.slotNumber != cont.islot)
        {
            for (int j = 0; j < 9; ++j)
            {
                if (p_146983_1_ == this.mc.gameSettings.keyBindsHotbar[j].getKeyCode())
                {
                    this.handleMouseClick(this.slotdummy, this.slotdummy.slotNumber, j, 2);
                    return true;
                }
            }
        }*/

        return false;
    }
}
/*******************************************************************************
 * Copyright (c) 2015 Malorolam.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the included license.
 * 
 *********************************************************************************/