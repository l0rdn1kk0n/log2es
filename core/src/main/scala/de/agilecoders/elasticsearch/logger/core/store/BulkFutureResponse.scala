package de.agilecoders.elasticsearch.logger.core.store

import akka.util.Timeout
import java.util.concurrent.TimeUnit
import org.elasticsearch.action.ListenableActionFuture
import org.elasticsearch.action.bulk.BulkResponse

/**
 * a special FutureResponse that wraps a ListenableActionFuture.
 */
protected[store] abstract class BulkFutureResponse(response: ListenableActionFuture[BulkResponse]) extends FutureResponse {

    override def await(timeout: Timeout) = {
        if (!response.isDone && !response.isCancelled) {
            try {
                get(timeout)
            } catch {
                case e: Throwable => // TODO: handle this error
            }
        }
    }

    override def get(timeout: Timeout) = response.get(timeout.duration.toSeconds, TimeUnit.SECONDS)
}
