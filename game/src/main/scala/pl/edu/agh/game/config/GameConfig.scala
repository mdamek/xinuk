package pl.edu.agh.game.config

import java.security.SecureRandom

import pl.edu.agh.xinuk.config.{GuiType, XinukConfig}
import pl.edu.agh.xinuk.model.{Signal, WorldType}

import scala.util.Random


final case class GameConfig(worldType: WorldType,
                            worldWidth: Int,
                            worldHeight: Int,
                            iterationsNumber: Long,
                            iterationFinishedLogFrequency: Long,
                            skipEmptyLogs: Boolean,

                            signalSuppressionFactor: Double,
                            signalAttenuationFactor: Double,
                            signalDisabled: Boolean,

                            workersX: Int,
                            workersY: Int,
                            isSupervisor: Boolean,
                            shardingMod: Int,

                            guiCellSize: Int,
                            guiType: GuiType,
                            guiStartIteration: Long,
                            guiUpdateFrequency: Long,
                            ledPanelPort: String,

                            lifeInitialSignal: Signal,
                            loadFromOutside: Boolean,
                            initialPositionPath: String,
                            cleanPositionsStatePath: List[String],
                            lifeSpawnChance: Double

                           ) extends XinukConfig {
  val random: Random = new SecureRandom
}