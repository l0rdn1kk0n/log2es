package de.agilecoders.logger.log2es.core.common

import java.io.{PrintStream, PrintWriter}

/**
 * special lightweight runtime exception that doesn't contain a stacktrace.
 *
 * @author miha
 */
class RuntimeExceptionWithoutStack(private val message: String,
                                   private val cause: Throwable) extends RuntimeException(message, cause, true, false) {
  private lazy val stack = new Array[StackTraceElement](0)

  /**
   * Construct.
   */
  def this() = this("", null)

  /**
   * Construct.
   */
  def this(message: String) = this(message, null)

  /**
   * Construct.
   */
  def this(cause: Throwable) = this(cause.getMessage, cause)

  /**
   * do nothing
   *
   * @param stackTrace the stack trace
   */
  override def setStackTrace(stackTrace: Array[StackTraceElement]) = {}

  /**
   * @return empty stack
   */
  override def getStackTrace = stack

  /**
   * do nothing
   *
   * @return this instance
   */
  override def fillInStackTrace() = this

  /**
   * do nothing
   *
   * @param s the writer to write stack to
   */
  override def printStackTrace(s: PrintWriter) = s.println("no stacktrace available")

  /**
   * do nothing
   *
   * @param s the writer to write stack to
   */
  override def printStackTrace(s: PrintStream) = s.println("no stacktrace available")

  /**
   * do nothing
   */
  override def printStackTrace() = printStackTrace(System.err)
}
