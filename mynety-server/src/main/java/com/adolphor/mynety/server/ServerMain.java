package com.adolphor.mynety.server;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.constants.LanStrategy;
import com.adolphor.mynety.server.config.Config;
import com.adolphor.mynety.server.config.ConfigLoader;
import com.adolphor.mynety.server.lan.LanOutBoundInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.server.config.Config.PROXY_STRATEGY;

/**
 * entrance of server
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class ServerMain {

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();

    new Thread(() -> {
      EventLoopGroup bossGroup = null;
      EventLoopGroup workerGroup = null;
      try {
        ServerBootstrap serverBoot = new ServerBootstrap();
        bossGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        workerGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        serverBoot.group(bossGroup, workerGroup)
            .channel(Constants.serverChannelClass)
            .handler(new LoggingHandler(LOG_LEVEL))
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(InBoundInitializer.INSTANCE);
        ChannelFuture future = serverBoot.bind(Config.PROXY_PORT).sync();
        future.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("ss服务端启动出错：：", e);
      } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
      }
    }, "socks-proxy-thread").start();

    if (LanStrategy.CLOSE != PROXY_STRATEGY) {
      new Thread(() -> {
        EventLoopGroup bossGroup = null;
        EventLoopGroup workerGroup = null;
        try {
          ServerBootstrap serverBoot = new ServerBootstrap();
          bossGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
          workerGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
          serverBoot.group(bossGroup, workerGroup)
              .channel(Constants.serverChannelClass)
              .handler(new LoggingHandler(LOG_LEVEL))
              .childOption(ChannelOption.TCP_NODELAY, true)
              .childOption(ChannelOption.SO_KEEPALIVE, true)
              .childHandler(LanOutBoundInitializer.INSTANCE);
          ChannelFuture future = serverBoot.bind(Config.LAN_SERVER_PORT).sync();
          future.channel().closeFuture().sync();
        } catch (Exception e) {
          logger.error("LAN服务端启动出错：：", e);
        } finally {
          bossGroup.shutdownGracefully();
          workerGroup.shutdownGracefully();
        }
      }, "socks-lan-thread").start();
    }

  }
}
