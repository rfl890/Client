package mathax.legacy.client.systems.modules.render;

import mathax.legacy.client.MatHaxLegacy;
import mathax.legacy.client.events.entity.EntityAddedEvent;
import mathax.legacy.client.events.render.Render2DEvent;
import mathax.legacy.client.events.render.Render3DEvent;
import mathax.legacy.client.events.world.TickEvent;
import mathax.legacy.client.eventbus.EventHandler;
import mathax.legacy.client.renderer.Renderer2D;
import mathax.legacy.client.renderer.ShapeMode;
import mathax.legacy.client.renderer.text.TextRenderer;
import mathax.legacy.client.systems.modules.Module;
import mathax.legacy.client.utils.misc.Vec3;
import mathax.legacy.client.utils.player.PlayerUtils;
import mathax.legacy.client.utils.render.NametagUtils;
import mathax.legacy.client.utils.render.color.Color;
import mathax.legacy.client.utils.render.color.SettingColor;
import mathax.legacy.client.utils.world.Dimension;
import mathax.legacy.client.systems.modules.Categories;
import mathax.legacy.client.settings.*;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogoutSpots extends Module {
    private final List<Entry> players = new ArrayList<>();

    private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();

    private int timer;
    private Dimension lastDimension;

    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> fullHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("full-height")
        .description("Displays the height as the player's full height.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .description("The name color.")
        .defaultValue(new SettingColor(MatHaxLegacy.INSTANCE.MATHAX_COLOR.r, MatHaxLegacy.INSTANCE.MATHAX_COLOR.g, MatHaxLegacy.INSTANCE.MATHAX_COLOR.b))
        .build()
    );

    private final Setting<SettingColor> nameBackgroundColor = sgRender.add(new ColorSetting.Builder()
        .name("name-background-color")
        .description("The name background color.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );

    public LogoutSpots() {
        super(Categories.Render, Items.LIME_STAINED_GLASS, "logout-spots", "Displays a box where another player has logged out at.");
        lineColor.changed();
    }

    @Override
    public void onActivate() {
        lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
        updateLastPlayers();

        timer = 10;
        lastDimension = PlayerUtils.getDimension();
    }

    @Override
    public void onDeactivate() {
        players.clear();
        lastPlayerList.clear();
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) lastPlayers.add((PlayerEntity) entity);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof PlayerEntity) {
            int toRemove = -1;

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid.equals(event.entity.getUuid())) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove != -1) {
                players.remove(toRemove);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getNetworkHandler().getPlayerList().size() != lastPlayerList.size()) {
            for (PlayerListEntry entry : lastPlayerList) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile()))) continue;

                for (PlayerEntity player : lastPlayers) {
                    if (player.getUuid().equals(entry.getProfile().getId())) {
                        add(new Entry(player));
                    }
                }
            }

            lastPlayerList.clear();
            lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
            updateLastPlayers();
        }

        if (timer <= 0) {
            updateLastPlayers();
            timer = 10;
        } else {
            timer--;
        }

        Dimension dimension = PlayerUtils.getDimension();
        if (dimension != lastDimension) players.clear();
        lastDimension = dimension;
    }

    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        for (Entry player : players) player.render3D(event);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entry player : players) player.render2D();
    }

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    private static final Vec3 pos = new Vec3();

    private class Entry {
        public final double x, y, z;
        public final double xWidth, zWidth, halfWidth, height;

        public final UUID uuid;
        public final String name;
        public final int health, maxHealth;
        public final String healthText;

        public Entry(PlayerEntity entity) {
            halfWidth = entity.getWidth() / 2;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;

            xWidth = entity.getBoundingBox().getXLength();
            zWidth = entity.getBoundingBox().getZLength();
            height = entity.getBoundingBox().getYLength();

            uuid = entity.getUuid();
            name = entity.getEntityName();
            health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
            maxHealth = Math.round(entity.getMaxHealth() + entity.getAbsorptionAmount());

            healthText = " " + health;
        }

        public void render3D(Render3DEvent event) {
            if (fullHeight.get()) event.renderer.box(x, y, z, x + xWidth, y + height, z + zWidth, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            else event.renderer.sideHorizontal(x, y, z, x + xWidth, z, sideColor.get(), lineColor.get(), shapeMode.get());
        }

        public void render2D() {
            if (PlayerUtils.distanceToCamera(x, y, z) > mc.options.viewDistance * 16) return;

            TextRenderer text = TextRenderer.get();
            double scale = LogoutSpots.this.scale.get();
            pos.set(x + halfWidth, y + height + 0.5, z + halfWidth);

            if (!NametagUtils.to2D(pos, scale)) return;

            NametagUtils.begin(pos);

            // Compute health things
            double healthPercentage = (double) health / maxHealth;

            // Get health color
            Color healthColor;
            if (healthPercentage <= 0.333) healthColor = RED;
            else if (healthPercentage <= 0.666) healthColor = ORANGE;
            else healthColor = GREEN;

            // Render background
            double i = text.getWidth(name) / 2.0 + text.getWidth(healthText) / 2.0;
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-i, 0, i * 2, text.getHeight(), nameBackgroundColor.get());
            Renderer2D.COLOR.render(null);

            // Render name and health texts
            text.beginBig();
            double hX = text.render(name, -i, 0, nameColor.get());
            text.render(healthText, hX, 0, healthColor);
            text.end();

            NametagUtils.end();
        }
    }
}
