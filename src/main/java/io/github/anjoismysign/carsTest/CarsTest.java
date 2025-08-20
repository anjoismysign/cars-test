package io.github.anjoismysign.carsTest;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CarsTest extends JavaPlugin implements Listener {

    private static final float VEHICLE_HEIGHT = 1.2f;
    private static final float VEHICLE_WIDTH = 0.6f;
    private static final double VEHICLE_Y_MODIFIER = -0.25;

    private final Map<UUID, FlyingVehicle> vehicles = new HashMap<>();

    private record FlyingVehicle(Display seat) {

        public void remove() {
            seat.remove();
        }

        public void destroy(){
            World world = seat().getWorld();
            Location location = seat().getLocation();
            remove();
            world.spawnParticle(Particle.EXPLOSION, location, 1);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
        }

        public static FlyingVehicle of(Player player) {
            World world = player.getWorld();
            Location location = player.getLocation();
            ItemDisplay seat = (ItemDisplay) world.spawnEntity(location, EntityType.ITEM_DISPLAY);
            seat.setItemStack(new ItemStack(Material.PLAYER_HEAD));
            seat.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            seat.setRotation(player.getYaw(), 0);
            seat.setTeleportDuration(1);
            seat.addPassenger(player);
            return new FlyingVehicle(seat);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        FlyingVehicle vehicle = FlyingVehicle.of(player);
        vehicles.put(vehicle.seat().getUniqueId(), vehicle);
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Iterator<Map.Entry<UUID, FlyingVehicle>> it = vehicles.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, FlyingVehicle> entry = it.next();
                FlyingVehicle flyingVehicle = entry.getValue();
                Display seat = flyingVehicle.seat;
                List<Entity> passengers = seat.getPassengers();
                if (passengers.isEmpty()) {
                    return;
                }
                Entity entity = passengers.getFirst();
                if (entity.getType() != EntityType.PLAYER) {
                    return;
                }
                Player player = (Player) entity;
                Input input = player.getCurrentInput();
                if (!input.isForward()) {
                    return;
                }

                World world = player.getWorld();
                BoundingBox boxBox = flyingVehicle.seat().getBoundingBox();
                Location boxLocation = boxBox.getCenter().setY(boxBox.getMinY()).toLocation(world).add(0,VEHICLE_Y_MODIFIER,0);

                Location playerLocation = player.getLocation();
                float yaw = playerLocation.getYaw();
                Vector direction = playerLocation.getDirection();
                direction.normalize();
                Vector at = direction.clone().multiply(200 / 100.0);
                Location location = flyingVehicle.seat().getLocation();
                location.add(at);
                Block block = boxLocation.getBlock();
                Material type = block.getType();
                if (
                        type.isSolid() ||
                        type == Material.WATER ||
                        type == Material.LAVA ||
                        !world.getWorldBorder().isInside(location)
                ){
                    it.remove();
                    flyingVehicle.destroy();
                    return;
                }
                location.setYaw(yaw);
                flyingVehicle.seat().teleport(location, TeleportFlag.EntityState.RETAIN_PASSENGERS);
            }
        }, 0, 1);
    }


    @Override
    public void onDisable() {
        vehicles.values().forEach(FlyingVehicle::remove);
    }

}