package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * descrption: 通过Netty处理Websocket的消息
 * authohr: wangji
 * date: 2017-10-13 16:33
 */
@Slf4j
public class NettyWebSocketServer {
    public static final Integer WEBSOCKET_DEFAULT_PORT = 9090;

    private NettyWebSocketServer() {
    }

    public static void doWork() {
        new NettyWebSocketServer().run(WEBSOCKET_DEFAULT_PORT);
    }

    public static void doWork(Integer websocket_port) {
        new NettyWebSocketServer().run(websocket_port);
    }

    public void run(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024) //连接数
                    .option(ChannelOption.TCP_NODELAY, true)  //不延迟，消息立即发送 ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);  //超时时间
                    .childOption(ChannelOption.SO_KEEPALIVE, true) //长连接
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast("http-codec", new HttpServerCodec()); // Http消息编码解码
                            pipeline.addLast("aggregator", new HttpObjectAggregator(65536)); // Http消息组装
                            pipeline.addLast("http-chunked", new ChunkedWriteHandler()); // WebSocket通信支持
                            pipeline.addLast("websocket-deal",new WebSocketServerHandler());
                        }
                    });
            ChannelFuture channelFuture = b.bind(port).sync();
            if (channelFuture.isSuccess()) {
                log.info("WebSocket 已经启动，端口：" + port + ".");
            }
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("error", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
