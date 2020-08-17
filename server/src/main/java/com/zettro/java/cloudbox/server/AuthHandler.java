package com.zettro.java.cloudbox.server;

import com.zettro.java.cloudbox.common.AuthAnswer;
import com.zettro.java.cloudbox.common.AuthRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.file.Files;
import java.nio.file.Paths;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthRequest) {
            AuthRequest authRequest = (AuthRequest) msg;
            String username = authRequest.getUsername();
            String password = authRequest.getPassword();
            if(username.equals("")) {
                System.out.println("Invalid credentials. Try again.");
                ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.FAILED));
                return;
            }
            // сначала проверяем по базе
            if (!SqlClient.isRegisteredUser(username, password)) {
                System.out.println("Invalid credentials. Try again.");
                ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.FAILED));
                return;
            }

            // проверяем наличие локального каталога
            String userPath = Server.serverStoragePath + username;
            // пока заглушка для аутентификации - есть каталог на сервере - значит success!)
            if (Files.exists(Paths.get(userPath)) && Files.isDirectory(Paths.get(userPath))) {
                System.out.println("Authentication is passed!");
                ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.PASSED));
                // самоудаляемся и добавляем нормальный обработчик входного потока
                ctx.pipeline().addLast(new MainHandler(username));
                ctx.pipeline().remove(this);
            } else {
                System.out.println("Invalid credentials. Try again.");
                ctx.writeAndFlush(new AuthAnswer(AuthAnswer.AuthResult.FAILED));
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
