package com.twitter.finagle.irc

import com.twitter.finagle.Service
import com.twitter.finagle.transport.{Transport => FTransport}
import com.twitter.concurrent.{Offer, Broker}
import com.twitter.util.{Closable, Future, NonFatal, Promise, Time}
import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffer

case class IrcHandle(
  messages: Offer[Message],
  onClose: Future[Unit],
  close: () => Future[Unit]
)

sealed trait Dispatcher extends Closable {
  val trans: FTransport[Message, Message]

  private[this] val inbound = new Broker[Message]
  private[this] val onClose = new Promise[Unit]

  protected val handle = IrcHandle(inbound.recv, onClose, close)

  private[this] def loopRead(): Future[Unit] =
    trans.read() flatMap inbound.! before loopRead()

  private[this] def loopWrite(out: Offer[Message]): Future[Unit] =
    out.sync() flatMap trans.write before loopWrite(out)

  protected def loop(out: Offer[Message]) {
    loopWrite(out) onFailure { _ => close() }
    loopRead() onFailure { _ => close() }
  }

  override def close(deadline: Time): Future[Unit] = {
    onClose.setDone()
    trans.close(deadline)
  }
}

private[finagle] class ClientDispatcher(
  val trans: FTransport[Message, Message]
) extends Service[Offer[Message], IrcHandle] with Dispatcher {
  def apply(out: Offer[Message]): Future[IrcHandle] = {
    loop(out)
    Future.value(handle)
  }
}

private[finagle] class ServerDispatcher(
  val trans: FTransport[Message, Message],
  service: Service[IrcHandle, Offer[Message]]
) extends Dispatcher with Closable {
  service(handle) onSuccess loop
}
