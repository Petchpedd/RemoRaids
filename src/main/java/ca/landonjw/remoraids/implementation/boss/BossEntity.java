package ca.landonjw.remoraids.implementation.boss;

import ca.landonjw.remoraids.RemoRaids;
import ca.landonjw.remoraids.api.battles.IBossBattle;
import ca.landonjw.remoraids.api.boss.IBoss;
import ca.landonjw.remoraids.api.boss.IBossEntity;
import ca.landonjw.remoraids.api.boss.engage.IBossEngager;
import ca.landonjw.remoraids.api.events.BossDeathEvent;
import ca.landonjw.remoraids.implementation.battles.BossBattleRegistry;
import ca.landonjw.remoraids.implementation.boss.engage.ActionBarEngager;
import ca.landonjw.remoraids.implementation.boss.engage.BossBarEngager;
import ca.landonjw.remoraids.implementation.boss.engage.OverlayEngager;
import ca.landonjw.remoraids.implementation.boss.engage.TitleEngager;
import ca.landonjw.remoraids.internal.config.GeneralConfig;
import ca.landonjw.remoraids.internal.config.MessageConfig;
import ca.landonjw.remoraids.internal.tasks.Task;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityStatue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.world.BossInfo;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class BossEntity implements IBossEntity {

    private UUID uniqueId;

    private IBoss boss;
    private EntityStatue entity;
    private EntityPixelmon battleEntity;

    private IBossEngager bossEngager;

    public BossEntity(@Nonnull IBoss boss,
                      @Nonnull EntityStatue entity,
                      @Nonnull EntityPixelmon battleEntity){
        this.uniqueId = UUID.randomUUID();
        this.boss = Objects.requireNonNull(boss, "boss must not be null");
        this.entity = entity;
        this.battleEntity = battleEntity;
        setEngager();
        vanishBattleEntity();

        BossEntityRegistry registry = BossEntityRegistry.getInstance();
        registry.register(this);
        BossBattleRegistry.getInstance().registerBossBattle(this);
    }

    private void setEngager(){
        switch(RemoRaids.getGeneralConfig().get(GeneralConfig.ENGAGE_MESSAGE_TYPE)){
            case 1:
                bossEngager = new ActionBarEngager(
                        this,
                        RemoRaids.getGeneralConfig().get(GeneralConfig.ENGAGE_RANGE),
                        RemoRaids.getMessageConfig().get(MessageConfig.RAID_ENGAGE)
                );
                break;
            case 2:
                bossEngager = new BossBarEngager(
                        this,
                        RemoRaids.getGeneralConfig().get(GeneralConfig.ENGAGE_RANGE),
                        RemoRaids.getMessageConfig().get(MessageConfig.RAID_ENGAGE),
                        BossInfo.Color.WHITE,
                        BossInfo.Overlay.PROGRESS
                );
                break;
            case 3:
                bossEngager = new OverlayEngager(
                        this,
                        RemoRaids.getGeneralConfig().get(GeneralConfig.ENGAGE_RANGE),
                        RemoRaids.getMessageConfig().get(MessageConfig.RAID_ENGAGE)
                );
                break;
            case 4:
                bossEngager = new TitleEngager(
                        this,
                        RemoRaids.getGeneralConfig().get(GeneralConfig.ENGAGE_RANGE),
                        RemoRaids.getMessageConfig().get(MessageConfig.RAID_ENGAGE),
                        SPacketTitle.Type.TITLE
                );
                break;
        }
    }

    private void vanishBattleEntity(){
        Task.builder()
                .execute((task) -> {
                    if(!entity.isDead){
                        for(EntityPlayer player : battleEntity.world.playerEntities){
                            if(player.getDistance(battleEntity) < 100){
                                SPacketDestroyEntities packet = new SPacketDestroyEntities(battleEntity.getEntityId());
                                ((EntityPlayerMP) player).connection.sendPacket(packet);
                            }
                        }
                    }
                    else{
                        task.setExpired();
                    }
                })
                .interval(10)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public IBoss getBoss() {
        return boss;
    }

    /** {@inheritDoc} */
    @Override
    public EntityStatue getEntity() {
        return entity;
    }

    /** {@inheritDoc} */
    @Override
    public EntityPixelmon getBattleEntity() {
        return battleEntity;
    }

    /** {@inheritDoc} */
    @Override
    public IBossEngager getBossEngager() {
        return bossEngager;
    }

    /** {@inheritDoc} */
    @Override
    public void despawn() {
        BossBattleRegistry battleRegistry = BossBattleRegistry.getInstance();
        Optional<IBossBattle> battle = battleRegistry.getBossBattle(this);
        battleRegistry.deregisterBossBattle(this);

        BossDeathEvent deathEvent = battle.map(iBossBattle -> new BossDeathEvent(this, iBossBattle))
                .orElseGet(() -> new BossDeathEvent(this, null));
        RemoRaids.EVENT_BUS.post(deathEvent);

        entity.setDead();
        battleEntity.setDead();

        /* This is delayed by one tick due to drops not cancelling properly due to the pixelmon entity
         * queueing drop items on death and the boss being deregistered simultaneously, causing the
         * drop listener to be incapable of cancelling the event.
         */
        Task.builder().execute(() -> BossEntityRegistry.getInstance().deregister(this))
                .delay(1)
                .iterations(1)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BossEntity that = (BossEntity) o;
        return Objects.equals(uniqueId, that.uniqueId) &&
                Objects.equals(boss, that.boss) &&
                Objects.equals(entity, that.entity) &&
                Objects.equals(battleEntity, that.battleEntity) &&
                Objects.equals(bossEngager, that.bossEngager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, boss, entity, battleEntity, bossEngager);
    }
}
