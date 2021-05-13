package pl.edu.agh.xinuk.simulation

import com.typesafe.config.Config
import net.liftweb.json.{DefaultFormats, parse}

object OutsideInitialPositionProvider {
  def Provide(config: Config): Array[Array[String]] = {
    val supervisor: String = config.getString("supervisor")
    val port: String = config.getString("ledPanelPort")
    val hosts: List[String] = config.getString("shard-allocation-order").split(',').map(_.trim).toList
    implicit val formats: DefaultFormats.type = DefaultFormats
    val response = requests.get(s"http://$supervisor:$port/pixelsGlobal")
    val jValue = parse(response.text())
    val outsidePositions = jValue.extract[Array[Array[String]]]
    hosts.foreach(host => {
      requests.get(s"http://$host:$port/clearPixelsState")
    })
    outsidePositions
  }
}
