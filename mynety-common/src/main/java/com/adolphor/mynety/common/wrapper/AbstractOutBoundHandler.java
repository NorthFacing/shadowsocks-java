package com.adolphor.mynety.common.wrapper;

import com.adolphor.mynety.common.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CONNECTED_TIMESTAMP;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static org.apache.commons.lang3.ClassUtils.getName;

/**
 * 带有缓存的远程连接处理器：
 * 1.覆写增加了LOG日志和channel关闭方法
 * 2.消费缓存的请求信息
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractOutBoundHandler<I> extends AbstractSimpleHandler<I> {

  /**
   * 将List改为单条消息
   * 1. HTTP 消息因为使用了聚合，所以只有一条
   * 2. ss-local因为使用了socks5协议，所以也只有一条
   * 3. ss-server可能会在目的地址建立成功之前收到多条ByteBuf，此时需要将后续的ByteBuf直接追加到之前的那一条缓存信息中
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    AtomicReference tempMsgRef = inRelayChannel.attr(ATTR_REQUEST_TEMP_MSG).get();
    if (tempMsgRef.get() == null) {
      return;
    }
    Object tempMsg = tempMsgRef.get();
    if (tempMsg instanceof ByteBuf) {
      ByteBuf byteBuf = (ByteBuf) tempMsg;
      if (byteBuf.readableBytes() == 0) {
        return;
      }
    }
    ChannelUtils.loggerHandlers(ctx.channel(), tempMsg);
    ctx.channel().writeAndFlush(tempMsg);
  }

  /**
   * 本方法没增加任何逻辑，只为了说明架构公共代码逻辑，可以删除没有任何影响。
   * <p>
   * 本方法的作用，将目的地返回的消息发送给客户端，inRelayChannel需要从outRelayChannel的绑定关系中获取，关键代码如下：
   * <p>
   * Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
   * inRelayChannel.writeAndFlush(msg);
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    if (inRelayChannel.isActive()) {
      long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
      logger.info("[ {} ] {} inRelayChannel will be closed, connection time: {}ms", inRelayChannel, getName(this), connTime);
      ChannelUtils.closeOnFlush(inRelayChannel);
    }

    super.channelClose(ctx);
  }

}
