package com.example.eyemark;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, EyeMarkMod.MODID);

    public static final RegistryObject<SoundEvent> APPEAR = reg("mark_appear");
    public static final RegistryObject<SoundEvent> SEEN = reg("mark_seen");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(EyeMarkMod.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
