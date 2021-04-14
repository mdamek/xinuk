package pl.edu.agh.game.algorithm

import pl.edu.agh.game.config.GameConfig
import pl.edu.agh.game.model.Life
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model.{CellContents, CellState, WorldBuilder}
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}

import net.liftweb.json._

object GameWorldCreator extends WorldCreator[GameConfig] {
  override def prepareWorld()(implicit config: GameConfig): WorldBuilder = {
    val worldBuilder = GridWorldBuilder().withGridConnections()
    val size = (config.worldWidth, config.worldHeight)
    var shape: String = ""

    size match {
      case (64, 64) => shape = "square"
      case (32, 128) => shape = "vertical"
      case (128, 32) => shape = "horizontal"
      case _ => shape = "square"
    }

    if (config.loadFromOutside) {
      implicit val formats: DefaultFormats.type = DefaultFormats
      val res = requests.get(config.initialPositionPath + "/" + shape)
      val jValue = parse(res.text())
      val outsidePositions = jValue.extract[Array[Array[Int]]]
      requests.get(config.cleanPositionsStatePath)
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
      } {
        val contents: Option[CellContents] = if (outsidePositions(x)(y) > 0) {
          Some(Life)
        } else {
          None
        }
        contents.foreach(c => worldBuilder(GridCellId(x, y)) = CellState(c))
      }
    } else {
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
      } {
        val contents: Option[CellContents] = if (config.random.nextDouble() < config.lifeSpawnChance) {
          Some(Life)
        }
        else {
          None
        }
        contents.foreach(c => worldBuilder(GridCellId(x, y)) = CellState(c))
      }
    }
    worldBuilder
  }
}
