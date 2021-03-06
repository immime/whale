package org.octopus.iot.netty;

import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
@IocBean
public class IotNettyTcpServerInitializer extends ChannelInitializer<AbstractChannel> {

    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();

    @Inject
    IotNettyTcpHandler iotNettyTcpHandler;

    private final SslContext sslCtx;
    
    public IotNettyTcpServerInitializer() {
		sslCtx = null;
	}

    public IotNettyTcpServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    public void initChannel(AbstractChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }

        // Add the text line codec combination first,
        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        // the encoder and decoder are static as these are sharable
        pipeline.addLast(DECODER);
        pipeline.addLast(ENCODER);

        // and then business logic.
        pipeline.addLast(iotNettyTcpHandler);
    }
}