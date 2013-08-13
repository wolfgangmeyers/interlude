package com.blacklander.messagebus
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.Channels
import java.net.InetSocketAddress
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.channel.group.DefaultChannelGroup

class MessageServer(host: String, port: Int, defaultGame: Game) {
    
    val bossExecutor = Executors.newCachedThreadPool()
    val workerExecutor = Executors.newCachedThreadPool()
    val clientChannels = new DefaultChannelGroup("clientChannels")
    
    val channelFactory = new NioServerSocketChannelFactory(
        bossExecutor,
        workerExecutor
    )
    val bootstrap = new ServerBootstrap(channelFactory)
    class MessagePipelineFactory extends ChannelPipelineFactory {
        override def getPipeline(): ChannelPipeline = {
            return Channels.pipeline(
                new MessageFrameDecoder(),
                new MessageChannelHandler(defaultGame, clientChannels)
            )
        }
    }
    bootstrap.setPipelineFactory(new MessagePipelineFactory())
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)
    val serverChannel = bootstrap.bind(new InetSocketAddress(host, port))
    
    def shutdown() {
        serverChannel.close().awaitUninterruptibly()
        clientChannels.close().awaitUninterruptibly()
        bossExecutor.shutdown()
        workerExecutor.shutdown()
    }
}