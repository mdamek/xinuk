package pl.edu.agh.fortwist.algorithm

import pl.edu.agh.fortwist.config.FortwistConfig
import pl.edu.agh.fortwist.model.{Foraminifera, Seabed}
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model.{CellState, WorldBuilder}
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}

import scala.util.Random

object FortwistWorldCreator extends WorldCreator[FortwistConfig] {

  private val random = new Random(System.nanoTime())

  def ConvertStringToType(value: String, types: List[Foraminifera]): Seq[Foraminifera] = {
    types.foreach(typeValue => {
      if (value != null && typeValue.getClass.getSimpleName.toLowerCase.contains(value.toLowerCase)) {
        Seq(typeValue)
      }
    })
    Seq()
  }

  override def prepareWorld(initialPositions: Array[Array[String]])(implicit config: FortwistConfig): WorldBuilder = {
    val worldBuilder = GridWorldBuilder().withGridConnections()
    val availableTypes: List[Foraminifera] = List(Foraminifera())
    if (initialPositions.isEmpty) {
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
      } {
        val foraminiferas: Seq[Foraminifera] = if (random.nextDouble() < config.foraminiferaSpawnChance) {
          Seq(Foraminifera())
        } else {
          Seq()
        }
        worldBuilder(GridCellId(x, y)) = CellState(Seabed(foraminiferas, config.algaeStartEnergy))
      }
    } else {
      for {
        x <- initialPositions.indices
        y <- initialPositions(0).indices
      } {
        val foraminiferas: Seq[Foraminifera] = ConvertStringToType(initialPositions(x)(y), availableTypes)
        worldBuilder(GridCellId(x, y)) = CellState(Seabed(foraminiferas, config.algaeStartEnergy))
      }
    }


    worldBuilder
  }
}
