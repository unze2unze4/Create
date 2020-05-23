package com.simibubi.create;

import java.util.function.Function;

import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionEntityRenderer;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueRenderer;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityType.Builder;
import net.minecraft.entity.EntityType.IFactory;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public enum AllEntityTypes {

	CONTRAPTION(ContraptionEntity::new, EntityClassification.MISC, 5, 3, true, ContraptionEntity::build),
	STATIONARY_CONTRAPTION(ContraptionEntity::new, EntityClassification.MISC, 20, 40, false, ContraptionEntity::build),
	SUPER_GLUE(SuperGlueEntity::new, EntityClassification.MISC, 10, Integer.MAX_VALUE, false, SuperGlueEntity::build),

	;

	private IFactory<?> factory;
	private int range;
	private int updateFrequency;
	private Function<EntityType.Builder<? extends Entity>, EntityType.Builder<? extends Entity>> propertyBuilder;
	private EntityClassification group;
	private boolean sendVelocity;

	public EntityType<? extends Entity> type;

	private AllEntityTypes(IFactory<?> factory, EntityClassification group, int range, int updateFrequency,
			boolean sendVelocity,
			Function<EntityType.Builder<? extends Entity>, EntityType.Builder<? extends Entity>> propertyBuilder) {
		this.factory = factory;
		this.group = group;
		this.range = range;
		this.updateFrequency = updateFrequency;
		this.sendVelocity = sendVelocity;
		this.propertyBuilder = propertyBuilder;
	}

	public static void register(final RegistryEvent.Register<EntityType<?>> event) {
		for (AllEntityTypes entity : values()) {
			String id = Lang.asId(entity.name());
			ResourceLocation resourceLocation = new ResourceLocation(Create.ID, id);
			Builder<? extends Entity> builder = EntityType.Builder
					.create(entity.factory, entity.group)
					.setTrackingRange(entity.range)
					.setUpdateInterval(entity.updateFrequency)
					.setShouldReceiveVelocityUpdates(entity.sendVelocity);
			if (entity.propertyBuilder != null)
				builder = entity.propertyBuilder.apply(builder);
			entity.type = builder.build(id).setRegistryName(resourceLocation);
			event.getRegistry().register(entity.type);
		}

	}

	@SuppressWarnings("unchecked") // TODO 1.15 this generic stuff is incompatible with the enum system - need strong types
	@OnlyIn(value = Dist.CLIENT)
	public static void registerRenderers() {
		RenderingRegistry.registerEntityRenderingHandler((EntityType<ContraptionEntity>) STATIONARY_CONTRAPTION.type, ContraptionEntityRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler((EntityType<ContraptionEntity>) CONTRAPTION.type, ContraptionEntityRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler((EntityType<SuperGlueEntity>) SUPER_GLUE.type, SuperGlueRenderer::new);
	}

}
