package pl.edu.agh.torch.algorithm

import pl.edu.agh.torch.config.TorchConfig
import pl.edu.agh.torch.model.{Exit, Fire, Person}
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model.{CellContents, CellState, Empty, WorldBuilder}
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}

import scala.util.Random

object TorchWorldCreator extends WorldCreator[TorchConfig] {

  private val random = new Random(System.nanoTime())

  def ConvertStringToType(value: String, types: List[CellContents], config: TorchConfig): Option[CellContents] = {
    types.foreach(typeValue => {
      if (value != null && typeValue.getClass.getSimpleName.toLowerCase.contains(value.toLowerCase)) {
        if (typeValue.getClass.getName.toLowerCase == "person") {
          Option(Person(random.nextInt(config.personMaxSpeed) + 1))
        } else {
          Option(typeValue)
        }
      }
    })
    None
  }

  override def prepareWorld(initialPositions: Array[Array[String]])(implicit config: TorchConfig): WorldBuilder = {
    val worldBuilder = GridWorldBuilder().withGridConnections()
    if (initialPositions(0).length == 0) {
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
        if (random.nextDouble() < config.spawnChance)
      } {
        val contents: Option[CellContents] = random.nextInt(3) match {
          case 0 if (random.nextDouble() < config.personSpawnChance) =>
            val speed = random.nextInt(config.personMaxSpeed) + 1
            Some(Person(speed))
          case 1 if (random.nextDouble() < config.exitSpawnChance) =>
            Some(Exit)
          case 2 if (random.nextDouble() < config.fireSpawnChance) =>
            Some(Fire)
          case _ =>
            None
        }
        contents.foreach(c => worldBuilder(GridCellId(x, y)) = CellState(c))
      }
    } else {
      for {
        x <- initialPositions(0).indices
        y <- initialPositions.indices
      } {
        if (initialPositions(y)(x) != null) {
          val actualValue = initialPositions(y)(x).toLowerCase
          actualValue match {
            case "person" =>
              val speed = random.nextInt(config.personMaxSpeed) + 1
              worldBuilder(GridCellId(x, y)) = CellState(Person(speed))
            case "exit" =>
              worldBuilder(GridCellId(x, y)) = CellState(Exit)
            case "fire" =>
              worldBuilder(GridCellId(x, y)) = CellState(Fire)
            case _ =>
          }
        }
      }
    }
    worldBuilder
  }

}
