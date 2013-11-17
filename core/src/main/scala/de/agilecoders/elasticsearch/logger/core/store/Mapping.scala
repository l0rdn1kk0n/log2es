package de.agilecoders.elasticsearch.logger.core.store

import scala.io.Source

trait Mapping {
    def load():String
}

case class FileBasedMapping(fileName: String = "/mapping.json") extends Mapping {
    private[this] lazy val content: String = Source.fromURL(getClass.getResource(fileName)).getLines().mkString("\n")

    def load(): String = content
}