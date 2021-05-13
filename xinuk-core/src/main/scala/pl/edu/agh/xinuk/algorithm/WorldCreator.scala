package pl.edu.agh.xinuk.algorithm

import pl.edu.agh.xinuk.config.XinukConfig
import pl.edu.agh.xinuk.model.WorldBuilder

trait WorldCreator[Config <: XinukConfig] {
  def prepareWorld(initialPositions: Array[Array[String]])(implicit config: Config): WorldBuilder
}
