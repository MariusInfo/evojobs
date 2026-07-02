package org.evocraft.evojobs.init;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.evocraft.evojobs.JobMenu;

public class EvoJobsMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "evojobs");

    public static final RegistryObject<MenuType<JobMenu>> JOB_MENU = MENUS.register("job_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> new JobMenu(windowId, inv)));
}