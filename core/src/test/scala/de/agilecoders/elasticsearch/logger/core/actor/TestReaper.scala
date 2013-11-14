package de.agilecoders.elasticsearch.logger.logback.actor

import akka.actor.ActorRef
import de.agilecoders.elasticsearch.logger.core.actor.Reaper

// Our test reaper.  Sends the snooper a message when all
// the souls have been reaped
class TestReaper(snooper: ActorRef) extends Reaper {
    def allSoulsReaped(): Unit = snooper ! "Dead"
}
