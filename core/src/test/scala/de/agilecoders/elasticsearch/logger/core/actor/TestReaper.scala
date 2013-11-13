package de.agilecoders.elasticsearch.logger.core.actor

import akka.actor.ActorRef
import de.agilecoders.logback.elasticsearch.actor.Reaper

// Our test reaper.  Sends the snooper a message when all
// the souls have been reaped
class TestReaper(snooper: ActorRef) extends Reaper {
    def allSoulsReaped(): Unit = snooper ! "Dead"
}
