package com.example.examplemod.ai;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.brain.Brain;
import net.minecraft.world.entity.ai.brain.MemoryModuleType;
import net.minecraft.world.entity.ai.brain.sensor.Sensor;
import net.minecraft.world.entity.ai.brain.sensor.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AutonomousBotEntity extends PathfinderMob {
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM
    );
    private static final ImmutableList<SensorType<? extends Sensor<? super AutonomousBotEntity>>> SENSOR_TYPES = ImmutableList.of(
        SensorType.NEAREST_ITEMS,
        SensorType.NEAREST_LIVING_ENTITIES
    );
    private static final List<TemplateBlock> BUILD_TEMPLATE = List.of(
        new TemplateBlock(new BlockPos(0, 0, 0), Blocks.OAK_PLANKS.defaultBlockState()),
        new TemplateBlock(new BlockPos(1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState()),
        new TemplateBlock(new BlockPos(0, 0, 1), Blocks.OAK_PLANKS.defaultBlockState()),
        new TemplateBlock(new BlockPos(1, 0, 1), Blocks.OAK_PLANKS.defaultBlockState()),
        new TemplateBlock(new BlockPos(0, 1, 0), Blocks.OAK_LOG.defaultBlockState())
    );

    private final SimpleContainer inventory = new SimpleContainer(9);

    public AutonomousBotEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.FOLLOW_RANGE, 24.0D)
            .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MineBlocksGoal(this, 1.0D));
        goalSelector.addGoal(2, new PickupItemGoal(this, 1.1D));
        goalSelector.addGoal(3, new BuildTemplateGoal(this, 1.0D));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected Brain.Provider<AutonomousBotEntity> brainProvider() {
        return Brain.provider(MEMORY_MODULES, SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return brainProvider().makeBrain(dynamic);
    }

    @Override
    public Brain<AutonomousBotEntity> getBrain() {
        return (Brain<AutonomousBotEntity>) super.getBrain();
    }

    @Override
    protected void customServerAiStep() {
        level().getProfiler().push("autonomousBotBrain");
        getBrain().tick((ServerLevel) level(), this);
        level().getProfiler().pop();
        super.customServerAiStep();
    }

    private ItemStack addToInventory(ItemStack stack) {
        return inventory.addItem(stack);
    }

    private record TemplateBlock(BlockPos offset, BlockState state) {
    }

    private static final class MineBlocksGoal extends Goal {
        private final AutonomousBotEntity bot;
        private final double speed;
        private BlockPos targetPos;
        private int breakCooldown;

        private MineBlocksGoal(AutonomousBotEntity bot, double speed) {
            this.bot = bot;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (bot.level().isClientSide) {
                return false;
            }
            if (breakCooldown > 0) {
                breakCooldown--;
                return false;
            }
            targetPos = findTargetBlock();
            return targetPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return targetPos != null && !bot.level().getBlockState(targetPos).isAir();
        }

        @Override
        public void start() {
            if (targetPos != null) {
                bot.getNavigation().moveTo(
                    targetPos.getX() + 0.5D,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5D,
                    speed
                );
            }
        }

        @Override
        public void tick() {
            if (targetPos == null) {
                return;
            }
            double distance = bot.distanceToSqr(Vec3.atCenterOf(targetPos));
            if (distance > 2.5D) {
                bot.getNavigation().moveTo(
                    targetPos.getX() + 0.5D,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5D,
                    speed
                );
                return;
            }
            BlockState state = bot.level().getBlockState(targetPos);
            if (!state.isAir() && state.getDestroySpeed(bot.level(), targetPos) >= 0.0F) {
                bot.level().destroyBlock(targetPos, true, bot);
                breakCooldown = 20;
            }
            targetPos = null;
        }

        @Override
        public void stop() {
            targetPos = null;
        }

        private BlockPos findTargetBlock() {
            BlockPos origin = bot.blockPosition();
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int y = -1; y <= 1; y++) {
                for (int x = -4; x <= 4; x++) {
                    for (int z = -4; z <= 4; z++) {
                        mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        BlockState state = bot.level().getBlockState(mutable);
                        if (state.isAir()) {
                            continue;
                        }
                        if (state.getDestroySpeed(bot.level(), mutable) >= 0.0F) {
                            return mutable.immutable();
                        }
                    }
                }
            }
            return null;
        }
    }

    private static final class PickupItemGoal extends Goal {
        private final AutonomousBotEntity bot;
        private final double speed;
        private ItemEntity targetItem;

        private PickupItemGoal(AutonomousBotEntity bot, double speed) {
            this.bot = bot;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (bot.level().isClientSide) {
                return false;
            }
            targetItem = findItem();
            return targetItem != null;
        }

        @Override
        public boolean canContinueToUse() {
            return targetItem != null && targetItem.isAlive();
        }

        @Override
        public void start() {
            if (targetItem != null) {
                bot.getNavigation().moveTo(targetItem, speed);
            }
        }

        @Override
        public void tick() {
            if (targetItem == null || !targetItem.isAlive()) {
                return;
            }
            double distance = bot.distanceToSqr(targetItem);
            if (distance > 2.0D) {
                bot.getNavigation().moveTo(targetItem, speed);
                return;
            }
            ItemStack stack = targetItem.getItem();
            if (!stack.isEmpty()) {
                ItemStack remaining = bot.addToInventory(stack.copy());
                if (remaining.isEmpty()) {
                    targetItem.discard();
                } else {
                    targetItem.setItem(remaining);
                }
            }
            targetItem = null;
        }

        @Override
        public void stop() {
            targetItem = null;
        }

        private ItemEntity findItem() {
            List<ItemEntity> items = bot.level().getEntitiesOfClass(
                ItemEntity.class,
                bot.getBoundingBox().inflate(6.0D),
                entity -> entity.isAlive() && !entity.getItem().isEmpty()
            );
            if (items.isEmpty()) {
                return null;
            }
            items.sort((left, right) -> Double.compare(bot.distanceToSqr(left), bot.distanceToSqr(right)));
            return items.get(0);
        }
    }

    private static final class BuildTemplateGoal extends Goal {
        private final AutonomousBotEntity bot;
        private final double speed;
        private BlockPos origin;
        private int templateIndex;
        private BlockPos activeTarget;
        private int buildCooldown;

        private BuildTemplateGoal(AutonomousBotEntity bot, double speed) {
            this.bot = bot;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (bot.level().isClientSide) {
                return false;
            }
            if (buildCooldown > 0) {
                buildCooldown--;
                return false;
            }
            origin = bot.blockPosition().offset(2, 0, 2);
            templateIndex = 0;
            activeTarget = null;
            return !BUILD_TEMPLATE.isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return templateIndex < BUILD_TEMPLATE.size();
        }

        @Override
        public void start() {
            selectNextTarget();
        }

        @Override
        public void tick() {
            if (activeTarget == null) {
                return;
            }
            BlockState current = bot.level().getBlockState(activeTarget);
            if (!current.isAir()) {
                templateIndex++;
                selectNextTarget();
                return;
            }
            bot.getNavigation().moveTo(
                activeTarget.getX() + 0.5D,
                activeTarget.getY(),
                activeTarget.getZ() + 0.5D,
                speed
            );
            double distance = bot.distanceToSqr(Vec3.atCenterOf(activeTarget));
            if (distance <= 3.0D) {
                BlockState targetState = BUILD_TEMPLATE.get(templateIndex).state();
                bot.level().setBlockAndUpdate(activeTarget, targetState);
                templateIndex++;
                selectNextTarget();
            }
        }

        @Override
        public void stop() {
            buildCooldown = 200;
            activeTarget = null;
        }

        private void selectNextTarget() {
            if (templateIndex >= BUILD_TEMPLATE.size()) {
                activeTarget = null;
                return;
            }
            TemplateBlock templateBlock = BUILD_TEMPLATE.get(templateIndex);
            BlockPos offsetPos = templateBlock.offset();
            activeTarget = origin.offset(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ());
        }
    }
}
