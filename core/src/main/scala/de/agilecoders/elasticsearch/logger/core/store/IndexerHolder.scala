package de.agilecoders.elasticsearch.logger.core.store

import de.agilecoders.elasticsearch.logger.core.Lifecycle
import de.agilecoders.elasticsearch.logger.core.conf.Configuration
import org.elasticsearch.index.query.QueryBuilders._
import scalastic.elasticsearch.{ClientIndexer, Indexer}

/**
 * The `IndexHolder` holds the elasticsearch indexer instance and manages his
 * lifecycle.
 */
case class IndexerHolder(configuration: Configuration) extends Lifecycle[Indexer] {
    private[this] lazy val mapping: String = newMapping().load()
    private[this] lazy val instance: Indexer = newIndexer(configuration)

    /**
     * starts the `Indexer` by accessing lazy variable `instance`
     */
    override def start(): Indexer = instance

    /**
     * stops the `Indexer` instance and its transport client
     */
    override def stop() = instance.stop()

    /**
     * @return number of elements that are stored in elasticsearch
     */
    protected def count(instance: Indexer): Long = try {
        instance.count(Seq(configuration.indexName),
                       Seq(configuration.typeName),
                       termQuery("_type", configuration.typeName)).getCount
    } catch {
        case _: Throwable => 0 // TODO miha: add exception handling (#???)
    }

    /**
     * initializes the elasticsearch mapping
     */
    protected def initializeMapping(instance: Indexer) {
        if (configuration.initializeMapping) {
            try {
                instance.putMapping(configuration.indexName, configuration.typeName, mapping)
            } catch {
                case e: Throwable => // TODO miha: add exception handling (#???)
            }
        }
    }

    /**
     * creates the index in elasticsearch
     */
    protected def createIndex(instance: Indexer) {
        try {
            instance.createIndex(configuration.indexName)
        } catch {
            case e: Throwable => // TODO miha: add exception handling (#???)
        }
    }

    /**
     * creates a new transport holder instance.
     *
     * @param configuration The main configuration
     */
    protected def newTransportClient(configuration: Configuration): TransportHolder = TransportHolder(configuration)

    /**
     * @return creates and return a new mapping instance
     */
    protected def newMapping(): Mapping = FileBasedMapping()

    /**
     * creates a new `Indexer` instance and initializes them by creating his index and mapping.
     * A newly created `Indexer` instance is already connected.
     *
     * @return new `Indexer` instance
     */
    protected def newIndexer(configuration: Configuration): Indexer = {
        val indexer = new ClientIndexer(TransportHolder(configuration).start())

        indexer.waitForNodes()
        indexer.waitTillActive()

        if (count(indexer) <= 0) {
            createIndex(indexer)
            initializeMapping(indexer)
        }

        indexer
    }
}

