package de.agilecoders.logback.elasticsearch.actor

import akka.actor.ActorRef

// Our test reaper.  Sends the snooper a message when all
// the souls have been reaped
class TestReaper(snooper: ActorRef) extends Reaper {
    def allSoulsReaped(): Unit = snooper ! "Dead"
}
