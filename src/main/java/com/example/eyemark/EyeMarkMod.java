package com.example.eyemark;

import com.example.eyemark.net.Net;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EyeMarkMod.MODID)
public class EyeMarkMod {
    public static final String MODID = "eyemark";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EyeMarkMod() {
        Net.register();
        ModSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
