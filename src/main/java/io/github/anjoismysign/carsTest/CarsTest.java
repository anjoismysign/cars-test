package io.github.anjoismysign.carsTest;

import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;

public final class CarsTest extends JavaPlugin implements Listener {

    private static final double FORWARD_SPEED = 150;
    private static final double MAX_SPEED = 1_000;

    /**
     * Car drive update event.
     * The Car's speed and direction is calculated.
     *
     * @param event {@link VehicleUpdateEvent}
     */
    @EventHandler
    public void onVehicleUpdate(VehicleUpdateEvent event) {
        if (!(event.getVehicle() instanceof Minecart minecart)) {
            return;
        }

        @Nullable Entity passenger = minecart.getPassengers().isEmpty() ? null : minecart.getPassengers().getFirst();

        if (passenger == null){
            return;
        }
        if (passenger.getType() != EntityType.PLAYER){
            return;
        }

        Player player = (Player) passenger;

        Vector vehicleVelocity = minecart.getVelocity();
        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();

        Input input = player.getCurrentInput();
        if (!input.isForward()){
            return;
        }

        Vector newVelocity = direction.clone().multiply(FORWARD_SPEED / 100.0);
        newVelocity.setY(vehicleVelocity.getY());
        minecart.setVelocity(newVelocity);
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event){
        Vehicle vehicle = event.getVehicle();
        if (vehicle.getType() != EntityType.MINECART){
            return;
        }
        Minecart minecart = (Minecart) vehicle;
        minecart.setMaxSpeed(MAX_SPEED);
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

}